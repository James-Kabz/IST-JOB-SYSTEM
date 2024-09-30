package com.example.istalumniapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.istalumniapp.nav.NavGraph
import com.example.istalumniapp.ui.theme.ISTALUMNIAPPTheme
import com.example.istalumniapp.utils.JobApplicationModel
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.NotificationViewModelFactory
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavHostController

    // Initialize the SharedViewModel and ProfileViewModel normally
    private val sharedViewModel: SharedViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val jobApplicationModel: JobApplicationModel by viewModels()

    // Use factory to create NotificationViewModel
    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(applicationContext) // Use the custom factory here
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase App
        FirebaseApp.initializeApp(this)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            ISTALUMNIAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    notificationViewModel.fetchNotifications()
                    // Initialize the NavController
                    navController = rememberNavController()
                    // Calling the NavGraph that contains the composable with screens
                    NavGraph(
                        navController = navController,
                        sharedViewModel = sharedViewModel,
                        profileViewModel = profileViewModel,
                        jobApplicationModel = jobApplicationModel,
                        notificationViewModel = notificationViewModel
                    )
                }
            }
        }

        // Call the function to retrieve FCM Token here inside onCreate
        retrieveFCMToken()
    }

    // This function retrieves the FCM Token
    private fun retrieveFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM Token", "FCM Token: $token")
                    // Handle token (e.g., send it to server)
                } else {
                    Log.e("FCM Token", "Fetching FCM token failed", task.exception)
                }
            }
    }
}
