package com.example.taxapp.user

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.user.AppUtil
//import com.example.smarttax_ver1.viewmodel.EditProfileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    editProfileViewModel: EditProfileViewModel = viewModel()
){
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = "Profile",

                        style = TextStyle(
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )
        },

        bottomBar = {
            BottomAppBar(
                actions = {
                    //home
                    IconButton(onClick = { navController.navigate("home")}) {
                        Icon(Icons.Filled.Home, contentDescription = "Localized description")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //upload receipt
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Localized description",
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //profile
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            Icons.Filled.Face,
                            contentDescription = "Localized description",
                        )
                    }

                    //add more
                },

                )
        }
    ) { innerPadding ->
        EditProfileScreenContent(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            editProfileViewModel = editProfileViewModel
        )
    }


}

@Composable
fun EditProfileScreenContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    editProfileViewModel: EditProfileViewModel
){
    // Use collectAsState to properly observe viewModel state
    val email = editProfileViewModel.email
    val name = editProfileViewModel.name
    val phone = editProfileViewModel.phone
    val dob = editProfileViewModel.dob
    val income = editProfileViewModel.income
    val isLoading = editProfileViewModel.isLoading
    val errorMessage = editProfileViewModel.errorMessage

    var context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Track if update was successful to show feedback
    var updateSuccess by remember { mutableStateOf(false) }

    // Effect to show success message
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            updateSuccess = false
        }
    }



    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(20.dp))
        }

        errorMessage?.let {
            Text(text = it, color = Color.Red)
            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                editProfileViewModel.email = it
            },
            label = {
                Text(text = "Email address")
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,  // This disables the field
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))


        OutlinedTextField(
            value = name,
            onValueChange = {
                editProfileViewModel.name = it
            },
            label = {
                Text(text = "Name")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                editProfileViewModel.phone = it
            },
            label = {
                Text(text = "Phone Number")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = dob,
            onValueChange = {
                editProfileViewModel.dob = it
            },
            label = {
                Text(text = "Date Of Birth")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value =income,
            onValueChange = {
                editProfileViewModel.income = it
            },
            label = {
                Text(text = "Total Income")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                //
                editProfileViewModel.updateUserProfile { success, error ->
                    if (success) {
                        updateSuccess = true
                        //Toast.makeText(LocalContext.current, "Profile Updated", Toast.LENGTH_SHORT).show()
                    } else {
                        AppUtil.showToast(context, errorMessage?:"Something Went Wrong...")
                    }
                }
            }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Save Profile")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                Firebase.auth.signOut()
                navController.navigate("auth"){
                    popUpTo("home"){inclusive = true}
                }
            }
        ) {
            Text(text = "Logout")
        }

    }

}

