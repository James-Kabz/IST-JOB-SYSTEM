package com.example.istalumniapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.core.net.toUri
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.SkillData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

interface ProfileRepository {
    suspend fun saveAlumniProfile(
        alumniProfileData: AlumniProfileData,
        profilePhotoUri: Uri?,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )

    suspend fun retrieveCurrentUserProfile(
        onLoading: (Boolean) -> Unit,
        onSuccess: (AlumniProfileData?) -> Unit,
        onFailure: (String) -> Unit
    )

    suspend fun retrieveAlumniProfiles(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<AlumniProfileData>) -> Unit,
        onFailure: (String) -> Unit
    )

    suspend fun saveSkill(
        skill: SkillData,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    )
    suspend fun retrieveSkills(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<SkillData>) -> Unit,
        onFailure: (String) -> Unit
    )
}


class ProfileRepositoryImplementation(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage,
    private val context: Context // Pass context to save the photo locally
) : ProfileRepository {

    override suspend fun saveAlumniProfile(
        alumniProfileData: AlumniProfileData,
        profilePhotoUri: Uri?,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            onError("No user is currently signed in.")
            return
        }

        val profilePhotoUrl = profilePhotoUri?.let {
            try {
                uploadProfilePhoto(uid, it)
            } catch (e: Exception) {
                onError(e.message ?: "Error uploading profile photo")
                return
            }
        }

        val updatedProfileData = alumniProfileData.copy(profilePhotoUri = profilePhotoUrl)
        val firestoreRef = firestore.collection("alumniProfiles").document(uid)

        try {
            firestoreRef.set(updatedProfileData).await()
            onComplete()
        } catch (e: Exception) {
            onError(e.message ?: "Error saving profile in Firestore")
        }
    }

    override suspend fun retrieveCurrentUserProfile(
        onLoading: (Boolean) -> Unit,
        onSuccess: (AlumniProfileData?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            onFailure("No user is currently signed in.")
            return
        }

        val firestoreRef = firestore.collection("alumniProfiles").document(uid)
        val storageRef = firebaseStorage.reference.child("profileImages/$uid.jpg")

        onLoading(true)
        try {
            val document = firestoreRef.get().await()
            if (document.exists()) {
                val profile = document.toObject(AlumniProfileData::class.java)
                profile?.profilePhotoUri = storageRef.downloadUrl.await().toString()
                onSuccess(profile)
            } else {
                onFailure("Profile not found for the current user.")
            }
        } catch (e: Exception) {
            onFailure("Error fetching profile: ${e.message}")
        } finally {
            onLoading(false)
        }
    }

    override suspend fun retrieveAlumniProfiles(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<AlumniProfileData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val firestoreRef = firestore.collection("alumniProfiles")
        onLoading(true)
        try {
            val result = firestoreRef.get().await()
            val profiles = result.mapNotNull { it.toObject(AlumniProfileData::class.java) }
            onSuccess(profiles)
        } catch (e: Exception) {
            onFailure("Error fetching profiles: ${e.message}")
        } finally {
            onLoading(false)
        }
    }

    override suspend fun saveSkill(
        skill: SkillData,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val firestoreRef = firestore.collection("skills").document(skill.skillID)
        try {
            firestoreRef.set(skill).await()
            onComplete()
        } catch (e: Exception) {
            onError(e.message ?: "Error saving skill")
        }
    }

    override suspend fun retrieveSkills(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<SkillData>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val firestoreRef = firestore.collection("skills")

        onLoading(true) // Indicate loading state
        try {
            val result = firestoreRef.get().await()  // Await to handle coroutine properly
            val skills = result.mapNotNull { it.toObject(SkillData::class.java) }
            onLoading(false)
            onSuccess(skills)
        } catch (e: Exception) {
            onLoading(false)
            onFailure("Error fetching skills: ${e.message}")
        }
    }

    // Helper method to upload profile photo
    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String {
        val storageRef = firebaseStorage.reference.child("profileImages/$uid.jpg")
        storageRef.putFile(uri).await()
        return storageRef.downloadUrl.await().toString()
    }

    // Helper method to save photo locally
    private suspend fun savePhotoToLocalStorage(uid: String, uri: Uri): Uri {
        val fileName = "$uid.jpg"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File(storageDir, fileName)

        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
        }

        return file.toUri()
    }
}
