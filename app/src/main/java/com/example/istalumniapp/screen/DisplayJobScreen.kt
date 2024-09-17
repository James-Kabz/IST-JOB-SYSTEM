package com.example.istalumniapp.screen

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


@Composable
fun DisplayJobScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel
) {
    var jobs by remember { mutableStateOf<List<JobData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }

    // Fetch user role
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "alumni"
        }
    }

    // Fetch jobs
    LaunchedEffect(Unit) {
        sharedViewModel.retrieveJobs(
            onLoading = { loading -> isLoading = loading },
            onSuccess = { retrievedJobs -> jobs = retrievedJobs },
            onFailure = { message -> errorMessage = message }
        )
    }

    // Show logout confirmation dialog
    if (showLogoutConfirmation) {
        LogoutConfirm(
            onConfirm = {
                FirebaseAuth.getInstance().signOut() // Log out the user
                navController.navigate(Screens.ISTLoginScreen.route) // Navigate to login screen
                showLogoutConfirmation = false
            },
            onDismiss = { showLogoutConfirmation = false }
        )
    }

    // If loading, show only the CircularProgressIndicator
    if (isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please wait...",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        // Once jobs are loaded, show the full UI
        Scaffold(
            topBar = {
                DashboardTopBar(navController = navController, onLogoutClick = { showLogoutConfirmation = true } , userRole = userRole )
            },
            bottomBar = {
                DashboardBottomBar(navController = navController, userRole = userRole)
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                when {
                    errorMessage != null -> {


                        // Show error message if fetching jobs fails
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    jobs.isEmpty() -> {
                        // Show message if no jobs are available
                        Row() {
                            Button(onClick = {navController.navigate(Screens.AddJobScreen.route)}) {
                                Text(text = "Add Job")
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Button(onClick = {navController.navigate(Screens.AddSkillScreen.route)}) {
                                Text(text = "Add Skill")
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No jobs available at the moment.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    else -> {
                        // Display the list of jobs once they are loaded
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(5.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(jobs) { job ->
                                JobItem(job = job)
                            }
                        }
                    }
                }
            }
        }
    }
}




@Composable
fun LogoutConfirm(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Logout") },
        text = { Text("Are you sure you want to logout?") }
    )
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun JobItem(job: JobData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Company Logo
            if (job.companyLogo.isNotEmpty()) {
                val painter = rememberImagePainter(
                    data = job.companyLogo,
                    builder = {
                        crossfade(true)
                    }
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Job Title
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Description Title
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Description Content
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Location
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = job.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Salary
            Text(
                text = "Salary",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = job.salary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Job Type
            Text(
                text = "Job Type",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${job.jobType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Experience Level
            Text(
                text = "Experience Level",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = job.experienceLevel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Education Level
            Text(
                text = "Education Level",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = job.educationLevel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Skills Section
            if (job.skills.isNotEmpty()) {
                Text(
                    text = "Skills Required:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    job.skills.forEach { skill ->
                        Text(
                            text = skill,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Apply Button
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        // Handle Apply Now action
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = "Apply Now")
                }
            }
        }
    }
}
