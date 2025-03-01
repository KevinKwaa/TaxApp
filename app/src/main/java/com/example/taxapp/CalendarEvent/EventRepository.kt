package com.example.taxapp.CalendarEvent

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
class EventRepository private constructor() {
    private val TAG = "EventRepository"
    // Change to use getCalendarFirestore() instead of getFirestore()
    private val db: FirebaseFirestore = FirebaseManager.getCalendarFirestore()

    private fun getCurrentUserId(): String? = FirebaseManager.getCurrentUserId()

    private val userEventsCollection
        get() = db.collection("user_events")

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addEvent(event: Event): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            val eventMap = event.toMap().toMutableMap().apply {
                this["userId"] = userId
            }

            userEventsCollection.add(eventMap).await()
            Log.d(TAG, "Event added successfully for user $userId: ${event.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateEvent(event: Event): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            val query = userEventsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                val eventMap = event.toMap().toMutableMap().apply {
                    this["userId"] = userId
                }

                userEventsCollection.document(documentId).set(eventMap).await()
                Log.d(TAG, "Event updated successfully: ${event.title}")
                true
            } else {
                Log.w(TAG, "Event not found to update")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteEvent(event: Event): Boolean {
        val userId = getCurrentUserId() ?: return false

        return try {
            val query = userEventsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                userEventsCollection.document(documentId).delete().await()
                Log.d(TAG, "Event deleted successfully: ${event.title}")
                true
            } else {
                Log.w(TAG, "Event not found to delete")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllEvents(): Flow<Map<LocalDate, MutableList<Event>>> = callbackFlow {
        val userId = getCurrentUserId() ?: run {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        // This query ensures only events created by the current user are returned
        val listener = userEventsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching events", error)
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                val eventsMap = mutableMapOf<LocalDate, MutableList<Event>>()
                snapshot?.documents?.forEach { document ->
                    try {
                        val event = document.toEvent()
                        eventsMap.getOrPut(event.date) { mutableListOf() }.add(event)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event", e)
                    }
                }

                Log.d(TAG, "Fetched ${eventsMap.size} unique dates with events for user $userId")
                trySend(eventsMap)
            }

        awaitClose { listener.remove() }
    }

    companion object {
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getInstance(): EventRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository().also { INSTANCE = it }
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
    val map = mutableMapOf(
        "title" to title,
        "description" to description,
        "dateTimestamp" to date.toTimestamp(),
        "startTime" to startTime,
        "endTime" to endTime,
        "hasReminder" to hasReminder
    )

    // Include userId if available
    userId?.let { map["userId"] = it }

    return map
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
    val userId = getString("userId")

    return Event(
        title = title,
        description = description,
        date = date,
        startTime = startTime,
        endTime = endTime,
        hasReminder = hasReminder,
        userId = userId
    )
}