package com.example.istalumniapp.utils

import com.google.type.Date

//import java.util.Date

enum class JobType {
    FULL_TIME,
    PART_TIME,
    CONTRACT
}

data class JobData(
    var jobID: String = "",
    var title: String = "",
    var description: String = "",
    var location: String = "",
    var salary: String = "",
    var companyName: String = "",
    var jobType: JobType = JobType.FULL_TIME, // Provide a default value
    var experienceLevel: String = "",
    var educationLevel: String = "",
    var companyLogo: String = "",
    var skills: List<String> = emptyList(),
//    var deadlineDate: Date? = null // Add a deadline date property
)
