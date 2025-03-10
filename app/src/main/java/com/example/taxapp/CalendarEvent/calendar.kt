package com.example.taxapp.CalendarEvent

import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
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
import java.util.*

data class Event(
    val title: String,
    val description: String,
    val date: LocalDate,
    val startTime: String,
    val endTime: String,
    var hasReminder: Boolean = false
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
    navController: NavHostController, // Add this parameter
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
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
                    }
                }
            } catch (e: Exception) {
                // Handle the error but don't crash
                Log.e("CalendarScreen", "Error collecting events: ${e.message}")
                // On error, use the last known good value
            }
        }
    }

    // Add a LaunchedEffect to respond to refreshKey
    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) {
            Log.d("CalendarScreen", "External refresh requested with key: $refreshKey")

            try {
                // Reset repository and force refresh sequentially
                EventRepository.resetInstance()
                delay(300) // Allow cleanup to complete

                val freshRepo = EventRepository.getInstance()
                freshRepo.forceRefresh()
            } catch (e: Exception) {
                Log.e("CalendarScreen", "Error during refresh: ${e.message}")
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
            }
        ) { innerPadding ->
            // Calendar content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accessibleColors.calendarBackground)
                    .padding(innerPadding) // Use the inner padding
                    .padding(16.dp)
            ) {
                // Date format
                if (isDarkMode) {
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accessibleColors.calendarText.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

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
                        }

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
                                .padding(vertical = 8.dp),
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
                                    )?.substring(0, 1)
                                } ?: "?"

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

                        Spacer(modifier = Modifier.height(4.dp))

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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Selected Date Events Section with localized date format
                        SelectedDateEvents(
                            selectedDate = selectedDate,
                            events = liveEvents[selectedDate] ?: mutableListOf(),
                            onEventClick = { event ->
                                // Use the captured ttsManager reference
                                ttsManager?.speak("Opening event: ${event.title}")
                                onNavigateToEventDetails(event)
                            },
                            onAddEventClick = {
                                // Use the captured ttsManager reference
                                ttsManager?.speak("Adding new event")
                                onNavigateToAddEvent(selectedDate)
                            },
                            currentLanguageCode = currentLanguageCode
                        )

                        // Add Event Button
                        Button(
                            onClick = { onNavigateToAddEvent(selectedDate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accessibleColors.buttonBackground
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(stringResource(id = R.string.add_new_event))
                            }
                        }

                    }
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
                    .height(48.dp)
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
                    val hasEvents = date?.let { events[it]?.isNotEmpty() == true } ?: false
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
                                    .size(if (isToday || isSelected) 40.dp else 36.dp)
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

                                // Show indicator for events
                                if (hasEvents) {
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectedDateEvents(
    selectedDate: LocalDate,
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onAddEventClick: () -> Unit,
    currentLanguageCode: String
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
                // Event list with fixed height container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Set a max height for the list container to prevent it from growing too large
                        .heightIn(max = 320.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events) { event ->
                            EventListItem(event = event, onClick = { onEventClick(event) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventListItem(
    event: Event,
    onClick: () -> Unit
) {
    val fontSizeAdjustment = LocalFontSizeAdjustment.current
    val isDarkMode = LocalDarkMode.current
    val accessibleColors = LocalThemeColors.current
    val isHighContrast = LocalHighContrastMode.current
    val ttsManager = LocalTtsManager.current

    // Add a description for TTS
    val eventDescription = buildString {
        append("Event: ${event.title}, ")
        append("from ${event.startTime} to ${event.endTime}")
        if (event.description.isNotBlank()) {
            append(", Description: ${event.description}")
        }
        if (event.hasReminder) {
            append(", Reminder is set")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(80.dp) // Fixed height to prevent varying heights
            .clickable {
                ttsManager?.speak(eventDescription)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode)
                accessibleColors.cardBackground.copy(alpha = 0.8f)
            else
                accessibleColors.cardBackground
        ),
        border = BorderStroke(
            width = if (isHighContrast) 1.5.dp else 0.5.dp,
            color = accessibleColors.cardBorder.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time indicator with colored accent
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(
                        accessibleColors.selectedDay.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = event.startTime,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = accessibleColors.selectedDay
                )

                Text(
                    text = "to",
                    style = MaterialTheme.typography.labelSmall,
                    color = accessibleColors.calendarText.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Text(
                    text = event.endTime,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = accessibleColors.selectedDay
                )
            }

            // Event content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accessibleColors.calendarText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Only show description if it's not blank
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = accessibleColors.calendarText.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right section with reminder indicator and chevron
            Row(
                modifier = Modifier
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reminder indicator
                if (event.hasReminder) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Reminder set",
                        tint = accessibleColors.selectedDay,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Add subtle chevron indicator
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = accessibleColors.calendarText.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}