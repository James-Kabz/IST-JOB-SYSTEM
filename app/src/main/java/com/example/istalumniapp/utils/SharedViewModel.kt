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
import androidx.compose.runtime.Composable
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
                _userRole.value = documentSnapshot.getString("role")
                    ?: "alumni" // Default to alumni if role is not found
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
                        Toast.makeText(context, "Job Posted Successfully", Toast.LENGTH_SHORT)
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
