package com.example.taxapp.CalendarEvent

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taxapp.CalendarEvent.Event
import com.example.taxapp.CalendarEvent.toEvent
import com.example.taxapp.CalendarEvent.toMap
import com.example.taxapp.firebase.FirebaseManager
import kotlinx.coroutines.tasks.await

/**
 * Utility class to migrate events from the old shared structure to the new user-specific structure
 */
@RequiresApi(Build.VERSION_CODES.O)
class EventMigrationUtil {
    private val TAG = "EventMigrationUtil"
    private val calendarDb = FirebaseManager.getCalendarFirestore()

    /**
     * Migrates events from the old 'events' collection to the new 'user_events' collection
     * with user ID association
     */
    suspend fun migrateEvents() {
        val userId = FirebaseManager.getCurrentUserId() ?: return
        Log.d(TAG, "Starting event migration for user: $userId")

        try {
            // First check if we've already migrated for this user
            val migrationDoc = calendarDb.collection("migrations")
                .document(userId)
                .get()
                .await()

            if (migrationDoc.exists()) {
                Log.d(TAG, "Migration already completed for user: $userId")
                return
            }

            // Get all events from the old collection
            val oldEvents = calendarDb.collection("events")
                .get()
                .await()

            if (oldEvents.isEmpty) {
                Log.d(TAG, "No events to migrate for user: $userId")
                markMigrationComplete(userId)
                return
            }

            Log.d(TAG, "Found ${oldEvents.size()} events to evaluate for migration")
            var migratedCount = 0

            // Process each event
            for (document in oldEvents.documents) {
                try {
                    val event = document.toEvent()

                    // Add user ID to the event data
                    val eventData = event.toMap().toMutableMap()
                    eventData["userId"] = userId

                    // Add to the new user_events collection
                    calendarDb.collection("user_events")
                        .add(eventData)
                        .await()

                    migratedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error migrating event: ${document.id}", e)
                }
            }

            Log.d(TAG, "Successfully migrated $migratedCount events for user: $userId")

            // Mark migration as complete for this user
            markMigrationComplete(userId)

        } catch (e: Exception) {
            Log.e(TAG, "Error during event migration", e)
        }
    }

    private suspend fun markMigrationComplete(userId: String) {
        calendarDb.collection("migrations")
            .document(userId)
            .set(mapOf(
                "userId" to userId,
                "eventsMigrated" to true,
                "timestamp" to com.google.firebase.Timestamp.now()
            ))
            .await()

        Log.d(TAG, "Marked migration complete for user: $userId")
    }

    companion object {
        @Volatile
        private var INSTANCE: EventMigrationUtil? = null

        fun getInstance(): EventMigrationUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventMigrationUtil().also { INSTANCE = it }
            }
        }
    }
}