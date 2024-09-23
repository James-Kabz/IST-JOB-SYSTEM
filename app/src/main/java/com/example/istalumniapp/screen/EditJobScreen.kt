package com.example.istalumniapp.screen

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.JobType
import com.example.istalumniapp.utils.SharedViewModel
import com.example.istalumniapp.utils.SkillData
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun EditJobScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    jobID: String
) {
    var jobData by remember { mutableStateOf<JobData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(jobID) {
        sharedViewModel.getJobByID(jobID,
            onSuccess = { fetchedJob ->
                jobData = fetchedJob
                isLoading = false
            },
            onFailure = { error ->
                errorMessage = "Failed to fetch job data: $error"
                isLoading = false
            }
        )
    }

    if (isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
    } else if (jobData != null) {
        JobEditForm(
            jobData = jobData!!,
            navController = navController,
            sharedViewModel = sharedViewModel
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun JobEditForm(
    jobData: JobData,
    navController: NavController,
    sharedViewModel: SharedViewModel
) {
    var title by remember { mutableStateOf(jobData.title) }
    var description by remember { mutableStateOf(jobData.description) }
    var location by remember { mutableStateOf(jobData.location) }
    var salary by remember { mutableStateOf(jobData.salary) }
    var companyName by remember { mutableStateOf(jobData.companyName) }
    var experienceLevel by remember { mutableStateOf(jobData.experienceLevel) }
    var educationLevel by remember { mutableStateOf(jobData.educationLevel) }
    var jobType by remember { mutableStateOf(jobData.jobType) }
    var selectedSkills by remember { mutableStateOf(jobData.skills) }
    var deadlineDate by remember { mutableStateOf(jobData.deadlineDate) }
    var isUpdating by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Define a background color
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant // Adjust based on your theme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),  // Make the form scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Edit Job",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                maxLines = 4
            )

            // Location Field
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Salary Field
            OutlinedTextField(
                value = salary,
                onValueChange = { salary = it },
                label = { Text("Salary") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Skills Selection
            SkillsSelectionField(
                selectedSkills = selectedSkills,
                onSkillsSelected = { selectedSkills = it },
                saveSkill = { skill ->
                    sharedViewModel.saveSkill(skill, context)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Experience Level Field
            OutlinedTextField(
                value = experienceLevel,
                onValueChange = { experienceLevel = it },
                label = { Text("Experience Level") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Education Level Field
            OutlinedTextField(
                value = educationLevel,
                onValueChange = { educationLevel = it },
                label = { Text("Education Level") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            // Job Type Dropdown
            DropdownMenuWithJobTypeSelection(
                selectedJobType = jobType,
                onJobTypeSelected = { jobType = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deadline Date Picker
            DatePickerField(deadlineDate) {
                deadlineDate = it
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Update Job Button
            Button(
                onClick = {
                    isUpdating = true
                    val updatedJob = jobData.copy(
                        title = title,
                        description = description,
                        location = location,
                        salary = salary,
                        companyName = companyName,
                        experienceLevel = experienceLevel,
                        educationLevel = educationLevel,
                        jobType = jobType,
                        skills = selectedSkills,
                        deadlineDate = deadlineDate
                    )

                    sharedViewModel.editJob(
                        jobID = jobData.jobID,
                        updatedJob = updatedJob,
                        onSuccess = {
                            isUpdating = false
                            navController.popBackStack()
                        },
                        onFailure = { error ->
                            isUpdating = false
                            Toast.makeText(context, "Failed to update job: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Updating Job, please wait...",
                        color = colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Update Job")
                }
            }
        }
    }
}


@Composable
fun SkillsSelectionField(
    selectedSkills: List<String>,
    onSkillsSelected: (List<String>) -> Unit,
    saveSkill: (SkillData) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var newSkill by remember { mutableStateOf("") }
    val allSkills = remember { mutableStateListOf("Kotlin", "Java", "Android", "Firebase") }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (selectedSkills.isEmpty()) "Select Skills" else selectedSkills.joinToString(", "))
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
            DropdownMenuItem(
                onClick = { /* Do nothing */ },
                text = {
                    OutlinedTextField(
                        value = newSkill,
                        onValueChange = { newSkill = it },
                        label = { Text("Add New Skill") }
                    )
                }
            )
            DropdownMenuItem(
                onClick = {
                    if (newSkill.isNotBlank() && !selectedSkills.contains(newSkill)) {
                        val skillData = SkillData(skillID = newSkill, skillName = newSkill) // Create new SkillData
                        saveSkill(skillData) // Save new skill to Firebase
                        onSkillsSelected(selectedSkills + newSkill) // Update UI
                        newSkill = "" // Reset the input field
                    }
                    expanded = false
                },
                text = { Text("Add Skill") }
            )
        }
    }
}



@Composable
fun DropdownMenuWithJobTypeSelection(
    selectedJobType: JobType,
    onJobTypeSelected: (JobType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(text = selectedJobType.name)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            JobType.entries.forEach { jobType ->
                DropdownMenuItem(
                    onClick = {
                        onJobTypeSelected(jobType)
                        expanded = false
                    },
                    text = {
                        Text(text = jobType.name)
                    }
                )
            }
        }
    }
}

@Composable
fun DatePickerField(deadlineDate: Date?, onDateSelected: (Date?) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val formattedDate = deadlineDate?.let {
        DateFormat.getDateInstance().format(it)
    } ?: "Select Deadline Date"

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        label = { Text("Deadline Date") },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(Icons.Default.DateRange, contentDescription = "Select Deadline Date")
            }
        }
    )
}
