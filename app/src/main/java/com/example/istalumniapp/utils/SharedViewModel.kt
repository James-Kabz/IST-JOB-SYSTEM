package com.example.istalumniapp.utils

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.example.istalumniapp.nav.Screens

import com.google.firebase.storage.StorageReference

import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

class SharedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

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
                _userRole.value = documentSnapshot.getString("role") ?: "alumni" // Default to alumni if role is not found
            }
        }
    }


    fun saveAlumniProfile(
        alumniProfileData: AlumniProfileData,
        profilePhotoUri: Uri?,
        context: Context,
        navController: NavController,  // Add NavController parameter
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No user is currently signed in.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        // Upload the profile photo if provided
        val profilePhotoUrl = if (profilePhotoUri != null) {
            try {
                uploadProfilePhoto(uid, profilePhotoUri)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to upload profile photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Error uploading profile photo")
                }
                return@launch
            }
        } else {
            null
        }

        // Save the photo locally if provided
        val localPhotoUri = if (profilePhotoUri != null) {
            try {
                savePhotoToLocalStorage(uid, profilePhotoUri, context)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save photo locally: ${e.message}", Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Error saving profile photo locally")
                }
                null
            }
        } else {
            null
        }

        // Update the alumniProfileData with the photo URLs
        val updatedProfileData = alumniProfileData.copy(
            profilePhotoUri = profilePhotoUrl  // Firebase URL
            // Add any other data you want to update
        )

        // Save the profile data in Firestore
        val firestoreRef = FirebaseFirestore.getInstance().collection("alumniProfiles").document(uid)
        try {
            firestoreRef.set(updatedProfileData)
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Profile Saved Successfully", Toast.LENGTH_SHORT).show()
                        onComplete() // Call the onComplete callback
                        navController.navigate(Screens.DashboardScreen.route) // Navigate to DashboardScreen
                    }
                }
                .addOnFailureListener { e ->
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        onError(e.message ?: "Error saving profile in Firestore")
                    }
                }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                onError(e.message ?: "Unknown error")
            }
        }
    }


    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$uid.jpg")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    private suspend fun savePhotoToLocalStorage(uid: String, uri: Uri, context: Context): Uri {
        val fileName = "$uid.jpg"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)

        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }
            } catch (e: Exception) {
                throw Exception("Failed to save photo locally: ${e.message}")
            }
        }

        return file.toUri()
    }




    fun retrieveCurrentUserProfile(
        onLoading: (Boolean) -> Unit,
        onSuccess: (AlumniProfileData?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            onFailure("No user is currently signed in.")
            return
        }

        val uid = currentUser.uid

        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = firestore.collection("alumniProfiles").document(uid)
            val storageRef = FirebaseStorage.getInstance().reference.child("profileImages/$uid.jpg") // Adjust path as needed
            onLoading(true)

            try {
                val document = firestoreRef.get().await()

                if (document != null && document.exists()) {
                    // Convert document snapshot to AlumniProfileData
                    val profile = document.toObject(AlumniProfileData::class.java)

                    try {
                        // Fetch the profile picture URL
                        val imageUrl = storageRef.downloadUrl.await().toString()
                        profile?.profilePhotoUri = imageUrl // Assign the URL to the profilePhoto field

                        onLoading(false)
                        onSuccess(profile)
                    } catch (e: Exception) {
                        // Handle image retrieval failure but still return the profile data
                        onLoading(false)
                        profile?.profilePhotoUri = null // or some placeholder URL
                        onSuccess(profile)
                    }
                } else {
                    onLoading(false)
                    onFailure("Profile not found for the current user.")
                }
            } catch (e: Exception) {
                onLoading(false)
                onFailure("Error fetching profile: ${e.message}")
            }
        }
    }



    // Function to retrieve all alumni profiles
    fun retrieveAlumniProfiles(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<AlumniProfileData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val firestoreRef = firestore.collection("alumniProfiles")
            onLoading(true)
            try {
                firestoreRef.get().addOnSuccessListener { result ->
                    val profiles = result.mapNotNull { it.toObject(AlumniProfileData::class.java) }
                    onLoading(false)
                    onSuccess(profiles)
                }.addOnFailureListener {
                    onLoading(false)
                    onFailure("Error fetching profiles: ${it.message}")
                }
            } catch (e: Exception) {
                onLoading(false)
                onFailure("Error: ${e.message}")
            }
        }
    }

    // Function to save job data
    fun saveJob(
        jobData: JobData,
        context: Context
    ) = CoroutineScope(Dispatchers.IO).launch {
        val firestoreRef = firestore.collection("jobs").document(jobData.jobID)
        try {
            firestoreRef.set(jobData)
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Job Posted Successfully", Toast.LENGTH_SHORT).show()
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
