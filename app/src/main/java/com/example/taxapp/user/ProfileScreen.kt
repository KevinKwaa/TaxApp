package com.example.taxapp.user

import android.os.Build
import android.speech.tts.TextToSpeech
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
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
fun ProfileScreen(modifier: Modifier = Modifier, navController: NavHostController, authViewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var income by remember { mutableStateOf("") }
    var employment by remember { mutableStateOf("employee") }

    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var incomeError by remember { mutableStateOf<String?>(null) }

    // Overall form validation state
    var isFormValid by remember { mutableStateOf(false) }

    // Function to validate all fields and update form state
    fun validateForm() {
        // Validate each field
        val nameValidation = ValidationUtil.validateName(name)
        val phoneValidation = ValidationUtil.validatePhone(phone)
        val dobValidation = ValidationUtil.validateDOB(dob)
        val incomeValidation = ValidationUtil.validateIncome(income)

        // Update error messages
        nameError = if (!nameValidation.isValid) nameValidation.errorMessage else null
        phoneError = if (!phoneValidation.isValid) phoneValidation.errorMessage else null
        dobError = if (!dobValidation.isValid) dobValidation.errorMessage else null
        incomeError = if (!incomeValidation.isValid) incomeValidation.errorMessage else null

        // Form is valid if all fields are valid
        isFormValid = nameValidation.isValid && phoneValidation.isValid &&
                dobValidation.isValid && incomeValidation.isValid
    }

    // Validate on each change
    LaunchedEffect(name, phone, dob, income, employment) {
        validateForm()
    }

    var isProfileUpdated by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }

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
    ScreenReader("Profile Screen")
    val ttsManager = LocalTtsManager.current
    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Replace the existing Row and Text components with this Row that combines both
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heading text on the left side
                Text(
                    text = stringResource(id = R.string.complete_profile),
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accessibleColors.headerText,
                    modifier = Modifier.weight(1f)
                )

                // Accessibility buttons on the right side
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language button with improved styling
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                accessibleColors.buttonText,
                                CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = accessibleColors.buttonText,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable { showLanguageSelector = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🌐",
                            style = MaterialTheme.typography.titleSmall,
                            color = accessibleColors.buttonText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Accessibility button with improved styling
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                accessibleColors.buttonText,
                                CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = accessibleColors.buttonText,
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable { showAccessibilitySettings = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚙️",
                            style = MaterialTheme.typography.titleSmall,
                            color = accessibleColors.buttonText
                        )
                    }
                }
            }

            // Name field with validation
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    ttsManager?.speak("Entering name")
                },
                label = {
                    Text(text = stringResource(id = R.string.name),
                        color = accessibleColors.calendarText)
                },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (nameError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                    unfocusedBorderColor = if (nameError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Phone field with validation
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    ttsManager?.speak("Entering phone number")
                },
                label = {
                    Text(text = stringResource(id = R.string.phone),
                        color = accessibleColors.calendarText)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError != null,
                supportingText = {
                    phoneError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (phoneError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                    unfocusedBorderColor = if (phoneError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Date of Birth with validation
            OutlinedTextField(
                value = dob,
                onValueChange = {
                    dob = it
                    ttsManager?.speak("Entering date of birth")
                },
                label = {
                    Text(text = stringResource(id = R.string.date_of_birth),
                        color = accessibleColors.calendarText)
                },
                placeholder = {
                    Text(
                        text = "MM/DD/YYYY",
                        color = accessibleColors.calendarText.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                isError = dobError != null,
                supportingText = {
                    dobError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (dobError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                    unfocusedBorderColor = if (dobError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
                    focusedTextColor = accessibleColors.calendarText,
                    unfocusedTextColor = accessibleColors.calendarText
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Income with validation
            OutlinedTextField(
                value = income,
                onValueChange = {
                    income = it
                    ttsManager?.speak("Entering income")
                },
                label = {
                    Text(text = stringResource(id = R.string.total_income),
                        color = accessibleColors.calendarText)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = incomeError != null,
                supportingText = {
                    incomeError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (incomeError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                    unfocusedBorderColor = if (incomeError != null) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
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
                        // employee filing option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    employment = "employee"
                                    ttsManager?.speak("Selected employee option")
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = employment == "employee",
                                onClick = {
                                    employment = "employee"
                                    ttsManager?.speak("Selected employee option")
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
                                    employment = "self-employed"
                                    ttsManager?.speak("Selected self-employed option")
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = employment == "self-employed",
                                onClick = {
                                    employment = "self-employed"
                                    ttsManager?.speak("Selected self-employed option")
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

            // Display general form error message if needed
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    // Validate once more before submission
                    validateForm()

                    if (isFormValid) {
                        isLoading = true
                        ttsManager?.speak("Saving profile")
                        authViewModel.userProfile(name, phone, dob, income, employment) { success, error ->
                            isLoading = false
                            if (success) {
                                ttsManager?.speak("Profile saved successfully")
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            } else {
                                ttsManager?.speak("Failed to save profile. ${error ?: "Please try again"}")
                                errorMessage = error ?: "Something went wrong while saving your profile"
                                AppUtil.showToast(context, error ?: "Something Went Wrong...")
                            }
                        }
                    } else {
                        // Form is invalid - provide feedback with TTS
                        ttsManager?.speak("Please correct the errors in the form before continuing")
                        AppUtil.showToast(context, "Please correct the form errors before continuing")
                    }
                },
                enabled = !isLoading && isFormValid,
                modifier = Modifier.fillMaxWidth(),
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
                Text(text = if (isLoading) "Saving..." else "Save Profile")
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