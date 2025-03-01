// Create a new file: app/src/main/java/com/example/taxapp/firebase/FirebaseManager.kt
package com.example.taxapp.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object FirebaseManager {
    // Constants for app names
    private const val AUTH_APP = "auth_app"
    private const val CALENDAR_APP = "calendar_app"
    private const val TAG = "FirebaseManager"

    private var isInitialized = false

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

            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error in Firebase initialization", e)
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
                Log.d(TAG, "Auth Firestore Details: ${it.app.name}")
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
                Log.d(TAG, "Calendar Firestore Details: ${it.app.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting CALENDAR_APP Firestore", e)
            // If calendar app fails, fall back to default
            FirebaseFirestore.getInstance()
        }
    }

    // Get the current authenticated user ID
    fun getCurrentUserId(): String? {
        val userId = getAuthInstance().currentUser?.uid
        Log.d(TAG, "Current user ID: $userId")
        return userId
    }
}