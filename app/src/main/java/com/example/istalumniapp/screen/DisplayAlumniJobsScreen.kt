package com.example.istalumniapp.screen

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.NotificationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun DisplayAlumniJobsScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    sharedViewModel: SharedViewModel,
    notificationViewModel: NotificationViewModel
) {
    var matchedJobs by remember { mutableStateOf<List<JobData>>(emptyList()) }
    val loading by profileViewModel.loading.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf<String?>(null) }

    // Fetch matched jobs based on alumni skills
    LaunchedEffect(Unit) {
        profileViewModel.fetchMatchingJobs(context = context)
        profileViewModel.matchedJobs.collect { jobs ->
            matchedJobs = jobs
        }
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "alumni"
        }

        // Fetch profile photo
        profileViewModel.retrieveProfilePhoto(
            onLoading = {  },  // Update loading state
            onSuccess = { url -> profilePhotoUrl = url },
            onFailure = { message ->
                Log.e("DisplayJobScreen", "Error fetching profile photo: $message")
            }
        )
    }

    // Show logout confirmation dialog
    if (showLogoutConfirmation) {
        LogoutConfirm(
            onConfirm = {
                FirebaseAuth.getInstance().signOut() // Log out the user
                navController.navigate(Screens.ISTPreviewScreen.route) {
                    popUpTo(0)
                }
                showLogoutConfirmation = false
            },
            onDismiss = { showLogoutConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                navController = navController,
                userRole = userRole ?: "alumni", // Use dynamic role
                profilePhotoUrl = profilePhotoUrl,
                onLogoutClick = { showLogoutConfirmation = true },
                notificationViewModel = notificationViewModel
            )
        },
        bottomBar = {
            DashboardBottomBar(navController = navController, userRole = userRole, notificationViewModel = notificationViewModel)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center // Ensure the loading indicator is centered
        ) {
            if (loading) {
                CircularProgressIndicator() // Loading indicator works!
            } else {
                when {
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    matchedJobs.isEmpty() -> {
                        Text(
                            text = "No matching jobs found for your skills.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(matchedJobs) { job ->
                                JobItem(
                                    job = job,
                                    userRole = "alumni", // Alumni role to show apply buttons
                                    navController = navController,
                                    sharedViewModel = sharedViewModel,
                                    onJobDeleted = { } // No deletion for alumni
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}


