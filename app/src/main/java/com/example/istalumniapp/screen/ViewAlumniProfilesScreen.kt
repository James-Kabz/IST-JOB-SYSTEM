package com.example.istalumniapp.screen

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
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.ProfileViewModel

@Composable
fun ViewAlumniProfilesScreen(navController: NavController, profileViewModel: ProfileViewModel) {
    val alumniProfiles by profileViewModel.alumniProfiles.collectAsState(initial = emptyList())
    val loading by profileViewModel.loading.collectAsState(initial = true)
    val errorMessage by profileViewModel.errorMessage.collectAsState(initial = null)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        profileViewModel.retrieveAlumniProfiles(context = context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
            errorMessage != null -> ErrorMessage(errorMessage!!)
            alumniProfiles.isNotEmpty() -> AlumniProfilesList(profiles = alumniProfiles,navController = navController)
            else -> NoProfilesMessage()
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
fun AlumniProfileCard(profile: AlumniProfileData,navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        // Add back button
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
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
