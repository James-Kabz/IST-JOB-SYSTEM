package com.example.istalumniapp.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.istalumniapp.R
import com.example.istalumniapp.nav.Screens

@Composable
fun ISTPreviewScreen(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Image as background
        
        Image(
            painter = painterResource(id = R.drawable.purple_honey),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Foreground: Login and Sign Up buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(50.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Move the Image to the top of the screen and increase its size
            Image(
                painter = painterResource(R.drawable.ist_logo), // Ensure R.drawable.ist_logo is correct
                contentDescription = "App Logo"
            )

            Text(
                text = "Welcome to IST Alumni App",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 20.sp
            )
            Text(
                text = "Dive in to find your dream job",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 18.sp
            )
            Button(
                onClick = {
                    navController.navigate(Screens.ISTLoginScreen.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),

            ) {
                Text(text = "Login")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Don't have an account?",
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = {
                    navController.navigate(Screens.ISTRegisterScreen.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(text = "Sign Up")
            }
        }
    }
}
