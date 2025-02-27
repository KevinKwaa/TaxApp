package com.example.taxapp

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.AccessibilityThemeProvider
import com.example.taxapp.accessibility.FontSizeProvider
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventDetailScreen(
    event: Event,
    onNavigateBack: () -> Unit,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditMode by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }

    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe the current language - using consistent collectAsState pattern
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    // Observe accessibility settings - using repository instead of local state
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

    // Get the locale directly from the language manager
    val locale = languageManager.getCurrentLocale()

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        if (showEditMode) {
            EventEditMode(
                event = event,
                onNavigateBack = { showEditMode = false },
                onEventSaved = { updatedEvent ->
                    onEditEvent(updatedEvent)
                    showEditMode = false

                    // Add TTS feedback for edit completion
                    if (accessibilityState.textToSpeech) {
                        tts?.speak("Event updated", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                },
                accessibilityState = accessibilityState,
                currentLanguageCode = currentLanguageCode
            )
        } else {
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

                // Event Details Card with accessible styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
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
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = accessibleColors.headerText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = accessibleColors.calendarBorder
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.date_colon),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = accessibleColors.calendarText
                            )

                            // Format date according to locale
                            val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)
                            Text(
                                text = event.date.format(dateFormat),
                                style = MaterialTheme.typography.bodyLarge,
                                color = accessibleColors.calendarText
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.time_colon),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = accessibleColors.calendarText
                            )
                            Text(
                                text = "${event.startTime} - ${event.endTime}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = accessibleColors.calendarText
                            )
                        }

                        if (event.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.description_colon),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = accessibleColors.calendarText,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = accessibleColors.calendarText
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.reminder_colon),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = accessibleColors.calendarText
                            )
                            Text(
                                text = stringResource(
                                    id = if (event.hasReminder)
                                        R.string.enabled
                                    else
                                        R.string.disabled
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = accessibleColors.calendarText
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Action Buttons with accessible styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showEditMode = true
                            // Add TTS feedback
                            if (accessibilityState.textToSpeech) {
                                tts?.speak("Editing event", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accessibleColors.buttonBackground,
                            contentColor = accessibleColors.buttonText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.edit_event),
                            modifier = Modifier.padding(end = 8.dp),
                            tint = accessibleColors.buttonText
                        )
                        Text(stringResource(id = R.string.edit_event))
                    }

                    Button(
                        onClick = {
                            showDeleteConfirmation = true
                            // Add TTS feedback
                            if (accessibilityState.textToSpeech) {
                                tts?.speak(
                                    "Delete confirmation",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete_event),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(id = R.string.delete_event))
                    }
                }

                OutlinedButton(
                    onClick = {
                        onNavigateBack()
                        // Add TTS feedback
                        if (accessibilityState.textToSpeech) {
                            tts?.speak(
                                "Returning to calendar",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
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
                    Text(stringResource(id = R.string.back_to_calendar))
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.delete_event),
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.delete_confirmation),
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                },
                containerColor = if (isDarkMode) Color(0xFF202020) else Color.White,
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteEvent(event)
                            showDeleteConfirmation = false

                            // Add TTS feedback
                            if (accessibilityState.textToSpeech) {
                                tts?.speak("Event deleted", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation = false }
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            )
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    // Apply language without recreating activity
                    languageManager.setLanguage(languageCode, activity)
                },
                onDismiss = { showLanguageSelector = false },
                activity = activity
            )
        }

        if (showAccessibilitySettings) {
            AccessibilitySettings(
                currentSettings = accessibilityState,
                onSettingsChanged = { newSettings ->
                    // Persist accessibility settings using coroutine
                    coroutineScope.launch {
                        accessibilityRepository.updateSettings(newSettings)
                    }
                },
                onDismiss = { showAccessibilitySettings = false }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventEditMode(
    event: Event,
    onNavigateBack: () -> Unit,
    onEventSaved: (Event) -> Unit,
    accessibilityState: AccessibilityState,
    currentLanguageCode: String,
    modifier: Modifier = Modifier
) {
    // Get the custom colors from the accessibility theme
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current

    // Create a TTS instance if text-to-speech is enabled
    val context = LocalContext.current
    val tts = remember {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }

    // Clean up TTS when not needed
    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Use the currentLanguageCode parameter that gets passed in
    var eventName by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var hasReminder by remember { mutableStateOf(event.hasReminder) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(accessibleColors.calendarBackground)
            .padding(16.dp)
    ) {
        // Header with enhanced styling
        Text(
            text = stringResource(id = R.string.edit_event),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = accessibleColors.headerText,
            modifier = Modifier.padding(bottom = 24.dp)
        )

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
                    onValueChange = { eventName = it },
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
                                val message = if (it) "Reminder enabled" else "Reminder disabled"
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons with accessible styling
        Button(
            onClick = {
                if (eventName.isNotBlank()) {
                    val updatedEvent = event.copy(
                        title = eventName,
                        description = description,
                        startTime = startTime,
                        endTime = endTime,
                        hasReminder = hasReminder
                    )
                    onEventSaved(updatedEvent)
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
            Text(stringResource(id = R.string.save_changes))
        }

        OutlinedButton(
            onClick = onNavigateBack,
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