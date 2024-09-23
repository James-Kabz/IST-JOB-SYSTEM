package com.example.istalumniapp.nav

sealed class Screens(val route:String)
{
//    profile screens
    data object ViewAlumniProfilesScreen:Screens(route = "view_profiles")
    data object ViewProfileScreen:Screens(route = "view_profile")
    data object CreateProfileScreen:Screens(route = "create_profile")
    data object EditProfileScreen : Screens(route = "edit_profile")

//    job screens
    data object AddSkillScreen:Screens(route = "add_skill")
    data object DisplayJobScreen:Screens(route = "view_job")
    data object AddJobScreen:Screens(route = "add_job")
    data object EditJobScreen:Screens(route = "edit_job/{jobID}")

//    job application screens
    data object JobApplicationScreen:Screens(route = "job_application")
    data object DisplayApplicationScreen:Screens(route = "display_application{applicationId}")
    data object ViewApplicationsScreen:Screens(route = "display_applications")
//    authentication screens
    data object ForgotPasswordScreen:Screens(route = "forgot_password")
    data object ISTLoginScreen:Screens(route = "login_screen")
    data object ISTRegisterScreen:Screens(route = "register_screen")
    data object DashboardScreen:Screens(route = "dashboard_screen")
}