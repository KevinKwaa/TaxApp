package com.example.taxapp.user

import android.os.Build
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.CalendarEvent.EventRepository
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import com.example.taxapp.user.AppUtil
//import com.example.smarttax_ver1.viewmodel.EditProfileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    editProfileViewModel: EditProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
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
                        text = stringResource(id = R.string.profile),

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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditProfileScreenContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    editProfileViewModel: EditProfileViewModel,
    authViewModel: AuthViewModel = viewModel()
){
    // Use collectAsState to properly observe viewModel state
    val email = editProfileViewModel.email
    val name = editProfileViewModel.name
    val phone = editProfileViewModel.phone
    val dob = editProfileViewModel.dob
    val income = editProfileViewModel.income
    val employment = editProfileViewModel.employment // Get the tax filing preference
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

    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe the current language
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    // Create a TTS instance if text-to-speech is enabled
    val tts = remember(accessibilityState.textToSpeech) {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }

    // Clean up TTS when not needed
    DisposableEffect(accessibilityState.textToSpeech) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    ScreenReader("Edit Profile Screen")
    val ttsManager = LocalTtsManager.current
    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground) // Add this critical line
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Language button with improved styling
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            accessibleColors.buttonBackground.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = accessibleColors.calendarBorder,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { showLanguageSelector = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "🌐",
                        style = MaterialTheme.typography.titleMedium,
                        color = accessibleColors.buttonText
                    )
                }

                // Accessibility button with improved styling
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            accessibleColors.buttonBackground.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = accessibleColors.calendarBorder,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable { showAccessibilitySettings = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⚙️",
                        style = MaterialTheme.typography.titleMedium,
                        color = accessibleColors.buttonText
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    color = accessibleColors.selectedDay // Use accessible color
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error, // Use theme error color instead of hard-coded Color.Red
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = {
                    editProfileViewModel.email = it
                },
                label = {
                    Text(text = stringResource(id = R.string.email))
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
                    ttsManager?.speak("Changing Name")
                    editProfileViewModel.name = it
                },
                label = {
                    Text(
                        text = stringResource(id = R.string.name),
                        color = accessibleColors.calendarText // Add proper color
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accessibleColors.selectedDay,
                    unfocusedBorderColor = accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    ttsManager?.speak("Changing phone number")
                    editProfileViewModel.phone = it
                },
                label = {
                    Text(text = stringResource(id = R.string.phone),
                        color = accessibleColors.calendarText)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accessibleColors.selectedDay,
                    unfocusedBorderColor = accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = dob,
                onValueChange = {
                    ttsManager?.speak("Changing Date of Birth")
                    editProfileViewModel.dob = it
                },
                label = {
                    Text(text = stringResource(id = R.string.date_of_birth),
                        color = accessibleColors.calendarText)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accessibleColors.selectedDay,
                    unfocusedBorderColor = accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = income,
                onValueChange = {
                    ttsManager?.speak("Changing income")
                    editProfileViewModel.income = it
                },
                label = {
                    Text(text = stringResource(id = R.string.total_income),
                        color = accessibleColors.calendarText)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accessibleColors.selectedDay,
                    unfocusedBorderColor = accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Add Tax Filing Preference Radio Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Tax Filing Preference",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Radio group
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Self filing option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editProfileViewModel.employment = "employee"
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = employment == "employee",
                                onClick = {
                                    editProfileViewModel.employment = "employee"
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.individual_employ),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.individual_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // self-employed filing option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editProfileViewModel.employment = "self-employed"
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = employment == "self-employed",
                                onClick = {
                                    editProfileViewModel.employment = "self-employed"
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.self_employed),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.self_employed_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    //
                    ttsManager?.speak("Saving Profile")
                    editProfileViewModel.updateUserProfile { success, error ->
                        if (success) {
                            updateSuccess = true
                            //Toast.makeText(LocalContext.current, "Profile Updated", Toast.LENGTH_SHORT).show()
                        } else {
                            AppUtil.showToast(context, errorMessage ?: "Something Went Wrong...")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accessibleColors.buttonBackground,
                    contentColor = accessibleColors.buttonText
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accessibleColors.buttonText,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(id = R.string.save_profile),)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Also update in EditProfileScreen.kt
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    ttsManager?.speak("Login out")
                    // Use authViewModel for logout with proper cleanup
                    authViewModel.logout {
                        navController.navigate("auth") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = accessibleColors.buttonBackground
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = accessibleColors.buttonBackground
                )
            ) {
                Text(text = stringResource(id = R.string.logout),)
            }
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    currentLanguageCode = languageCode
                },
                onDismiss = { showLanguageSelector = false },
                activity = activity  // Pass the activity
            )
        }

        if (showAccessibilitySettings) {
            AccessibilitySettings(
                currentSettings = accessibilityState,
                onSettingsChanged = { newSettings ->
                    coroutineScope.launch {
                        accessibilityRepository.updateSettings(newSettings)
                    }
                },
                onDismiss = { showAccessibilitySettings = false }
            )
        }
    }

}

