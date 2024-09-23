package com.example.istalumniapp.nav

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.istalumniapp.screen.AddJobScreen
import com.example.istalumniapp.screen.AddSkillScreen
import com.example.istalumniapp.screen.CreateProfileScreen
import com.example.istalumniapp.screen.DashboardScreen
import com.example.istalumniapp.screen.DisplayApplicationScreen
import com.example.istalumniapp.screen.DisplayJobScreen
import com.example.istalumniapp.screen.EditJobScreen
import com.example.istalumniapp.screen.EditProfileScreen
import com.example.istalumniapp.screen.ForgotPasswordScreen
import com.example.istalumniapp.screen.ISTLoginScreen
import com.example.istalumniapp.screen.ISTRegisterScreen
import com.example.istalumniapp.screen.JobApplicationScreen
import com.example.istalumniapp.screen.ViewAlumniProfilesScreen
import com.example.istalumniapp.screen.ViewApplicationScreen
import com.example.istalumniapp.screen.ViewProfileScreen
import com.example.istalumniapp.utils.JobApplicationModel
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.example.istalumniapp.utils.SkillData


@Composable
fun NavGraph(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
    profileViewModel: ProfileViewModel,
    jobApplicationModel: JobApplicationModel
) {
    NavHost(
        navController = navController,
        startDestination = Screens.ISTLoginScreen.route
    ) {

//        JOB APPLICATION SCREENS
        composable(
            route = Screens.JobApplicationScreen.route + "/{jobID}",
            arguments = listOf(
                navArgument("jobID") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobID = backStackEntry.arguments?.getString("jobID") ?: return@composable

            JobApplicationScreen(
                navController = navController,
                jobID = jobID, // Pass the jobID obtained from the navigation
                jobApplicationModel = jobApplicationModel
            )
        }

//        view applications screen
        composable(
            route = "${Screens.DisplayApplicationScreen.route}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Retrieve the userId from the backStack arguments
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            // Pass the userId to DisplayApplicationScreen to fetch applications for the logged-in user
            DisplayApplicationScreen(
                navController = navController,
                jobApplicationModel = viewModel(),
                profileViewModel,
                userId = userId // Pass userId instead of applicationId
            )
        }

//        view applications screen
        composable(
            route = Screens.ViewApplicationsScreen.route
        ) {
            ViewApplicationScreen(navController = navController, applicationModel = jobApplicationModel )
        }




//        PROFILE SCREENS

//        edit profile screen
        composable(
            route = Screens.EditProfileScreen.route
        ) {
            EditProfileScreen(
                navController = navController,
                profileViewModel = profileViewModel,
                saveSkill = { skill, context ->
                    profileViewModel.saveSkill(skill, context)
                }
            )
        }

//        view profile
        composable(
            route = Screens.ViewAlumniProfilesScreen.route
        ){
            ViewAlumniProfilesScreen(navController = navController ,profileViewModel = profileViewModel)
        }
        //        view profile
        composable(
            route = Screens.ViewProfileScreen.route
        ){
            ViewProfileScreen(navController = navController ,profileViewModel = profileViewModel)
        }
//      create  profile
        composable(
            route = Screens.CreateProfileScreen.route
        ) {
            CreateProfileScreen(navController = navController,profileViewModel = profileViewModel)
        }

//        JOB SCREENS

//        edit job screen
        composable(
            route = "edit_job/{jobID}",
            arguments = listOf(navArgument("jobID") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobID = backStackEntry.arguments?.getString("jobID") ?: ""
            if (jobID.isNotEmpty()) {
                EditJobScreen(
                    navController = navController,
                    sharedViewModel = sharedViewModel,
                    jobID = jobID
                )
            } else {
                // Handle invalid jobID case
                Text("Invalid Job ID", color = MaterialTheme.colorScheme.error)
            }
        }

//        add job screen
        composable(
            route = Screens.AddJobScreen.route
        ) {
            AddJobScreen(navController = navController, sharedViewModel = sharedViewModel)
        }
//        display job
        composable(
            route = Screens.DisplayJobScreen.route
        ) {
            DisplayJobScreen(navController = navController, sharedViewModel = sharedViewModel,profileViewModel = profileViewModel)
        }
//        addSkills
        composable(
            route = Screens.AddSkillScreen.route
        ) {
            AddSkillScreen(navController = navController, sharedViewModel = sharedViewModel)
        }


//        AUTHENTICATION SCREENS

//        forgot password
        composable(
            route = Screens.ForgotPasswordScreen.route
        ) {
            ForgotPasswordScreen(navController = navController)
        }
//        register screen
        composable(
            route = Screens.ISTRegisterScreen.route
        ) {
            ISTRegisterScreen(navController = navController)
        }
//        login screen
        composable(
            route = Screens.ISTLoginScreen.route
        ) {
            ISTLoginScreen(navController = navController)
        }
//        dashboard screen
        composable(
            route = Screens.DashboardScreen.route
        ) {
            DashboardScreen(navController = navController,profileViewModel =profileViewModel)
        }


    }
}

