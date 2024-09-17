package com.example.istalumniapp.screen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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

@Composable
fun ISTLoginScreen(navController: NavController) {
    // Initialize Firebase Auth
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordToggle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) } // State to track loading

    val visualTransformation = if (passwordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    val navigateToDashboard by rememberUpdatedState(newValue = { _: String ->
        navController.navigate(Screens.DashboardScreen.route) {
            popUpTo(0) { inclusive = true } // Clears entire back stack
            launchSingleTop = true // Prevent multiple instances of the dashboard screen
        }
    })

    val navigateToCreateProfile by rememberUpdatedState(newValue = {
        navController.navigate(Screens.CreateProfileScreen.route) {
            popUpTo(0) { inclusive = true } // Clears entire back stack
        }
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
            }
        } else {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Login",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(56.dp)),
                        painter = painterResource(R.drawable.project_logo),
                        contentDescription = "Login"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = visualTransformation,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                        Text(text = "Show Password", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Forgot Password?",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate(Screens.ForgotPasswordScreen.route)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true // Start loading
                                signIn(email, password, auth,
                                    onSuccess = { user ->
                                        fetchUserRoleAndNavigate(auth, navController, navigateToDashboard, navigateToCreateProfile)
                                    },
                                    onFailure = { error ->
                                        isLoading = false // Stop loading
                                        errorMessage = error
                                    })
                            } else {
                                Toast.makeText(navController.context, "Please enter both email and password", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log In")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Don't have an account?",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(onClick = { navController.navigate(Screens.ISTRegisterScreen.route) }) {
                        Text(text = "Register")
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun signIn(
    email: String,
    password: String,
    auth: FirebaseAuth,
    onSuccess: (FirebaseUser?) -> Unit,
    onFailure: (String) -> Unit
) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null && user.isEmailVerified) {
                    onSuccess(user)
                } else {
                    auth.signOut()
                    onFailure("Email not verified. Please verify your email.")
                }
            } else {
                val error = task.exception?.message ?: "Sign in failed"
                onFailure(error)
            }
        }
}
private fun fetchUserRoleAndNavigate(
    auth: FirebaseAuth,
    navController: NavController,
    navigateToDashboard: (String) -> Unit,
    navigateToCreateProfile: () -> Unit
) {
    val uid = auth.currentUser?.uid
    if (uid != null) {
        val db = FirebaseFirestore.getInstance()
        // Fetch user role
        db.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                val role = documentSnapshot.getString("role") ?: "alumni" // Default to alumni

                if (role == "admin") {
                    // Directly navigate to the dashboard if the user is an admin
                    navigateToDashboard("admin")
                } else {
                    // Check if the alumni has created a profile
                    checkIfProfileExists(uid,navController, navigateToDashboard, navigateToCreateProfile)
                }
            }
            .addOnFailureListener { e ->
                // Handle failure (show error or retry)
                Toast.makeText(navController.context, "Error fetching user role: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

private fun checkIfProfileExists(
    uid: String,
    navController: NavController,
    navigateToDashboard: (String) -> Unit,
    navigateToCreateProfile: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    // Query the 'alumniProfiles' collection to check if the profile exists
    db.collection("alumniProfiles").document(uid).get()
        .addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                // If the profile exists, navigate to the DashboardScreen
                navigateToDashboard("alumni")
            } else {
                // If the profile doesn't exist, navigate to the CreateProfileScreen
                navigateToCreateProfile()
            }
        }
        .addOnFailureListener { e ->
            // Handle the error, e.g., show a message
            Toast.makeText(navController.context, "Error checking profile: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

