package com.example.taxapp

import android.os.Build
import android.os.Bundle
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
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

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
        // Check for upcoming tax deadlines
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TaxDeadlineHelper.checkUpcomingDeadline(this, lifecycleScope)
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


