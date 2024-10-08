package com.example.istalumniapp.screen


import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.JobType
import com.example.istalumniapp.utils.SharedViewModel
import com.example.istalumniapp.utils.SkillData
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AddJobScreen(navController: NavController, sharedViewModel: SharedViewModel) {
    // States for each field
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("") }
    var companyLogo by remember { mutableStateOf("") }

    var jobType by remember { mutableStateOf(JobType.FULL_TIME) }
    var expanded by remember { mutableStateOf(false) }

    var selectedSkills by remember { mutableStateOf(listOf<String>()) }
    var skills by remember { mutableStateOf(listOf<SkillData>()) }
    var skillsLoading by remember { mutableStateOf(false) }
    var skillsError by remember { mutableStateOf("") }
    var skillsExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Deadline date state
    var deadlineDate by remember { mutableStateOf<Date?>(null) }
    val deadlineDateFormatted =
        deadlineDate?.let { DateFormat.getDateInstance().format(it) } ?: "Select Deadline Date"

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            deadlineDate = calendar.time
        }, year, month, day
    )

    var currentStep by remember { mutableIntStateOf(1) }

    if (currentStep == 5 && skills.isEmpty() && !skillsLoading) {
        sharedViewModel.retrieveSkills(
            onLoading = { skillsLoading = it },
            onSuccess = { skills = it },
            onFailure = { skillsError = it }
        )
    }

    var errorMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Define the total steps
    val totalSteps = 5
    val progress = currentStep.toFloat() / totalSteps.toFloat()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Back Button
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "back_button"
                    )
                }
            }

            // Step Progress Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Step $currentStep of $totalSteps",
                    style = MaterialTheme.typography.bodyLarge
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Show Progress Indicator if submitting
            if (isSubmitting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Error Message Display
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = androidx.compose.ui.graphics.Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Steps Layout
            when (currentStep) {
                1 -> StepOne(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    onNext = {
                        if (title.isBlank() || description.isBlank()) {
                            errorMessage = "Please fill in all fields to proceed."
                        } else {
                            errorMessage = ""
                            currentStep = 2
                        }
                    }
                )

                2 -> StepTwo(
                    location = location,
                    onLocationChange = { location = it },
                    salary = salary,
                    onSalaryChange = { salary = it },
                    onPrevious = { currentStep = 1 },
                    onNext = {
                        if (location.isBlank() || salary.isBlank()) {
                            errorMessage = "Please fill in all fields to proceed."
                        } else {
                            errorMessage = ""
                            currentStep = 3
                        }
                    }
                )

                3 -> StepThree(
                    companyName = companyName,
                    onCompanyNameChange = { companyName = it },
                    jobType = jobType,
                    onJobTypeChange = { jobType = it },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    companyLogo = companyLogo,
                    onCompanyLogoChange = { companyLogo = it },
                    onPrevious = { currentStep = 2 },
                    onNext = {
                        if (companyName.isBlank()) {
                            errorMessage = "Please fill in all fields to proceed."
                        } else {
                            errorMessage = ""
                            currentStep = 4
                        }
                    }
                )

                4 -> StepFour(
                    experienceLevel = experienceLevel,
                    onExperienceLevelChange = { experienceLevel = it },
                    educationLevel = educationLevel,
                    onEducationLevelChange = { educationLevel = it },
                    onPrevious = { currentStep = 3 },
                    onNext = {
                        if (experienceLevel.isBlank() || educationLevel.isBlank()) {
                            errorMessage = "Please fill in all the fields to process."
                        } else {
                            errorMessage = ""
                            currentStep = 5
                        }
                    }
                )

                5 -> {
                    StepFive(
                        selectedSkills = selectedSkills,
                        onSkillSelected = { selectedSkills = selectedSkills + it },
                        onSkillDeselected = { selectedSkills = selectedSkills - it },
                        skills = skills,
                        expanded = skillsExpanded,
                        deadlineDateFormatted = deadlineDateFormatted,
                        onDeadlineDateClick = { datePickerDialog.show() },
                        onExpandedChange = { skillsExpanded = it },
                        onPrevious = { currentStep = 4 },
                        sharedViewModel = sharedViewModel,
                        onSubmit = {
                            if (selectedSkills.isEmpty()) {
                                errorMessage = "Please select at least one skill."
                            } else {
                                errorMessage = ""
                                isSubmitting = true
                                val jobData = JobData(
                                    jobID = UUID.randomUUID().toString(),
                                    title = title,
                                    description = description,
                                    location = location,
                                    salary = salary,
                                    companyName = companyName,
                                    jobType = jobType,
                                    experienceLevel = experienceLevel,
                                    educationLevel = educationLevel,
                                    companyLogo = companyLogo,
                                    skills = selectedSkills,
                                    deadlineDate = deadlineDate
                                )
                                sharedViewModel.saveJob(
                                    jobData = jobData,
                                    context = context,
                                    onJobSaved = { Unit },
                                    onError = { errorMessage = it }
                                )
                                isSubmitting = false
                                navController.navigate(Screens.DisplayJobScreen.route) {
                                    Toast.makeText(
                                        context,
                                        "Job Posted successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )


                }
            }
        }
    }
}


@Composable
fun StepOne(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding()
//            .background(MaterialTheme.colorScheme.primary)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val schoolLogo = R.drawable.ist_logo

        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        // Title
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = onTitleChange,
            label = { Text(text = "Title") },
            isError = title.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
        )
        // Description
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(text = "Description") },
            isError = description.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
        )
        // Next Button
        Button(
            modifier = Modifier.padding(top = 10.dp),
            onClick = onNext
        ) {
            Text(text = "Next")
        }
    }
}

@Composable
fun StepTwo(
    location: String,
    onLocationChange: (String) -> Unit,
    salary: String,
    onSalaryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val schoolLogo = R.drawable.ist_logo

        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        // Location
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = location,
            onValueChange = onLocationChange,
            label = { Text(text = "Location") },
            isError = location.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
        )
        // Salary
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = salary,
            onValueChange = onSalaryChange,
            label = { Text(text = "Salary") },
            isError = salary.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
        )
        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onPrevious) {
                Text(text = "Previous")
            }
            Button(onClick = onNext) {
                Text(text = "Next")
            }
        }
    }
}

@Composable
fun StepThree(
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    jobType: JobType,
    onJobTypeChange: (JobType) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    companyLogo: String,
    onCompanyLogoChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val schoolLogo = R.drawable.ist_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        // Company Name
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = companyName,
            onValueChange = onCompanyNameChange,
            label = { Text(text = "Company Name") },
            isError = companyName.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
        )
        // Job Type Dropdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(text = "Job Type")
            Button(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = jobType.name)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                JobType.entries.forEach { type ->
                    DropdownMenuItem(
                        onClick = {
                            onJobTypeChange(type)
                            onExpandedChange(false)
                        },
                        text = { Text(text = type.name) }
                    )
                }
            }
        }
        // Company Logo
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = companyLogo,
            onValueChange = onCompanyLogoChange,
            label = { Text(text = "Company Logo URL") },
            isError = companyLogo.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
        )
        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onPrevious) {
                Text(text = "Previous")
            }
            Button(onClick = onNext) {
                Text(text = "Next")
            }
        }
    }
}

@Composable
fun StepFour(
    experienceLevel: String,
    onExperienceLevelChange: (String) -> Unit,
    educationLevel: String,
    onEducationLevelChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val schoolLogo = R.drawable.ist_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )
        // Experience Level
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = experienceLevel,
            onValueChange = onExperienceLevelChange,
            label = { Text(text = "Experience Level") },
            isError = experienceLevel.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
        )
        // Education Level
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = educationLevel,
            onValueChange = onEducationLevelChange,
            label = { Text(text = "Education Level") },
            isError = educationLevel.isBlank(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
        )
        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onPrevious) {
                Text(text = "Previous")
            }
            Button(onClick = onNext) {
                Text(text = "Next")
            }
        }
    }
}

@Composable
fun StepFive(
    selectedSkills: List<String>,
    onSkillSelected: (String) -> Unit,
    onSkillDeselected: (String) -> Unit,
    skills: List<SkillData>,
    expanded: Boolean,
    deadlineDateFormatted: String,
    onDeadlineDateClick: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    sharedViewModel: SharedViewModel
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(
                start = 60.dp,
                end = 60.dp,
                bottom = 100.dp
            )  // Adjusted bottom padding for better layout
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val schoolLogo = R.drawable.ist_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )

        // Deadline Date Selector
        OutlinedTextField(
            value = deadlineDateFormatted,
            onValueChange = {},
            label = { Text("Deadline Date") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            trailingIcon = {
                IconButton(onClick = onDeadlineDateClick) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Deadline Date")
                }
            }
        )

        // Skill Selection
        SkilledSelection(
            selectedSkills = selectedSkills,
            onSkillSelected = onSkillSelected,
            onSkillDeselected = onSkillDeselected,
            skills = skills,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            saveSkill = { skillData ->
                sharedViewModel.saveSkill(
                    skillData,
                    context = context
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(25f)
                    .height(40.dp)
            ) {
                Text(text = "Previous")
            }
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(25f)
                    .height(40.dp)
            ) {
                Text(text = "Submit")
            }
        }
    }
}

@Composable
fun SkilledSelection(
    selectedSkills: List<String>,
    onSkillSelected: (String) -> Unit,    // Function to add skill
    onSkillDeselected: (String) -> Unit,  // Function to remove skill
    skills: List<SkillData>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    saveSkill: (SkillData) -> Unit        // Function to save new skill to Firestore
) {
    val context = LocalContext.current
    var newSkill by remember { mutableStateOf("") }
    val firestore = FirebaseFirestore.getInstance() // Firestore instance

    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = { onExpandedChange(true) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (selectedSkills.isEmpty()) "Select Skills" else selectedSkills.joinToString(", "))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            // Display existing skills from Firestore or list
            skills.forEach { skillData ->
                val skillName = skillData.skillName
                DropdownMenuItem(
                    onClick = {
                        if (selectedSkills.contains(skillName)) {
                            onSkillDeselected(skillName) // Deselect skill
                        } else {
                            onSkillSelected(skillName) // Select skill
                        }
                        onExpandedChange(false) // Close the menu
                    },
                    text = { Text(skillName) }
                )
            }

            // Add new skill input field
            DropdownMenuItem(
                onClick = { /* Do nothing */ },
                text = {
                    OutlinedTextField(
                        value = newSkill,
                        onValueChange = { newSkill = it },
                        label = { Text("Add New Skill") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            )

            // Add New Skill Button
            DropdownMenuItem(
                onClick = {
                    if (newSkill.isNotBlank()) {
                        // Check if skill exists in the list
                        val skillExists =
                            skills.any { it.skillName.equals(newSkill, ignoreCase = true) }

                        if (!skillExists && !selectedSkills.contains(newSkill)) {
                            // Save skill to Firebase Firestore
                            val newSkillID = UUID.randomUUID().toString() // Generate unique ID
                            val newSkillData = SkillData(skillID = newSkillID, skillName = newSkill)

                            // Add new skill to Firestore collection
                            firestore.collection("skills")
                                .document(newSkillID) // Use the generated UUID as the document ID
                                .set(newSkillData)
                                .addOnSuccessListener {
                                    // Skill successfully added to Firestore
                                    onSkillSelected(newSkill) // Add the new skill locally
                                    newSkill = "" // Reset the input field
                                    Toast.makeText(
                                        context,
                                        "Skill added successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    // Handle Firestore error
                                    Toast.makeText(
                                        context,
                                        "Failed to add skill: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(context, "Skill already exists", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    onExpandedChange(false) // Close the dropdown menu
                },
                text = { Text("Add Skill") }
            )
        }
    }
}











