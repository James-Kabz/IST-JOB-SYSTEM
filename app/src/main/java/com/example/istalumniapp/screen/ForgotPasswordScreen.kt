package com.example.istalumniapp.screen


import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable

fun ForgotPasswordScreen(navController: NavController)
{
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var isLoading by remember{ mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement =  Arrangement.Center
    ) {
        Text(text = "Forgot Password", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = email,
            onValueChange = {email = it} ,
            label = { Text(text = "Email")}
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                sendPasswordResetEmail(email.text, navController){
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }else(
                    Text(text = "Reset Password")
                    )
        }
    }
}

private fun sendPasswordResetEmail(email:String,navController: NavController,onComplete:()->Unit ) {
    val auth = FirebaseAuth.getInstance()

    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            onComplete()
            if (task.isSuccessful) {
                Toast.makeText(navController.context, "Password reset email sent", Toast.LENGTH_LONG).show()
//                back to login screen
                navController.popBackStack()
            }else{
                val errorMessage = task.exception?.message ?:"An error occurred"
                Toast.makeText(navController.context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
}