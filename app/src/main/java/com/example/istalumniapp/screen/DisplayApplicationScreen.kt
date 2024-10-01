package com.example.istalumniapp.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.istalumniapp.R
import com.example.istalumniapp.utils.JobApplicationModel
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.JobApplicationData
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun DisplayApplicationScreen(
    navController: NavController,
    jobApplicationModel: JobApplicationModel,
    profileViewModel: ProfileViewModel,
    sharedViewModel: SharedViewModel,
    notificationViewModel: NotificationViewModel,
    userId: String
) {
    val applicationState = jobApplicationModel.applicationState.collectAsState(initial = emptyList())
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var applicationToDelete by remember { mutableStateOf<JobApplicationData?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var userRole by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) } // Loading state
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Fetch applications for the current user
    LaunchedEffect(userId) {
        Log.d("DisplayApplicationScreen", "Fetching applications for user $userId")
        loading = true
        jobApplicationModel.fetchApplicationsForUser(userId) {
            loading = false // Set loading to false after fetching is complete
        }
    }

    LaunchedEffect(userId) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "alumni"
        }
    }

    LaunchedEffect(userRole) {
        if (userRole == "alumni") {
            profileViewModel.retrieveProfilePhoto(
                onLoading = { loading = it },
                onSuccess = { url -> profilePhotoUrl = url },
                onFailure = { message ->
                    Log.e("DisplayJobScreen", "Error fetching profile photo: $message")
                }
            )
        }
    }

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
                onLogoutClick = { showLogoutConfirmation = true },
                userRole = userRole,
                profilePhotoUrl = profilePhotoUrl,
                notificationViewModel = notificationViewModel
            )
        },
        bottomBar = {
            DashboardBottomBar(navController = navController, userRole = userRole, notificationViewModel = notificationViewModel)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        Log.d("DisplayApplicationScreen", "Application state value: ${applicationState.value}")

        // Show a CircularProgressIndicator while loading
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Handle different states based on whether the list is null or empty
            when {
                applicationState.value == null -> {
                    Log.d("DisplayApplicationScreen", "Unexpected null state.")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("An unexpected error occurred.")
                    }
                }

                applicationState.value!!.isEmpty() -> {
                    Log.d("DisplayApplicationScreen", "No applications found.")
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "You have not submitted any applications yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = {
                                navController.navigate(Screens.DisplayAlumniJobsScreen.route)
                            }) {
                                Text("Browse Jobs")
                            }
                        }
                    }
                }

                else -> {
                    Log.d("DisplayApplicationScreen", "Displaying applications.")
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                text = "Your Applications",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(applicationState.value!!) { application ->
                            // Fetch feedback for the application
                            var feedback by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(application.applicationId,application.userId) {
                                jobApplicationModel.retrieveFeedback(application.applicationId,application.userId) { retrievedFeedback ->
                                    feedback = retrievedFeedback
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(application.companyLogo),
                                        contentDescription = "Company Logo",
                                        modifier = Modifier.size(60.dp),
                                        contentScale = ContentScale.Crop
                                    )

                                    Text(
                                        text = " ${application.title}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Experience: ${application.experience.years} Years",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Education: ${application.education}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Phone: ${application.phone}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Status: ${application.status ?: "Pending"}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = when (application.status) {
                                            "Approved" -> Color.Green
                                            "Rejected" -> Color.Red
                                            else -> Color.Gray
                                        }
                                    )
                                    if (!application.feedback.isNullOrEmpty()) {
                                        Text(
                                            "Feedback: ${application.feedback}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(onClick = {
                                            applicationToDelete = application
                                            showDeleteConfirmation = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Application",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && applicationToDelete != null) {
        DeleteApplicationConfirmationDialog(
            onConfirm = {
                jobApplicationModel.deleteApplication(
                    applicationId = applicationToDelete!!.applicationId,
                    onResult = { success, error ->
                        if (success) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Application deleted successfully")
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Failed to delete application: $error")
                            }
                        }
                        showDeleteConfirmation = false
                        applicationToDelete = null
                    }
                )
            },
            onDismiss = {
                showDeleteConfirmation = false
                applicationToDelete = null
            }
        )
    }
}


@Composable
fun DeleteApplicationConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Application") },
        text = { Text("Are you sure you want to delete this application? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


