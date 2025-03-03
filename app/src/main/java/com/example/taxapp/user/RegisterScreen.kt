package com.example.taxapp.user

import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreen(modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel) {
    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var context = LocalContext.current

    var isLoading by remember {
        mutableStateOf(false)
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
    ScreenReader("Home Screen")
    val ttsManager = LocalTtsManager.current
    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Add back button at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        // Navigate back to auth screen
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to selection screen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

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

            Text(
                text = stringResource(id = R.string.hello),
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontSize = 30.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(id = R.string.create_account),
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontSize = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(id = R.drawable.register),
                contentDescription = "login",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text(text = stringResource(id = R.string.email),)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text(text = stringResource(id = R.string.password),)
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    isLoading = true
                    authViewModel.register(email, password) { success, errorMessage ->
                        if (success) {
                            ttsManager?.speak("Profile registered.")
                            isLoading = false
                            navController.navigate("profile") {
                                popUpTo("auth") { inclusive = true }
                            }
                        } else {
                            isLoading = false
                            AppUtil.showToast(context, errorMessage ?: "Something Went Wrong...")
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(text = if (isLoading) "Creating account" else "Register", fontSize = 22.sp)
            }

            if (isDarkMode) {
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accessibleColors.calendarText.copy(alpha = 0.7f)
                )
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