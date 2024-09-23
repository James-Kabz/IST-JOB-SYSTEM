package com.example.istalumniapp.utils

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class JobApplicationModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Update it to handle a list of job applications
    private val _applicationState = MutableStateFlow<List<JobApplicationData>?>(null)
    val applicationState: StateFlow<List<JobApplicationData>?> = _applicationState

    fun fetchApplicationsForUser(userId: String) {
        viewModelScope.launch {
            try {
                val applicationsRef = firestore.collection("job_applications")

                applicationsRef
                    .whereEqualTo("userId", userId)  // Query by userId
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val applications = querySnapshot.toObjects(JobApplicationData::class.java)

                        // List to store applications with job details
                        val updatedApplications = mutableListOf<JobApplicationData>()

                        // For each application, fetch the corresponding job details using jobID
                        applications.forEach { application ->
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

                                    // Once all applications are updated, set the new state
                                    if (updatedApplications.size == applications.size) {
                                        _applicationState.value = updatedApplications
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("FirestoreError", "Error fetching job details", exception)
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        _applicationState.value = emptyList() // Handle failure with an empty list
                        Log.e("FirestoreError", "Error fetching applications", exception)
                    }
            } catch (e: Exception) {
                _applicationState.value = emptyList() // Catch any other exceptions
                Log.e("ApplicationError", "Exception occurred while fetching applications", e)
            }
        }
    }


//    fun fetchApplicationDetails(applicationId: String) {
//        viewModelScope.launch {
//            try {
//                val firestore = FirebaseFirestore.getInstance()
//                val applicationRef = firestore.collection("job_applications").document(applicationId)
//
//                applicationRef.get().addOnSuccessListener { documentSnapshot ->
//                    if (documentSnapshot.exists()) {
//                        val applicationData = documentSnapshot.toObject(JobApplicationData::class.java)
//                        _applicationState.value = applicationData
//                    } else {
//                        _applicationState.value = null // No application found
//                    }
//                }.addOnFailureListener { exception ->
//                    // Handle any errors that occur
//                    _applicationState.value = null
//                    Log.e("FirestoreError", "Error fetching application details", exception)
//                }
//            } catch (e: Exception) {
//                _applicationState.value = null
//                Log.e("ApplicationError", "Exception occurred while fetching application", e)
//            }
//        }
//    }

    // Function to save the job application
    fun saveApplication(
        jobId: String,
        userId: String,
        applicationData: JobApplicationData,
        cvUri: Uri?,
        onResult: (Boolean, String?) -> Unit
    ) {
        // Generate a unique application ID if it's not already set
        val applicationId = if (applicationData.applicationId.isEmpty()) UUID.randomUUID().toString() else applicationData.applicationId

        val updatedApplicationData = applicationData.copy(
            jobID = jobId,
            userId = userId,
            applicationId = applicationId  // Ensure applicationId is set
        )

        // If a CV file is provided, upload it to Firebase Storage first
        if (cvUri != null) {
            val storageRef = storage.reference.child("cv_files/${UUID.randomUUID()}.pdf")  // Save CV with unique ID

            storageRef.putFile(cvUri)
                .addOnSuccessListener {
                    // Get the download URL for the CV
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // After uploading the CV, save application data with CV URL
                        saveApplicationData(updatedApplicationData.copy(cv = downloadUrl.toString()), onResult)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors in uploading CV
                    onResult(false, "Failed to upload CV: ${exception.message}")
                }
        } else {
            // No CV provided, just save the application data
            saveApplicationData(updatedApplicationData, onResult)
        }
    }

    // Helper function to save the application data in Firestore
    private fun saveApplicationData(applicationData: JobApplicationData, onResult: (Boolean, String?) -> Unit) {
        firestore.collection("job_applications")
            .document(applicationData.applicationId)  // Ensure document reference has a valid applicationId
            .set(applicationData)
            .addOnSuccessListener {
                // Application saved successfully
                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                // Handle any errors in saving application data
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

    fun fetchAllApplicationsForAdminReview(onResult: (List<JobApplicationData>) -> Unit) {
        firestore.collection("job_applications")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val applications = querySnapshot.toObjects(JobApplicationData::class.java)

                // List to store updated applications with user details and job information
                val updatedApplications = mutableListOf<JobApplicationData>()

                // Fetch user details and job details for each application
                applications.forEach { application ->
                    // Fetch user details using userId
                    firestore.collection("users").document(application.userId).get()
                        .addOnSuccessListener { userSnapshot ->
                            val userEmail = userSnapshot.getString("email") ?: "Unknown"
                            application.email = userEmail  // Add user email to application data

                            // Now fetch job details using jobID
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
                                        onResult(updatedApplications)  // Return the updated list
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("FirestoreError", "Error fetching job details", exception)

                                    // Add the application even if job details fail
                                    updatedApplications.add(application)

                                    // Once all applications are updated, return the result
                                    if (updatedApplications.size == applications.size) {
                                        onResult(updatedApplications)  // Return the updated list
                                    }
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirestoreError", "Error fetching user details", exception)

                            // Add the application even if user details fail
                            updatedApplications.add(application)

                            // Once all applications are updated, return the result
                            if (updatedApplications.size == applications.size) {
                                onResult(updatedApplications)  // Return the updated list
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

