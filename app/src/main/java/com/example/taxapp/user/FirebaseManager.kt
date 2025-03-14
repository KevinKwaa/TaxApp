package com.example.taxapp.firebase

import android.content.Context
import android.util.Log
import com.example.taxapp.CalendarEvent.EventRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FirebaseManager {
    // Constants for app names - keeping CALENDAR_APP for calendar features
    private const val CALENDAR_APP = "calendar_app"
    private const val TAG = "FirebaseManager"

    // Track current user ID with StateFlow for observability
    private val _currentUserFlow = MutableStateFlow<String?>(null)
    val currentUserFlow: StateFlow<String?> = _currentUserFlow

    // We'll use a single Firebase app for simplicity
    private var isInitialized = false

    // Cache variable to reduce excessive auth calls
    private var lastCheckedUserId: String? = null
    private var lastCheckedTimestamp: Long = 0

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            Log.d(TAG, "Initializing Firebase Manager")

            // First, make sure the default Firebase app is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Default Firebase app initialized")
            }

            // Initialize CALENDAR_APP (keeping this for calendar functionality)
            if (FirebaseApp.getApps(context).none { it.name == CALENDAR_APP }) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyBD8ZL2htx6yIntgDybAjTwUoeBHcVIKAs")  // from taxapp-calendar
                        .setApplicationId("1:712026376311:android:af5debec2f548175d09e72")
                        .setProjectId("taxapp-calendar")
                        .build()

                    FirebaseApp.initializeApp(context, options, CALENDAR_APP)
                    Log.d(TAG, "CALENDAR_APP Firebase app initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing CALENDAR_APP", e)
                }
            }

            // Log all initialized Firebase apps
            val appsList = FirebaseApp.getApps(context).map { it.name }
            Log.d(TAG, "Initialized Firebase apps: $appsList")

            // Set up auth state listener to track user changes
            setupAuthStateListener()

            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error in Firebase initialization", e)
        }
    }

    // Set up auth state listener to detect user changes
    private fun setupAuthStateListener() {
        try {
            // Using default Firebase auth instance to match AuthViewModel
            val auth = Firebase.auth
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                handleUserChange(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up auth state listener", e)
        }
    }

    // Handle user change
    private fun handleUserChange(user: FirebaseUser?) {
        val newUserId = user?.uid
        val currentValue = _currentUserFlow.value

        if (newUserId != currentValue) {
            Log.d(TAG, "User changed from $currentValue to $newUserId")

            // Reset last checked cache
            lastCheckedUserId = newUserId
            lastCheckedTimestamp = System.currentTimeMillis()

            // Update the flow
            _currentUserFlow.value = newUserId

            // Reset repositories that depend on user
            EventRepository.resetInstance()
        }
    }

    // Get Auth instance - now using DEFAULT Firebase auth instance to match AuthViewModel
    fun getAuthInstance(): FirebaseAuth {
        return Firebase.auth.also {
            Log.d(TAG, "Retrieved DEFAULT FirebaseAuth")
        }
    }

    // Get Firestore instance for auth/user data - using DEFAULT Firestore instance
    fun getAuthFirestore(): FirebaseFirestore {
        return Firebase.firestore.also {
            Log.d(TAG, "Retrieved DEFAULT Firestore")
        }
    }

    // Get Firestore instance from the calendar project
    fun getCalendarFirestore(): FirebaseFirestore {
        return try {
            val calendarApp = FirebaseApp.getInstance(CALENDAR_APP)
            FirebaseFirestore.getInstance(calendarApp).also {
                Log.d(TAG, "Retrieved Firestore from CALENDAR_APP")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting CALENDAR_APP Firestore", e)
            throw e
        }
    }

    // Get the current authenticated user ID - with freshness check
    fun getCurrentUserId(): String? {
        // Check if we need a fresh check (if last check was more than 1 second ago)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheckedTimestamp > 1000) {
            // Using default Firebase auth instance to match AuthViewModel
            val userId = Firebase.auth.currentUser?.uid
            Log.d(TAG, "Current user ID (fresh check): $userId")

            // Cache the result
            lastCheckedUserId = userId
            lastCheckedTimestamp = currentTime

            // Update the flow if needed
            if (_currentUserFlow.value != userId) {
                _currentUserFlow.value = userId
            }

            return userId
        }

        // Return cached value
        return lastCheckedUserId
    }

    // Force refresh the current user ID (for use after login/logout)
    fun refreshCurrentUser() {
        // Reset cache timestamps to force a fresh check
        lastCheckedTimestamp = 0

        // Get fresh user ID and update flow - using default Firebase auth
        val userId = Firebase.auth.currentUser?.uid
        Log.d(TAG, "Refreshed current user ID: $userId")

        // Only update if changed to avoid unnecessary recompositions
        if (_currentUserFlow.value != userId) {
            _currentUserFlow.value = userId
        }

        // Update cache
        lastCheckedUserId = userId
        lastCheckedTimestamp = System.currentTimeMillis()

        // If we've logged out, schedule the persistence clear for later
        if (userId == null) {
            try {
                // Instead of clearing persistence immediately, just log that it can't be done now
                Log.d(TAG, "Logged out - persistence clearing skipped while app is running")

                // If you really want to clear persistence, you could do it on app restart instead
                // Or implement a proper shutdown sequence for Firestore before clearing
            } catch (e: Exception) {
                Log.e(TAG, "Error planning persistence clearing", e)
            }
        }
    }

    fun getStorageInstance(): FirebaseStorage {
        return FirebaseStorage.getInstance().also {
            Log.d(TAG, "Retrieved DEFAULT FirebaseStorage")
        }
    }
}