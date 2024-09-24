package com.example.istalumniapp.utils

import android.os.Build

enum class Experience(val years: Int) {
    One_Year(1),
    Two_Years(2),
    Three_Years(3),
    Four_Years(4),
    Five_Years(5),
    Six_Years(6),
    Seven_Years(7),
    Eight_Years(8),
    Nine_Years(9),
    Ten_Years(10),
    Eleven_Years(11),
    Twelve_Years(12),
    Thirteen_Years(13),
    Fourteen_Years(14),
    Fifteen_Years(15),
    Sixteen_Years(16),
    Seventeen_Years(17),
    Eighteen_Years(18),
    Nineteen_Years(19),
    Twenty_Years(20);
}

enum class Gender {
    Male,
    Female
}


data class JobApplicationData(
    val applicationId: String = "",
    val jobID: String = "",
    val userId: String = "",
    val experience: Experience = com.example.istalumniapp.utils.Experience.One_Year,
    val gender: Gender = com.example.istalumniapp.utils.Gender.Male,
    val linkedIn: String = "",
    val education: String = "",
    val address: String = "",
    val phone: String = "",
    val cv: String = "",
    var companyLogo: String = "",
    var title: String = "",
    var email: String = "",
    var status: String? = null,
    var feedback: String? = null, // Automatically set the creation time
)

