package com.example.istalumniapp.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.istalumniapp.nav.Screens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.async


class ProfileViewModel :ViewModel()  {


    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val _userRole = MutableStateFlow<String?>(null)

    private var lastRequestTime = 0L
    private val requestInterval = 5000L // 5 seconds

    init {
        fetchUserRole()
    }

    private fun fetchUserRole() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val documentSnapshot = firestore.collection("users").document(uid).get().await()
                _userRole.value = documentSnapshot.getString("role") ?: "alumni" // Default to alumni if role is not found
            }
        }
    }

    fun saveAlumniProfile(
        alumniProfileData: AlumniProfileData,
        profilePhotoUri: Uri?,
        navController: NavController,
        context: Context,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "No user is currently signed in.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        val profilePhotoUrl = profilePhotoUri?.let {
            try {
                uploadProfilePhoto(uid, it).also { downloadUrl ->
                    // Save the download URL to Firestore instead of the local path
                    alumniProfileData.copy(profilePhotoUri = downloadUrl)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to upload profile photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    onError(e.message ?: "Error uploading profile photo")
                }
                return@launch
            }
        }

        val updatedProfileData = alumniProfileData.copy(
            profilePhotoUri = profilePhotoUrl
        )

        val firestoreRef = firestore.collection("alumniProfiles").document(uid)
        try {
            firestoreRef.set(updatedProfileData).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Profile Saved Successfully", Toast.LENGTH_SHORT).show()
                onComplete()
                navController.navigate(Screens.DashboardScreen.route)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                onError(e.message ?: "Error saving profile in Firestore")
            }
        }
    }

    fun retrieveCurrentUserProfile(
        onLoading: (Boolean) -> Unit,
        onSuccess: (AlumniProfileData?) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRequestTime < requestInterval) {
            withContext(Dispatchers.Main) {
                onFailure("Please wait before trying again.")
            }
            return@launch
        }

        lastRequestTime = currentTime

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) {
                onFailure("No user is currently signed in.")
            }
            return@launch
        }

        onLoading(true)

        try {
            // Fetch user profile and profile photo URL in parallel
            val firestoreRef = firestore.collection("alumniProfiles").document(uid)
            val profileDeferred = async { firestoreRef.get().await() }
            val imageUrlDeferred = async {
                storage.reference.child("profileImages/$uid.jpg").downloadUrl.await().toString()
            }

            val document = profileDeferred.await()
            if (!document.exists()) {
                withContext(Dispatchers.Main) {
                    onLoading(false)
                    onFailure("Profile not found for the current user.")
                }
                return@launch
            }

            val profile = document.toObject(AlumniProfileData::class.java)
            profile?.profilePhotoUri = imageUrlDeferred.await()

            withContext(Dispatchers.Main) {
                onLoading(false)
                onSuccess(profile)
            }

        } catch (e: Exception) {
            Log.e("FirestoreDebug", "Error fetching user data", e)
            withContext(Dispatchers.Main) {
                onLoading(false)
                onFailure("Error fetching user data: ${e.message}")
            }
        }
    }


    // New function to retrieve the profile photo URL
    fun retrieveProfilePhoto(
        onLoading: (Boolean) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) {
                onFailure("No user is currently signed in.")
            }
            return@launch
        }

        onLoading(true)

        try {
            val storageRef = storage.reference.child("profileImages/$uid.jpg")
            val imageUrl = storageRef.downloadUrl.await().toString()
            withContext(Dispatchers.Main) {
                onLoading(false)
                onSuccess(imageUrl)
            }
        } catch (e: Exception) {
            Log.e("ProfileViewModel", "Error fetching profile photo", e)
            withContext(Dispatchers.Main) {
                onLoading(false)
                onFailure("Error fetching profile photo: ${e.message}")
            }
        }
    }


    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        val storageRef = storage.reference.child("profileImages/$uid.jpg")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    fun retrieveAlumniProfiles(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<AlumniProfileData>) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        onLoading(true)
        try {
            val firestoreRef = firestore.collection("alumniProfiles")
            val result = firestoreRef.get().await()
            val profiles = result.mapNotNull { it.toObject(AlumniProfileData::class.java) }
            withContext(Dispatchers.Main) {
                onLoading(false)
                onSuccess(profiles)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoading(false)
                onFailure("Error fetching profiles: ${e.message}")
            }
        }
    }

    fun retrieveSkills(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<SkillData>) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        onLoading(true)
        try {
            val firestoreRef = firestore.collection("skills")
            val result = firestoreRef.get().await()
            val skills = result.mapNotNull { it.toObject(SkillData::class.java) }
            withContext(Dispatchers.Main) {
                onLoading(false)
                onSuccess(skills)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoading(false)
                onFailure("Error fetching skills: ${e.message}")
            }
        }
    }
}
