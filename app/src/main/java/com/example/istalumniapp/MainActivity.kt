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
import androidx.core.app.NotificationCompat
import com.example.istalumniapp.nav.NavGraph
import com.example.istalumniapp.screen.NotificationScreen
import com.example.istalumniapp.ui.theme.ISTALUMNIAPPTheme
import com.example.istalumniapp.utils.JobApplicationModel
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavHostController
    private val sharedViewModel: SharedViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val jobApplicationModel: JobApplicationModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase App
        FirebaseApp.initializeApp(this)

        // Initialize Firebase App Check with Debug mode for testing
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance() // For testing, switch to SafetyNetAppCheckProviderFactory for production
        )

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContent {
            ISTALUMNIAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    navController = rememberNavController()

                    // Calling the NavGraph that contains the composable with screens
                    NavGraph(
                        navController = navController,
                        sharedViewModel = sharedViewModel,
                        profileViewModel = profileViewModel,
                        jobApplicationModel = jobApplicationModel,
                        notificationViewModel = notificationViewModel
                    )
//                    NotificationScreen()
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
