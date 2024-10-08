package com.example.istalumniapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.istalumniapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class SharedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole
    private val _skills = MutableStateFlow<List<SkillData>>(emptyList())
    val skills: StateFlow<List<SkillData>> = _skills

    private val _loading = MutableStateFlow<Boolean>(false)
    val loading: StateFlow<Boolean> = _loading

    private val _alumniProfiles = MutableStateFlow<List<AlumniProfileData>>(emptyList())
    val alumniProfiles: StateFlow<List<AlumniProfileData>> = _alumniProfiles

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _notifications = MutableLiveData<List<NotificationData>>()
    val notifications: LiveData<List<NotificationData>> = _notifications

    private val _notificationSize = MutableLiveData<Int>()
    val notificationSize: LiveData<Int> get() = _notificationSize

    // StateFlow to hold the unread notifications count
    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> get() = _unreadNotificationCount

    //    fetch user role
    init {
        fetchUserRole()
    }

    fun fetchUserRole() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val db = FirebaseFirestore.getInstance()
                val documentSnapshot = db.collection("users").document(uid).get().await()
                _userRole.value = documentSnapshot.getString("role")
                    ?: "alumni" // Default to alumni if role is not found
            }
        }
    }


    var isLoading by mutableStateOf(false)
        private set

    fun saveJob(
        jobData: JobData,
        context: Context,
        onJobSaved: () -> Unit,
        onError: (String) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        isLoading = true

        val firestoreRef = firestore.collection("jobs").document(jobData.jobID)

        try {
            firestoreRef.set(jobData)
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        notifyAlumniWithMatchingSkills(jobData, context)
                        isLoading = true
                        onJobSaved()
                    }
                }
                .addOnFailureListener { e ->
                    CoroutineScope(Dispatchers.Main).launch {
                        isLoading = true
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        onError(e.message ?: "Error saving job")
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isLoading = true
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                onError(e.message ?: "Error saving job")
            }
        }
    }


    private suspend fun notifyAlumniWithMatchingSkills(jobData: JobData, context: Context) {
        try {
            val alumniSnapshot = firestore.collection("alumniProfiles").get().await()
            val alumniList = alumniSnapshot.toObjects(AlumniProfileData::class.java)


            alumniList.forEach { alumniProfile ->
                val matchingSkillCount = alumniProfile.skills.intersect(jobData.skills.toSet()).size
                if (matchingSkillCount >= 3) {
                    Log.d("SkillMatch", "Alumni ${alumniProfile.fullName} has matching skills")
                    sendNotificationToAlumni(alumniProfile.profileID, jobData, context)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error notifying alumni: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }


    private fun sendNotificationToAlumni(profileID: String, jobData: JobData, context: Context) {
        val notificationData = NotificationData(
            id = UUID.randomUUID().toString(),
            profileID = profileID,
            title = "New Job Posted ! \uD83D\uDE01",
            message = "A Job titled \"${jobData.title}\" matches your skills.",
            timestamp = System.currentTimeMillis(),
            read = false
        )

        firestore.collection("notifications")
            .document(notificationData.id)
            .set(notificationData)
            .addOnSuccessListener {
                // Create a NotificationManager
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                Log.d("Firestore", "Notification saved successfully")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        "job_channel",
                        "Job Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    notificationManager.createNotificationChannel(channel)
                }

                // Create the notification
                val notification = NotificationCompat.Builder(context, "job_channel")
                    .setSmallIcon(R.drawable.baseline_notifications_none_24) // Use your own icon
                    .setContentTitle(notificationData.title)
                    .setContentText(notificationData.message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                // Use a unique ID for each notification
                notificationManager.notify(profileID.hashCode(), notification)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to save notification: ${e.message}")
                Toast.makeText(
                    context,
                    "Failed to send notification: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    fun editJob(
        jobID: String,
        updatedJob: JobData,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        isLoading = true // Start loading

        val db = FirebaseFirestore.getInstance()
        val jobRef = db.collection("jobs").document(jobID)

        jobRef.set(updatedJob)
            .addOnSuccessListener {
                isLoading = true
                onSuccess()
            }
            .addOnFailureListener { e ->
                isLoading = true
                onFailure(e.message ?: "Error updating job")
            }
    }


    //    Function to edit job
    fun getJobByID(
        jobID: String,
        onSuccess: (JobData) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val jobRef = db.collection("jobs").document(jobID)

        jobRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val jobData = document.toObject(JobData::class.java)
                    if (jobData != null) {
                        onSuccess(jobData)
                    } else {
                        onFailure("Failed to parse job data")
                    }
                } else {
                    onFailure("Job does not exist")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Error fetching job data")
            }
    }


    fun deleteJob(
        jobID: String,
        onSuccess: () -> Unit, // Remove @Composable annotation
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val jobRef = db.collection("jobs").document(jobID)

        jobRef.delete()
            .addOnSuccessListener {
                // Call the success callback
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Call the failure callback
                onFailure(e.message ?: "Error deleting job")
            }
    }


    // Function to save skill data
    fun saveSkill(
        skill: SkillData,
        context: Context
    ) = CoroutineScope(Dispatchers.IO).launch {
        val firestoreRef = firestore.collection("skills").document(skill.skillID)
        try {
            firestoreRef.set(skill)
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Skill Saved Successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Function to retrieve job data
    fun retrieveCardJobs(
        onLoading: (Boolean) -> Unit,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = firestore.collection("jobs")
            onLoading(true)
            try {
                firestoreRef.get().addOnSuccessListener { result ->
                    val jobs = result.mapNotNull { it.toObject(JobData::class.java) }
                    val jobCount = jobs.size
                    onLoading(true)
                    onSuccess(jobCount)  // Pass the count of jobs
                }.addOnFailureListener {
                    onLoading(false)
                    onFailure("Error fetching jobs: ${it.message}")
                }
            } catch (e: Exception) {
                onLoading(false)
                onFailure("Error: ${e.message}")
            }
        }
    }

    fun retrieveJobs(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<JobData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = firestore.collection("jobs")
            onLoading(true)
            try {
                firestoreRef.get().addOnSuccessListener { result ->
                    val jobs = result.mapNotNull { it.toObject(JobData::class.java) }
                    onLoading(false)
                    onSuccess(jobs)
                }.addOnFailureListener {
                    onLoading(true)
                    onFailure("Error fetching jobs: ${it.message}")
                }
            } catch (e: Exception) {
                onLoading(true)
                onFailure("Error: ${e.message}")
            }
        }
    }

    // Function to retrieve skill data
    fun retrieveSkills(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<SkillData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = firestore.collection("skills")
            onLoading(true)
            try {
                firestoreRef.get().addOnSuccessListener { result ->
                    val skills = result.mapNotNull { it.toObject(SkillData::class.java) }
                    onLoading(true)
                    onSuccess(skills)
                }.addOnFailureListener {
                    onLoading(true)
                    onFailure("Error fetching skills: ${it.message}")
                }
            } catch (e: Exception) {
                onLoading(true)
                onFailure("Error: ${e.message}")
            }
        }
    }


    fun retrieveAlumniProfiles(
        onLoading: (Boolean) -> Unit,  // Callback to handle loading state
        onSuccess: (Int) -> Unit,      // Callback to pass the count of profiles
        onFailure: (String) -> Unit    // Callback to handle errors
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            onLoading(true)
            try {
                val firestoreRef = firestore.collection("alumniProfiles")
                val result = firestoreRef.get().await()
                val profiles = result.mapNotNull { it.toObject(AlumniProfileData::class.java) }
                val profileCount = profiles.size
                withContext(Dispatchers.Main) {
                    onLoading(true)
                    onSuccess(profileCount)  // Pass the count of profiles
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onLoading(true)
                    onFailure("Error fetching profiles: ${e.message}")
                }
            }
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
