package com.example.istalumniapp.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

interface AppContainer {
    val profileRepository: ProfileRepository
}

class DefaultAppContainer(
    private val context: Context // Add context here for local storage operations
) : AppContainer {

    // Lazily initialize ProfileRepository with all dependencies
    override val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImplementation(
            firestore = FirebaseFirestore.getInstance(), // Firebase Firestore instance
            firebaseAuth = FirebaseAuth.getInstance(),   // Firebase Auth instance
            firebaseStorage = FirebaseStorage.getInstance(), // Firebase Storage instance
            context = context  // Application context for local storage operations
        )
    }
}
