package com.example.istalumniapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.istalumniapp.nav.NavGraph
import com.example.istalumniapp.ui.theme.ISTALUMNIAPPTheme
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavHostController
    private val sharedViewModel: SharedViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

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

        setContentView(R.layout.activity_main)

        setContent {
            ISTALUMNIAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    navController = rememberNavController()

                    // Calling the NavGraph that contains the composable with screens
                    NavGraph(
                        navController = navController,
                        sharedViewModel = sharedViewModel,
                        profileViewModel = profileViewModel
                    )
                }
            }
        }
    }
}
