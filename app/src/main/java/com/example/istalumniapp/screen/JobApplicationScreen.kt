package com.example.istalumniapp.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.*

@Composable
fun JobApplicationScreen(
    navController: NavController,
    jobID: String,  // Automatically passed from navigation
    jobApplicationModel: JobApplicationModel
) {
    val context = LocalContext.current
    val userId =
        getInstance().currentUser?.uid ?: ""  // Get the userID from Firebase Authentication
    val backgroundColor = MaterialTheme.colorScheme.background

    // UI State
    var experience by remember { mutableStateOf(Experience.One_Year) }
    var gender by remember { mutableStateOf(Gender.Male) }
    var linkedIn by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cvUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }  // New loading state

    // File picker for CV
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        cvUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "Submitting Application...",
                color = colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Job Application Form", style = MaterialTheme.typography.headlineMedium)

                // Experience Dropdown
                var expandedExperience by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedExperience = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Experience: ${experience.years} Years")
                    }
                    DropdownMenu(
                        expanded = expandedExperience,
                        onDismissRequest = { expandedExperience = false }
                    ) {
                        Experience.entries.forEach { exp ->
                            DropdownMenuItem(onClick = {
                                experience = exp
                                expandedExperience = false
                            },
                                text = { Text("${exp.years} Years") })
                        }
                    }
                }

                // Gender Dropdown
                var expandedGender by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedGender = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gender: ${gender.name}")
                    }
                    DropdownMenu(
                        expanded = expandedGender,
                        onDismissRequest = { expandedGender = false }
                    ) {
                        Gender.entries.forEach { g ->
                            DropdownMenuItem(onClick = {
                                gender = g
                                expandedGender = false
                            },
                                text = { Text(g.name) }
                            )
                        }
                    }
                }

                // LinkedIn Input
                TextField(
                    value = linkedIn,
                    onValueChange = { linkedIn = it },
                    label = { Text("LinkedIn Profile") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                )

                // Education Input
                TextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Education") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                )

                // Address Input
                TextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                )

                // Phone Input
                TextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // CV Upload Button
                Button(
                    onClick = { launcher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (cvUri != null) "CV Selected" else "Upload CV (PDF)")
                }

                // Submit Button
                Button(
                    onClick = {
                        isLoading = true // Start loading
                        // Create a JobApplicationData object
                        val applicationData = JobApplicationData(
                            jobID = jobID,       // Automatically use passed jobID
                            userId = userId,     // Automatically use current user's ID
                            experience = experience,
                            gender = gender,
                            linkedIn = linkedIn,
                            education = education,
                            address = address,
                            phone = phone,
                            cv = ""  // Initially empty, will be filled if a CV is uploaded
                        )

                        // Save the job application using the ViewModel
                        jobApplicationModel.saveApplication(
                            jobID,
                            userId,
                            applicationData,
                            cvUri
                        ) { success, message ->
                            isLoading = false // Stop loading
                            if (success) {
                                Toast.makeText(
                                    context,
                                    "Application submitted successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("${Screens.DisplayApplicationScreen.route}/{userId}") {
                                    Toast.makeText(
                                        context,
                                        "Application submitted successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }// Go back to the previous screen
                            } else {
                                Toast.makeText(context, "Error: $message", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading // Disable button while loading
                ) {
                    Text(text = if (isLoading) "Submitting..." else "Submit Application")
                }
            }
        }
    }
}
