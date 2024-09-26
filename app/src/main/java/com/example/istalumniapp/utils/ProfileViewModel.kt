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
import kotlinx.coroutines.flow.StateFlow
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.CoroutineScope


@Suppress("UNCHECKED_CAST")
class ProfileViewModel :ViewModel()  {


    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }



    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val _userRole = MutableStateFlow<String?>(null)

    private var lastRequestTime = 0L
    private val requestInterval = 5000L // 5 seconds

    private val _alumniProfiles = MutableStateFlow<List<AlumniProfileData>>(emptyList())
    val alumniProfiles: StateFlow<List<AlumniProfileData>> = _alumniProfiles

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _matchedJobs = MutableStateFlow<List<JobData>>(emptyList())
    val matchedJobs: StateFlow<List<JobData>> = _matchedJobs


    // Function to retrieve the current user's skills from their profile
    private suspend fun fetchAlumniSkills(uid: String): List<String> {
        return try {
            val documentSnapshot = firestore.collection("alumniProfiles").document(uid).get().await()
            documentSnapshot.get("skills") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Function to retrieve jobs that match the alumnus skills with at least 3 matching skills
    fun fetchMatchingJobs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _errorMessage.value = null
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            if (uid == null) {
                _loading.value = false
                _errorMessage.value = "No user is currently signed in."
                return@launch
            }

            try {
                // Fetch alumni's skills
                val alumniSkills = fetchAlumniSkills(uid)
                if (alumniSkills.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _loading.value = false
                        _errorMessage.value = "No skills found for the user."
                        Toast.makeText(context, "No skills found for the user.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Query jobs that match at least one of the alumni's skills
                val jobQuery = firestore.collection("jobs")
                    .whereArrayContainsAny("skills", alumniSkills) // Matches jobs with any skill in alumniSkills

                val result = jobQuery.get().await()

                // Parse the job data into a list
                val jobs = result.mapNotNull { it.toObject(JobData::class.java) }

                // Filter jobs to ensure at least 3 skills match
                val matchingJobs = jobs.filter { job ->
                    val jobSkills = job.skills ?: emptyList() // Assuming "requiredSkills" is a list of skills in the job
                    val matchingSkillCount = alumniSkills.intersect(jobSkills.toSet()).size // Get the intersection of the two lists
                    matchingSkillCount >= 3
                }

                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _matchedJobs.value = matchingJobs
                    Toast.makeText(context, "Found ${matchingJobs.size} matching jobs", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _errorMessage.value = "Error fetching matching jobs: ${e.message}"
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    fun retrieveAlumniProfiles(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isInternetAvailable(context)) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _errorMessage.value = "No internet connection."
                }
                return@launch
            }
            _loading.value = true
            try {
                val firestoreRef = firestore.collection("alumniProfiles")
                val result = firestoreRef.get().await()
                val profiles = result.mapNotNull { it.toObject(AlumniProfileData::class.java) }
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _alumniProfiles.value = profiles
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _errorMessage.value = "Error fetching profiles: ${e.message}"
                }
            }
        }
    }


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


    fun updateAlumniProfile(
        updatedProfileData: AlumniProfileData,
        newProfilePhotoUri: Uri?,
        context: Context,
        onLoading: (Boolean) -> Unit, // Loading callback
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        onLoading(true) // Start loading
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) {
                onError("No user is currently signed in.")
                onLoading(false) // End loading
            }
            return@launch
        }

        try {
            val updatedProfilePhotoUrl = newProfilePhotoUri?.let {
                uploadProfilePhoto(uid, it)
            }

            val finalProfileData = updatedProfileData.copy(
                profilePhotoUri = updatedProfilePhotoUrl ?: updatedProfileData.profilePhotoUri
            )

            val firestoreRef = firestore.collection("alumniProfiles").document(uid)
            firestoreRef.set(finalProfileData).await()

            withContext(Dispatchers.Main) {
                onComplete()
                onLoading(false) // End loading
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Error updating profile: ${e.message}")
                onLoading(false) // End loading
            }
        }
    }


    fun retrieveCurrentUserProfile(
        context: Context,
        onLoading: (Boolean) -> Unit,
        onSuccess: (AlumniProfileData?) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (!isInternetAvailable(context)) {
            withContext(Dispatchers.Main) {
                onLoading(false)
                onFailure("No internet connection.")
            }
            return@launch
        }
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastRequestTime < requestInterval) {
//            withContext(Dispatchers.Main) {
//                onFailure("Please wait before trying again.")
//            }
//            return@launch
//        }
//
//        lastRequestTime = currentTime

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

//    fun retrieveAlumniProfiles(
//        onLoading: (Boolean) -> Unit,
//        onSuccess: (List<AlumniProfileData>) -> Unit,
//        onFailure: (String) -> Unit
//    ) = viewModelScope.launch(Dispatchers.IO) {
//        onLoading(true)
//        try {
//            val firestoreRef = firestore.collection("alumniProfiles")
//            val result = firestoreRef.get().await()
//            val profiles = result.mapNotNull { it.toObject(AlumniProfileData::class.java) }
//            withContext(Dispatchers.Main) {
//                onLoading(false)
//                onSuccess(profiles)
//            }
//        } catch (e: Exception) {
//            withContext(Dispatchers.Main) {
//                onLoading(false)
//                onFailure("Error fetching profiles: ${e.message}")
//            }
//        }
//    }

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
