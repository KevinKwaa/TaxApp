package com.example.taxapp.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.CalendarEvent.EventRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = "Smart Tax Handler",

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
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(Icons.Filled.Home, contentDescription = "Localized description")
                    }

                    //Spacer(modifier = Modifier.weight(1f))

//                    //upload receipt
//                    IconButton(onClick = { navController.navigate("uploadReceipt") }) {
//                        Icon(
//                            Icons.Filled.AddCircle,
//                            contentDescription = "Localized description",
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.weight(1f))

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
        HomeScreenContent(modifier = modifier.padding(innerPadding), navController = navController)
    }

}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Smart Tax Handler",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Add buttons to access your tax features
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navController.navigate("calendar") }
        ) {
            Text(text = "Tax Calendar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navController.navigate("uploadReceipt") }
        ) {
            Text(text = "Upload Receipt")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { navController.navigate("category") }
        ) {
            Text(text = "Tax Categories")
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Use authViewModel for logout with proper cleanup
                authViewModel.logout {
                    navController.navigate("auth"){
                        popUpTo("home"){inclusive = true}
                    }
                }
            }
        ) {
            Text(text = "Logout")
        }
    }
}



