package com.example.istalumniapp.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ViewAlumniProfilesScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel
) {
    val alumniProfiles by profileViewModel.alumniProfiles.collectAsState(initial = emptyList())
    val loading by profileViewModel.loading.collectAsState(initial = true)
    val errorMessage by profileViewModel.errorMessage.collectAsState(initial = null)
    val context = LocalContext.current

    // State variables for the dashboard
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    val loadingProfile = remember { mutableStateOf(true) }

    // Pagination state
    var currentPage by remember { mutableStateOf(0) } // Track the current page (starts at 0)
    val itemsPerPage = 5 // Number of items to show per page

    // Fetch user role and profile photo
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "alumni"
        }

    }

    // Fetch alumni profiles
    LaunchedEffect(Unit) {
        profileViewModel.retrieveAlumniProfiles(context = context)
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

    // Calculate the total number of pages based on profiles size and items per page
    val totalPages = (alumniProfiles.size + itemsPerPage - 1) / itemsPerPage

    // Main Scaffold with the TopBar and BottomBar
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp)) // Add space between the spinner and text
                        Text(
                            text = "Retrieving Profiles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                errorMessage != null -> ErrorMessage(errorMessage!!)
                alumniProfiles.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Pagination Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { if (currentPage > 0) currentPage -= 1 },
                                enabled = currentPage > 0
                            ) {
                                Text(text = "Previous")
                            }

                            Text(
                                text = "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )

                            Button(
                                onClick = { if (currentPage < totalPages - 1) currentPage += 1 },
                                enabled = currentPage < totalPages - 1
                            ) {
                                Text(text = "Next")
                            }
                        }
                        // Display the list of alumni profiles for the current page
                        AlumniProfilesList(
                            profiles = alumniProfiles.subList(
                                currentPage * itemsPerPage,
                                (currentPage * itemsPerPage + itemsPerPage).coerceAtMost(
                                    alumniProfiles.size
                                )
                            ),
                            navController = navController
                        )


                    }

                }

                else -> NoProfilesMessage()
            }

        }
    }
}


@Composable
fun ErrorMessage(message: String) {
    Text(
        text = "Error: $message",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun NoProfilesMessage() {
    Text(
        text = "No alumni profiles available.",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun AlumniProfilesList(profiles: List<AlumniProfileData>, navController: NavController) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        profiles.forEach { profile ->
            AlumniProfileCard(profile = profile, navController = navController)
        }
    }
}


@Composable
fun AlumniProfileCard(profile: AlumniProfileData, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            profile.profilePhotoUri?.let { ProfilePic(profilePhotoUri = it) }
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = profile.location,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SkillsSector(profile.skills)

        }
    }
}


@Composable
fun SkillsSector(skills: List<String>) {
    if (skills.isNotEmpty()) {
        // Join the skills with commas and display in a single text component
        val skillsText = skills.joinToString(separator = ", ")

        Text(
            text = skillsText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp) // Add padding for better spacing
        )
    } else {
        // Display message when there are no skills
        Text(
            text = "No skills listed.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfilePic(profilePhotoUri: String) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(profilePhotoUri)
            .apply {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(R.drawable.error)
            }
            .build()
    )

    Image(
        painter = painter,
        contentDescription = "Profile Photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    )
}
