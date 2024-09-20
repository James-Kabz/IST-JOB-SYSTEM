package com.example.istalumniapp.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.istalumniapp.screen.AddJobScreen
import com.example.istalumniapp.screen.AddSkillScreen
import com.example.istalumniapp.screen.CreateProfileScreen
import com.example.istalumniapp.screen.DashboardScreen
import com.example.istalumniapp.screen.DisplayJobScreen
import com.example.istalumniapp.screen.ForgotPasswordScreen
import com.example.istalumniapp.screen.ISTLoginScreen
import com.example.istalumniapp.screen.ISTRegisterScreen
import com.example.istalumniapp.screen.ViewAlumniProfilesScreen
import com.example.istalumniapp.screen.ViewProfileScreen
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel


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

