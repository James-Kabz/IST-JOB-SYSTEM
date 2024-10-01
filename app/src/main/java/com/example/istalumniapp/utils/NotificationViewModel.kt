package com.example.istalumniapp.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.istalumniapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel(private val context: Context) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _notifications = MutableLiveData<List<NotificationData>>()
    val notifications: LiveData<List<NotificationData>> = _notifications

    // StateFlow to hold the unread notifications count
    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> get() = _unreadNotificationCount

    private var mediaPlayer: MediaPlayer? = null

    init {
        fetchNotifications() // Fetch notifications when the ViewModel is created
    }

    fun fetchNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                try {
                    // Fetch notifications from Firestore for the current user
                    val result = firestore.collection("notifications")
                        .whereEqualTo("profileID", uid)
                        .get().await()

                    // Parse the result into a list of NotificationData
                    val notificationList =
                        result.mapNotNull { it.toObject(NotificationData::class.java) }

                    // Update the LiveData with the list of notifications
                    _notifications.postValue(notificationList)

                    // Update the unread notification count
                    val unreadCount = notificationList.count { !it.read }
                    _unreadNotificationCount.value = unreadCount

//                    // If there are unread notifications, play a sound
//                    if (unreadCount > 0) {
//                        playNotificationSound()
//                    }

                } catch (e: Exception) {
                    Log.e("NotificationViewModel", "Error fetching notifications: ${e.message}")
                }
            }
        }
    }

    fun playNotificationSound() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            // Fetch notifications from Firestore for the current user using task listeners
            firestore.collection("notifications")
                .whereEqualTo("profileID", uid)
                .get()
                .addOnSuccessListener { result ->
                    // Parse the result into a list of NotificationData
                    val notificationList =
                        result.mapNotNull { it.toObject(NotificationData::class.java) }

                    // Update the LiveData with the list of notifications
                    _notifications.postValue(notificationList)

                    // Update the unread notification count
                    val unreadCount = notificationList.count { !it.read }
                    _unreadNotificationCount.value = unreadCount

                    if (unreadCount > 0) {
                        // Play the notification sound
                        try {
                            // If a MediaPlayer instance already exists, release it before creating a new one
                            mediaPlayer?.release()
                            mediaPlayer = MediaPlayer.create(context, R.raw.music2) // Replace with your actual sound resource

                            // Set a listener to release the player after the sound finishes
                            mediaPlayer?.setOnCompletionListener { player ->
                                player.release() // Release the MediaPlayer to free resources
                                mediaPlayer = null
                            }

                            // Start playing the sound
                            mediaPlayer?.start()
                        } catch (e: Exception) {
                            Log.e("NotificationViewModel", "Error playing sound: ${e.message}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationViewModel", "Error fetching notifications: ${e.message}")
                }
        }
    }



    fun markNotificationAsRead(id: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            firestore.collection("notifications")
                .document(id)
                .update("read", true)
                .addOnSuccessListener {
                    fetchNotifications() // Refresh the notifications after marking as read
                }
                .addOnFailureListener { e ->
                    Log.e("NotificationViewModel", "Failed to mark as read: ${e.message}")
                }
        }
    }
}
