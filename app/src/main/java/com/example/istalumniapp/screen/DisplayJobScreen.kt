package com.example.istalumniapp.screen

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId


@Composable
fun DisplayJobScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    profileViewModel: ProfileViewModel
) {
    var jobs by remember { mutableStateOf<List<JobData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    val loading = remember { mutableStateOf(true) }

    // Fetch user role
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "alumni"
        }

        profileViewModel.retrieveProfilePhoto(
            onLoading = { loading.value = it },
            onSuccess = { url -> profilePhotoUrl = url },
            onFailure = { message -> Log.e("DisplayJobScreen", "Error fetching profile photo: $message") }
        )
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
                DashboardTopBar(
                    navController = navController,
                    onLogoutClick = { showLogoutConfirmation = true },
                    userRole = userRole,
                    profilePhotoUrl = profilePhotoUrl
                )
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
                        // Conditionally show the "Add Job" and "Add Skill" buttons if the user is an admin
                        if (userRole == "admin") {
                            Row(
                            ) {
                                Button(onClick = { navController.navigate(Screens.AddJobScreen.route) }) {
                                    Text(text = "Add Job")
                                }

                                Button(onClick = { navController.navigate(Screens.AddSkillScreen.route) }) {
                                    Text(text = "Add Skill")
                                }
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
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            items(jobs) { job ->
                                // Conditionally show the "Add Job" and "Add Skill" buttons if the user is an admin
                                if (userRole == "admin") {
                                    Row(
                                    horizontalArrangement = Arrangement.Center, // Center the Row's content
                                    modifier = Modifier
                                        .fillMaxWidth() // Ensure the Row takes up the full width
                                ) {
                                    Button(onClick = { navController.navigate(Screens.AddJobScreen.route) }) {
                                        Text(text = "Add Job")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp)) // Space between buttons
                                    Button(onClick = { navController.navigate(Screens.AddSkillScreen.route) }) {
                                        Text(text = "Add Skill")
                                    }
                                }
                                    Spacer(modifier = Modifier.height(16.dp)) // Space below the buttons
                                }
                                // Pass the userRole to the JobItem composable
                                JobItem(job = job, userRole = userRole ?: "", navController = navController)
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


@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun JobItem(job: JobData, userRole: String, navController: NavController) {
    val currentDate = LocalDate.now() // Get the current date
    val deadlineDate = job.deadlineDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

    // Debugging: Log the currentDate and deadlineDate values
    Log.d("JobItem", "Current Date: $currentDate, Deadline Date: $deadlineDate")

    val isBeforeDeadline = deadlineDate != null && !currentDate.isAfter(deadlineDate)

    // Log the result of the condition
    Log.d("JobItem", "Is Before Deadline: $isBeforeDeadline")

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
            modifier = Modifier.padding(10.dp)
        ) {
            // Company Logo
            if (job.companyLogo.isNotEmpty()) {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(data = job.companyLogo)
                        .apply(block = fun ImageRequest.Builder.() {
                            crossfade(true)
                        }).build()
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

            Text(
                text = "Deadline: ${deadlineDate?.toString() ?: "N/A"}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                    modifier = Modifier.fillMaxSize()
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
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navController.navigate(Screens.AddJobScreen.route)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deadlineDate != null && !currentDate.isAfter(deadlineDate))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        contentColor = if (deadlineDate != null && !currentDate.isAfter(deadlineDate))
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    enabled = deadlineDate != null && !currentDate.isAfter(deadlineDate) // Disable button if deadline has passed
                ) {
                    Text(text = if (deadlineDate != null && !currentDate.isAfter(deadlineDate)) "Apply" else "Deadline Passed")
                }


            }
        }
    }
}