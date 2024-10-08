package com.example.istalumniapp.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.istalumniapp.utils.SharedViewModel
import com.example.istalumniapp.utils.SkillData
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSkillScreen(navController: NavController, sharedViewModel: SharedViewModel) {
    var skillName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Skill") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back_button"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TextField(
                    value = skillName,
                    onValueChange = { skillName = it },
                    label = { Text("Skill Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (skillName.isNotEmpty()) {
                            isSaving = true
                            val skill = SkillData(
                                skillID = UUID.randomUUID().toString(), // Generate a unique ID
                                skillName = skillName
                            )

                            sharedViewModel.saveSkill(skill, context = navController.context)
                            navController.popBackStack()  // Navigate back after saving
                        } else {
                            Toast.makeText(
                                navController.context,
                                "Please enter a skill name",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Skill")
                    }
                }
            }
        }
    }
}
