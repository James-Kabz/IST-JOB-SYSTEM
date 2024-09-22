package com.example.istalumniapp.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SkillData

@Composable
fun EditProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    saveSkill: (SkillData, Context) -> Unit
) {
    val context = LocalContext.current
    var profileData by remember { mutableStateOf(AlumniProfileData()) }
    var newProfilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            newProfilePhotoUri = uri
        }
    )

    LaunchedEffect(Unit) {
        profileViewModel.retrieveCurrentUserProfile(
            context = context,
            onLoading = { loading = it },
            onSuccess = { profile ->
                profileData = profile ?: AlumniProfileData() // Pre-fill all data
            },
            onFailure = { errorMessage ->
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()  // Show loading indicator while waiting
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            // Profile Photo Section
            if (newProfilePhotoUri != null) {
                AsyncImage(
                    model = newProfilePhotoUri,
                    contentDescription = "New Profile Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp)
                        .clip(CircleShape)
                )
            } else if (profileData.profilePhotoUri != null) {
                AsyncImage(
                    model = profileData.profilePhotoUri,
                    contentDescription = "Profile Photo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Photo")
                }
            }

            // Upload New Profile Photo Button
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Upload New Photo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fill Out Other Fields
            OutlinedTextField(
                value = profileData.fullName,
                onValueChange = { profileData = profileData.copy(fullName = it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            OutlinedTextField(
                value = profileData.email,
                onValueChange = { profileData = profileData.copy(email = it) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            OutlinedTextField(
                value = profileData.graduationYear,
                onValueChange = { profileData = profileData.copy(graduationYear = it) },
                label = { Text("Graduation Year") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileData.customDegree ?: "",
                onValueChange = { profileData = profileData.copy(customDegree = it) },
                label = { Text("Custom Degree") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileData.currentJob,
                onValueChange = { profileData = profileData.copy(currentJob = it) },
                label = { Text("Current Job") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileData.currentEmployee,
                onValueChange = { profileData = profileData.copy(currentEmployee = it) },
                label = { Text("Current Employer") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileData.location,
                onValueChange = { profileData = profileData.copy(location = it) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileData.phone,
                onValueChange = { profileData = profileData.copy(phone = it) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profileData.linkedIn,
                onValueChange = { profileData = profileData.copy(linkedIn = it) },
                label = { Text("LinkedIn") },
                modifier = Modifier.fillMaxWidth()
            )

            // Skills Selection Field
            SkillsSelection(
                selectedSkills = profileData.skills,
                onSkillsSelected = { updatedSkills ->
                    profileData = profileData.copy(skills = updatedSkills)
                },
                saveSkill = { skillName ->
                    val newSkill = SkillData(skillID = skillName, skillName = skillName) // Assuming SkillData has these fields
                    saveSkill(newSkill, context)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Update Profile Button
            Button(onClick = {
                profileViewModel.updateAlumniProfile(
                    updatedProfileData = profileData,
                    newProfilePhotoUri = newProfilePhotoUri,
                    context = context,
                    onLoading = { loading = it },
                    onComplete = {
                        navController.navigate(Screens.ViewProfileScreen.route)
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                )
            }) {
                Text("Update Profile")
            }
        }
    }
}
@Composable
fun SkillsSelection(
    selectedSkills: List<String>,
    onSkillsSelected: (List<String>) -> Unit,
    saveSkill: (String) -> Unit // Accept the saveSkill function
) {
    val allSkills = remember { mutableStateListOf("Kotlin", "Java", "Android", "Firebase") }
    var expanded by remember { mutableStateOf(false) }
    var newSkill by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (selectedSkills.isEmpty()) "Select Skills" else selectedSkills.joinToString(", ")
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allSkills.forEach { skill ->
                DropdownMenuItem(
                    onClick = {
                        val newSkills = if (selectedSkills.contains(skill)) {
                            selectedSkills - skill
                        } else {
                            selectedSkills + skill
                        }
                        onSkillsSelected(newSkills)
                        expanded = false
                    },
                    text = { Text(skill) }
                )
            }
            // Add a text field for adding new skills
            DropdownMenuItem(
                onClick = { /* Do nothing */ },
                text = {
                    OutlinedTextField(
                        value = newSkill,
                        onValueChange = { newSkill = it },
                        label = { Text("Add New Skill") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
            DropdownMenuItem(
                onClick = {
                    if (newSkill.isNotBlank() && !selectedSkills.contains(newSkill)) {
                        onSkillsSelected(selectedSkills + newSkill)
                        saveSkill(newSkill) // Call saveSkill to save the new skill
                        newSkill = ""
                    }
                    expanded = false
                },
                text = {
                    Text(
                        text = "Add Skill",
                        color = MaterialTheme.colorScheme.primary // Set your desired color here
                    )
                }
            )



        }
    }
}
