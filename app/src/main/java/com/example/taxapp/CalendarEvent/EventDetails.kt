package com.example.taxapp.CalendarEvent

import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.AccessibleColors
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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

    // Determine if this is a tax deadline event
    val isTaxDeadlineEvent = event.title.contains("Tax Filing Deadline", ignoreCase = true)

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
                            IconButton(onClick = { /* Navigate to home */ }) {
                                Icon(
                                    Icons.Filled.Home,
                                    contentDescription = "Home"
                                )
                            }

                            IconButton(onClick = { /* Navigate to calendar */ }) {
                                Icon(
                                    Icons.Filled.CalendarMonth,
                                    contentDescription = "Calendar"
                                )
                            }

                            IconButton(onClick = { /* Navigate to receipt */ }) {
                                Icon(
                                    Icons.Filled.Receipt,
                                    contentDescription = "Upload Receipt"
                                )
                            }

                            IconButton(onClick = { /* Navigate to category */ }) {
                                Icon(
                                    Icons.Filled.Category,
                                    contentDescription = "Categories"
                                )
                            }

                            IconButton(onClick = { /* Navigate to profile */ }) {
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
                            // Edit FAB
                            FloatingActionButton(
                                onClick = { showEditMode = true },
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
                    // Event details card - without duplicate controls
                    EventDetailCard(
                        event = event,
                        locale = locale,
                        accessibleColors = accessibleColors,
                        isDarkMode = isDarkMode,
                        onNavigateBack = onNavigateBack,
                        onShowEditMode = { showEditMode = true },
                        onShowDeleteConfirmation = { showDeleteConfirmation = true },
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(accessibleColors.calendarBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Enhanced Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDarkMode) {
                                listOf(
                                    accessibleColors.selectedDay.copy(alpha = 0.8f),
                                    accessibleColors.calendarBackground
                                )
                            } else {
                                listOf(
                                    accessibleColors.selectedDay.copy(alpha = 0.6f),
                                    accessibleColors.calendarBackground
                                )
                            }
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top row with back button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = accessibleColors.buttonBackground.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.cancel),
                                tint = accessibleColors.buttonText
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Screen title
                    Text(
                        text = stringResource(id = R.string.edit_event),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = accessibleColors.headerText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            // Form content
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = if (isDarkMode) 12.dp else 4.dp,
                        spotColor = accessibleColors.selectedDay.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
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
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = accessibleColors.selectedDay
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = accessibleColors.selectedDay
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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

                    // Reminder Toggle with accessible styling
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = accessibleColors.cardBackground.copy(
                                alpha = if (isDarkMode) 0.7f else 0.9f
                            )
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkMode) 4.dp else 1.dp
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

                    Spacer(modifier = Modifier.weight(1f))

                    // Action Buttons
                    val buttonShape = RoundedCornerShape(12.dp)

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
                            .height(56.dp),
                        enabled = eventName.isNotBlank(), // Only enable if event has a name
                        shape = buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accessibleColors.buttonBackground,
                            contentColor = accessibleColors.buttonText,
                            disabledContainerColor = accessibleColors.buttonBackground.copy(alpha = 0.5f),
                            disabledContentColor = accessibleColors.buttonText.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.save_changes),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = buttonShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accessibleColors.buttonBackground
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = accessibleColors.buttonBackground
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
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

    Card(
        modifier = modifier
            .shadow(
                elevation = if (isDarkMode) 12.dp else 4.dp,
                spotColor = accessibleColors.selectedDay.copy(alpha = 0.3f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                modifier = Modifier.padding(bottom = 16.dp),
                color = accessibleColors.selectedDay.copy(alpha = 0.3f),
                thickness = 2.dp
            )

            // Date section
            EventDetailRow(
                icon = Icons.Default.CalendarToday,
                label = stringResource(id = R.string.date_colon),
                value = event.date.format(dateFormat),
                colors = accessibleColors
            )

            // Time section
            EventDetailRow(
                icon = Icons.Default.AccessTime,
                label = stringResource(id = R.string.time_colon),
                value = "${event.startTime} - ${event.endTime}",
                colors = accessibleColors
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