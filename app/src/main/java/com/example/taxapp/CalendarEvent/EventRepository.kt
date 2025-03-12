package com.example.taxapp.CalendarEvent

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Repository for managing events with Firebase Firestore
 */
class EventRepository {
    private val TAG = "EventRepository"

    // Use default Firebase instances to align with AuthViewModel
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    // Add a stateflow that can be observed to force UI updates
    private val _forceRefreshTrigger = MutableStateFlow(0L)
    val forceRefreshTrigger: StateFlow<Long> = _forceRefreshTrigger

    private val _eventsCache = MutableStateFlow<Map<LocalDate, MutableList<Event>>>(emptyMap())

    // Track active listeners to ensure proper cleanup
    private var activeListenerRegistration: ListenerRegistration? = null

    fun clearEvents() {
        _eventsCache.value = emptyMap()
    }

    // Flag to track if repository is currently operating - prevents concurrent operations
    private val isRefreshing = AtomicBoolean(false)

    // Get the user-specific events collection
    private fun getUserEventsCollection(userId: String) =
        db.collection("users").document(userId).collection("events")

    // These are the key methods in EventRepository.kt that need attention:

    // Update the forceRefresh method to be more thorough
    fun forceRefresh() {
        // Use atomic flag to prevent multiple concurrent refreshes
        if (!isRefreshing.compareAndSet(false, true)) {
            Log.d(TAG, "Refresh already in progress, skipping")
            return
        }

        try {
            Log.d(TAG, "Force refresh triggered")

            // Reset current cache
            _eventsCache.value = emptyMap()

            // Clean up any existing listener
            cleanupListener()

            // Get current user ID
            val userId = auth.currentUser?.uid
            if (userId != null) {
                Log.d(TAG, "Current user: $userId - forcing refresh of events")
            }

            // Increment the refresh trigger to notify observers
            _forceRefreshTrigger.value = System.currentTimeMillis()

            Log.d(TAG, "Force refresh complete with new trigger: ${_forceRefreshTrigger.value}")
        } finally {
            // Always reset the flag when done
            isRefreshing.set(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addEvent(event: Event): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot add event: No user logged in")
            return false
        }

        try {
            Log.d(TAG, "Attempting to add event: ${event.title} for user: $userId")
            val eventMap = event.toMap()
            Log.d(TAG, "Event data: $eventMap")

            val docRef = getUserEventsCollection(userId).add(eventMap).await()
            Log.d(TAG, "Event added successfully with ID: ${docRef.id}")

            // Force refresh to update UI immediately
            forceRefresh()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event", e)
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun deleteEvent(event: Event): Boolean {
        // Get the current user ID directly from Firebase Auth
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot delete event: No user logged in")
            return false
        }

        return try {
            // Find the event document in the user's events collection
            val query = getUserEventsCollection(userId)
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                getUserEventsCollection(userId).document(documentId).delete().await()
                Log.d(TAG, "Event deleted successfully: ${event.title}")

                // Force refresh to update UI immediately
                forceRefresh()
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun getAllEvents(userId: String?): Flow<Map<LocalDate, MutableList<Event>>> = callbackFlow {
        // First, clean up any existing listeners
        cleanupListener()

        // If no user is provided, return empty map and close the flow
        if (userId == null) {
            Log.d(TAG, "No user ID provided, returning empty events")
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        Log.d(TAG, "Setting up NEW events listener for user: $userId")

        // Use a fresh listener each time
        val query = getUserEventsCollection(userId)
        activeListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening for events", error)
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // More detailed logging
                val eventCount = snapshot.documents.size
                Log.d(TAG, "Received $eventCount events for user: $userId")

                // Count tax deadline events specifically
                val taxDeadlineCount = snapshot.documents.count {
                    val title = it.getString("title") ?: ""
                    title.contains("Tax Filing Deadline", ignoreCase = true)
                }
                Log.d(TAG, "Of these, $taxDeadlineCount are tax deadline events")

                val eventsMap = mutableMapOf<LocalDate, MutableList<Event>>()
                for (document in snapshot.documents) {
                    try {
                        val event = document.toEvent()
                        val date = event.date
                        eventsMap.getOrPut(date) { mutableListOf() }.add(event)

                        // Log tax deadline events specifically
                        if (event.title.contains("Tax Filing Deadline", ignoreCase = true)) {
                            Log.d(TAG, "Found tax deadline event: ${event.title} on ${event.date}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event document: ${document.id}", e)
                    }
                }
                trySend(eventsMap)
            }
        }

        // Ensure listener is removed when flow is cancelled
        awaitClose {
            Log.d(TAG, "Closing events listener for user: $userId")
            cleanupListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateEvent(event: Event): Boolean {
        // Get the current user ID directly from Firebase Auth
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot update event: No user logged in")
            return false
        }

        return try {
            // Find the event document in the user's events collection
            val query = getUserEventsCollection(userId)
                .whereEqualTo("title", event.title)
                .whereEqualTo("dateTimestamp", event.date.toTimestamp())
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val documentId = query.documents[0].id
                getUserEventsCollection(userId).document(documentId).set(event.toMap()).await()
                Log.d(TAG, "Event updated successfully: ${event.title}")

                // Force refresh to update UI immediately
                forceRefresh()
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

    // Helper method to clean up the active listener
    private fun cleanupListener() {
        activeListenerRegistration?.let {
            Log.d(TAG, "Removing existing event listener")
            it.remove()
        }
        activeListenerRegistration = null
    }

    // Method to reset the repository when a user logs out
    fun reset() {
        Log.d(TAG, "Resetting EventRepository")
        forceRefresh()
    }

    companion object {
        private const val TAG = "EventRepository"
        @Volatile
        private var INSTANCE: EventRepository? = null

        fun getInstance(): EventRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EventRepository().also { INSTANCE = it }
            }
        }

        // Method to reset the singleton instance when a user logs out
        fun resetInstance() {
            Log.d(TAG, "Resetting EventRepository instance with thorough cleanup")

            // Clean up the existing instance if it exists
            INSTANCE?.let {
                // Clean up any active listeners
                it.cleanupListener()
                // Clear any cached data
                it._eventsCache.value = emptyMap()
                // Increment the refresh trigger one last time
                it._forceRefreshTrigger.value = System.currentTimeMillis()
            }

            // Force the instance to null to ensure a fresh one is created
            INSTANCE = null

            Log.d(TAG, "EventRepository instance has been reset completely")
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
        "hasReminder" to hasReminder,
        "isTodoEvent" to isTodoEvent,
        "isCompleted" to isCompleted
    )
}


// Convert Firestore Document to Event
@RequiresApi(Build.VERSION_CODES.O)
fun com.google.firebase.firestore.DocumentSnapshot.toEvent(): Event {
    val title = getString("title") ?: ""
    val description = getString("description") ?: ""
    val timestamp = get("dateTimestamp") as? Timestamp
        ?: throw IllegalStateException("Missing or invalid dateTimestamp field")
    val date = timestamp.toLocalDate()
    val startTime = getString("startTime") ?: "00:00"
    val endTime = getString("endTime") ?: "00:00"
    val hasReminder = getBoolean("hasReminder") ?: false
    val isTodoEvent = getBoolean("isTodoEvent") ?: false
    val isCompleted = getBoolean("isCompleted") ?: false

    return Event(
        title = title,
        description = description,
        date = date,
        startTime = startTime,
        endTime = endTime,
        hasReminder = hasReminder,
        isTodoEvent = isTodoEvent,
        isCompleted = isCompleted
    )
}