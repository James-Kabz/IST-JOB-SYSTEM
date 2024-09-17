package com.example.istalumniapp.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.example.istalumniapp.utils.AlumniProfileData
import com.example.istalumniapp.utils.DegreeChoice
import com.example.istalumniapp.utils.SharedViewModel
import com.example.istalumniapp.utils.SkillData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.colors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens

@Composable
fun CreateProfileScreen(navController: NavController, sharedViewModel: SharedViewModel) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var currentJob by remember { mutableStateOf("") }
    var currentEmployer by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf(listOf<String>()) }
    var skills by remember { mutableStateOf(listOf<SkillData>()) }
    var skillsLoading by remember { mutableStateOf(false) }
    var skillsError by remember { mutableStateOf("") }
    var skillsExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var currentStep by remember { mutableIntStateOf(1) }

    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var linkedIn by remember { mutableStateOf("") }
    val context = LocalContext.current
    var degree by remember { mutableStateOf(DegreeChoice.Degree_In_Software_Engineering) }
    var customDegree by remember { mutableStateOf("") }
    var graduationYear by remember { mutableStateOf("") }
    var extraCourse by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) } // Loading state


    // Safe state update using LaunchedEffect
    LaunchedEffect(Unit) {
        sharedViewModel.retrieveSkills(
            onLoading = { skillsLoading = it },
            onSuccess = { skills = it },
            onFailure = { skillsError = it }
        )
    }

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            profilePhotoUri = uri
        }
    )

    // If loading is true, show a blank screen with the loading indicator
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color.Blue, // Customize the color
                strokeWidth = 4.dp
            )
        }
    } else {
        // Main layout
        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),  // Rounded corners
            elevation = CardDefaults.cardElevation(20.dp),  // Elevation (shadow)
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.onSecondary,  // Background color of the card
                contentColor = colorScheme.onBackground  // Content color
            ),
            border = BorderStroke(1.dp, colorScheme.background)  // Optional border color and thickness
        )  {

            StepProgressIndicator(currentStep = currentStep)

            // Error Message Display
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Steps layout
            when (currentStep) {
                1 -> ProcedureOne(
                    fullName = fullName,
                    onFullNameChange = { fullName = it },
                    email = email,
                    onEmailChange = { email = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    location = location,
                    onLocationChange = { location = it },
                    onNext = {
                        if (fullName.isBlank() || email.isBlank() || phone.isBlank() || location.isBlank()) {
                            errorMessage = "Please fill in all the fields to proceed."
                        } else {
                            errorMessage = ""
                            currentStep = 2
                        }
                    }
                )
                2 -> ProcedureTwo(
                    currentJob = currentJob,
                    onCurrentJobChange = { currentJob = it },
                    currentEmployer = currentEmployer,
                    onCurrentEmployeeChange = { currentEmployer = it },
                    selectedSkills = selectedSkills,
                    onSkillSelected = { selectedSkills = selectedSkills + it },
                    onSkillDeselected = { selectedSkills = selectedSkills - it },
                    skills = skills,
                    expanded = skillsExpanded,
                    onExpandedChange = { skillsExpanded = it },
                    onPrevious = { currentStep = 1 },
                    onNext = {
                        if (currentJob.isBlank() || currentEmployer.isBlank() || selectedSkills.isEmpty()) {
                            errorMessage = "Please fill in all the fields"
                        } else {
                            errorMessage = ""
                            currentStep = 3
                        }
                    }
                )
                3 -> ProcedureThree(
                    degree = degree,
                    onDegreeChange = { degree = it },
                    customDegree = customDegree,
                    onCustomDegreeChange = { customDegree = it },
                    graduationYear = graduationYear,
                    onGraduationYearChange = { graduationYear = it },
                    extraCourse = extraCourse,
                    onExtraCourseChange = { extraCourse = it },
                    onPrevious = { currentStep = 2 },
                    onNext = {
                        if (degree == DegreeChoice.Other && customDegree.isBlank()) {
                            errorMessage = "Please Enter Your Custom Degree"
                        } else if (graduationYear.isBlank()) {
                            errorMessage = "Please fill in all the fields to proceed"
                        } else {
                            errorMessage = ""
                            currentStep = 4
                        }
                    }
                )
                4 -> ProcedureFour(
                    profilePhoto = profilePhotoUri,
                    onSelectPhotoClick = {
                        launcher.launch("image/*") // Launch image picker
                    },
                    linkedIn = linkedIn,
                    onLinkedInChange = { linkedIn = it },
                    onPrevious = { currentStep = 3 },
                    onNext = {
                        if (linkedIn.isBlank() || profilePhotoUri == null) {
                            errorMessage = "Please upload a profile photo and provide your LinkedIn profile URL."
                        } else {
                            errorMessage = ""
                            loading = true // Set loading to true before starting profile creation

                            val alumniProfileData = AlumniProfileData(
                                profileId = "", // Set a unique ID for the profile
                                fullName = fullName,
                                email = email,
                                degree = degree,
                                customDegree = if (degree == DegreeChoice.Other) customDegree else null,
                                graduationYear = graduationYear,
                                extraCourse = extraCourse,
                                profilePhotoUri = "", // Will be replaced after uploading
                                currentJob = currentJob,
                                currentEmployee = currentEmployer,
                                location = location,
                                phone = phone,
                                linkedIn = linkedIn,
                                skills = selectedSkills,
                            )

                            // Save profile and handle success and error cases
                            sharedViewModel.saveAlumniProfile(
                                alumniProfileData = alumniProfileData,
                                profilePhotoUri = profilePhotoUri,
                                context = context,
                                navController = navController, // Pass the NavController here
                                onComplete = {
                                    loading = false // Set loading to false after completion
                                    navController.navigate(Screens.ViewProfileScreen.route)
                                },
                                onError = { error ->
                                    loading = false
                                    errorMessage = error // Display error message
                                }
                            )

                        }
                    }
                )
            }
        }
    }
}

@Composable
fun StepProgressIndicator(currentStep: Int) {
    val progress = when (currentStep) {
        1 -> 0.25f
        2 -> 0.50f
        3 -> 0.75f
        4 -> 1.0f
        else -> 0f
    }

    Column(
        modifier = Modifier
            .padding(top = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step $currentStep of 4",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = Color.Blue,
        )
    }
}

@Composable
fun ProcedureOne(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        val schoolLogo = R.drawable.project_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Full Name
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            value = fullName,
            onValueChange = onFullNameChange,

            label = { Text(text = "Full Name") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            isError = fullName.isBlank()
        )

        // Email
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            value = email,
            onValueChange = onEmailChange,
            label = { Text(text = "Email") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            isError = email.isBlank()
        )

        // Phone
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text(text = "Phone") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            isError = phone.isBlank()
        )

        // Location
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            value = location,
            onValueChange = onLocationChange,
            label = { Text(text = "Location") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            isError = location.isBlank()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Next Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(text = "Next", style = MaterialTheme.typography.labelLarge)
        }
    }
}



@Composable
fun ProcedureTwo(
    currentJob: String,
    onCurrentJobChange: (String) -> Unit,
    currentEmployer: String,
    onCurrentEmployeeChange: (String) -> Unit,
    selectedSkills: List<String>,
    onSkillSelected: (String) -> Unit,
    onSkillDeselected: (String) -> Unit,
    skills: List<SkillData>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var skillSelectionError by remember { mutableStateOf("") }

    val onSkillSelectedWithLimit = { skill: String ->
        if (selectedSkills.size >= 5 && !selectedSkills.contains(skill)) {
            skillSelectionError = "You can select up to 5 skills only."
        } else {
            onSkillSelected(skill)
            skillSelectionError = ""
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),  // Scrollable content if needed
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val schoolLogo = R.drawable.project_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current Job
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            value = currentJob,
            onValueChange = onCurrentJobChange,
            label = { Text(text = "Current Job") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            isError = currentJob.isBlank()
        )

        // Current Employer
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            value = currentEmployer,
            onValueChange = onCurrentEmployeeChange,
            label = { Text(text = "Current Employer") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            isError = currentEmployer.isBlank()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Skill Selection Section
        Text(text = "Select Your Skills", style = MaterialTheme.typography.headlineMedium)

        SkillSelection(
            selectedSkills = selectedSkills,
            onSkillSelected = onSkillSelectedWithLimit,
            onSkillDeselected = onSkillDeselected,
            skills = skills,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        )

        if (skillSelectionError.isNotEmpty()) {
            Text(
                text = skillSelectionError,
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
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
fun ProcedureThree(
    degree: DegreeChoice,
    onDegreeChange: (DegreeChoice) -> Unit,
    customDegree: String,
    onCustomDegreeChange: (String) -> Unit,
    graduationYear: String,
    onGraduationYearChange: (String) -> Unit,
    extraCourse: String,
    onExtraCourseChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),  // Allow scrolling for long content
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val schoolLogo = R.drawable.project_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Degree Dropdown
        OutlinedTextField(
            value = degree.name.replace("_", " "),
            onValueChange = {},
            label = { Text("Degree") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Degree")
                }
            }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DegreeChoice.entries.forEach { choice ->
                DropdownMenuItem(text = { Text(text = choice.name.replace("_", " ")) },
                    onClick = {
                        onDegreeChange(choice)
                        expanded = false
                    })
            }
        }

        // Custom Degree
        if (degree == DegreeChoice.Other) {
            OutlinedTextField(
                value = customDegree,
                onValueChange = onCustomDegreeChange,
                label = { Text("Custom Degree") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                isError = customDegree.isBlank()
            )
        }

        // Graduation Year
        OutlinedTextField(
            value = graduationYear,
            onValueChange = onGraduationYearChange,
            label = { Text("Graduation Year") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            isError = graduationYear.isBlank()
        )

        // Extra Courses
        OutlinedTextField(
            value = extraCourse,
            onValueChange = onExtraCourseChange,
            label = { Text("Extra Courses") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
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
fun ProcedureFour(
    profilePhoto: Uri?,
    onSelectPhotoClick: () -> Unit,
    linkedIn: String,
    onLinkedInChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        val schoolLogo = R.drawable.project_logo
        Image(
            painter = painterResource(id = schoolLogo),
            contentDescription = "School Logo",
            modifier = Modifier
                .size(200.dp)
                .shadow(4.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Photo
        if (profilePhoto != null) {
            Image(
                painter = rememberAsyncImagePainter(profilePhoto),
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .shadow(4.dp)
                    .padding(16.dp)
            )
        } else {
            Text("No profile photo selected", modifier = Modifier.padding(vertical = 8.dp))
        }

        Button(onClick = onSelectPhotoClick, modifier = Modifier.shadow(4.dp)) {
            Text("Select Profile Photo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LinkedIn URL
        OutlinedTextField(
            value = linkedIn,
            onValueChange = onLinkedInChange,
            label = { Text("LinkedIn Profile URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            isError = linkedIn.isBlank()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onPrevious) {
                Text(text = "Previous")
            }
            Button(onClick = onNext) {
                Text(text = "Submit")
            }
        }
    }
}
