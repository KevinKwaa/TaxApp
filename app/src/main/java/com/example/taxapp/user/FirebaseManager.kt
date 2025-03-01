// Create a new file: app/src/main/java/com/example/taxapp/firebase/FirebaseManager.kt
package com.example.taxapp.firebase

import android.content.Context
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

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            // Initialize the auth Firebase app first (main app stays as DEFAULT)
            // Try to initialize AUTH_APP - this is for user authentication
            if (FirebaseApp.getApps(context).none { it.name == AUTH_APP }) {
                // Just use the default Firebase app for auth
                // This ensures we have a Firebase app named AUTH_APP
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDoBYOMePopcoYW-SpIDHkfDmuPEzYEf1A")  // from smarttaxver1
                        .setApplicationId("1:174963480472:android:b66a24180ffc5eea943ded")
                        .setProjectId("smarttaxver1")
                        .build()

                    FirebaseApp.initializeApp(context, options, AUTH_APP)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Initialize calendar app
            if (FirebaseApp.getApps(context).none { it.name == CALENDAR_APP }) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyBD8ZL2htx6yIntgDybAjTwUoeBHcVIKAs")  // from taxapp-calendar
                        .setApplicationId("1:712026376311:android:af5debec2f548175d09e72")
                        .setProjectId("taxapp-calendar")
                        .build()

                    FirebaseApp.initializeApp(context, options, CALENDAR_APP)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Get Auth instance from the default app for authentication
    fun getAuthInstance(): FirebaseAuth {
        return try {
            // Try to get the auth app first
            FirebaseAuth.getInstance(FirebaseApp.getInstance(AUTH_APP))
        } catch (e: Exception) {
            // Fall back to the default app if auth_app isn't initialized
            FirebaseAuth.getInstance()
        }
    }

    // Get Firestore instance for auth/user data
    fun getAuthFirestore(): FirebaseFirestore {
        return try {
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(AUTH_APP))
        } catch (e: Exception) {
            // Fall back to the default app
            FirebaseFirestore.getInstance()
        }
    }

    // Get Firestore instance from the calendar project
    fun getCalendarFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance(FirebaseApp.getInstance(CALENDAR_APP))
    }
}