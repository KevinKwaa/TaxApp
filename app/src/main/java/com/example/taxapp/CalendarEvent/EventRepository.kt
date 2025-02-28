package com.example.taxapp.CalendarEvent

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
    private val db: FirebaseFirestore = Firebase.firestore
    private val eventsCollection = db.collection("events")

    // Adds a new event to Firestore
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addEvent(event: Event): Boolean {
        return try {
            val eventMap = event.toMap()
            eventsCollection.add(eventMap).await()
            Log.d(TAG, "Event added successfully: ${event.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event", e)
            false
        }
    }

    // Updates an existing event in Firestore
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateEvent(event: Event): Boolean {
        return try {
            // We need to first find the event document
            val query = eventsCollection
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                eventsCollection.document(documentId).set(event.toMap()).await()
                Log.d(TAG, "Event updated successfully: ${event.title}")
                true
            } else {
                Log.w(TAG, "Event not found to update: ${event.title}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event", e)
            false
        }
    }

    // Deletes an event from Firestore
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteEvent(event: Event): Boolean {
        return try {
            // Find the event document
            val query = eventsCollection
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                eventsCollection.document(documentId).delete().await()
                Log.d(TAG, "Event deleted successfully: ${event.title}")
                true
            } else {
                Log.w(TAG, "Event not found to delete: ${event.title}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
            false
        }
    }

    // Gets all events as a Flow for reactivity
    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllEvents(): Flow<Map<LocalDate, MutableList<Event>>> = callbackFlow {
        val subscription = eventsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for events", error)
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
                trySend(eventsMap)
            }
        }

        awaitClose { subscription.remove() }
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