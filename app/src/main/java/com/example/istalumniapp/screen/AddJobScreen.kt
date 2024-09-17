package com.example.istalumniapp.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import java.util.UUID

@Composable
fun AddJobScreen(navController: NavController, sharedViewModel: SharedViewModel) {
    // States for each field
//    val jobID by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("") }
    var companyLogo by remember { mutableStateOf("") }

    var jobType by remember { mutableStateOf(JobType.FULL_TIME) }
    var expanded by remember { mutableStateOf(false) }  // For dropdown menu

    var selectedSkills by remember { mutableStateOf(listOf<String>()) }
    var skills by remember { mutableStateOf(listOf<SkillData>()) }
    var skillsLoading by remember { mutableStateOf(false) }
    var skillsError by remember { mutableStateOf("") }
    var skillsExpanded by remember { mutableStateOf(false) }


    val context = LocalContext.current

    // State to manage current step
    var currentStep by remember { mutableIntStateOf(1) }

    // Load skills on entering step 5
    if (currentStep == 5 && skills.isEmpty() && !skillsLoading) {
        sharedViewModel.retrieveSkills(
            onLoading = { skillsLoading = it },
            onSuccess = { skills = it },
            onFailure = { skillsError = it }
        )
    }
    // Error state for validation
    var errorMessage by remember { mutableStateOf("") }

    // Main Layout
    Column(modifier = Modifier.fillMaxSize()) {
        // Back Button
        Row(
            modifier = Modifier
                .padding(start = 15.dp, top = 55.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back_button")
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
                    if ( title.isBlank() || description.isBlank()) {
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
                    if (companyName.isBlank() || companyLogo.isBlank()) {
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
                        errorMessage = "Please fill in all the fields to process"
                    } else{
                        errorMessage = ""
                        currentStep = 5
                    }
                }
            )
            5 -> StepFive(
                selectedSkills = selectedSkills,
                onSkillSelected = { selectedSkills = selectedSkills + it },
                onSkillDeselected = { selectedSkills = selectedSkills - it },
                skills = skills,
                expanded = skillsExpanded,
                onExpandedChange = { skillsExpanded = it },
                onPrevious = { currentStep = 4 },
                onSubmit = {
                    if (selectedSkills.isEmpty()) {
                        errorMessage = "Please select at least one skill."
                    } else {
                        errorMessage = ""
                        // Save job with selected skills
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
                            skills = selectedSkills // Add selected skills to job data
                        )
                        sharedViewModel.saveJob(jobData = jobData, context = context)
                        navController.popBackStack() // Go back after submitting
                    }
                }
            )

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
        Text(text = "Step 1", fontSize = 24.sp)
        // Title
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = onTitleChange,
            label = { Text(text = "Title") },
            isError = title.isBlank()
        )
        // Description
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(text = "Description") },
            isError = description.isBlank()
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
        Text(text = "Step 2", fontSize = 24.sp)
        // Location
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = location,
            onValueChange = onLocationChange,
            label = { Text(text = "Location") },
            isError = location.isBlank()
        )
        // Salary
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = salary,
            onValueChange = onSalaryChange,
            label = { Text(text = "Salary") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = salary.isBlank()
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
        Text(text = "Step 3", fontSize = 24.sp)
        // Company Name
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = companyName,
            onValueChange = onCompanyNameChange,
            label = { Text(text = "Company Name") },
            isError = companyName.isBlank()
        )
        // Job Type Dropdown
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)) {
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
            isError = companyLogo.isBlank()
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
        Text(text = "Step 4", fontSize = 24.sp)
        // Experience Level
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = experienceLevel,
            onValueChange = onExperienceLevelChange,
            label = { Text(text = "Experience Level") },
            isError = experienceLevel.isBlank()
        )
        // Education Level
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = educationLevel,
            onValueChange = onEducationLevelChange,
            label = { Text(text = "Education Level") },
            isError = educationLevel.isBlank()
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
    onExpandedChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(start = 60.dp, end = 60.dp, bottom = 400.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Step 5", fontSize = 24.sp)
        // Skill Selection
        SkillSelection(
            selectedSkills = selectedSkills,
            onSkillSelected = onSkillSelected,
            onSkillDeselected = onSkillDeselected,
            skills = skills,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        )

        // Navigation Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onPrevious) {
                Text(text = "Previous")
            }
            Button(onClick = onSubmit) {
                Text(text = "Submit")
            }
        }
    }
}


@Composable
fun SkillSelection(
    selectedSkills: List<String>,
    onSkillSelected: (String) -> Unit,
    onSkillDeselected: (String) -> Unit,
    skills: List<SkillData>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Select Skills")
        Button(
            onClick = { onExpandedChange(true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (selectedSkills.isEmpty()) "Select Skills" else selectedSkills.joinToString(", "))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            skills.forEach { skill ->
                val isSelected = selectedSkills.contains(skill.skillName)
                DropdownMenuItem(
                    onClick = {
                        if (isSelected) {
                            onSkillDeselected(skill.skillName)
                        } else {
                            onSkillSelected(skill.skillName)
                        }
                    },
                    text = { Text(text = skill.skillName) }
                )
            }
        }
    }
}


