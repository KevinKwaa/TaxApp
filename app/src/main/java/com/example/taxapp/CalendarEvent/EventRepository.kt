package com.example.taxapp.CalendarEvent

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taxapp.firebase.FirebaseManager
//import com.example.taxapp.Event
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Repository for managing events with Firebase Firestore
 */
class EventRepository {
    private val TAG = "EventRepository"
    // Use the calendar project for event data
    private val db: FirebaseFirestore = FirebaseManager.getCalendarFirestore()

    // Get the current user ID from the auth app
    private fun getCurrentUserId(): String? {
        return FirebaseManager.getCurrentUserId()
    }

    // Reference to the user's events collection
    private fun getUserEventsCollection() = db.collection("user_events")

    // Adds a new event to Firestore, associated with the current user
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addEvent(event: Event): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            // Add userId to the event data
            val eventMap = event.toMap().toMutableMap()
            eventMap["userId"] = userId

            // Save to Firestore under the user_events collection
            getUserEventsCollection().add(eventMap).await()
            Log.d(TAG, "Event added successfully for user $userId: ${event.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event for user $userId", e)
            false
        }
    }

    // Updates an existing event in Firestore
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateEvent(event: Event): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            // Find the event document for this specific user
            val query = getUserEventsCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                // Add userId to ensure it's preserved
                val eventMap = event.toMap().toMutableMap()
                eventMap["userId"] = userId

                getUserEventsCollection().document(documentId).set(eventMap).await()
                Log.d(TAG, "Event updated successfully for user $userId: ${event.title}")
                true
            } else {
                Log.w(TAG, "Event not found to update for user $userId: ${event.title}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event for user $userId", e)
            false
        }
    }

    // Deletes an event from Firestore
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteEvent(event: Event): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            // Find the event document for this specific user
            val query = getUserEventsCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                getUserEventsCollection().document(documentId).delete().await()
                Log.d(TAG, "Event deleted successfully for user $userId: ${event.title}")
                true
            } else {
                Log.w(TAG, "Event not found to delete for user $userId: ${event.title}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event for user $userId", e)
            false
        }
    }

    // Gets all events for the current user as a Flow for reactivity
    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllEvents(): Flow<Map<LocalDate, MutableList<Event>>> = callbackFlow {
        val userId = getCurrentUserId()

        if (userId == null) {
            // If no user is logged in, emit an empty map
            trySend(emptyMap())
            Log.w(TAG, "No user logged in, returning empty events map")
            return@callbackFlow
        }

        // Listen only to events that belong to the current user
        val subscription = getUserEventsCollection()
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for events for user $userId", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val eventsMap = mutableMapOf<LocalDate, MutableList<Event>>()
                    for (document in snapshot.documents) {
                        try {
                            val event = document.toEvent()
                            val date = event.date
                            if (!eventsMap.containsKey(date)) {
                                eventsMap[date] = mutableListOf()
                            }
                            eventsMap[date]?.add(event)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing event document: ${document.id}", e)
                        }
                    }
                    Log.d(TAG, "Retrieved ${snapshot.size()} events for user $userId")
                    trySend(eventsMap)
                }
            }

        awaitClose {
            subscription.remove()
            Log.d(TAG, "Closing events listener for user $userId")
        }
    }

    companion object {
        private const val TAG = "EventRepository"

        // Singleton pattern
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getInstance(): EventRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository().also { INSTANCE = it }
            }
        }
    }
}

// Extension functions to help with conversions between Event and Firestore

// Convert LocalDate to Firestore Timestamp
@RequiresApi(Build.VERSION_CODES.O)
fun LocalDate.toTimestamp(): Timestamp {
    val date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
    return Timestamp(date)
}

// Convert Timestamp to LocalDate
@RequiresApi(Build.VERSION_CODES.O)
fun Timestamp.toLocalDate(): LocalDate {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
}

// Convert Event to Map for Firestore
@RequiresApi(Build.VERSION_CODES.O)
fun Event.toMap(): Map<String, Any> {
    return mapOf(
        "title" to title,
        "description" to description,
        "dateTimestamp" to date.toTimestamp(),
        "startTime" to startTime,
        "endTime" to endTime,
        "hasReminder" to hasReminder
    )
}

// Convert Firestore Document to Event
@RequiresApi(Build.VERSION_CODES.O)
fun com.google.firebase.firestore.DocumentSnapshot.toEvent(): Event {
    val title = getString("title") ?: ""
    val description = getString("description") ?: ""
    val timestamp = get("dateTimestamp") as Timestamp
    val date = timestamp.toLocalDate()
    val startTime = getString("startTime") ?: "00:00"
    val endTime = getString("endTime") ?: "00:00"
    val hasReminder = getBoolean("hasReminder") ?: false

    return Event(
        title = title,
        description = description,
        date = date,
        startTime = startTime,
        endTime = endTime,
        hasReminder = hasReminder
    )
}