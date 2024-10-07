package com.example.istalumniapp.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.ProfileViewModel


@Composable
fun ViewProfileScreen(navController: NavController, profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val profileData = remember { mutableStateOf<AlumniProfileData?>(null) }
    val profilePhotoUrl = remember { mutableStateOf<String?>(null) }
    val loading = remember { mutableStateOf(true) }
    val isProfileLoaded = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // Fetch the profile photo separately
    LaunchedEffect(Unit) {
        profileViewModel.retrieveProfilePhoto(
            onLoading = { loading.value = it },
            onSuccess = { url ->
                profilePhotoUrl.value = url
                Log.d("ViewProfileScreen", "Retrieved profile photo URL: $url")
            },
            onFailure = { error ->
                errorMessage.value = error
                Log.e("ViewProfileScreen", "Error retrieving profile photo: $error")
            }
        )
    }

    // Fetch the profile data
    LaunchedEffect(Unit) {
        profileViewModel.retrieveCurrentUserProfile(
            context = context,
            onLoading = { loading.value = it },
            onSuccess = { profile ->
                profileData.value = profile
                isProfileLoaded.value = true
                if (profile != null) {
                    Log.d("ViewProfileScreen", "Retrieved profile data")
                }
            },
            onFailure = { error ->
                errorMessage.value = error
                isProfileLoaded.value = true
                Log.e("ViewProfileScreen", "Error retrieving profile: $error")
            }
        )
    }

    // Main Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, top = 30.dp)
    ) {
        // Add back button
        IconButton(onClick = { navController.navigate(Screens.DashboardScreen.route) }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading.value -> {
                    // Show loading indicator
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
                }
                errorMessage.value != null -> {
                    // Show error message
                    Text(
                        text = "Error: ${errorMessage.value}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                isProfileLoaded.value && profileData.value == null -> {
                    // Show "No profile data available" only after loading is complete
                    Text(
                        text = "No profile data available.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                else -> {





                    // Show the profile details
                    profileData.value?.let { profile ->
                        ProfileDetails(profile, profilePhotoUrl.value,navController)
                    }
                }
            }
        }
    }
}


@Composable
fun ProfileDetails(profile: AlumniProfileData, profilePhotoUrl: String?, navController: NavController) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile header: Photo, Name, and Institute
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Profile image
            ProfileImage(profile.profilePhotoUri ?: "", modifier = Modifier.padding(end = 50.dp))

//            Spacer(modifier = Modifier.padding(end = 40.dp))

            // Profile name and details
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = profile.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "Open to work" section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                ) {
                    Text(
                        text = "Current Job: ${profile.currentJob}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons for editing and adding sections
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                navController.navigate(Screens.EditProfileScreen.route)
            }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = { /* Handle Add Section */ }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Section")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Section")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile details (Contact Info, Education, Skills)
        HorizontalDivider()

        // Contact Information
        SectionHeader("Contact Information")
        ContactInfoSection(profile)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Education Section
        SectionHeader("Education")
        EducationSection(profile)

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Skills Section
        SectionHeader("Skills")
        SkillsSection(profile.skills)
    }
}


@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ContactInfoSection(profile: AlumniProfileData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (profile.email.isNotBlank()) {
            InfoRow(Icons.Default.Email, "Email", profile.email)
        }
        if (profile.phone.isNotBlank()) {
            InfoRow(Icons.Default.Phone, "Phone", profile.phone)
        }
        if (profile.linkedIn.isNotBlank()) {
            LinkedInRow(profile.linkedIn)
        }
    }
}

@Composable
fun EducationSection(profile: AlumniProfileData) {
    Column(modifier = Modifier.fillMaxWidth()) {
        InfoRow(
            Icons.Default.Star,
            "Degree",
            profile.degree.name.replace("_", " ") + (profile.customDegree?.let { " ($it)" } ?: "")
        )
        InfoRow(Icons.Default.DateRange, "Graduation Year", profile.graduationYear)
        if (profile.extraCourse.isNotBlank()) {
            InfoRow(Icons.Default.Add, "Extra Course", profile.extraCourse)
        }
    }
}


@Composable
fun LinkedInRow(linkedInUrl: String) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .clickable {
                // Open LinkedIn URL in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedInUrl))
                context.startActivity(intent)
            }
    ) {
        // Replace with your LinkedIn icon drawable
        Icon(
            painter = painterResource(id = R.drawable.icons8_linkedin_48), // Your LinkedIn icon
            contentDescription = "LinkedIn",
            tint = Color.Unspecified, // Make sure the icon shows original colors
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "LinkedIn",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
//            Text(
//                text = linkedInUrl,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.primary // You can also make the text appear clickable
//            )
        }
    }
}

@Composable
fun SkillsSection(skills: List<String>) {
    if (skills.isNotEmpty()) {
        Text(
            text = skills.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp),
            color = Color.White
        )
    } else {
        Text(
            text = "No skills listed.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}


@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun ProfileImage(profileUrl: String,modifier: Modifier = Modifier) {
    // Load profile image with Coil, and handle empty URL case efficiently
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(profileUrl.ifBlank { R.drawable.placeholder }) // Handle empty URL properly
            .crossfade(true)
//            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .build()
    )

    Image(
        painter = painter,
        contentDescription = "Profile Photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    )
}
