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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


@Suppress("UNCHECKED_CAST")
class ProfileViewModel : ViewModel() {


    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
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

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _loading1 = MutableLiveData<Boolean>()
    val loading1: LiveData<Boolean> = _loading1


    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _matchedJobs = MutableStateFlow<List<JobData>>(emptyList())
    val matchedJobs: StateFlow<List<JobData>> = _matchedJobs


    // Function to retrieve the current user's skills from their profile
    private suspend fun fetchAlumniSkills(uid: String): List<String> {
        return try {
            val documentSnapshot =
                firestore.collection("alumniProfiles").document(uid).get().await()
            documentSnapshot.get("skills") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun countMatchingJobs(
        context: Context,
        onSuccess: (Int) -> Unit,  // Callback to return the count
        onFailure: (String) -> Unit  // Callback to handle errors
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Switch to main thread to update LiveData
                withContext(Dispatchers.Main) {
                    _loading1.value = true
                }

                _errorMessage.value = (null)
                val uid = FirebaseAuth.getInstance().currentUser?.uid

                if (uid == null) {
                    withContext(Dispatchers.Main) {
                        _loading1.value = false
                        onFailure("No user is currently signed in.")
                        Toast.makeText(
                            context,
                            "No user is currently signed in.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Fetch alumni's skills
                val alumniSkills = fetchAlumniSkills(uid)
                if (alumniSkills.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _loading1.value = false
                        onFailure("No skills found for the user.")
                        Toast.makeText(context, "No skills found for the user.", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@launch
                }

                // Query jobs that match at least one of the alumni's skills
                val jobQuery = firestore.collection("jobs")
                    .whereArrayContainsAny("skills", alumniSkills)

                val result = jobQuery.get().await()

                // Parse the job data into a list
                val jobs = result.mapNotNull { it.toObject(JobData::class.java) }

                // Filter jobs to ensure at least 3 skills match
                val matchingJobsCount = jobs.count { job ->
                    val jobSkills = job.skills
                    val matchingSkillCount = alumniSkills.intersect(jobSkills.toSet()).size
                    matchingSkillCount >= 3
                }

                // Switch to main thread to update UI and call onSuccess
                withContext(Dispatchers.Main) {
                    _loading1.value = false
                    onSuccess(matchingJobsCount)  // Return the count of matching jobs
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _loading1.value = false
                    onFailure("Error fetching matching jobs: ${e.message}")
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


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
                val alumniSkills = fetchAlumniSkills(uid)
                if (alumniSkills.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _loading.value = false
                        _errorMessage.value = "No skills found for the user."
                        Toast.makeText(context, "No skills found for the user.", Toast.LENGTH_SHORT)
                            .show()
                    }
                    return@launch
                }

                val jobQuery = firestore.collection("jobs")
                    .whereArrayContainsAny("skills", alumniSkills)

                val result = jobQuery.get().await()

                // Parse the job data into a list
                val jobs = result.mapNotNull { it.toObject(JobData::class.java) }

                // Filter jobs to ensure at least 3 skills match
                val matchingJobs = jobs.filter { job ->
                    val jobSkills = job.skills
                    val matchingSkillCount = alumniSkills.intersect(jobSkills.toSet()).size
                    matchingSkillCount >= 3
                }

                withContext(Dispatchers.Main) {
                    _loading.value = false
                    _matchedJobs.value = matchingJobs
                    Toast.makeText(
                        context,
                        "Found ${matchingJobs.size} matching job postings.",
                        Toast.LENGTH_SHORT
                    ).show()
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
                _userRole.value = documentSnapshot.getString("role")
                    ?: "alumni" // Default to alumni if role is not found
            }
        }
    }

    // Inside your ViewModel
    var isLoading by mutableStateOf(false)
        private set

    fun saveAlumniProfile(
        alumniProfileData: AlumniProfileData,
        profilePhotoUri: Uri?,
        navController: NavController,
        context: Context,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        isLoading = true // Start loading

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) {
                isLoading = false // Stop loading if no user is signed in
            }
            return@launch
        }

        val profilePhotoUrl = profilePhotoUri?.let {
            try {
                uploadProfilePhoto(uid, it) // Upload the photo and get the download URL
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = true // Stop loading if photo upload fails
                    onError(e.message ?: "Error uploading profile photo")
                }
                return@launch
            }
        }

        val updatedProfileData = alumniProfileData.copy(
            profileID = uid,
            profilePhotoUri = profilePhotoUrl
        )

        val firestoreRef = firestore.collection("alumniProfiles").document(uid)
        try {
            firestoreRef.set(updatedProfileData).await()
            withContext(Dispatchers.Main) {
                onComplete()
                isLoading = true // Stop loading after successful save
                navController.navigate(Screens.DashboardScreen.route)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isLoading = true // Stop loading if Firestore save fails
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
                onLoading(true) // End loading
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
                onLoading(true) // End loading
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Error updating profile: ${e.message}")
                onLoading(true) // End loading
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
                try {
                    storage.reference.child("profileImages/$uid.jpg").downloadUrl.await().toString()
                } catch (e: Exception) {
                    null // Handle the case where the image doesn't exist
                }
            }

            val document = profileDeferred.await()
            if (!document.exists()) {
                withContext(Dispatchers.Main) {
                    onLoading(true)
                    onFailure("Profile not found for the current user.")
                }
                return@launch
            }

            val profile = document.toObject(AlumniProfileData::class.java)?.apply {
                profilePhotoUri = imageUrlDeferred.await() // Assign the photo URL if it exists
            }

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


    private var cachedUrl: String? = null

    fun retrieveProfilePhoto(
        onLoading: (Boolean) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        cachedUrl?.let {
            withContext(Dispatchers.Main) {
                onSuccess(it)
            }
            return@launch
        }

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
            cachedUrl = imageUrl // Cache the result
            withContext(Dispatchers.Main) {
                onLoading(true)
                onSuccess(imageUrl)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoading(true)
                onFailure("Error fetching profile photo: ${e.message}")
            }
        }
    }


    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        val storageRef = storage.reference.child("profileImages/$uid.jpg")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

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
                onLoading(true)
                onSuccess(skills)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoading(true)
                onFailure("Error fetching skills: ${e.message}")
            }
        }
    }
}
