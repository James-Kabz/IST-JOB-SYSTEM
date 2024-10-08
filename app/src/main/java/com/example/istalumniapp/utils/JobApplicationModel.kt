package com.example.istalumniapp.utils

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class JobApplicationModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Update it to handle a list of job applications
    private val _applicationState = MutableStateFlow<List<JobApplicationData>?>(null)
    val applicationState: StateFlow<List<JobApplicationData>?> = _applicationState

    fun fetchApplicationsForUser(userId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _applicationState.value = emptyList()

                val applicationsRef = firestore.collection("job_applications")
                val querySnapshot = applicationsRef
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val applications = querySnapshot.toObjects(JobApplicationData::class.java)
                val updatedApplications = mutableListOf<JobApplicationData>()

                applications.forEach { application ->
                    try {
                        val jobSnapshot = firestore.collection("jobs")
                            .document(application.jobID)
                            .get()
                            .await()

                        if (jobSnapshot.exists()) {
                            application.title = jobSnapshot.getString("title") ?: ""
                            application.companyLogo = jobSnapshot.getString("companyLogo") ?: ""
                        }

                        updatedApplications.add(application)
                    } catch (e: Exception) {
                        Log.e("FirestoreError", "Error fetching job details", e)
                    }
                }

                _applicationState.value = updatedApplications
            } catch (e: Exception) {
                _applicationState.value = emptyList()
                Log.e("ApplicationError", "Exception occurred while fetching applications", e)
            } finally {
                onComplete()
            }
        }
    }


    var isLoading by mutableStateOf(false)
        private set

    fun saveApplication(
        jobId: String,
        userId: String,
        applicationData: JobApplicationData,
        cvUri: Uri?,
        onResult: (Boolean, String?) -> Unit
    ) {
        isLoading = true

        // Generate a unique application ID if it's not already set
        val applicationId = applicationData.applicationId.ifEmpty { UUID.randomUUID().toString() }

        val updatedApplicationData = applicationData.copy(
            jobID = jobId,
            userId = userId,
            applicationId = applicationId
        )

        // upload CV to Firebase Storage first
        if (cvUri != null) {
            val storageRef = storage.reference.child("cv_files/${UUID.randomUUID()}.pdf")   //save with a random ID

            storageRef.putFile(cvUri)
                .addOnSuccessListener {
                    // Get the download URL for the CV
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // After uploading the CV, save application data with CV URL
                        saveApplicationData(updatedApplicationData.copy(cv = downloadUrl.toString())) { success, error ->
                            isLoading = true
                            onResult(success, error)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    isLoading = true
                    onResult(false, "Failed to upload CV: ${exception.message}")
                }
        } else {
            // No CV provided, just save the application data
            saveApplicationData(updatedApplicationData) { success, error ->
                isLoading = true
                onResult(success, error) // Pass the result
            }
        }
    }


    // Helper function to save the application data in Firestore
    private fun saveApplicationData(
        applicationData: JobApplicationData,
        onResult: (Boolean, String?) -> Unit
    ) {
        isLoading = true

        firestore.collection("job_applications")
            .document(applicationData.applicationId)
            .set(applicationData)
            .addOnSuccessListener {
                isLoading = true
                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                isLoading = true
                onResult(false, "Failed to save application: ${exception.message}")
            }
    }





    fun deleteApplication(applicationId: String, onResult: (Boolean, String?) -> Unit) {
        firestore.collection("job_applications")
            .document(applicationId)
            .delete()
            .addOnSuccessListener {
                onResult(true, null)  // Deletion successful
            }
            .addOnFailureListener { exception ->
                onResult(false, "Failed to delete application: ${exception.message}")  // Handle failure
            }
    }



    // Function to update the application status (approve/reject)
    fun updateApplicationStatus(applicationId: String, isApproved: Boolean, onResult: (Boolean, String?) -> Unit) {
        val status = if (isApproved) "Approved" else "Rejected"

        firestore.collection("job_applications")
            .document(applicationId)
            .update("status", status)  // Firestore will create the field if it doesn't exist
            .addOnSuccessListener {
                onResult(true, null)  // Update was successful
            }
            .addOnFailureListener { exception ->
                onResult(false, "Failed to update application status: ${exception.message}")  // Handle failure
            }
    }

    // Function to send feedback (could be email or Firestore feedback field)
    fun sendFeedback(applicationId: String, feedback: String, onResult: (Boolean, String?) -> Unit) {
        firestore.collection("job_applications")
            .document(applicationId)
            .update("feedback", feedback)  // Firestore will create the field if it doesn't exist
            .addOnSuccessListener {
                onResult(true, null)  // Feedback sent successfully
            }
            .addOnFailureListener { exception ->
                onResult(false, "Failed to send feedback: ${exception.message}")  // Handle failure
            }
    }


    // Retrieve feedback for a specific job application by its ID
    fun retrieveFeedback(applicationId: String, userId: String, onResult: (String?) -> Unit) {
        firestore.collection("job_applications")
            .whereEqualTo("applicationId", applicationId) // Ensure correct application
            .whereEqualTo("userId", userId) // Ensure feedback belongs to the specific user
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Assume only one matching document (or take the first one)
                    val document = querySnapshot.documents.firstOrNull()
                    val feedback = document?.getString("feedback")
                    onResult(feedback) // Return the feedback if found
                } else {
                    onResult(null) // No matching document or no feedback
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Failed to retrieve feedback: ${exception.message}")
                onResult(null) // Return null on failure
            }
    }

    fun fetchAllApplicationsForAdminReview(onResult: (List<JobApplicationData>) -> Unit) {
        firestore.collection("job_applications")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val applications = querySnapshot.toObjects(JobApplicationData::class.java)

                // List to store updated applications with user details and job information
                val updatedApplications = mutableListOf<JobApplicationData>()

                applications.forEach { application ->
                    firestore.collection("users").document(application.userId).get()
                        .addOnSuccessListener { userSnapshot ->
                            val userEmail = userSnapshot.getString("email") ?: "Unknown"
                            application.email = userEmail

                            firestore.collection("jobs").document(application.jobID).get()
                                .addOnSuccessListener { jobSnapshot ->
                                    if (jobSnapshot.exists()) {
                                        val jobTitle = jobSnapshot.getString("title") ?: ""
                                        val companyLogo = jobSnapshot.getString("companyLogo") ?: ""

                                        // Update the application with job details
                                        application.title = jobTitle
                                        application.companyLogo = companyLogo
                                    }

                                    // Add the updated application to the list
                                    updatedApplications.add(application)

                                    // Once all applications are updated, return the result
                                    if (updatedApplications.size == applications.size) {
                                        onResult(updatedApplications)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("FirestoreError", "Error fetching job details", exception)

                                    // Add the application even if job details fail
                                    updatedApplications.add(application)

                                    if (updatedApplications.size == applications.size) {
                                        onResult(updatedApplications)
                                    }
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirestoreError", "Error fetching user details", exception)

                            // Add the application even if user details fail
                            updatedApplications.add(application)

                            if (updatedApplications.size == applications.size) {
                                onResult(updatedApplications)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreError", "Error fetching applications for admin", exception)
                onResult(emptyList())  // Return an empty list in case of failure
            }
    }




}

