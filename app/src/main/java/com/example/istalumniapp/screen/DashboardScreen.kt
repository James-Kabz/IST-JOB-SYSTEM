package com.example.istalumniapp.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.components.Lazy


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel,
    sharedViewModel: SharedViewModel = viewModel() // Pass sharedViewModel to AdminDashboardCards
) {
    // Observe the user role from the ViewModel
    val userRole by sharedViewModel.userRole.collectAsState()
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var loading by remember { mutableStateOf(true) }

    // Fetch user role when the composable is launched
    LaunchedEffect(Unit) {
        sharedViewModel.fetchUserRole()
    }

    LaunchedEffect(userRole) {
        if (userRole == "alumni") {
            notificationViewModel.playNotificationSound()
            profileViewModel.retrieveProfilePhoto(
                onLoading = { loading = it },
                onSuccess = { url -> profilePhotoUrl = url },
                onFailure = { message ->
                    Log.e("DashboardScreen", "Error fetching profile photo: $message")
                }
            )
        }
    }

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
        Scaffold(
            topBar = {
                DashboardTopBar(
                    navController = navController,
                    userRole = userRole,
                    profilePhotoUrl = profilePhotoUrl,
                    onLogoutClick = { showLogoutConfirmation = true },
                    notificationViewModel = notificationViewModel
                )
            },
            bottomBar = {
                DashboardBottomBar(
                    navController = navController,
                    userRole = userRole,
                    notificationViewModel = notificationViewModel
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Display different cards based on the userRole
                Text(
                    text = "Welcome, $userRole",  // Display the fetched role
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Conditionally render cards based on the user role
                if (userRole == "admin") {
                    AdminDashboardCards(
                        navController = navController,
                        sharedViewModel = sharedViewModel
                    )
                } else if (userRole == "alumni") {
                    AlumniDashboardCards(
                        navController = navController,
                        profileViewModel = profileViewModel,
                    )
                }
            }
        }

        if (showLogoutConfirmation) {
            LogoutConfirm(
                onConfirm = {
                    FirebaseAuth.getInstance().signOut() // Log out the user
                    navController.navigate(Screens.ISTPreviewScreen.route) {
                        popUpTo(0)
                    }
                    showLogoutConfirmation = false
                },
                onDismiss = { showLogoutConfirmation = false }
            )
        }
    }
}


@Composable
fun AdminDashboardCards(
    navController: NavController,
    sharedViewModel: SharedViewModel
) {
    var alumniCount by remember { mutableStateOf(0) }
    var jobCount by remember { mutableStateOf(0) }
    var applicationCount by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        sharedViewModel.retrieveAlumniProfiles(
            onLoading = { isLoading -> loading = isLoading },
            onSuccess = { count -> alumniCount = count },
            onFailure = { message -> Log.e("AdminDashboard", message) }
        )

        sharedViewModel.retrieveCardJobs(
            onLoading = { isLoading -> loading = isLoading },
            onSuccess = { count -> jobCount = count },
            onFailure = { message -> Log.e("AdminDashboard", message) }
        )

        sharedViewModel.fetchAllApplicationsForAdminReview { applications ->
            applicationCount = applications.size
            loading = false
        }
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            item {
                // Card 1: Alumni Profiles Count
                DashboardCard(
                    title = "View Alumni Profiles",
                    count = alumniCount,
                    icon = Icons.Filled.Group,
                    cardColor = MaterialTheme.colorScheme.primary,
                    navController = navController,
                    route = Screens.ViewAlumniProfilesScreen.route
                )
            }
            item {
                // Card 1: Alumni Profiles Count
                DashboardCard(
                    title = "Create Job Opportunities",
                    count = 20,
                    icon = Icons.Filled.Group,
                    cardColor = MaterialTheme.colorScheme.primary,
                    navController = navController,
                    route = Screens.AddJobScreen.route
                )
            }
            item {
                // Card 2: Job Listings Count
                DashboardCard(
                    title = "Job Listings Created",
                    count = jobCount,
                    icon = Icons.Filled.Work,
                    cardColor = MaterialTheme.colorScheme.secondary,
                    navController = navController,
                    route = Screens.DisplayJobScreen.route
                )
            }
            item {
                // Card 3: Job Applications Count
                DashboardCard(
                    title = "Job Applications Made",
                    count = applicationCount,
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    cardColor = MaterialTheme.colorScheme.tertiary,
                    navController = navController,
                    route = Screens.ViewApplicationsScreen.route
                )
            }
        }
    }
}


@Composable
fun DashboardCard(
    title: String,
    count: Int,
    icon: ImageVector,
    cardColor: androidx.compose.ui.graphics.Color,
    navController: NavController,
    route: String
) {
    Card(
        onClick = { navController.navigate(route) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(8.dp),  // Elevation for depth
        shape = RoundedCornerShape(16.dp)  // Rounded corners
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = cardColor,
                modifier = Modifier.size(48.dp)  // Larger icon size
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium, // Larger headline
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total: $count",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,  // Bold for emphasis
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun AlumniDashboardCards(
    navController: NavController,
    profileViewModel: ProfileViewModel
) {
    val context = LocalContext.current
    var jobCount by remember { mutableStateOf(0) }
    val loading by profileViewModel.loading1.observeAsState(initial = true)

    // Get current user and userId
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid // Retrieve the current user's ID

    LaunchedEffect(Unit) {
        profileViewModel.countMatchingJobs(
            context = context,
            onSuccess = { count -> jobCount = count },
            onFailure = { errorMessage -> Log.e("AlumniDashboard", errorMessage) }
        )
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            item {
                // Card 1: View Jobs
                DashboardCard2(
                    title = "View Jobs",
                    description = "Browse available jobs for you.",
                    countText = "Total Jobs: $jobCount",
                    icon = Icons.Filled.Work,
                    cardColor = MaterialTheme.colorScheme.primary,
                    navController = navController,
                    route = Screens.DisplayAlumniJobsScreen.route
                )
            }
            item {
                // Card 2: My Applications
                userId?.let {
                    DashboardCard2(
                        title = "My Applications",
                        description = "View and manage your job applications.",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        cardColor = MaterialTheme.colorScheme.secondary,
                        navController = navController,
                        route = "${Screens.DisplayApplicationScreen.route}/$it" // Use userId here
                    )
                }
            }
            item {
                // Card 3: Notifications or Profile Update
                DashboardCard2(
                    title = "Notifications",
                    description = "Check your notifications and updates.",
                    icon = Icons.Filled.Notifications,
                    cardColor = MaterialTheme.colorScheme.tertiary,
                    navController = navController,
                    route = Screens.NotificationsScreen.route
                )
            }
        }
    }
}


@Composable
fun DashboardCard2(
    title: String,
    description: String,
    countText: String? = null,
    icon: ImageVector,
    cardColor: Color,
    navController: NavController,
    route: String
) {
    Card(
        onClick = { navController.navigate(route) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),  // External padding to add spacing between cards
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.2f)),  // Slight color fill
        elevation = CardDefaults.cardElevation(8.dp),  // Elevation for shadow
        shape = RoundedCornerShape(16.dp)  // Rounded corners for softer edges
    ) {
        Column(
            modifier = Modifier.padding(24.dp),  // Internal padding for card content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = cardColor,
                modifier = Modifier.size(40.dp)  // Larger icon for better visual emphasis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,  // Larger title
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,  // Subtle description text
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            countText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,  // Bold count text
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}


@Composable
fun DashboardBottomBar(
    navController: NavController, notificationViewModel: NotificationViewModel, userRole: String?
) {
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
                "alumni" -> AlumniDashboard(
                    navController = navController, notificationViewModel = notificationViewModel
                )

                "admin" -> AdminDashboard(navController = navController)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(navController: NavController, onLogoutClick: () -> Unit) {
    TopAppBar(modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary),
        title = { Text("Admin Dashboard") },
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Person, contentDescription = "Profile")
            }

        },
        actions = {
            IconButton(
                onClick = onLogoutClick, colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
            }
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniTopBar(
    navController: NavController,
    onLogoutClick: () -> Unit,
    profilePhotoUrl: String?,
    notificationViewModel: NotificationViewModel
) {
    // Assume we have a live data or state holding the number of unread notifications
    val unreadNotificationsCount by notificationViewModel.unreadNotificationCount.collectAsState(0)

    TopAppBar(title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))
            Text("Alumni Dashboard")
        }
    }, navigationIcon = {
        IconButton(onClick = { navController.navigate(Screens.ViewProfileScreen.route) }) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profilePhotoUrl != null) {

                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(profilePhotoUrl.ifBlank { R.drawable.placeholder }) // Handle empty URL properly
                            .crossfade(true).placeholder(R.drawable.placeholder)
                            .error(R.drawable.error).build()
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
    }, actions = {
        Box {
            IconButton(onClick = {
                navController.navigate(Screens.NotificationsScreen.route)  // Navigate to the notifications screen
            }) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(40.dp)
                )
            }
            // Always show the badge, even if the count is 0
            Badge(
                containerColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(unreadNotificationsCount.toString())
            }
        }

        IconButton(
            onClick = onLogoutClick, colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
        }
    })
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    navController: NavController,
    notificationViewModel: NotificationViewModel,
    userRole: String?,
    onLogoutClick: () -> Unit,
    profilePhotoUrl: String?
) {
    when (userRole) {
        "alumni" -> AlumniTopBar(
            navController = navController,
            onLogoutClick = onLogoutClick,
            profilePhotoUrl,
            notificationViewModel = notificationViewModel
        )

        "admin" -> AdminTopBar(navController = navController, onLogoutClick = onLogoutClick)
//        else -> DefaultTopBar(navController = navController, onLogoutClick = onLogoutClick) // If needed, provide a default top bar
    }
}

@Composable
fun AlumniDashboard(navController: NavController, notificationViewModel: NotificationViewModel) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.onPrimary
    ) {
        // Home Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Home") },
            selected = currentDestination == Screens.DashboardScreen.route,
            onClick = { navController.navigate(Screens.DashboardScreen.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,    // Color when selected
                unselectedIconColor = Color.Gray,  // Color when not selected
                selectedTextColor = MaterialTheme.colorScheme.onBackground,    // Text color when selected
                unselectedTextColor = Color.Gray   // Text color when not selected
            )
        )

        // Jobs Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.AssignmentTurnedIn,
                    contentDescription = "Applications",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Applications") },
            selected = currentDestination?.startsWith(Screens.DisplayApplicationScreen.route) == true,
            onClick = {
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.let {
                    val userId = it.uid
                    navController.navigate("${Screens.DisplayApplicationScreen.route}/$userId")
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray
            )
        )

        // Applications Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Work,
                    contentDescription = "Jobs",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Jobs") },
            selected = currentDestination == Screens.DisplayAlumniJobsScreen.route,
            onClick = { navController.navigate(Screens.DisplayAlumniJobsScreen.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray
            )
        )
    }
}


@Composable
fun AdminDashboard(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.onPrimary
    ) {
        // Home Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Home") },
            selected = currentDestination == Screens.DashboardScreen.route,
            onClick = { navController.navigate(Screens.DashboardScreen.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray
            )
        )

        // Applications Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = "Applications",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Applications") },
            selected = currentDestination == Screens.ViewApplicationsScreen.route,
            onClick = { navController.navigate(Screens.ViewApplicationsScreen.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray
            )
        )

        // Manage Users Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.SupervisedUserCircle,
                    contentDescription = "Manage Users",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Users") },
            selected = currentDestination == Screens.ViewAlumniProfilesScreen.route,
            onClick = { navController.navigate(Screens.ViewAlumniProfilesScreen.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray
            )
        )


        // Jobs Button with Icon and Text
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Work,
                    contentDescription = "Jobs",
                    modifier = Modifier.size(40.dp)
                )
            },
            label = { Text(text = "Jobs") },
            selected = currentDestination == Screens.DisplayJobScreen.route,
            onClick = { navController.navigate(Screens.DisplayJobScreen.route) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = Color.Gray,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

