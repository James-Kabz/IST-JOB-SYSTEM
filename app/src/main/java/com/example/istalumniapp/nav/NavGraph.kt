package com.example.istalumniapp.nav

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.istalumniapp.screen.AddJobScreen
import com.example.istalumniapp.screen.AddSkillScreen
import com.example.istalumniapp.screen.CreateProfileScreen
import com.example.istalumniapp.screen.DashboardScreen
import com.example.istalumniapp.screen.DisplayJobScreen
import com.example.istalumniapp.screen.EditJobScreen
import com.example.istalumniapp.screen.EditProfileScreen
import com.example.istalumniapp.screen.ForgotPasswordScreen
import com.example.istalumniapp.screen.ISTLoginScreen
import com.example.istalumniapp.screen.ISTRegisterScreen
import com.example.istalumniapp.screen.ViewAlumniProfilesScreen
import com.example.istalumniapp.screen.ViewProfileScreen
import com.example.istalumniapp.utils.JobData
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.example.istalumniapp.utils.SkillData


@Composable
fun NavGraph(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
    profileViewModel: ProfileViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screens.ISTLoginScreen.route
    ) {



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

