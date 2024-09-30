package com.example.istalumniapp.utils

data class NotificationData(
    val id: String = "", // Unique identifier for the notification
    val profileID: String = "", // ID of the user the notification is related to
    val title: String = "", // Title of the notification
    val message: String = "", // Message content of the notification
    val timestamp: Long = System.currentTimeMillis(), // Time when the notification was created
    val read: Boolean = false // Flag to indicate if the notification has been read
)
