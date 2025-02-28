package com.example.taxapp

import android.app.Application
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.multiLanguage.AppLanguageManager
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


class TaxApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            configureFirestore()
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }

        // Initialize language manager
        val languageManager = AppLanguageManager.getInstance(this)

        // Initialize accessibility repository (this ensures early creation of singleton)
        AccessibilityRepository.getInstance(this)

        // Set the default locale based on saved preferences
        val locale = languageManager.getCurrentLocale()
    }

    private fun configureFirestore() {
        // Configure Firestore for offline persistence and caching
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Allow unlimited cache size
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }

    companion object {
        private const val TAG = "TaxApp"
    }
}