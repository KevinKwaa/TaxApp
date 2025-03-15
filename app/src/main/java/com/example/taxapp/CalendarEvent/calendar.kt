package com.example.taxapp.CalendarEvent

import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.AccessibleColors
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalFontSizeAdjustment
import com.example.taxapp.accessibility.LocalHighContrastMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.accessibility.SpeakButton
import com.example.taxapp.accessibility.SpeakableContent
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

data class Event(
    val title: String,
    val description: String,
    val date: LocalDate,
    val startTime: String,
    val endTime: String,
    var hasReminder: Boolean = false,
    var isTodoEvent: Boolean = false,
    var isCompleted: Boolean = false
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    events: Map<LocalDate, List<Event>>,
    currentUserId: String,
    refreshKey: Long = 0,
    onNavigateToAddEvent: (LocalDate) -> Unit,
    onNavigateToEventDetails: (Event) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    var showPastDueNotification by remember { mutableStateOf(false) }
    var pastDueCount by remember { mutableStateOf(0) }

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

    // Get event repository
    val eventRepository = remember { EventRepository.getInstance() }

    // Force refresh key - this will trigger recomposition when refreshed
    val refreshTrigger by eventRepository.forceRefreshTrigger.collectAsState()

    // Get the lifecycle owner to handle proper cleanup
    val lifecycleOwner = LocalLifecycleOwner.current

    // Create state for events - this is key to refreshing UI
    var eventsState by remember(refreshTrigger, currentUserId) {
        mutableStateOf(events)
    }

    // Update the events state when events change
    LaunchedEffect(events, refreshTrigger, refreshKey) {
        Log.d("CalendarScreen", "Events updated, count: ${events.size}")
        eventsState = events
    }

    // Clean up TTS when not needed
    DisposableEffect(accessibilityState.textToSpeech) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Collect events safely with lifecycle awareness
    val liveEvents by produceState(
        initialValue = events,
        key1 = refreshKey,
        key2 = currentUserId,
        key3 = refreshTrigger
    ) {
        // Create a Job that can be cancelled
        val collectJob = coroutineScope.launch {
            try {
                // Use a larger timeout for initial loading
                withTimeout(10000) { // Increased from 5000ms to 10000ms
                    eventRepository.getAllEvents(currentUserId).collect { freshEvents ->
                        Log.d("CalendarScreen", "UI Update: Live events updated, count: ${freshEvents.size}")
                        value = freshEvents

                        // Check for past due todo events
                        val today = LocalDate.now()
                        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        val allPastDueEvents = freshEvents.values.flatten().filter { event ->
                            event.isTodoEvent && !event.isCompleted && (
                                    event.date.isBefore(today) ||
                                            (event.date.isEqual(today) && event.endTime < currentTime)
                                    )
                        }

                        pastDueCount = allPastDueEvents.size
                        if (pastDueCount > 0 && !showPastDueNotification) {
                            showPastDueNotification = true
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle the error but don't crash
                Log.e("CalendarScreen", "Error collecting events: ${e.message}")
                // On error, use the last known good value
            }
        }
    }

    // Function to handle todo status changes
    val handleTodoStatusChange = { event: Event, isCompleted: Boolean ->
        coroutineScope.launch {
            try {
                val updatedEvent = event.copy(isCompleted = isCompleted)
                val success = eventRepository.updateEvent(updatedEvent)
                if (success) {
                    Log.d("CalendarScreen", "Successfully updated todo status to $isCompleted for event: ${event.title}")
                    // Show success status
                    // Provide audio feedback if accessibility is enabled
                    if (accessibilityState.textToSpeech) {
                        val message = if (isCompleted) "Marked as completed" else "Marked as not completed"
                        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                } else {
                    Log.e("CalendarScreen", "Failed to update todo status for event: ${event.title}")
                }
            } catch (e: Exception) {
                Log.e("CalendarScreen", "Error updating todo status", e)
            }
        }
    }

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    ScreenReader("Calendar")
    val ttsManager = LocalTtsManager.current

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.scheduler),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    actions = {
                        // Past Due Notification Badge (if needed)
                        if (pastDueCount > 0) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = { showPastDueNotification = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Past Due Tasks",
                                        tint = Color.Red,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    // Display count badge
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .offset(x = 8.dp, y = (-8).dp)
                                            .background(Color.Red, CircleShape)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (pastDueCount > 9) "9+" else pastDueCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

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

                        IconButton(onClick = { /* Already on Calendar */ }) {
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
                // Move the Add Event button to a FAB to ensure it's always visible
                FloatingActionButton(
                    onClick = { onNavigateToAddEvent(selectedDate) },
                    containerColor = accessibleColors.buttonBackground,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add_new_event),
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->
            // Calendar content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accessibleColors.calendarBackground)
                    .padding(innerPadding) // Use the inner padding
                    .padding(16.dp)
            ) {
                // Make calendar section collapsible
                var isCalendarExpanded by remember { mutableStateOf(true) }

                // Calendar card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                        // Month navigation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        accessibleColors.buttonBackground.copy(alpha = 0.6f),
                                        CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable { currentYearMonth = currentYearMonth.minusMonths(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "<",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = accessibleColors.buttonText
                                )
                            }
                            // Previous month button
//                            IconButton(
//                                onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
//                                modifier = Modifier.size(36.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.ChevronLeft,
//                                    contentDescription = "Previous Month",
//                                    tint = accessibleColors.buttonText
//                                )
//                            }

                            // Format the month name according to the current locale
                            val locale = languageManager.getCurrentLocale()
                            val monthYearFormat =
                                DateTimeFormatter.ofPattern("MMMM yyyy", locale)

                            Text(
                                text = currentYearMonth.format(monthYearFormat),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    shadow = Shadow(
                                        color = if (isDarkMode) Color.Black.copy(alpha = 0.5f)
                                        else Color.Transparent,
                                        offset = Offset(1f, 1f),
                                        blurRadius = 2f
                                    )
                                ),
                                color = accessibleColors.headerText
                            )

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        accessibleColors.buttonBackground.copy(alpha = 0.6f),
                                        CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable { currentYearMonth = currentYearMonth.plusMonths(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    ">",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = accessibleColors.buttonText
                                )
                            }

                            // Expand/collapse button
                            IconButton(onClick = { isCalendarExpanded = !isCalendarExpanded }) {
                                Icon(
                                    imageVector = if (isCalendarExpanded)
                                        Icons.Default.ExpandLess
                                    else
                                        Icons.Default.ExpandMore,
                                    contentDescription = if (isCalendarExpanded)
                                        "Collapse Calendar"
                                    else
                                        "Expand Calendar",
                                    tint = accessibleColors.calendarText
                                )
                            }
                        }

                        // Calendar and Legend in an AnimatedVisibility
                        AnimatedVisibility(
                            visible = isCalendarExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = accessibleColors.cardBackground
                                ),
                                border = BorderStroke(
                                    width = 0.5.dp,
                                    color = accessibleColors.cardBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    // More compact weekday headers
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isDarkMode)
                                                    accessibleColors.buttonBackground.copy(alpha = 0.3f)
                                                else
                                                    MaterialTheme.colorScheme.primaryContainer
                                            )
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Weekday Headers with localized day names
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isDarkMode)
                                                        accessibleColors.buttonBackground.copy(alpha = 0.3f)
                                                    else
                                                        MaterialTheme.colorScheme.primaryContainer
                                                )
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            // Get localized weekday abbreviations with better styling
                                            val locale = languageManager.getCurrentLocale()
                                            val calendar = Calendar.getInstance(locale)
                                            calendar.firstDayOfWeek = Calendar.SUNDAY

                                            for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
                                                calendar.set(Calendar.DAY_OF_WEEK, i)
                                                val dayName = if (isDarkMode && !isSmallScreen()) {
                                                    calendar.getDisplayName(
                                                        Calendar.DAY_OF_WEEK,
                                                        Calendar.SHORT,
                                                        locale
                                                    )
                                                } else {
                                                    calendar.getDisplayName(
                                                        Calendar.DAY_OF_WEEK,
                                                        Calendar.SHORT,
                                                        locale
                                                    )//?.substring(0, 1)
                                                } //?: "?"

                                                Text(
                                                    text = dayName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = if (isDarkMode)
                                                        accessibleColors.calendarText
                                                    else
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    // Calendar grid - keep existing but with more compact sizing
                                    CalendarGrid(
                                        yearMonth = currentYearMonth,
                                        selectedDate = selectedDate,
                                        events = liveEvents,
                                        onDateSelect = { date ->
                                            selectedDate = date
                                            // Optional: Add subtle feedback when selecting a date
                                            if (accessibilityState.textToSpeech) {
                                                val message =
                                                    "Selected ${date.format(DateTimeFormatter.ofPattern("MMMM d"))}"
                                                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                                            }
                                        }
                                    )

                                    CalendarLegend(
                                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                                        accessibleColors = accessibleColors,
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }
                        }

//                        // Selected Date Events Section with localized date format
//                        EventTabView(
//                            events = liveEvents,
//                            selectedDate = selectedDate,
//                            onEventClick = { event ->
//                                ttsManager?.speak("Opening event: ${event.title}")
//                                onNavigateToEventDetails(event)
//                            },
//                            onTodoStatusChange = { event, isCompleted ->
//                                handleTodoStatusChange(event, isCompleted)
//                            },
//                            accessibleColors = accessibleColors,
//                            isDarkMode = isDarkMode,
//                            //ttsManager = ttsManager
//                        )

                        // Selected date header
                        val locale = when (currentLanguageCode) {
                            "zh" -> Locale.CHINA
                            "ms" -> Locale("ms", "MY")
                            else -> Locale.ENGLISH
                        }
                        val dateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
                        val formattedDate = selectedDate.format(dateFormat)

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = accessibleColors.headerText,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Tab view - use existing EventTabView but give it more space
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Give all remaining space to the event list
                        ) {
                            EventTabView(
                                events = liveEvents,
                                selectedDate = selectedDate,
                                onEventClick = { event ->
                                    ttsManager?.speak("Opening event: ${event.title}")
                                    onNavigateToEventDetails(event)
                                },
                                onTodoStatusChange = { event, isCompleted ->
                                    handleTodoStatusChange(event, isCompleted)
                                },
                                accessibleColors = accessibleColors,
                                isDarkMode = isDarkMode,
                            )
                        }

                        // Add Event Button
//                        Button(
//                            onClick = { onNavigateToAddEvent(selectedDate) },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 8.dp),
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = accessibleColors.buttonBackground
//                            )
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.Center,
//                                modifier = Modifier.padding(vertical = 4.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Add,
//                                    contentDescription = null,
//                                    modifier = Modifier.padding(end = 8.dp),
//                                    tint = accessibleColors.headerText
//                                )
//                                Text(stringResource(id = R.string.add_new_event), color = accessibleColors.headerText)
//                            }
//                        }

                    }
                }
            }
        }

        // Past Due Notification Dialog
        if (showPastDueNotification && pastDueCount > 0) {
            AlertDialog(
                onDismissRequest = { showPastDueNotification = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.past_due_todo_events),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Red
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.past_due_message, pastDueCount),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Show a list of the past due events
                        val today = LocalDate.now()
                        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        val pastDueEvents = liveEvents.values.flatten().filter { event ->
                            event.isTodoEvent && !event.isCompleted && (
                                    event.date.isBefore(today) ||
                                            (event.date.isEqual(today) && event.endTime < currentTime)
                                    )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(pastDueEvents) { event ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            showPastDueNotification = false
                                            onNavigateToEventDetails(event)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Red.copy(alpha = 0.1f)
                                    ),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = event.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = Color.Red
                                            )

                                            Text(
                                                text = "${event.date.format(DateTimeFormatter.ofPattern("MMM d"))} (${event.startTime} - ${event.endTime})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Red.copy(alpha = 0.7f)
                                            )
                                        }

                                        // Checkbox to mark as complete
                                        Checkbox(
                                            checked = false,
                                            onCheckedChange = {
                                                handleTodoStatusChange(event, true)
                                                // Refresh the dialog
                                                coroutineScope.launch {
                                                    delay(300)
                                                    if (pastDueCount <= 1) {
                                                        showPastDueNotification = false
                                                    }
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color.Red,
                                                uncheckedColor = Color.Red
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPastDueNotification = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(id = R.string.acknowledge))
                    }
                }
            )
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    currentLanguageCode = languageCode
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

// Helper function to determine if we're on a small screen
@Composable
private fun isSmallScreen(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp < 360
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    events: Map<LocalDate, List<Event>>,
    onDateSelect: (LocalDate) -> Unit,
) {
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val isHighContrast = LocalHighContrastMode.current

    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
    val currentMonth = YearMonth.now()
    val today = LocalDate.now()
    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isHighContrast) 1.5.dp else 1.dp,
                color = accessibleColors.calendarBorder,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        repeat(6) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { col ->
                    val day = row * 7 + col - startOffset + 1
                    val date = if (day in 1..yearMonth.lengthOfMonth()) {
                        yearMonth.atDay(day)
                    } else null

                    val isCurrentMonth = date?.let { yearMonth == YearMonth.from(it) } ?: false
                    val isToday = date == today
                    val isSelected = date == selectedDate
                    val dateEvents = date?.let { events[it] } ?: emptyList()
                    val hasEvents = dateEvents.isNotEmpty()
                    val hasTodoEvents = dateEvents.any { it.isTodoEvent }
                    val hasUncompletedTodos = dateEvents.any { it.isTodoEvent && !it.isCompleted }
                    val hasPastDueTodos = date?.let {
                        dateEvents.any { event ->
                            event.isTodoEvent &&
                                    !event.isCompleted &&
                                    (date.isBefore(today) ||
                                            (date.isEqual(today) && event.endTime < currentTime))
                        }
                    } ?: false
                    val isWeekend = date?.dayOfWeek?.let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } ?: false

                    // Alternative shading for enhanced grid visibility
                    val isAlternateBackground = (row + col) % 2 == 0
                    val cellBackground = when {
                        !isCurrentMonth -> Color.Transparent
                        isSelected -> accessibleColors.selectedDay
                        isToday -> accessibleColors.todayBackground
                        isAlternateBackground && isDarkMode -> accessibleColors.cardBackground.copy(alpha = 0.7f)
                        else -> Color.Transparent
                    }

                    // Determine text color for better visibility
                    val textColor = when {
                        !isCurrentMonth -> accessibleColors.calendarText.copy(alpha = 0.3f)
                        isSelected -> accessibleColors.selectedDayText
                        isToday -> accessibleColors.todayText
                        isWeekend && isDarkMode -> accessibleColors.calendarText.copy(alpha = 0.8f)
                        else -> accessibleColors.calendarText
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = if (isHighContrast) 1.5.dp else 0.5.dp,
                                color = accessibleColors.calendarBorder
                            )
                            .background(
                                if (isAlternateBackground && !isSelected && !isToday && isDarkMode)
                                    accessibleColors.cardBackground.copy(alpha = 0.4f)
                                else
                                    Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            // Enhanced day cell
                            Box(
                                modifier = Modifier
                                    .size(if (isToday || isSelected) 38.dp else 34.dp)
                                    .clip(CircleShape)
                                    .background(cellBackground)
                                    // Add subtle elevation shadow in dark mode
                                    .shadow(
                                        elevation = if ((isSelected || isToday) && isDarkMode) 4.dp else 0.dp,
                                        shape = CircleShape
                                    )
                                    // Add subtle border for today in dark mode
                                    .then(
                                        if (isToday && !isSelected && isDarkMode)
                                            Modifier.border(1.dp, accessibleColors.selectedDay.copy(alpha = 0.6f), CircleShape)
                                        else
                                            Modifier
                                    )
                                    .clickable(enabled = isCurrentMonth) {
                                        onDateSelect(date)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = when {
                                            isSelected -> FontWeight.Bold
                                            isToday -> FontWeight.Bold
                                            else -> FontWeight.Normal
                                        }
                                    )
                                )

                                // Enhanced indicators for different types of events
                                if (hasEvents) {
                                    if (hasPastDueTodos) {
                                        // Red indicator for past due to-dos
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                                .size(8.dp),
                                            containerColor = Color.Red
                                        )
                                    } else if (hasUncompletedTodos) {
                                        // Orange indicator for incomplete to-dos
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                                .size(8.dp),
                                            containerColor = Color(0xFFF57C00) // Orange
                                        )
                                    } else if (hasTodoEvents) {
                                        // Green indicator for completed to-dos
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                                .size(8.dp),
                                            containerColor = Color(0xFF4CAF50) // Green
                                        )
                                    } else {
                                        // Blue indicator for regular events
                                        Badge(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                                .size(8.dp),
                                            containerColor = accessibleColors.eventIndicator
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectedDateEvents(
    selectedDate: LocalDate,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onAddEventClick: () -> Unit,
    currentLanguageCode: String,
    onTodoStatusChange: (Event, Boolean) -> Unit // New parameter
) {
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = accessibleColors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Format the date according to the current locale
            val locale = when (currentLanguageCode) {
                "zh" -> Locale.CHINA
                "ms" -> Locale("ms", "MY")
                else -> Locale.ENGLISH
            }

            val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)
            val formattedDate = selectedDate.format(dateFormat)

            // Header row with date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = accessibleColors.selectedDay,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 8.dp)
                )

                Text(
                    text = stringResource(id = R.string.events_for, formattedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Content
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDarkMode)
                                accessibleColors.cardBackground.copy(alpha = 0.5f)
                            else
                                accessibleColors.calendarBorder.copy(alpha = 0.1f)
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = null,
                            tint = accessibleColors.calendarText.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(id = R.string.no_events),
                            style = MaterialTheme.typography.bodyLarge,
                            color = accessibleColors.calendarText.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Enhanced: Add to-do events section if any exist
                val todoEvents = events.filter { it.isTodoEvent }
                val regularEvents = events.filter { !it.isTodoEvent }

                if (todoEvents.isNotEmpty()) {
                    Text(
                        text = stringResource(id = R.string.todo_events),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // To-do events list
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (regularEvents.isEmpty()) 320.dp else 160.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(todoEvents) { event ->
                                EventListItem(
                                    event = event,
                                    onClick = { onEventClick(event) },
                                    onTodoStatusChange = onTodoStatusChange
                                )
                            }
                        }
                    }

                    if (regularEvents.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        Text(
                            text = stringResource(id = R.string.regular_events),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.headerText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Regular events list (or all events if there's no separation)
                if (todoEvents.isEmpty() || regularEvents.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (todoEvents.isEmpty()) 320.dp else 160.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(if (todoEvents.isEmpty()) events else regularEvents) { event ->
                                EventListItem(
                                    event = event,
                                    onClick = { onEventClick(event) },
                                    onTodoStatusChange = if (event.isTodoEvent) onTodoStatusChange else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventListItem(
    event: Event,
    onClick: () -> Unit,
    onTodoStatusChange: ((Event, Boolean) -> Unit)? = null
) {
    val isDarkMode = LocalDarkMode.current
    val accessibleColors = LocalThemeColors.current
    val isHighContrast = LocalHighContrastMode.current
    val ttsManager = LocalTtsManager.current

    // Check if the event is past due
    val isPastDue = event.isTodoEvent && !event.isCompleted &&
            (LocalDate.now().isAfter(event.date) ||
                    (LocalDate.now().isEqual(event.date) &&
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) > event.endTime))

    // Add a description for TTS
    val eventDescription = buildString {
        if (event.isTodoEvent) {
            append(if (event.isCompleted) "Completed to-do: " else "To-do: ")
        } else {
            append("Event: ")
        }
        append("${event.title}, ")
        append("from ${event.startTime} to ${event.endTime}")
        if (event.description.isNotBlank()) {
            append(", Description: ${event.description}")
        }
        if (event.hasReminder) {
            append(", Reminder is set")
        }
        if (event.isTodoEvent && isPastDue) {
            append(", This to-do is past due")
        }
    }

    // Define accent colors for different states
    val accentColor = when {
        event.isTodoEvent && isPastDue -> Color.Red
        event.isTodoEvent && event.isCompleted -> Color.Gray
        event.isTodoEvent -> Color(0xFFF57C00) // Orange for pending to-dos
        else -> accessibleColors.selectedDay
    }

    // Define text colors
    val titleColor = when {
        event.isTodoEvent && isPastDue -> Color.Red
        event.isTodoEvent && event.isCompleted -> Color.Gray
        else -> accessibleColors.calendarText
    }

    // Base card background
    val cardBackground = if (isDarkMode)
        accessibleColors.cardBackground.copy(alpha = 0.8f)
    else
        accessibleColors.cardBackground

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(72.dp) // Slightly reduced height
            .clickable {
                ttsManager?.speak(eventDescription)
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp), // Reduced left padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time indicator - vertical accent bar
            Box(
                modifier = Modifier
                    .width(6.dp) // Thinner accent bar
                    .fillMaxHeight()
                    .background(accentColor.copy(alpha = 0.7f))
            )

            // Time display area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(68.dp) // Slightly narrower
                    .fillMaxHeight()
                    .background(accentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = event.startTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = accentColor
                )

                Text(
                    text = "to",
                    style = MaterialTheme.typography.labelSmall,
                    color = accessibleColors.calendarText.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 1.dp) // Reduced spacing
                )

                Text(
                    text = event.endTime,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = accentColor
                )
            }

            // Event content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (event.isTodoEvent && event.isCompleted)
                            TextDecoration.LineThrough
                        else
                            TextDecoration.None
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Only show description if it's not blank
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = titleColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right section with to-do checkbox and indicators
            if (event.isTodoEvent) {
                // Use a custom checkbox with better visual integration
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background circle with subtle color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                accentColor.copy(alpha = if (event.isCompleted) 0.2f else 0.05f),
                                CircleShape
                            )
                    )

                    // Checkbox on top
                    Checkbox(
                        checked = event.isCompleted,
                        onCheckedChange = { isChecked ->
                            onTodoStatusChange?.invoke(event, isChecked)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = accentColor.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Reminder indicator and chevron in a vertical layout
            Column(
                modifier = Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Reminder indicator if present
                if (event.hasReminder) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Reminder set",
                        tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Chevron indicator
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accessibleColors.calendarText.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EventTabView(
    events: Map<LocalDate, List<Event>>,
    selectedDate: LocalDate,
    onEventClick: (Event) -> Unit,
    onTodoStatusChange: (Event, Boolean) -> Unit,
    accessibleColors: AccessibleColors,
    isDarkMode: Boolean,
    //ttsManager: TextToSpeech?
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.all_events),
        stringResource(id = R.string.todo_events)
    )
    val context = LocalContext.current
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    val tts = remember(accessibilityState.textToSpeech) {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }
    val ttsManager = LocalTtsManager.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tab Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isDarkMode)
                        accessibleColors.buttonBackground.copy(alpha = 0.2f)
                    else
                        accessibleColors.calendarBorder.copy(alpha = 0.2f)
                )
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        ttsManager?.speak("Selected ${tabs[index]} tab")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .then(
                                if (selectedTabIndex == index)
                                    Modifier.background(
                                        accessibleColors.selectedDay.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                else
                                    Modifier
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.CalendarToday else Icons.Default.Assignment,
                                contentDescription = null,
                                tint = if (selectedTabIndex == index)
                                    accessibleColors.selectedDay
                                else
                                    accessibleColors.calendarText.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )

                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTabIndex == index)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal
                                ),
                                color = if (selectedTabIndex == index)
                                    accessibleColors.selectedDay
                                else
                                    accessibleColors.calendarText.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        val allEvents = events[selectedDate] ?: emptyList()
        val todoEvents = allEvents.filter { it.isTodoEvent }
        val regularEvents = allEvents.filter { !it.isTodoEvent }

        val displayEvents = when (selectedTabIndex) {
            0 -> allEvents // All events tab
            1 -> todoEvents // To-do events tab
            else -> allEvents
        }

        // Event counts info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedTabIndex) {
                    0 -> stringResource(id = R.string.events_count_format, allEvents.size)
                    1 -> stringResource(id = R.string.todo_events_count_format, todoEvents.size)
                    else -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                color = accessibleColors.calendarText.copy(alpha = 0.6f)
            )

            if (selectedTabIndex == 1 && todoEvents.isNotEmpty()) {
                // Filter options for to-do events
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val completedCount = todoEvents.count { it.isCompleted }
                    val pendingCount = todoEvents.size - completedCount

                    Text(
                        text = stringResource(
                            id = R.string.pending_completed_format,
                            pendingCount,
                            completedCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = accessibleColors.calendarText.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (displayEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkMode)
                            accessibleColors.cardBackground.copy(alpha = 0.5f)
                        else
                            accessibleColors.calendarBorder.copy(alpha = 0.1f)
                    )
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selectedTabIndex == 0) Icons.Default.EventNote else Icons.Default.Assignment,
                        contentDescription = null,
                        tint = accessibleColors.calendarText.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (selectedTabIndex) {
                            0 -> stringResource(id = R.string.no_events)
                            1 -> stringResource(id = R.string.no_todo_events)
                            else -> stringResource(id = R.string.no_events)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = accessibleColors.calendarText.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Event list
            val sortedEvents = if (selectedTabIndex == 1) {
                // Sort to-do events: first uncompleted, then by date/time, then completed
                displayEvents.sortedWith(
                    compareBy<Event> { it.isCompleted }
                        .thenBy { it.date }
                        .thenBy { it.startTime }
                )
            } else {
                // Sort all events by time
                displayEvents.sortedWith(
                    compareBy<Event> { it.date }
                        .thenBy { it.startTime }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedEvents) { event ->
                        EventListItem(
                            event = event,
                            onClick = { onEventClick(event) },
                            onTodoStatusChange = onTodoStatusChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarLegend(
    modifier: Modifier = Modifier,
    accessibleColors: AccessibleColors,
    isDarkMode: Boolean
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode)
                accessibleColors.cardBackground.copy(alpha = 0.5f)
            else
                accessibleColors.cardBackground
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = accessibleColors.calendarBorder.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.calendar_indicators),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accessibleColors.calendarText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Create two columns for the legend items
                Column(modifier = Modifier.weight(1f)) {
                    LegendItem(
                        color = accessibleColors.eventIndicator,
                        text = stringResource(id = R.string.regular_event),
                        textColor = accessibleColors.calendarText
                    )

                    LegendItem(
                        color = Color(0xFF4CAF50), // Green
                        text = stringResource(id = R.string.completed_todo),
                        textColor = accessibleColors.calendarText
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    LegendItem(
                        color = Color(0xFFF57C00), // Orange
                        text = stringResource(id = R.string.pending_todo),
                        textColor = accessibleColors.calendarText
                    )

                    LegendItem(
                        color = Color.Red,
                        text = stringResource(id = R.string.past_due_todo),
                        textColor = accessibleColors.calendarText
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}