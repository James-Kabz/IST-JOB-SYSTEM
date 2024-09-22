package com.example.istalumniapp.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController,profileViewModel: ProfileViewModel, sharedViewModel: SharedViewModel = viewModel()) {
    // Observe the user role from the ViewModel
    val userRole by sharedViewModel.userRole.collectAsState()
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    // Variable to show logout confirmation dialog
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    // Fetch user role when the composable is launched
    LaunchedEffect(Unit) {
        sharedViewModel.fetchUserRole()

        profileViewModel.retrieveProfilePhoto(
            onLoading = { loading -> isLoading = loading },
            onSuccess = { url -> profilePhotoUrl = url.toString() },
            onFailure = { message -> Log.e("DisplayJobScreen", "Error fetching profile photo: $message") }
        )
    }

    // Check if the userRole is null, meaning it's still loading
    if (userRole == null) {
        // Show loading screen while the userRole is being fetched
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
    } else {
        // Once the userRole is available, show the dashboard
        Scaffold(
            topBar = {
                DashboardTopBar(
                    navController = navController,
                    userRole = userRole,  // Pass the user role to DashboardTopBar
                    profilePhotoUrl = profilePhotoUrl,
                    onLogoutClick = { showLogoutConfirmation = true }
                )
            },
            bottomBar = {
                DashboardBottomBar(navController = navController, userRole = userRole)
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = "Welcome, $userRole",  // Display the fetched role
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (showLogoutConfirmation) {
            LogoutConfirmationDialog(
                onConfirm = {
                    showLogoutConfirmation = false
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login_screen") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                },
                onDismiss = { showLogoutConfirmation = false }
            )
        }
    }
}



@Composable
fun DashboardBottomBar(navController: NavController, userRole: String?) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
//            IconButton(onClick = { navController.navigate(Screens.DashboardScreen.route) }) {
//                Icon(Icons.Filled.Home, contentDescription = "Home", modifier = Modifier.size(40.dp))
//            }
            when (userRole) {
                "alumni" -> AlumniDashboard(navController = navController)
                "admin" -> AdminDashboard(navController = navController)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(navController: NavController, onLogoutClick: () -> Unit) {
    TopAppBar(
        modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary),
        title = { Text("Admin Dashboard") },
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Person, contentDescription = "Profile")
            }
        },
        actions = {
            IconButton(
                onClick = onLogoutClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            }
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniTopBar(
    navController: NavController,
    onLogoutClick: () -> Unit,
    profilePhotoUrl: String?
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alumni Dashboard")
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigate(Screens.ViewProfileScreen.route) }) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (profilePhotoUrl != null) {

                        val painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(profilePhotoUrl.ifBlank { R.drawable.placeholder }) // Handle empty URL properly
                                .crossfade(true)
                                .placeholder(R.drawable.placeholder)
                                .error(R.drawable.error)
                                .build()
                        )
                        Image(
                            painter = painter,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onLogoutClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(navController: NavController, userRole: String?, onLogoutClick: () -> Unit,profilePhotoUrl: String?) {
    when (userRole) {
        "alumni" -> AlumniTopBar(navController = navController, onLogoutClick = onLogoutClick,profilePhotoUrl)
        "admin" -> AdminTopBar(navController = navController, onLogoutClick = onLogoutClick)
//        else -> DefaultTopBar(navController = navController, onLogoutClick = onLogoutClick) // If needed, provide a default top bar
    }
}


    @Composable
    fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") }
        )
    }


    @Composable
    fun AlumniDashboard(navController: NavController) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate(Screens.DashboardScreen.route) }) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { /* TODO: Handle Notifications */ }) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { navController.navigate(Screens.CreateProfileScreen.route) }) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { navController.navigate(Screens.DisplayJobScreen.route) }) {
                    Icon(
                        Icons.Filled.MailOutline,
                        contentDescription = "Jobs",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun AdminDashboard(navController: NavController) {
        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate(Screens.DashboardScreen.route) }) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { /* TODO: Handle Admin Settings */ }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Admin Settings",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { navController.navigate(Screens.ViewAlumniProfilesScreen.route) }) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Manage Users",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { navController.navigate(Screens.DisplayJobScreen.route) }) {
                    Icon(
                        Icons.Filled.MailOutline,
                        contentDescription = "Jobs",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
