package com.example.istalumniapp.nav

sealed class Screens(val route:String)
{
    data object ViewAlumniProfilesScreen:Screens(route = "view_profiles")
    data object  ViewProfileScreen:Screens(route = "view_profile")
    data object CreateProfileScreen:Screens(route = "create_profile")
    data object AddSkillScreen:Screens(route = "add_skill")
    data object DisplayJobScreen:Screens(route = "view_job")
    data object AddJobScreen:Screens(route = "add_job")
    data object ForgotPasswordScreen:Screens(route = "forgot_password")
    data object ISTLoginScreen:Screens(route = "login_screen")
    data object ISTRegisterScreen:Screens(route = "register_screen")
    data object DashboardScreen:Screens(route = "dashboard_screen")
}