package com.example.taxapp.user

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Patterns
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.taxapp.accessibility.LocalColorBlindMode
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalHighContrastMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.accessibility.scaledSp
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

    // Add confirmation password state
    var confirmPassword by remember {
        mutableStateOf("")
    }

    // Add validation states
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Form validation state
    var isFormValid by remember { mutableStateOf(false) }

    var context = LocalContext.current

    // Function to validate the form
    fun validateForm(context: Context) {
        // Get context for string resources

        // Email basic validation with localized strings
        emailError = if (email.isBlank()) context.getString(R.string.error_email_empty)
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) context.getString(R.string.error_email_format)
        else null

        // Password validation with context
        val passwordValidation = ValidationUtil.validatePassword(password, context)
        passwordError = if (!passwordValidation.isValid) passwordValidation.errorMessage else null

        // Confirm password validation with context
        val confirmValidation = ValidationUtil.validatePasswordConfirmation(password, confirmPassword, context)
        confirmPasswordError = if (!confirmValidation.isValid) confirmValidation.errorMessage else null

        // Form is valid if all fields are valid
        isFormValid = emailError == null && passwordError == null && confirmPasswordError == null &&
                email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
    }

    // Validate on each change
    LaunchedEffect(email, password, confirmPassword) {
        validateForm(context)
    }

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
    val isColorBlind = LocalColorBlindMode.current
    val isHighContrast = LocalHighContrastMode.current
    val ttsManager = LocalTtsManager.current

    // Screen reader for accessibility
    ScreenReader("Register Screen")

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground)
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())  // Add scrolling for the longer form,
                    .background(accessibleColors.calendarBackground),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add back button at the top
                    IconButton(
                        onClick = {
                            ttsManager?.speak("Returning")
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to selection screen",
                            tint = accessibleColors.headerText
                        )
                    }

                    // This spacer pushes the buttons to opposite sides
                    Spacer(modifier = Modifier.weight(1f))

                    // Language button
                    IconButton(
                        onClick = { showLanguageSelector = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                showLanguageSelector = true
                                ttsManager?.speak("Opening language selector")
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Change Language",
                            modifier = Modifier.size(24.dp),
                            tint = accessibleColors.headerText
                        )
                    }


                    // Small space between the right buttons
                    Spacer(modifier = Modifier.width(8.dp))

                    // Accessibility button with improved styling
                    // Accessibility button
                    IconButton(
                        onClick = { showAccessibilitySettings = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                showLanguageSelector = true
                                ttsManager?.speak("Opening language selector")
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Accessibility Settings",
                            modifier = Modifier.size(24.dp),
                            tint = accessibleColors.headerText
                        )
                    }
                }

                Text(
                    text = stringResource(id = R.string.hello),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = accessibleColors.headerText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(id = R.string.create_account),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    color = accessibleColors.calendarText
                )

                Spacer(modifier = Modifier.height(20.dp))

                Image(
                    painter = painterResource(id = R.drawable.register),
                    contentDescription = "resgiter",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = {
                        Text(
                            text = stringResource(id = R.string.email),
                            color = accessibleColors.calendarText.copy(alpha = 0.8f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accessibleColors.selectedDay,
                        unfocusedBorderColor = accessibleColors.calendarBorder,
                        focusedTextColor = accessibleColors.calendarText,
                        unfocusedTextColor = accessibleColors.calendarText,
                        cursorColor = accessibleColors.selectedDay,
                        focusedLabelColor = accessibleColors.selectedDay,
                        unfocusedLabelColor = accessibleColors.calendarText.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field with validation
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            text = stringResource(id = R.string.password),
                            color = accessibleColors.calendarText.copy(alpha = 0.8f)
                        )
                    },
                    isError = passwordError != null,
                    supportingText = {
                        passwordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                        unfocusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
                        focusedTextColor = accessibleColors.calendarText,
                        unfocusedTextColor = accessibleColors.calendarText,
                        cursorColor = accessibleColors.selectedDay,
                        focusedLabelColor = if (passwordError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                        unfocusedLabelColor = if (passwordError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarText.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm password field with validation
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = {
                        Text(
                            text = stringResource(id = R.string.confirm_password),
                            color = accessibleColors.calendarText.copy(alpha = 0.8f)
                        )
                    },
                    isError = confirmPasswordError != null,
                    supportingText = {
                        confirmPasswordError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                        unfocusedBorderColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
                        focusedTextColor = accessibleColors.calendarText,
                        unfocusedTextColor = accessibleColors.calendarText,
                        cursorColor = accessibleColors.selectedDay,
                        focusedLabelColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                        unfocusedLabelColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarText.copy(alpha = 0.7f)
                    )
                )

                // Password requirements text
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.password_requirements),
                    //"Password must be at least 8 characters with uppercase, number, and special character"
                    style = MaterialTheme.typography.bodySmall,
                    color = accessibleColors.calendarText.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Validate once more before submission
                        validateForm(context)

                        if (isFormValid) {
                            isLoading = true
                            ttsManager?.speak("Creating account")
                            authViewModel.register(email, password) { success, errorMessage ->
                                if (success) {
                                    isLoading = false
                                    ttsManager?.speak("Registration successful")
                                    navController.navigate("profile") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                } else {
                                    isLoading = false
                                    ttsManager?.speak("Registration failed. ${errorMessage ?: "Please try again"}")
                                    AppUtil.showToast(context, errorMessage ?: "Something went wrong")
                                }
                            }
                        } else {
                            // Form is invalid - provide feedback with TTS
                            ttsManager?.speak("Please correct the errors in the form before continuing")
                            AppUtil.showToast(context, "Please correct the form errors before continuing")
                        }
                    },
                    enabled = !isLoading && isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accessibleColors.buttonBackground,
                        contentColor = accessibleColors.buttonText,
                        disabledContainerColor = accessibleColors.buttonBackground.copy(alpha = 0.5f),
                        disabledContentColor = accessibleColors.buttonText.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accessibleColors.buttonText,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isLoading) stringResource(id = R.string.creating_account) else stringResource(id = R.string.register),
                        fontSize = scaledSp(22),
                        fontWeight = FontWeight.Medium
                    )
                }
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