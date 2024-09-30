package com.example.istalumniapp.screen
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.example.istalumniapp.nav.Screens
import com.example.istalumniapp.utils.NotificationData
import com.example.istalumniapp.utils.NotificationViewModel
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun NotificationScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    profileViewModel: ProfileViewModel,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    // Observe notifications from the ViewModel using observeAsState (since it's LiveData)
    val notifications by notificationViewModel.notifications.observeAsState(initial = emptyList())
    var userRole by remember { mutableStateOf<String?>(null) }
    var profilePhotoUrl by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) } // Track loading state
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch user role and profile photo
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            val documentSnapshot = db.collection("users").document(uid).get().await()
            userRole = documentSnapshot.getString("role") ?: "alumni"
        }

        if (userRole == "alumni") {
            profileViewModel.retrieveProfilePhoto(
                onLoading = { /* handle loading if needed */ },
                onSuccess = { url -> profilePhotoUrl = url },
                onFailure = { message -> Log.e("NotificationScreen", "Error fetching profile photo: $message") }
            )
        }

        // Simulate fetching notifications
        notificationViewModel.fetchNotifications()
        isLoading = false // Set loading to false after fetching
    }

    // Show logout confirmation dialog
    if (showLogoutConfirmation) {
        LogoutConfirm(
            onConfirm = {
                FirebaseAuth.getInstance().signOut() // Log out the user
                navController.navigate(Screens.ISTLoginScreen.route) // Navigate to login screen
                showLogoutConfirmation = false
            },
            onDismiss = { showLogoutConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                navController = navController,
                onLogoutClick = { showLogoutConfirmation = true },
                userRole = userRole,
                profilePhotoUrl = profilePhotoUrl
            )
        },
        bottomBar = {
            DashboardBottomBar(navController = navController, userRole = userRole, notificationViewModel = notificationViewModel)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                // Show loading indicator
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                    }                }
            } else if (notifications.isEmpty()) {
                // Show message if no notifications are available
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notifications available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            } else {
                // Display the notifications in a list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationItemUI(
                            notification = notification,
                            onNotificationClick = {
                                notificationViewModel.markNotificationAsRead(notification.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}



@Composable
fun NotificationItemUI(
    notification: NotificationData,
    onNotificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onNotificationClick() }, // Mark as read when clicked
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notification bell icon
            Icon(
                Icons.Default.Notifications, // You can use Icons.Filled.Notifications
                contentDescription = "Notification Bell",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title from NotificationData
                Text(
                    text = notification.title,  // Dynamic title from Firestore
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Message from NotificationData
                Text(
                    text = notification.message,  // Dynamic message from Firestore
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Timestamp with clock icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime, // Clock icon
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatTimestamp(notification.timestamp.toLong()), // Convert to string
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Blue dot if unread
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }
        }
    }
}


// Convert Long timestamp to formatted String
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
    val date = Date(timestamp)
    return sdf.format(date)
}
