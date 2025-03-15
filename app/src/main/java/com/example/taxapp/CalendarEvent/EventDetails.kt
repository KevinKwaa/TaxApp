package com.example.taxapp.CalendarEvent

import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.AccessibleColors
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalThemeColorsAccessible
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event,
    onNavigateBack: () -> Unit,
    onEditEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onTodoStatusChange: (Event, Boolean) -> Unit, // Add handler for todo status change
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditMode by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }

    // Keep track of the current event state that might change during the screen's lifetime
    var currentEvent by remember { mutableStateOf(event) }

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
    val accessibleThemeColors = LocalThemeColorsAccessible.current
    val isDarkMode = LocalDarkMode.current

    var showSuccessMessage by remember { mutableStateOf(false) }

    // Determine if this is a tax deadline event
    val isTaxDeadlineEvent = currentEvent.title.contains("Tax Filing Deadline", ignoreCase = true)

    // Handler for todo status change
    val handleTodoStatusChange = { isCompleted: Boolean ->
        val updatedEvent = currentEvent.copy(isCompleted = isCompleted)
        onTodoStatusChange(updatedEvent, isCompleted)
        currentEvent = updatedEvent

        // Provide audio feedback if accessibility is enabled
        if (accessibilityState.textToSpeech) {
            val message = if (isCompleted) "Marked as completed" else "Marked as not completed"
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    val eventRepository = remember { EventRepository.getInstance() }

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        if (showEditMode) {
            EventEditMode(
                event = currentEvent,
                onNavigateBack = { showEditMode = false },
                onEventSaved = { updatedEvent ->
                    // Log the event update
                    Log.d("EventDetail", "Event update requested: ${updatedEvent.title}")
                    Log.d("EventDetail", "Original date: ${currentEvent.date}, Updated date: ${updatedEvent.date}")

                    // Update the repository and handle result
                    coroutineScope.launch {
                        val success = eventRepository.updateEvent(updatedEvent)
                        if (success) {
                            // Update local state
                            currentEvent = updatedEvent
                            // Show a success message
                            showSuccessMessage = true
                            // Set a timer to hide the message
                            delay(3000)
                            showSuccessMessage = false
                            // Force immediate refresh to update the calendar
                            eventRepository.forceRefresh()
                        } else {
                            // Show error message (implement as needed)
                            Log.e("EventDetail", "Failed to update event")
                        }
                    }

                    // Close edit mode
                    showEditMode = false

                    // Add TTS feedback for edit completion
                    if (accessibilityState.textToSpeech) {
                        tts?.speak("Event updated", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                },
                accessibilityState = accessibilityState,
                currentLanguageCode = currentLanguageCode,
                navController = navController,
                eventRepository = eventRepository // Pass the repository to edit mode
            )
        } else {
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
                    // Show action buttons only if not a tax deadline event
                    if (!isTaxDeadlineEvent) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // To-do toggle FAB (if it's a to-do event)
                            if (currentEvent.isTodoEvent) {
                                FloatingActionButton(
                                    onClick = { handleTodoStatusChange(!currentEvent.isCompleted) },
                                    containerColor = when {
                                        currentEvent.isCompleted -> MaterialTheme.colorScheme.error
                                        isPastDue(currentEvent) -> accessibleThemeColors.primaryButtonBackground
                                        else -> accessibleColors.selectedDay
                                    },
                                    contentColor = accessibleColors.buttonText
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (currentEvent.isCompleted)
                                                Icons.Default.Clear
                                            else
                                                Icons.Default.Check,
                                            contentDescription = null
                                        )
//                                        Text(
//                                            if (currentEvent.isCompleted)
//                                                stringResource(id = R.string.mark_as_incomplete)
//                                            else
//                                                stringResource(id = R.string.mark_as_complete)
//                                        )
                                    }
                                }
                            }

                            // Edit FAB
                            // Modify the call to showEditMode
                            FloatingActionButton(
                                onClick = {
                                    // Store the original event for reference
                                    val originalEvent = currentEvent
                                    showEditMode = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Event")
                            }

                            // Delete FAB
                            FloatingActionButton(
                                onClick = { showDeleteConfirmation = true },
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                            }
                        }
                    } else {
                        // For tax deadline events, just show a back button
                        FloatingActionButton(
                            onClick = onNavigateBack,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Calendar")
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.End
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accessibleColors.calendarBackground)
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    // Event details card - with todo status change handler
                    EventDetailCard(
                        event = currentEvent,
                        locale = locale,
                        accessibleColors = accessibleColors,
                        isDarkMode = isDarkMode,
                        onNavigateBack = onNavigateBack,
                        onShowEditMode = { showEditMode = true },
                        onShowDeleteConfirmation = { showDeleteConfirmation = true },
                        onTodoStatusChange = { event, isCompleted ->
                            handleTodoStatusChange(isCompleted)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 15.dp),
                        showButtons = false // Hide buttons as they're now in the FAB
                    )
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
                            onDeleteEvent(currentEvent)
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

// Helper function to check if a to-do event is past due
@RequiresApi(Build.VERSION_CODES.O)
fun isPastDue(event: Event): Boolean {
    if (!event.isTodoEvent || event.isCompleted) return false

    val today = LocalDate.now()
    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    return event.date.isBefore(today) ||
            (event.date.isEqual(today) && event.endTime < currentTime)
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventEditMode(
    event: Event,
    onNavigateBack: () -> Unit,
    onEventSaved: (Event) -> Unit,
    accessibilityState: AccessibilityState,
    currentLanguageCode: String,
    modifier: Modifier = Modifier,
    navController: NavHostController,
    eventRepository: EventRepository
) {
    // Get the custom colors from the accessibility theme
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Create a TTS instance if text-to-speech is enabled
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

    // Form state variables
    var eventName by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime) }
    var hasReminder by remember { mutableStateOf(event.hasReminder) }
    var isTodoEvent by remember { mutableStateOf(event.isTodoEvent) }
    var isCompleted by remember { mutableStateOf(event.isCompleted) }
    var selectedDate by remember { mutableStateOf(event.date) }

    // State for picker dialogs
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }

    // Validation state
    var timeError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    // Get the locale from the language manager
    val locale = languageManager.getCurrentLocale()

    // Date formatter for display
    val displayDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
    val timeErrorMessage = stringResource(id = R.string.time_validator)

    // Check if the event is past due (for to-do events)
    val isPastDue = isTodoEvent && !isCompleted &&
            (LocalDate.now().isAfter(event.date) ||
                    (LocalDate.now().isEqual(event.date) &&
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) > endTime))

    // Scaffold implementation for consistent UI
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.edit_event),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Cancel Edit"
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
            FloatingActionButton(
                onClick = {
                    // Validate input before saving
                    if (eventName.isBlank()) {
                        // Don't allow blank event names
                        return@FloatingActionButton
                    }

                    // Validate end time is after start time
                    val timeValidation = TimeValidator.validateTimeOrder(startTime, endTime, timeErrorMessage)
                    if (!timeValidation.first) {
                        timeError = timeValidation.second
                        return@FloatingActionButton
                    }

                    // If all validation passes, create and save the updated event
                    val updatedEvent = event.copy(
                        title = eventName,
                        description = description,
                        date = selectedDate,
                        startTime = startTime,
                        endTime = endTime,
                        hasReminder = hasReminder,
                        isTodoEvent = isTodoEvent,
                        isCompleted = isCompleted
                    )
                    onEventSaved(updatedEvent)

                    // Add TTS feedback
                    if (accessibilityState.textToSpeech) {
                        tts?.speak("Saving changes", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save Changes")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        // Main content area
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = accessibleColors.calendarBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Form card with fields
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
                        color = when {
                            isTodoEvent && isPastDue -> Color.Red.copy(alpha = 0.5f)
                            isTodoEvent && !isCompleted -> accessibleColors.selectedDay.copy(alpha = 0.5f)
                            else -> accessibleColors.cardBorder
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Event Name Field with accessible styling and icon
                        OutlinedTextField(
                            value = eventName,
                            onValueChange = { eventName = it },
                            label = {
                                Text(
                                    stringResource(id = R.string.event_name),
                                    color = accessibleColors.calendarText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = accessibleColors.selectedDay
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accessibleColors.selectedDay,
                                unfocusedBorderColor = accessibleColors.calendarBorder,
                                focusedTextColor = accessibleColors.calendarText,
                                unfocusedTextColor = accessibleColors.calendarText,
                                cursorColor = accessibleColors.selectedDay
                            )
                        )

                        // Event Date Field with accessible date picker
                        OutlinedTextField(
                            value = selectedDate.format(displayDateFormat),
                            onValueChange = { /* Read-only */ },
                            label = {
                                Text(
                                    stringResource(id = R.string.event_date),
                                    color = accessibleColors.calendarText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = accessibleColors.selectedDay
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
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

                        // Start Time Field
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { /* Read-only */ },
                            label = {
                                Text(
                                    stringResource(id = R.string.start_time),
                                    color = accessibleColors.calendarText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = accessibleColors.selectedDay
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStartTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
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

                        // End Time Field
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { /* Read-only */ },
                            label = {
                                Text(
                                    stringResource(id = R.string.end_time),
                                    color = accessibleColors.calendarText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = accessibleColors.selectedDay
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showEndTimePicker = true },
                            shape = RoundedCornerShape(12.dp),
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

                        // Description Field
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = {
                                Text(
                                    stringResource(id = R.string.description),
                                    color = accessibleColors.calendarText
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = accessibleColors.selectedDay
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accessibleColors.selectedDay,
                                unfocusedBorderColor = accessibleColors.calendarBorder,
                                focusedTextColor = accessibleColors.calendarText,
                                unfocusedTextColor = accessibleColors.calendarText,
                                cursorColor = accessibleColors.selectedDay
                            )
                        )

                        // Todo Event Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTodoEvent)
                                    accessibleColors.selectedDay.copy(alpha = 0.1f)
                                else
                                    accessibleColors.cardBackground.copy(
                                        alpha = if (isDarkMode) 0.7f else 0.9f
                                    )
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isTodoEvent)
                                    accessibleColors.selectedDay.copy(alpha = 0.5f)
                                else
                                    accessibleColors.cardBorder.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Label with icon
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = null,
                                        tint = if (isTodoEvent)
                                            accessibleColors.selectedDay
                                        else
                                            accessibleColors.calendarBorder
                                    )

                                    Text(
                                        text = stringResource(id = R.string.make_todo_event),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = accessibleColors.calendarText
                                    )
                                }

                                // Switch
                                Switch(
                                    checked = isTodoEvent,
                                    onCheckedChange = {
                                        isTodoEvent = it
                                        // Reset completed status if turning off to-do
                                        if (!it) isCompleted = false

                                        // Add TTS feedback for switch toggle
                                        if (accessibilityState.textToSpeech) {
                                            val message = if (it) "To-do event enabled" else "To-do event disabled"
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

                        // Completed toggle (only show if it's a to-do event)
//                        AnimatedVisibility(visible = isTodoEvent) {
//                            Card(
//                                modifier = Modifier.fillMaxWidth(),
//                                shape = RoundedCornerShape(12.dp),
//                                colors = CardDefaults.cardColors(
//                                    containerColor = if (isCompleted)
//                                        Color.Gray.copy(alpha = 0.1f)
//                                    else if (isPastDue)
//                                        Color.Red.copy(alpha = 0.1f)
//                                    else
//                                        accessibleColors.cardBackground.copy(
//                                            alpha = if (isDarkMode) 0.7f else 0.9f
//                                        )
//                                ),
//                                border = BorderStroke(
//                                    width = 1.dp,
//                                    color = when {
//                                        isCompleted -> Color.Gray.copy(alpha = 0.5f)
//                                        isPastDue -> Color.Red.copy(alpha = 0.5f)
//                                        else -> accessibleColors.cardBorder.copy(alpha = 0.5f)
//                                    }
//                                )
//                            ) {
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(horizontal = 16.dp, vertical = 12.dp),
//                                    horizontalArrangement = Arrangement.SpaceBetween,
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    // Label with icon
//                                    Row(
//                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Icon(
//                                            imageVector = if (isCompleted)
//                                                Icons.Default.Check
//                                            else if (isPastDue)
//                                                Icons.Default.Warning
//                                            else
//                                                Icons.Default.RadioButtonUnchecked,
//                                            contentDescription = null,
//                                            tint = when {
//                                                isCompleted -> Color.Gray
//                                                isPastDue -> Color.Red
//                                                else -> accessibleColors.calendarBorder
//                                            }
//                                        )
//
//                                        Column {
//                                            Text(
//                                                text = stringResource(id = R.string.mark_as_completed),
//                                                style = MaterialTheme.typography.bodyLarge,
//                                                color = when {
//                                                    isCompleted -> Color.Gray
//                                                    isPastDue -> Color.Red
//                                                    else -> accessibleColors.calendarText
//                                                }
//                                            )
//
//                                            if (isPastDue) {
//                                                Text(
//                                                    text = stringResource(id = R.string.past_due_warning),
//                                                    style = MaterialTheme.typography.bodySmall,
//                                                    color = Color.Red
//                                                )
//                                            }
//                                        }
//                                    }
//
//                                    // Checkbox
//                                    Checkbox(
//                                        checked = isCompleted,
//                                        onCheckedChange = {
//                                            isCompleted = it
//                                            // Add TTS feedback for switch toggle
//                                            if (accessibilityState.textToSpeech) {
//                                                val message = if (it) "Marked as completed" else "Marked as incomplete"
//                                                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
//                                            }
//                                        },
//                                        colors = CheckboxDefaults.colors(
//                                            checkedColor = Color.Gray,
//                                            uncheckedColor = if (isPastDue) Color.Red else accessibleColors.calendarBorder
//                                        )
//                                    )
//                                }
//                            }
//                        }

                        // Reminder Toggle with card styling to match other screens
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accessibleColors.cardBackground.copy(
                                    alpha = if (isDarkMode) 0.7f else 0.9f
                                )
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = accessibleColors.cardBorder.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Label with icon
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (hasReminder)
                                            Icons.Default.Notifications
                                        else
                                            Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (hasReminder)
                                            accessibleColors.selectedDay
                                        else
                                            accessibleColors.calendarBorder
                                    )

                                    Text(
                                        text = stringResource(id = R.string.reminder_for_event),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = accessibleColors.calendarText
                                    )
                                }

                                // Switch
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
                        }
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
//            validateDate = { date ->
//                // For editing, we'll allow past dates (user might be editing an old event)
//                Pair(true, null)
//            }
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventDetailCard(
    event: Event,
    locale: java.util.Locale,
    accessibleColors: AccessibleColors,
    isDarkMode: Boolean,
    onNavigateBack: () -> Unit,
    onShowEditMode: () -> Unit,
    onShowDeleteConfirmation: () -> Unit,
    onTodoStatusChange: ((Event, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    showButtons: Boolean = true // Parameter to control button visibility
) {
    val context = LocalContext.current
    val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

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

    // Determine if this is a tax deadline event
    val isTaxDeadlineEvent = event.title.contains("Tax Filing Deadline", ignoreCase = true)

    // Check if event is past due
    val isPastDue = event.isTodoEvent && !event.isCompleted &&
            (LocalDate.now().isAfter(event.date) ||
                    (LocalDate.now().isEqual(event.date) &&
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) > event.endTime))

    Card(
        modifier = modifier
//            .shadow(
//                elevation = if (isDarkMode) 12.dp else 4.dp,
//                //spotColor = Color.Transparent,
//                spotColor = when {
//                    event.isTodoEvent && isPastDue -> Color.Red.copy(alpha = 0.5f)
//                    event.isTodoEvent && !event.isCompleted -> accessibleColors.selectedDay.copy(alpha = 0.3f)
//                    else -> accessibleColors.selectedDay.copy(alpha = 0.3f)
//                },
//                shape = RoundedCornerShape(24.dp)
            ,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                event.isTodoEvent && event.isCompleted -> Color.Gray.copy(alpha = 0.1f)
                event.isTodoEvent && isPastDue -> Color.Red.copy(alpha = 0.05f)
                event.isTodoEvent -> accessibleColors.selectedDay.copy(alpha = 0.05f)
                else -> accessibleColors.cardBackground
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                event.isTodoEvent && isPastDue -> Color.Red.copy(alpha = 0.5f)
                event.isTodoEvent && !event.isCompleted -> accessibleColors.selectedDay.copy(alpha = 0.7f)
                else -> accessibleColors.cardBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title with to-do indicator if applicable
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (event.isTodoEvent) {
                    // Todo indicator icon
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "To-do Event",
                        tint = when {
                            isPastDue -> Color.Red
                            event.isCompleted -> Color.Gray
                            else -> accessibleColors.selectedDay
                        },
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (event.isTodoEvent && event.isCompleted)
                            TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = when {
                        event.isTodoEvent && isPastDue -> Color.Red
                        event.isTodoEvent && event.isCompleted -> Color.Gray
                        else -> accessibleColors.headerText
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // To-do status section (if applicable)
            if (event.isTodoEvent) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isPastDue -> Color.Red.copy(alpha = 0.1f)
                            event.isCompleted -> Color.Gray.copy(alpha = 0.1f)
                            else -> accessibleColors.selectedDay.copy(alpha = 0.1f)
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isPastDue -> Color.Red.copy(alpha = 0.3f)
                            event.isCompleted -> Color.Gray.copy(alpha = 0.3f)
                            else -> accessibleColors.selectedDay.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.todo_status),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isPastDue -> Color.Red
                                    event.isCompleted -> Color.Gray
                                    else -> accessibleColors.headerText
                                }
                            )

                            Text(
                                text = when {
                                    isPastDue -> stringResource(id = R.string.past_due)
                                    event.isCompleted -> stringResource(id = R.string.completed)
                                    else -> stringResource(id = R.string.pending)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isPastDue -> Color.Red
                                    event.isCompleted -> Color.Gray
                                    else -> accessibleColors.calendarText
                                }
                            )
                        }

                        // Only show checkbox if we have the handler function
                        if (onTodoStatusChange != null) {
                            Checkbox(
                                checked = event.isCompleted,
                                onCheckedChange = { isChecked ->
                                    onTodoStatusChange(event, isChecked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = when {
                                        isPastDue -> Color.Red
                                        else -> accessibleColors.selectedDay
                                    },
                                    uncheckedColor = when {
                                        isPastDue -> Color.Red
                                        else -> accessibleColors.calendarBorder
                                    }
                                )
                            )
                        }
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(bottom = 16.dp),
                color = accessibleColors.selectedDay.copy(alpha = 0.3f),
                thickness = 2.dp
            )

            // Date section
            EventDetailRow(
                icon = Icons.Default.CalendarToday,
                label = stringResource(id = R.string.date_colon),
                value = event.date.format(dateFormat),
                colors = accessibleColors,
                warningColor = if (isPastDue) Color.Red else null
            )

            // Time section
            EventDetailRow(
                icon = Icons.Default.AccessTime,
                label = stringResource(id = R.string.time_colon),
                value = "${event.startTime} - ${event.endTime}",
                colors = accessibleColors,
                warningColor = if (isPastDue) Color.Red else null
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = accessibleColors.calendarBorder.copy(alpha = 0.5f)
            )

            // Description section (if present)
            if (event.description.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = accessibleColors.selectedDay,
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = stringResource(id = R.string.description_colon),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.calendarText
                        )
                    }

                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = accessibleColors.calendarText,
                        modifier = Modifier.padding(start = 40.dp)
                    )
                }

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = accessibleColors.calendarBorder.copy(alpha = 0.5f)
                )
            }

            // Reminder section
            EventReminderRow(
                hasReminder = event.hasReminder,
                colors = accessibleColors
            )

            Spacer(modifier = Modifier.weight(1f))

            // Only show buttons if requested (default is true)
            if (showButtons) {
                // Show Edit and Delete buttons only if not a tax deadline event
                if (!isTaxDeadlineEvent) {
                    // If it's a to-do event, add a button to toggle completion
                    if (event.isTodoEvent && onTodoStatusChange != null) {
                        Button(
                            onClick = { onTodoStatusChange(event, !event.isCompleted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isPastDue -> Color.Red
                                    event.isCompleted -> Color.Gray
                                    else -> accessibleColors.selectedDay
                                },
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (event.isCompleted) Icons.Default.Clear else Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = if (event.isCompleted)
                                        stringResource(id = R.string.mark_as_incomplete)
                                    else
                                        stringResource(id = R.string.mark_as_complete),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Edit button - with improved styling
                    Button(
                        onClick = onShowEditMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accessibleColors.buttonBackground,
                            contentColor = accessibleColors.buttonText
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = accessibleColors.calendarBorder.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                                tint = accessibleColors.buttonText
                            )
                            Text(
                                text = stringResource(id = R.string.edit_event),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete button - with improved styling
                    Button(
                        onClick = onShowDeleteConfirmation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.delete_event),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Back to calendar button - with improved styling
                // ALWAYS show this button for ALL events
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accessibleColors.buttonBackground,
                        contentColor = accessibleColors.buttonText
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.back_to_calendar),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EventDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    colors: AccessibleColors,
    warningColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = warningColor ?: colors.selectedDay,
            modifier = Modifier.size(28.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = warningColor ?: colors.calendarText
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = warningColor ?: colors.calendarText
            )
        }
    }
}

@Composable
fun EventDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    colors: AccessibleColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.selectedDay,
            modifier = Modifier.size(28.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.calendarText
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.calendarText
            )
        }
    }
}

@Composable
fun EventReminderRow(
    hasReminder: Boolean,
    colors: AccessibleColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (hasReminder) Icons.Default.Notifications else Icons.Default.NotificationsOff,
            contentDescription = null,
            tint = if (hasReminder) colors.selectedDay else colors.calendarBorder,
            modifier = Modifier.size(28.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(id = R.string.reminder_colon),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.calendarText
            )

            Text(
                text = stringResource(id = if (hasReminder) R.string.enabled else R.string.disabled),
                style = MaterialTheme.typography.bodyLarge,
                color = if (hasReminder) colors.selectedDay else colors.calendarText
            )
        }

        // Status indicator
        val color by animateColorAsState(
            targetValue = if (hasReminder) colors.selectedDay else colors.calendarBorder,
            label = "reminderColor"
        )

        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
    }
}