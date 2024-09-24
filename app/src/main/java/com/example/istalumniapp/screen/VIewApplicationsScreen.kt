package com.example.istalumniapp.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.istalumniapp.utils.JobApplicationData
import com.example.istalumniapp.utils.JobApplicationModel
import com.example.istalumniapp.nav.Screens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("UnrememberedMutableState")
@Composable
fun ViewApplicationScreen(
    navController: NavController,
    applicationModel: JobApplicationModel
) {
    val applicationState = remember { mutableStateOf<List<JobApplicationData>?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    val itemsPerPage = 10
    val totalPages by derivedStateOf {
        val totalItems = applicationState.value?.size ?: 0
        (totalItems + itemsPerPage - 1) / itemsPerPage
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var userRole by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Fetch user role and profile photo
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "admin"
        }

        // Fetch all applications for admin review
        applicationModel.fetchAllApplicationsForAdminReview { applications ->
            applicationState.value = applications
            loading = false
        }
    }

    if (showLogoutConfirmation) {
        LogoutConfirm(
            onConfirm = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate(Screens.ISTLoginScreen.route)
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
                profilePhotoUrl = profilePhotoUrl
            )
        },
        bottomBar = {
            DashboardBottomBar(navController = navController, userRole = userRole)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        if (loading) {
            // Show circular indicator while loading applications
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val applications = applicationState.value?.let { list ->
                val fromIndex = currentPage * itemsPerPage
                val toIndex = (fromIndex + itemsPerPage).coerceAtMost(list.size)
                list.subList(fromIndex, toIndex)
            } ?: emptyList()

            if (applications.isEmpty()) {
                // Show message if no applications are found
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No applications available for review.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    itemsIndexed(applications) { index, application ->
                        var showFeedbackDialog by remember { mutableStateOf(false) }
                        var feedbackText by remember { mutableStateOf("") }
                        var showProgress by remember { mutableStateOf(false) }

                        // Each application card for admin review
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Applicant Email: ${application.email}",
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                Image(
                                    painter = rememberAsyncImagePainter(application.companyLogo),
                                    contentDescription = "Company Logo",
                                    modifier = Modifier.size(48.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    "Job Title: ${application.title}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
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

                                val statusColor = when (application.status) {
                                    "Approved" -> Color.Green
                                    "Rejected" -> Color.Red
                                    else -> Color.Gray
                                }
                                Text(
                                    "Status: ${application.status ?: "Pending"}",
                                    color = statusColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                if (!application.feedback.isNullOrEmpty()) {
                                    Text(
                                        "Feedback: ${application.feedback}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row {
                                    OutlinedButton(onClick = {
                                        showProgress = true
                                        applicationModel.updateApplicationStatus(
                                            application.applicationId,
                                            true
                                        ) { success, message ->
                                            showProgress = false
                                            if (success) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Application approved successfully.")
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Error: $message")
                                                }
                                            }
                                        }
                                    }) {
                                        Text("Approve")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(onClick = {
                                        showProgress = true
                                        applicationModel.updateApplicationStatus(
                                            application.applicationId,
                                            false
                                        ) { success, message ->
                                            showProgress = false
                                            if (success) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Application rejected successfully.")
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Error: $message")
                                                }
                                            }
                                        }
                                    }) {
                                        Text("Reject")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(onClick = {
                                        showFeedbackDialog = true
                                    }) {
                                        Text("Send Feedback")
                                    }
                                }

                                if (showProgress) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator()
                                }

                                if (showFeedbackDialog) {
                                    FeedbackDialog(
                                        feedbackText = feedbackText,
                                        onFeedbackChange = { feedbackText = it },
                                        onDismiss = { showFeedbackDialog = false },
                                        onSend = {
                                            showProgress = true
                                            applicationModel.sendFeedback(
                                                application.applicationId,
                                                feedbackText
                                            ) { success, message ->
                                                showProgress = false
                                                if (success) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Feedback sent successfully.")
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Error: $message")
                                                    }
                                                }
                                                showFeedbackDialog = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (totalPages > 1) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (currentPage > 0) {
                                    TextButton(onClick = { currentPage-- }) {
                                        Text("Previous")
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                if (currentPage < totalPages - 1) {
                                    TextButton(onClick = { currentPage++ }) {
                                        Text("Next")
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

@Composable
fun FeedbackDialog(
    feedbackText: String,
    onFeedbackChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Feedback") },
        text = {
            Column {
                Text("Enter your feedback:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = feedbackText,
                    onValueChange = onFeedbackChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSend) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
