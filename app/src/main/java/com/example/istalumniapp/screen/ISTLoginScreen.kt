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
import android.content.Context
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch




// Define the DataStore
private val Context.dataStore by preferencesDataStore(name = "user_preferences")

// Define the key for storing email
private val EMAIL_KEY = stringPreferencesKey("email")

// Function to save the email
suspend fun saveEmail(context: Context, email: String) {
    context.dataStore.edit { preferences ->
        preferences[EMAIL_KEY] = email
    }
}

// Function to retrieve the email
// Function to retrieve the email, now using flow collection inside the coroutine
suspend fun getEmail(context: Context): String? {
    return context.dataStore.data
        .map { preferences ->
            preferences[EMAIL_KEY] ?: ""
        }
        .firstOrNull() // This collects the flow and returns the first result
}


@Composable
fun ISTLoginScreen(navController: NavController) {
    // Initialize Firebase Auth
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordToggle by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val visualTransformation = if (passwordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Only load email if it has been recently saved, not on every app launch
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val savedEmail = getEmail(context)  // Call the refactored getEmail function
            if (savedEmail != null) {
                email = savedEmail
            }
        }
    }

    val navigateToDashboard by rememberUpdatedState(newValue = { _: String ->
        navController.navigate(Screens.DashboardScreen.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    })

    val navigateToCreateProfile by rememberUpdatedState(newValue = {
        navController.navigate(Screens.CreateProfileScreen.route) {
            popUpTo(0) { inclusive = true }
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
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(top = 25.dp)
                    .padding(15.dp)
            ) {
                // Move the Image to the top of the screen and increase its size
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
                        .size(250.dp),
                    painter = painterResource(R.drawable.ist_logo), // Ensure R.drawable.ist_logo is correct
                    contentDescription = "App Logo"
                )

                Spacer(modifier = Modifier.height(15.dp))

                Card(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
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

                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = visualTransformation,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
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

                        Button(modifier = Modifier.align(Alignment.CenterHorizontally)
                            .width(250.dp),
                            onClick = {
                                if (email.isNotEmpty() && password.isNotEmpty()) {
                                    isLoading = true
                                    signIn(email, password, auth,
                                        onSuccess = { user ->
                                            coroutineScope.launch {
                                                saveEmail(context, email) // Save email after successful login
                                            }
                                            fetchUserRoleAndNavigate(auth, navController, navigateToDashboard, navigateToCreateProfile)
                                        },
                                        onFailure = { error ->
                                            isLoading = false
                                            errorMessage = error
                                        })
                                } else {
                                    Toast.makeText(navController.context, "Please enter both email and password", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = email.isNotEmpty() && password.isNotEmpty()
                        ) {
                            Text("Log In")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
        db.collection("users").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                val role = documentSnapshot.getString("role") ?: "alumni"
                if (role == "admin") {
                    navigateToDashboard("admin")
                } else {
                    checkIfProfileExists(uid, navController, navigateToDashboard, navigateToCreateProfile)
                }
            }
            .addOnFailureListener { e ->
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
    db.collection("alumniProfiles").document(uid).get()
        .addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                navigateToDashboard("alumni")
            } else {
                navigateToCreateProfile()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(navController.context, "Error checking profile: ${e.message}", Toast.LENGTH_LONG).show()
        }
}
