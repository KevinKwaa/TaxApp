package com.example.taxapp.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Manager class for Firebase services.
 * Handles initialization and provides access to Firebase instances.
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"

    // We'll use a single Firebase app for simplicity
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            Log.d(TAG, "Initializing Firebase Manager")

            // Initialize the default Firebase app
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Default Firebase app initialized")
            }

            // Configure Firestore settings for better performance
            configureFirestoreSettings()

            isInitialized = true
            Log.d(TAG, "Firebase Manager initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }

    private fun configureFirestoreSettings() {
        try {
            // Configure Firestore for offline persistence and caching
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()

            // Apply settings to Firestore
            FirebaseFirestore.getInstance().firestoreSettings = settings
            Log.d(TAG, "Firestore settings configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Firestore settings", e)
        }
    }

    // Get the standard authentication instance
    fun getAuthInstance(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // Get the standard Firestore instance for auth/user data
    fun getAuthFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // Get the Firestore instance for calendar events (same as auth, for simplicity)
    fun getCalendarFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // Get the current authenticated user ID
    fun getCurrentUserId(): String? {
        val userId = getAuthInstance().currentUser?.uid
        if (userId == null) {
            Log.d(TAG, "No current user ID - user not logged in")
        }
        return userId
    }
}