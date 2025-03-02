package com.example.taxapp.firebase

import android.content.Context
import android.util.Log
import com.example.taxapp.CalendarEvent.EventRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FirebaseManager {
    // Constants for app names
    private const val AUTH_APP = "auth_app"
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

            // Initialize AUTH_APP - this is for user authentication
            if (FirebaseApp.getApps(context).none { it.name == AUTH_APP }) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDoBYOMePopcoYW-SpIDHkfDmuPEzYEf1A")  // from smarttaxver1
                        .setApplicationId("1:174963480472:android:b66a24180ffc5eea943ded")
                        .setProjectId("smarttaxver1")
                        .build()

                    FirebaseApp.initializeApp(context, options, AUTH_APP)
                    Log.d(TAG, "AUTH_APP Firebase app initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing AUTH_APP", e)
                }
            }

            // Initialize CALENDAR_APP
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
            val auth = getAuthInstance()
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

    // Get Auth instance from the auth app for authentication
    fun getAuthInstance(): FirebaseAuth {
        return try {
            val authApp = FirebaseApp.getInstance(AUTH_APP)
            FirebaseAuth.getInstance(authApp).also {
                Log.d(TAG, "Retrieved FirebaseAuth from AUTH_APP")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting AUTH_APP instance, falling back to default", e)
            // Fall back to the default app
            FirebaseAuth.getInstance()
        }
    }

    // Get Firestore instance for auth/user data
    fun getAuthFirestore(): FirebaseFirestore {
        return try {
            val authApp = FirebaseApp.getInstance(AUTH_APP)
            FirebaseFirestore.getInstance(authApp).also {
                Log.d(TAG, "Retrieved Firestore from AUTH_APP")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting AUTH_APP Firestore, falling back to default", e)
            // Fall back to the default app
            FirebaseFirestore.getInstance()
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
            val userId = getAuthInstance().currentUser?.uid
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

        // Get fresh user ID and update flow
        val userId = getAuthInstance().currentUser?.uid
        Log.d(TAG, "Refreshed current user ID: $userId")

        // Only update if changed to avoid unnecessary recompositions
        if (_currentUserFlow.value != userId) {
            _currentUserFlow.value = userId
        }

        // Update cache
        lastCheckedUserId = userId
        lastCheckedTimestamp = System.currentTimeMillis()
    }
}