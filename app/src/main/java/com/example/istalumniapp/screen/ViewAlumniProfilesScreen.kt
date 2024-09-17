package com.example.istalumniapp.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale

@Composable
fun ViewAlumniProfilesScreen(navController: NavController, profileViewModel: ProfileViewModel) {
    val alumniProfiles = remember { mutableStateOf<List<AlumniProfileData>?>(null) }
    val loading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // Use LaunchedEffect to trigger the retrieval when the composable enters composition
    LaunchedEffect(Unit) {
        profileViewModel.retrieveAlumniProfiles(
            onLoading = { loading.value = it },
            onSuccess = { profiles ->
                alumniProfiles.value = profiles
            },
            onFailure = { error ->
                errorMessage.value = error
            }
        )
    }

    // UI Layout for displaying profiles or status
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading.value -> {
                // Loading indicator
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
            errorMessage.value != null -> {
                // Display error message
                Text(
                    text = "Error: ${errorMessage.value}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            alumniProfiles.value != null -> {
                // Display the list of alumni profiles
                AlumniProfilesList(profiles = alumniProfiles.value!!)
            }
            else -> {
                Text(
                    text = "No alumni profiles available.",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AlumniProfilesList(profiles: List<AlumniProfileData>) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        profiles.forEach { profile ->
            AlumniProfileCard(profile)
        }
    }
}

@Composable
fun AlumniProfileCard(profile: AlumniProfileData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            profile.profilePhotoUri?.let {
                ProfilePhoto(profilePhotoUri = it)
            }
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Admin Settings",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = profile.location,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SectionHeader("Skills")
            SkillsSector(profile.skills)

        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfilePhoto(profilePhotoUri: String) {
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
