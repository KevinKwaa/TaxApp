package com.example.taxapp.CalendarEvent

import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    date: LocalDate,
    onNavigateBack: () -> Unit,
    onEventSaved: (Event) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    var eventName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var hasReminder by remember { mutableStateOf(false) }
    var isTodoEvent by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }

    // State for date and time pickers
    var selectedDate by remember { mutableStateOf(date) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Validation state
    var timeError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

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

    val displayDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
    val timeErrorMessage = stringResource(id = R.string.time_validator)

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.event_details),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        // Language button
                        IconButton(onClick = { showLanguageSelector = true }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Change Language",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Accessibility button
                        IconButton(onClick = { showAccessibilitySettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Accessibility Settings",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { navController.navigate("home") }) {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { onNavigateBack() }) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.primary // Highlight current screen
                            )
                        }

                        IconButton(onClick = { navController.navigate("uploadReceipt") }) {
                            Icon(
                                Icons.Filled.Receipt,
                                contentDescription = "Upload Receipt"
                            )
                        }

                        IconButton(onClick = { navController.navigate("category") }) {
                            Icon(
                                Icons.Filled.Category,
                                contentDescription = "Categories"
                            )
                        }

                        IconButton(onClick = { navController.navigate("editProfile") }) {
                            Icon(
                                Icons.Filled.AccountCircle,
                                contentDescription = "Account"
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        // Validate input before saving
                        if (eventName.isBlank()) {
                            // Show error for empty event name
                            return@ExtendedFloatingActionButton
                        }

                        // Validate end time is after start time
                        val timeValidation = TimeValidator.validateTimeOrder(startTime, endTime, timeErrorMessage)
                        if (!timeValidation.first) {
                            timeError = timeValidation.second
                            return@ExtendedFloatingActionButton
                        }

                        // If all validation passes, create and save the event
                        val newEvent = Event(
                            title = eventName,
                            description = description,
                            date = selectedDate,
                            startTime = startTime,
                            endTime = endTime,
                            hasReminder = hasReminder,
                            isTodoEvent = isTodoEvent,
                            isCompleted = false
                        )
                        onEventSaved(newEvent)
                    },
                    icon = { Icon(Icons.Filled.CalendarMonth, "Save Event") },
                    text = { Text(stringResource(id = R.string.save_event)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    expanded = eventName.isNotBlank()
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(accessibleColors.calendarBackground)
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
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

                        // Event Date Field with date picker
                        OutlinedTextField(
                            value = selectedDate.format(displayDateFormat),
                            onValueChange = { /* Read-only */ },
                            label = {
                                Text(
                                    stringResource(id = R.string.event_date),
                                    color = accessibleColors.calendarText
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showDatePicker = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = stringResource(id = R.string.select_date),
                                        tint = accessibleColors.selectedDay
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accessibleColors.selectedDay,
                                unfocusedBorderColor = accessibleColors.calendarBorder,
                                focusedTextColor = accessibleColors.calendarText,
                                unfocusedTextColor = accessibleColors.calendarText,
                                cursorColor = accessibleColors.selectedDay
                            )
                        )

                        // Start Time Field with time picker
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { /* Read-only */ },
                            label = {
                                Text(
                                    stringResource(id = R.string.start_time),
                                    color = accessibleColors.calendarText
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showStartTimePicker = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showStartTimePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = stringResource(id = R.string.select_start_time),
                                        tint = accessibleColors.selectedDay
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accessibleColors.selectedDay,
                                unfocusedBorderColor = accessibleColors.calendarBorder,
                                focusedTextColor = accessibleColors.calendarText,
                                unfocusedTextColor = accessibleColors.calendarText,
                                cursorColor = accessibleColors.selectedDay
                            )
                        )

                        // End Time Field with time picker
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { /* Read-only */ },
                            label = {
                                Text(
                                    stringResource(id = R.string.end_time),
                                    color = accessibleColors.calendarText
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showEndTimePicker = true },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showEndTimePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = stringResource(id = R.string.select_end_time),
                                        tint = accessibleColors.selectedDay
                                    )
                                }
                            },
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

                        // Add the To-Do Toggle after the reminder toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.make_todo_event),
                                style = MaterialTheme.typography.bodyLarge,
                                color = accessibleColors.calendarText
                            )
                            Switch(
                                checked = isTodoEvent,
                                onCheckedChange = {
                                    isTodoEvent = it
                                    // Add TTS feedback for switch toggle
                                    if (accessibilityState.textToSpeech) {
                                        val message =
                                            if (it) "To-do event enabled" else "To-do event disabled"
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
                    }
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            AccessibleDatePickerDialog(
                onDateSelected = { newDate ->
                    selectedDate = newDate
                    if (accessibilityState.textToSpeech) {
                        tts?.speak(
                            "Selected date: ${newDate.format(displayDateFormat)}",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                    // Clear date error if any
                    dateError = null
                },
                onDismiss = { showDatePicker = false },
                initialDate = selectedDate,
            )
        }

        // Start Time Picker Dialog
        if (showStartTimePicker) {
            AccessibleTimePickerDialog(
                onTimeSelected = { newTime ->
                    startTime = newTime
                    // Optionally adjust end time if start time is later
                    if (startTime > endTime) {
                        endTime = startTime
                    }
                    if (accessibilityState.textToSpeech) {
                        tts?.speak(
                            "Selected start time: $newTime",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            null
                        )
                    }
                    // Clear time error if any
                    timeError = null
                },
                onDismiss = { showStartTimePicker = false },
                initialTime = startTime,
                title = stringResource(id = R.string.select_start_time),
                validateTime = { time ->
                    // Basic time format validation
                    TimeValidator.validateTimeString(time)
                }
            )
        }

        // End Time Picker Dialog
        if (showEndTimePicker) {
            AccessibleTimePickerDialog(
                onTimeSelected = { newTime ->
                    // Validate end time is after start time
                    val validation = TimeValidator.validateTimeOrder(startTime, newTime, timeErrorMessage)
                    if (validation.first) {
                        endTime = newTime
                        timeError = null

                        if (accessibilityState.textToSpeech) {
                            tts?.speak(
                                "Selected end time: $newTime",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    } else {
                        timeError = validation.second

                        if (accessibilityState.textToSpeech) {
                            tts?.speak(
                                validation.second ?: "Invalid time",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                null
                            )
                        }
                    }
                },
                onDismiss = { showEndTimePicker = false },
                initialTime = endTime,
                title = stringResource(id = R.string.select_end_time),
                validateTime = { time ->
                    // Validate compared to start time
                    TimeValidator.validateTimeOrder(startTime, time, timeErrorMessage)
                }
            )
        }

        // Show validation errors if present
        if (timeError != null || dateError != null) {
            AlertDialog(
                onDismissRequest = {
                    timeError = null
                    dateError = null
                },
                title = { Text("Validation Error") },
                text = {
                    Column {
                        if (timeError != null) {
                            Text(
                                text = timeError ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (dateError != null) {
                            Text(
                                text = dateError ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        timeError = null
                        dateError = null
                    }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    // Update language code
                    currentLanguageCode = languageCode

                    // Update language in the manager which affects the whole app
                    languageManager.setLanguage(languageCode)
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
