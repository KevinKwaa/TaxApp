package com.example.taxapp

import android.app.Application
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.multiLanguage.AppLanguageManager
import android.util.Log
import com.example.taxapp.user.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


class TaxApp : Application() {
    companion object {
        private const val TAG = "TaxApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TaxApp: onCreate")

        try {
            Log.d(TAG, "Initializing Firebase Manager")
            FirebaseManager.initialize(this)
            configureFirestore()
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }


        // Initialize language manager
        val languageManager = AppLanguageManager.getInstance(this)
        Log.d(TAG, "Language manager initialized")

        // Initialize accessibility repository (this ensures early creation of singleton)
        AccessibilityRepository.getInstance(this)
        Log.d(TAG, "Accessibility repository initialized")

        // Set the default locale based on saved preferences
        val locale = languageManager.getCurrentLocale()
        Log.d(TAG, "Default locale set to: $locale")
    }

    private fun configureFirestore() {
        // Configure Firestore for offline persistence and caching
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Allow unlimited cache size
            .build()

        // Configure each Firestore instance
        try {
            FirebaseFirestore.getInstance().firestoreSettings = settings
            FirebaseManager.getAuthFirestore().firestoreSettings = settings
            FirebaseManager.getCalendarFirestore().firestoreSettings = settings
            Log.d(TAG, "Firestore settings configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring Firestore settings", e)
        }
    }
}