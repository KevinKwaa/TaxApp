package com.example.taxapp.CalendarEvent

import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.SpeakButton
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddEventScreen(
    date: LocalDate,
    onNavigateBack: () -> Unit,
    onEventSaved: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var eventName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var hasReminder by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }

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

    var showAccessibilitySettings by remember { mutableStateOf(false) }
    //var accessibilityState by remember { mutableStateOf(AccessibilityState()) }

    // Create a TTS instance if text-to-speech is enabled
    val tts = remember {
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

    // Get the locale from the language manager
    val locale = languageManager.getCurrentLocale()

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current

    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)
    var eventDate by remember { mutableStateOf(date.format(dateFormat)) }

    // Effect to update date format when language changes
    LaunchedEffect(currentLanguageCode) {
        val newDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", languageManager.getCurrentLocale())
        eventDate = date.format(newDateFormat)
    }

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground)
                .padding(16.dp)
        ) {
            // Header with enhanced styling
            Text(
                text = stringResource(id = R.string.event_details),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = accessibleColors.headerText,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDarkMode) 8.dp else 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = accessibleColors.cardBackground
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = accessibleColors.cardBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Event Name Field with accessible styling
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = {
                            eventName = it
                            // Optional: Add TTS feedback for input changes
                            if (accessibilityState.textToSpeech && eventName.isNotBlank()) {
                                tts?.speak(
                                    "Event name: $eventName",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        },
                        label = {
                            Text(
                                stringResource(id = R.string.event_name),
                                color = accessibleColors.calendarText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accessibleColors.selectedDay,
                            unfocusedBorderColor = accessibleColors.calendarBorder,
                            focusedTextColor = accessibleColors.calendarText,
                            unfocusedTextColor = accessibleColors.calendarText,
                            cursorColor = accessibleColors.selectedDay
                        )
                    )

                    // Event Date Field with accessible styling
                    OutlinedTextField(
                        value = eventDate,
                        onValueChange = { eventDate = it },
                        label = {
                            Text(
                                stringResource(id = R.string.event_date),
                                color = accessibleColors.calendarText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accessibleColors.selectedDay,
                            unfocusedBorderColor = accessibleColors.calendarBorder,
                            focusedTextColor = accessibleColors.calendarText,
                            unfocusedTextColor = accessibleColors.calendarText,
                            cursorColor = accessibleColors.selectedDay
                        )
                    )

                    // Start Time Field with accessible styling
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = {
                            Text(
                                stringResource(id = R.string.start_time),
                                color = accessibleColors.calendarText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accessibleColors.selectedDay,
                            unfocusedBorderColor = accessibleColors.calendarBorder,
                            focusedTextColor = accessibleColors.calendarText,
                            unfocusedTextColor = accessibleColors.calendarText,
                            cursorColor = accessibleColors.selectedDay
                        )
                    )

                    // End Time Field with accessible styling
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = {
                            Text(
                                stringResource(id = R.string.end_time),
                                color = accessibleColors.calendarText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accessibleColors.selectedDay,
                            unfocusedBorderColor = accessibleColors.calendarBorder,
                            focusedTextColor = accessibleColors.calendarText,
                            unfocusedTextColor = accessibleColors.calendarText,
                            cursorColor = accessibleColors.selectedDay
                        )
                    )

                    // Description Field with accessible styling
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(
                                stringResource(id = R.string.description),
                                color = accessibleColors.calendarText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(vertical = 8.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accessibleColors.selectedDay,
                            unfocusedBorderColor = accessibleColors.calendarBorder,
                            focusedTextColor = accessibleColors.calendarText,
                            unfocusedTextColor = accessibleColors.calendarText,
                            cursorColor = accessibleColors.selectedDay
                        )
                    )

                    // Reminder Toggle with accessible styling
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.reminder_for_event),
                            style = MaterialTheme.typography.bodyLarge,
                            color = accessibleColors.calendarText
                        )
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = {
                                hasReminder = it
                                // Add TTS feedback for switch toggle
                                if (accessibilityState.textToSpeech) {
                                    val message =
                                        if (it) "Reminder enabled" else "Reminder disabled"
                                    tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accessibleColors.selectedDay,
                                checkedTrackColor = accessibleColors.selectedDay.copy(alpha = 0.5f),
                                uncheckedThumbColor = accessibleColors.buttonBackground,
                                uncheckedTrackColor = accessibleColors.calendarBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons with accessible styling
                    Button(
                        onClick = {
                            if (eventName.isNotBlank()) {
                                val newEvent = Event(
                                    title = eventName,
                                    description = description,
                                    date = date,
                                    startTime = startTime,
                                    endTime = endTime,
                                    hasReminder = hasReminder
                                )
                                onEventSaved(newEvent)

                                // Add TTS feedback for save action
                                if (accessibilityState.textToSpeech) {
                                    tts?.speak("Event saved", TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        enabled = eventName.isNotBlank(), // Only enable if event has a name
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accessibleColors.buttonBackground,
                            contentColor = accessibleColors.buttonText,
                            disabledContainerColor = accessibleColors.buttonBackground.copy(alpha = 0.5f),
                            disabledContentColor = accessibleColors.buttonText.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(stringResource(id = R.string.save_event))
                    }

                    OutlinedButton(
                        onClick = {
                            onNavigateBack()
                            // Add TTS feedback for cancel action
                            if (accessibilityState.textToSpeech) {
                                tts?.speak("Cancelled", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accessibleColors.buttonBackground
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = accessibleColors.buttonBackground
                        )
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            }
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    // Update language code
                    currentLanguageCode = languageCode

                    // Get the updated locale from language manager
                    val newLocale = when (languageCode) {
                        "zh" -> Locale.CHINA
                        "ms" -> Locale("ms", "MY")
                        else -> Locale.ENGLISH
                    }

                    // Update language in the manager which affects the whole app
                    languageManager.setLanguage(languageCode)

                    // Refresh the date format
                    val newDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", newLocale)
                    eventDate = date.format(newDateFormat)
                },
                onDismiss = { showLanguageSelector = false },
                activity = activity
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

@Composable
fun AccessibleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelText: String,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    maxLines: Int = 1,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    val ttsManager = LocalTtsManager.current

    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it)
            // Optionally announce changes for more real-time feedback
            if (ttsManager != null && it.length % 5 == 0 && it.isNotEmpty()) {
                ttsManager.speak("$labelText: $it", TextToSpeech.QUEUE_FLUSH)
            }
        },
        label = { Text(labelText) },
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Read $labelText field") {
                    ttsManager?.speak("$labelText: $value")
                    true
                }
            )
        },
        readOnly = isReadOnly,
        maxLines = maxLines,
        colors = colors,
        trailingIcon = {
            // Add a speak button for each field
            ttsManager?.let {
                SpeakButton(
                    text = "$labelText: $value",
                    tint = LocalContentColor.current.copy(alpha = 0.6f),
                    contentDescription = "Read $labelText value"
                )
            }
        }
    )
}
