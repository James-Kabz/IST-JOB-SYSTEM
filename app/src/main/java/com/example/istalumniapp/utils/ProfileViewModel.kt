package com.example.istalumniapp.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.example.istalumniapp.IstAlumniApp
import com.example.istalumniapp.data.ProfileRepository
import com.example.istalumniapp.nav.Screens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    private val _skills = MutableLiveData<List<SkillData>>()
    val skills: LiveData<List<SkillData>> = _skills

    private val _profile = MutableLiveData<AlumniProfileData?>()
    val profile: LiveData<AlumniProfileData?> = _profile

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun saveAlumniProfile(
        alumniProfileData: AlumniProfileData,
        profilePhotoUri: Uri?,
        context: Context,
        navController: NavController
    ) {
        viewModelScope.launch {
            profileRepository.saveAlumniProfile(
                alumniProfileData = alumniProfileData,
                profilePhotoUri = profilePhotoUri,
                onComplete = {
                    // Show success message and navigate to the dashboard
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Profile Saved Successfully", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screens.DashboardScreen.route)  // Navigate to dashboard
                    }
                },
                onError = { errorMessage ->
                    // Show error message
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // Fetch skills and update the LiveData
    // Update to accept external callbacks
    fun retrieveSkills(
        onLoading: (Boolean) -> Unit = { _isLoading.value = it },
        onSuccess: (List<SkillData>) -> Unit = { _skills.value = it },
        onFailure: (String) -> Unit = { _errorMessage.value = it }
    ) {
        viewModelScope.launch {
            profileRepository.retrieveSkills(
                onLoading = onLoading,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        }
    }

    // Function to retrieve the current user's profile, accepting custom callbacks
    fun retrieveCurrentUserProfile(
        onLoading: (Boolean) -> Unit = { _isLoading.value = it },
        onSuccess: (AlumniProfileData?) -> Unit = { _profile.value = it },
        onFailure: (String) -> Unit = { _errorMessage.value = it }
    ) {
        viewModelScope.launch {
            profileRepository.retrieveCurrentUserProfile(
                onLoading = onLoading,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        }
    }

    fun retrieveAlumniProfiles(
        onLoading: (Boolean) -> Unit,
        onSuccess: (List<AlumniProfileData>) -> Unit,
        onFailure: (String) -> Unit
    )
    {
        viewModelScope.launch {
            profileRepository.retrieveAlumniProfiles(
                onLoading = onLoading,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        }
    }
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {

                val application = (this[APPLICATION_KEY] as IstAlumniApp)
                val profileRepository = application.container.profileRepository
                ProfileViewModel(profileRepository = profileRepository)
            }
        }
    }
}
