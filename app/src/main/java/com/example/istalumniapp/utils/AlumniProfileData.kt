package com.example.istalumniapp.utils

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DegreeChoice {
    Degree_In_Software_Engineering,
    Degree_In_Computer_Science,
    Degree_In_Information_Technology,
    Degree_In_Data_Science,
    Degree_In_Cyber_Security,
    Degree_In_Artificial_Intelligence,
    Other // Add an option for alumni who have a different degree
}

//@Entity(tableName = "alumniProfiles")
data class AlumniProfileData(
    @PrimaryKey val profileId: String = "",
    var fullName: String = "",
    var email: String = "",
    var degree: DegreeChoice = DegreeChoice.Degree_In_Software_Engineering,
    var graduationYear: String = "",
    var extraCourse: String = "",
    var profilePhotoUri: String? = null,
    var currentJob: String = "",
    var currentEmployee: String = "",
    var location: String = "",
    var phone: String = "",
    var linkedIn: String = "",
    var customDegree: String? = null,// New field for custom degree input
    var skills: List<String> = emptyList(),
)
