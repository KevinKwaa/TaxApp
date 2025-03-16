package com.example.taxapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.taxapp.accessibility.AppAccessibilityProvider
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.ui.theme.TaxAppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.taxapp.CalendarEvent.EventRepository
import com.example.taxapp.CalendarEvent.FadeTransition
import com.example.taxapp.CalendarEvent.LoadingScreen
import com.example.taxapp.CalendarEvent.TaxDeadlineHelper
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.user.FirebaseManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : BaseActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val languageManager = AppLanguageManager.getInstance(this)
        val langCode = languageManager.getCurrentLanguageCode()
        languageManager.setLanguage(langCode, this)
        enableEdgeToEdge()
        setContent {
            TaxAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppAccessibilityProvider {
                        MainScreen()
                    }
                }
            }
        }

        // Ensure default tax events exist for current user
        ensureDefaultTaxEvents()

        // Check for upcoming tax deadlines
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TaxDeadlineHelper.checkUpcomingDeadline(this, lifecycleScope)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check if the user is logged in
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                lifecycleScope.launch {
                    try {
                        // Get user's employment status (default to employee if not set)
                        val userId = currentUser.uid
                        val userDoc = Firebase.firestore.collection("users").document(userId).get().await()
                        val employment = userDoc.getString("employment") ?: "employee"

                        Log.d("MainActivity", "Ensuring default tax events for employment: $employment")

                        // Check if there are any tax deadline events
                        val eventRepository = EventRepository.getInstance()
                        val eventsExist = withTimeoutOrNull(3000) {
                            // Try to fetch events, with a timeout in case Firestore is slow
                            val events = eventRepository.getAllEvents(userId).first()

                            // Check if any tax deadline events exist
                            events.values.flatten().any { event ->
                                event.title.contains("Tax Filing Deadline", ignoreCase = true)
                            }
                        } ?: false

                        if (!eventsExist) {
                            Log.d("MainActivity", "No tax deadline events found, creating defaults")
                            // No tax events found, create defaults
                            TaxDeadlineHelper.updateTaxDeadlineEvents(employment, lifecycleScope) { success ->
                                Log.d("MainActivity", "Default tax events created: $success")
                            }
                        } else {
                            Log.d("MainActivity", "Tax deadline events already exist")
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error checking for default tax events", e)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureDefaultTaxEvents() {
        // Check if the user is logged in
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    // Get user's employment status (default to employee if not set)
                    val userId = currentUser.uid
                    val userDoc = Firebase.firestore.collection("users").document(userId).get().await()
                    val employment = userDoc.getString("employment") ?: "employee"

                    Log.d("MainActivity", "Ensuring default tax events for employment: $employment")

                    // Reset repository to ensure fresh data
                    EventRepository.resetInstance()
                    val eventRepository = EventRepository.getInstance()

                    // Check if there are any tax deadline events
                    var taxEventsExist = false
                    try {
                        withTimeout(5000) { // 5 second timeout
                            // Try to fetch events
                            val events = eventRepository.getAllEvents(userId).first()

                            // Check if any tax deadline events exist
                            taxEventsExist = events.values.flatten().any { event ->
                                event.title.contains("Tax Filing Deadline", ignoreCase = true)
                            }

                            val taxEvents = events.values.flatten().filter {
                                it.title.contains("Tax Filing Deadline", ignoreCase = true)
                            }

                            Log.d("MainActivity", "Found ${taxEvents.size} existing tax events:")
                            taxEvents.forEach { event ->
                                Log.d("MainActivity", "  - ${event.title} on ${event.date}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error checking for tax events", e)
                        taxEventsExist = false
                    }

                    if (!taxEventsExist) {
                        Log.d("MainActivity", "No tax deadline events found, creating defaults")
                        // Force a complete refresh
                        EventRepository.resetInstance()

                        // No tax events found, create defaults with direct update
                        TaxDeadlineHelper.updateTaxDeadlineEvents(employment, lifecycleScope) { success ->
                            Log.d("MainActivity", "Default tax events created: $success")
                            // Force repository refresh
                            EventRepository.getInstance().forceRefresh()
                        }
                    } else {
                        Log.d("MainActivity", "Tax deadline events already exist")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error setting up default tax events", e)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if user is still authenticated
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            // If not logged in, make sure event repository is reset
            EventRepository.resetInstance()
        }
    }

    override fun onResume() {
        super.onResume()

        // Force refresh repository when app comes to foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Firebase.auth.currentUser != null) {
            EventRepository.resetInstance()
            EventRepository.getInstance().forceRefresh()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = com.example.taxapp.accessibility.AccessibilityState()
    )

    // State for tracking if Firebase is ready
    var isFirebaseReady by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("Loading your events...") }
    val eventRepository = remember { EventRepository.getInstance() }

    // Collect events from Firestore to check if connection is established
    val currentUserId = FirebaseManager.getCurrentUserId()
    val eventsFlow by eventRepository.getAllEvents(currentUserId).collectAsState(initial = mapOf())

    // Consider Firebase ready once we've received data from Firestore
    // or after a timeout period (to prevent indefinite loading)
    LaunchedEffect(Unit) {
        try {
            // Set a timeout in case Firestore doesn't respond or has an error
            for (i in 5 downTo 1) {
                if (eventsFlow != null) {
                    // Add a small delay to ensure smooth transition
                    delay(500)
                    isFirebaseReady = true
                    break
                }

                // Update loading message with countdown if taking longer than expected
                if (i < 4) {
                    loadingMessage = "Still loading... $i"
                }

                delay(1000)
            }

            // If still not ready after timeout, proceed anyway
            if (!isFirebaseReady) {
                loadingMessage = "Continuing without loading saved events"
                delay(1500)
                isFirebaseReady = true
            }
        } catch (e: Exception) {
            // In case of any error, proceed with the app
            loadingMessage = "Unable to load saved events"
            delay(1500)
            isFirebaseReady = true
        }
    }

    // Show loading screen or main app depending on Firebase readiness using fade transitions
    FadeTransition(
        visible = !isFirebaseReady,
        durationMillis = 800
    ) {
        LoadingScreen(
            message = loadingMessage,
            accessibilityState = accessibilityState
        )
    }

    FadeTransition(
        visible = isFirebaseReady,
        durationMillis = 800
    ) {
        AppNavigation()
    }
}


