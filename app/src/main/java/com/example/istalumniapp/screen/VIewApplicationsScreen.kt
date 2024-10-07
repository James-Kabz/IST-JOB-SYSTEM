package com.example.istalumniapp.screen

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import coil.request.ImageRequest
import com.example.istalumniapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

@SuppressLint("UnrememberedMutableState")
@Composable
fun ViewApplicationScreen(
    navController: NavController,
    applicationModel: JobApplicationModel,
    notificationViewModel: NotificationViewModel
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

    // Add state for CV download progress
    var showCVDownloadProgress by remember { mutableStateOf(false) }

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
            DashboardBottomBar(
                navController = navController,
                userRole = userRole,
                notificationViewModel = notificationViewModel
            )
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
            // If no applications are available, display a message
            val applications = applicationState.value
            if (applications.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No applications available for review.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Pagination Controls at the top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0
                        ) {
                            Text("Previous")
                        }

                        Text(
                            text = "Page ${currentPage + 1} of $totalPages",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Button(
                            onClick = { if (currentPage < totalPages - 1) currentPage++ },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Text("Next")
                        }
                    }

                    // Function to download the CV
                    suspend fun downloadCV(context: Context, cvUrl: String) {
                        showCVDownloadProgress = true // Show loading indicator
                        withContext(Dispatchers.IO) {
                            try {
                                val url = URL(cvUrl)
                                val connection = url.openConnection()
                                connection.connect()

                                // Get input stream from the URL
                                val input: InputStream = connection.getInputStream()
                                val file =
                                    File(context.getExternalFilesDir(null), "downloaded_cv.pdf")
                                val output = FileOutputStream(file)

                                // Write to the output file
                                input.copyTo(output)

                                // Close streams
                                output.flush()
                                output.close()
                                input.close()

                                // Notify the user
                                withContext(Dispatchers.Main) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("CV downloaded successfully: ${file.absolutePath}")
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Error downloading CV: ${e.message}")
                                    }
                                }
                            } finally {
                                showCVDownloadProgress = false // Hide loading indicator
                            }
                        }
                    }

                    // Display job applications for the current page
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val start = currentPage * itemsPerPage
                        val end = (start + itemsPerPage).coerceAtMost(applications.size)

                        items(applications.subList(start, end)) { application ->
                            var showFeedbackDialog by remember { mutableStateOf(false) }
                            var feedbackText by remember { mutableStateOf("") }
                            var showProgress by remember { mutableStateOf(false) }
                            val context = LocalContext.current
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

                                    val defaultPhoto =
                                        painterResource(id = R.drawable.dashboard_default) // Use the default "ist_logo.png"

                                    if (application.companyLogo.isNotEmpty()) {
                                        // If the company logo is available, load it
                                        val painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(LocalContext.current)
                                                .data(application.companyLogo)
                                                .apply { crossfade(true) }.build()
                                        )
                                        Image(
                                            painter = painter,
                                            contentDescription = "Company Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .align(Alignment.CenterHorizontally)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        // Display the default job logo (ist_logo.png) when companyLogo is null or empty
                                        Image(
                                            painter = defaultPhoto,
                                            contentDescription = "Default Job Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .align(Alignment.CenterHorizontally)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }

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

                                    // Add a button for downloading CV
                                    OutlinedButton(onClick = {
                                        if (application.cv.isNotEmpty()) {
                                            coroutineScope.launch {
                                                downloadCV(context, cvUrl = application.cv)
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("No CV available to download.")
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = "CV Download"
                                        )
                                        Text("CV")
                                    }

                                    if (showCVDownloadProgress) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CircularProgressIndicator()
                                    }

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

                                    }

                                    Row(
                                    ) {
                                        OutlinedButton(onClick = {
                                            showFeedbackDialog = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send Feedback"
                                            )
                                            Text("Feedback")
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
