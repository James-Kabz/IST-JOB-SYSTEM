package com.example.istalumniapp.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.R
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel

@Composable
fun ViewProfileScreen(navController: NavController, profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val profileData = remember { mutableStateOf<AlumniProfileData?>(null) }
    val loading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        profileViewModel.retrieveCurrentUserProfile(
            onLoading = { loading.value = it },
            onSuccess = { profile ->
                profileData.value = profile
                if (profile != null) {
                    Log.d("ViewProfileScreen", "Retrieved profile photo URI: ${profile.profilePhotoUri}")
                }
            },
            onFailure = { error ->
                errorMessage.value = error
                Log.e("ViewProfileScreen", "Error retrieving profile: $error")
            }
        )
    }

    // Main Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading.value -> {
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
                Text(
                    text = "Error: ${errorMessage.value}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                profileData.value?.let { profile ->
                    ProfileDetails(profile)
                } ?: run {
                    Text(
                        text = "No profile data available.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetails(profile: AlumniProfileData) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            ProfileImage(profile.profilePhotoUri ?: "")
        }

        // Basic Info
        Text(
            text = profile.fullName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = profile.currentJob,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = profile.location, style = MaterialTheme.typography.bodyMedium)
        }

        // Section: Contact Info
        HorizontalDivider()
        SectionHeader("Contact Information")
        ContactInfoSection(profile)

        // Section: Education
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        SectionHeader("Education")
        EducationSection(profile)

        // Section: Skills
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        SectionHeader("Skills")
        SkillsSection(profile.skills)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(vertical = 8.dp)
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
            InfoRow(Icons.Default.Info, "LinkedIn", profile.linkedIn)
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
fun SkillsSection(skills: List<String>) {
    if (skills.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            skills.forEach { skill ->
                Text(
                    text = skill,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
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
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileImage(profile: String) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(profile.ifBlank { R.drawable.placeholder })
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
            .size(120.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    )
}