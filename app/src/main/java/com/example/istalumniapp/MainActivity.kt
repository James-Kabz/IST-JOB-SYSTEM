package com.example.istalumniapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.istalumniapp.nav.NavGraph
import com.example.istalumniapp.ui.theme.ISTALUMNIAPPTheme
import com.example.istalumniapp.utils.ProfileViewModel
import com.example.istalumniapp.utils.SharedViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var navController: NavHostController
    private val sharedViewModel: SharedViewModel by viewModels()
    private val profileViewModel : ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ISTALUMNIAPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val profileViewModel: ProfileViewModel =
                        viewModel(factory = ProfileViewModel.Factory)
                    navController = rememberNavController()

                    //Calling the NavGraph that contains the composable with screens
                    NavGraph(
                        navController = navController, sharedViewModel = sharedViewModel,
                        profileViewModel = profileViewModel)

                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ISTALUMNIAPPTheme {
//        Screens.ISTLoginScreen
    }
}