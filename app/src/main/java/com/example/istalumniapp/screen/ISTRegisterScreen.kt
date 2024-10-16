package com.example.istalumniapp.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

lateinit var auth: FirebaseAuth
@Composable
fun ISTRegisterScreen(navController: NavController) {
    // Initialize Firebase Auth
    auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordToggle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }  // Loading state
    var isPasswordValid by remember { mutableStateOf(false) }  // Password validation state

    val visualTransformation = if (passwordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    // Password validation rules
    val isPasswordLengthValid = password.length >= 8
    val containsUppercase = password.any { it.isUpperCase() }
    val containsDigit = password.any { it.isDigit() }
    val containsSpecialChar = password.any { !it.isLetterOrDigit() }
    val context = LocalContext.current

    // Password is valid if all conditions are met
    isPasswordValid = isPasswordLengthValid && containsUppercase && containsDigit && containsSpecialChar

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {

            // Show Circular Progress Indicator when loading
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please wait as we register you...",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        } else {
            Column(
                modifier = Modifier.fillMaxHeight()
                    .padding(top = 25.dp)
                    .padding(20.dp)
            ) {
                IconButton(onClick = { navController.navigate(Screens.ISTPreviewScreen.route) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.inverseSurface
                    )
                }

                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(150.dp),
                    painter = painterResource(R.drawable.ist_logo),
                    contentDescription = "App Logo"
                )

            // Main registration form
            Card(
                modifier = Modifier.padding(5.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    Text(
                        text = "Create an Account",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email TextField
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password TextField
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = visualTransformation,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                    )

                    // Password validation indicators
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isPasswordLengthValid) "✓ At least 8 characters" else "✕ At least 8 characters",
                            color = if (isPasswordLengthValid) Color.Green else MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (containsUppercase) "✓ Contains an uppercase letter" else "✕ Contains an uppercase letter",
                            color = if (containsUppercase) Color.Green else MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (containsDigit) "✓ Contains a digit" else "✕ Contains a digit",
                            color = if (containsDigit) Color.Green else MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (containsSpecialChar) "✓ Contains a special character" else "✕ Contains a special character",
                            color = if (containsSpecialChar) Color.Green else MaterialTheme.colorScheme.tertiary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password TextField
                    TextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = visualTransformation,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Checkbox for toggling password visibility
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showPasswordToggle,
                            onCheckedChange = { isChecked ->
                                showPasswordToggle = isChecked
                                passwordVisible = isChecked
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Show Password", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Register button (disabled if password is invalid)
                    Button(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .width(250.dp),
                        onClick = {
                            isLoading = true  // Start loading
                            if (isPasswordValid && password == confirmPassword) {
                                signUp(
                                    email,
                                    password,
                                    navController,
                                    { user ->
                                        successMessage = "Verification email sent to ${user?.email}"
                                        isLoading = false
                                        navController.navigate(Screens.ISTLoginScreen.route)
                                        {
                                            Toast.makeText(context, "Profile Created successfully", Toast.LENGTH_SHORT).show()

                                        }// Navigate to Login screen
                                    },
                                    { error ->
                                        errorMessage = error
                                        isLoading = false
                                        Toast.makeText(
                                            navController.context,
                                            errorMessage,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            } else {
                                val error = if (!isPasswordValid) {
                                    "Please ensure the password meets all requirements."
                                } else {
                                    "Passwords do not match."
                                }
                                Toast.makeText(navController.context, error, Toast.LENGTH_LONG)
                                    .show()
                                isLoading = false
                            }
                        },
                        enabled = isPasswordValid,
                    ) {
                        Text("Register")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display error or success message
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    successMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        }
    }
}


private fun signUp(
    email: String,
    password: String,
    navController: NavController,
    onSuccess: (FirebaseUser?) -> Unit,
    onFailure: (String) -> Unit
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    // Default role set to "alumni"
                    val role = "alumni"
                    val db = FirebaseFirestore.getInstance()

                    // Create a user document with email and role in Firestore
                    val userDoc = mapOf(
                        "email" to email,
                        "role" to role
                    )

                    db.collection("users").document(user.uid)
                        .set(userDoc)
                        .addOnSuccessListener {
                            // Send verification email
                            user.sendEmailVerification()
                                .addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        navController.navigate(Screens.ISTLoginScreen.route)
                                        onSuccess(user)
                                    } else {
                                        onFailure(verificationTask.exception?.message ?: "Failed to send verification email")
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.message ?: "Failed to save user data")
                        }
                }
            } else {
                onFailure(task.exception?.message ?: "Sign up failed")
            }
        }
}
