package com.example.istalumniapp.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.istalumniapp.R
import com.example.istalumniapp.screen.auth
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



    fun saveJob(
        jobData: JobData,
        context: Context,
        onJobSaved: () -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        val firestoreRef = firestore.collection("jobs").document(jobData.jobID)

        try {
            firestoreRef.set(jobData)
                .addOnSuccessListener {
                    // Notify the user that the job was successfully posted
                    CoroutineScope(Dispatchers.Main).launch {
                        notifyAlumniWithMatchingSkills(jobData, context)
                        Toast.makeText(context, "Job Posted Successfully", Toast.LENGTH_SHORT).show()
                        onJobSaved()
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

    private suspend fun notifyAlumniWithMatchingSkills(jobData: JobData, context: Context) {
        try {
            // Fetch all alumni profiles
            val alumniSnapshot = firestore.collection("alumniProfiles").get().await()
            val alumniList = alumniSnapshot.toObjects(AlumniProfileData::class.java)

            // Iterate over all alumni profiles and match skills
            alumniList.forEach { alumniProfile ->
                val matchingSkillCount = alumniProfile.skills.intersect(jobData.skills.toSet()).size
                if (matchingSkillCount >= 3) {
                    Log.d("SkillMatch", "Alumni ${alumniProfile.fullName} has matching skills")
                    sendNotificationToAlumni(alumniProfile.profileID, jobData, context)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error notifying alumni: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun sendNotificationToAlumni(profileID: String, jobData: JobData, context: Context) {
        // Save the notification in Firestore
        val notificationData = NotificationData(
            id = UUID.randomUUID().toString(),
            profileID =   profileID,
            title = "New Job Matches Your Skills!",
            message = "A job titled \"${jobData.title}\" matches your skills.",
            timestamp = System.currentTimeMillis(),
            read = false
        )

        firestore.collection("notifications")
            .document(notificationData.id)
            .set(notificationData)
            .addOnSuccessListener {
                // Create a NotificationManager
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                Log.d("Firestore", "Notification saved successfully")
                // Create the notification channel (for Android 8+)
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
                Toast.makeText(context, "Failed to send notification: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }





    // Your editJob method remains unchanged
    fun editJob(
        jobID: String,
        updatedJob: JobData,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val jobRef = db.collection("jobs").document(jobID)

        jobRef.set(updatedJob)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message ?: "Error updating job") }
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
                        Toast.makeText(context, "Skill Saved Successfully", Toast.LENGTH_SHORT).show()
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
                    onLoading(false)
                    onFailure("Error fetching jobs: ${it.message}")
                }
            } catch (e: Exception) {
                onLoading(false)
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
                    onLoading(false)
                    onSuccess(skills)
                }.addOnFailureListener {
                    onLoading(false)
                    onFailure("Error fetching skills: ${it.message}")
                }
            } catch (e: Exception) {
                onLoading(false)
                onFailure("Error: ${e.message}")
            }
        }
    }


}
