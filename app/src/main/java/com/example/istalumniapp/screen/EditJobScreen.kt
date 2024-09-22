package com.example.istalumniapp.screen

import android.app.DatePickerDialog
import android.graphics.Paint.Align
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.JobType
import com.example.istalumniapp.utils.SharedViewModel
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
    var isUpdating by remember { mutableStateOf(false) } // Loader state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Spacer(modifier = Modifier.height(25.dp))
        // Add back button
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Update Job",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
            isError = title.isBlank(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            isError = description.isBlank(),
            maxLines = 4
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            isError = location.isBlank(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = salary,
            onValueChange = { salary = it },
            label = { Text("Salary") },
            modifier = Modifier.fillMaxWidth(),
            isError = salary.isBlank(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Company Name") },
            modifier = Modifier.fillMaxWidth(),
            isError = companyName.isBlank(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        DropdownMenuWithJobTypeSelection(
            selectedJobType = jobType,
            onJobTypeSelected = { jobType = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        DatePickerField(deadlineDate) {
            deadlineDate = it
        }
        Spacer(modifier = Modifier.height(16.dp))

        SkillsSelectionField(
            selectedSkills = selectedSkills,
            onSkillsSelected = { selectedSkills = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isUpdating = true // Show loader when button is clicked
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
                        isUpdating = false // Hide loader on success
                        navController.popBackStack()
                    },
                    onFailure = { error ->
                        isUpdating = false // Hide loader on failure
                        var errorMessage = "Failed to update job: $error"
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Update Job")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SkillsSelectionField(selectedSkills: List<String>, onSkillsSelected: (List<String>) -> Unit) {
    val allSkills = listOf("Kotlin", "Java", "Android", "Firebase")
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (selectedSkills.isEmpty()) "Select Skills" else selectedSkills.joinToString(
                    ", "
                )
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
