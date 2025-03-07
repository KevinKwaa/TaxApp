package com.example.taxapp.CalendarEvent

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.temporal.ChronoUnit

/**
 * Helper class to manage tax deadline events in the calendar
 */
object TaxDeadlineHelper {
    private const val TAG = "TaxDeadlineHelper"

    // Event ID prefixes for tax deadline events
    private const val EMPLOYEE_PREFIX = "Tax Filing Deadline (Employee)"
    private const val SELF_EMPLOY_PREFIX = "Tax Filing Deadline (Self-Employed)"

    /**
     * Update tax deadline events based on the user's tax filing preference
     * Returns true if the update was successful, false otherwise
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun updateTaxDeadlineEvents(
        employment: String,
        scope: CoroutineScope,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val userId = FirebaseManager.getCurrentUserId()
        if (userId == null) {
            Log.e(TAG, "No user ID available")
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
            return
        }

        Log.d(TAG, "Starting tax deadline update with employment type: $employment for user: $userId")

        try {
            // First, make sure we have a fresh repository instance and clear events
            EventRepository.resetInstance()
            val eventRepository = EventRepository.getInstance()

            // Step 1: Find and delete ALL existing tax deadline events DIRECTLY in Firestore
            val success = deleteAllTaxDeadlineEvents(userId)
            Log.d(TAG, "Direct Firestore deletion of tax events complete, success: $success")

            // Small delay to ensure deletions are processed
            withContext(Dispatchers.IO) {
                Thread.sleep(300)
            }

            // Step 2: Create new deadline events based on employment type
            Log.d(TAG, "Creating new tax deadline events for employment type: $employment")
            val createSuccess = when (employment) {
                "employee" -> createEmployeeDeadlineEvents(userId, eventRepository)
                "self-employed" -> createSelfEmployDeadlineEvents(userId, eventRepository)
                else -> {
                    Log.d(TAG, "Unknown employment type: $employment, defaulting to employee")
                    createEmployeeDeadlineEvents(userId, eventRepository)
                }
            }

            Log.d(TAG, "Creation of new tax events complete, success: $createSuccess")

            // Small delay to ensure creations are processed
            withContext(Dispatchers.IO) {
                Thread.sleep(300)
            }

            // Step 3: CRITICAL - Force refresh the repository to update the UI
            eventRepository.forceRefresh()

            // Log event count after update
            eventRepository.getAllEvents(userId).first().let { events ->
                Log.d(TAG, "After update: Found ${events.size} event dates with a total of ${events.values.sumOf { it.size }} events")
                val taxEvents = events.values.flatten().filter { it.title.contains("Tax Filing Deadline", ignoreCase = true) }
                Log.d(TAG, "Tax deadline events after update: ${taxEvents.size}")
                taxEvents.forEach { event ->
                    Log.d(TAG, "  - Tax event: ${event.title} on ${event.date}")
                }
            }

            // Report final status
            val finalSuccess = createSuccess && success
            Log.d(TAG, "Tax deadline update completed. Overall success: $finalSuccess")
            withContext(Dispatchers.Main) {
                onComplete(finalSuccess)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tax deadline events", e)
            // Force refresh even on error to ensure UI updates
            EventRepository.getInstance().forceRefresh()
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }

    /**
     * Delete all existing tax deadline events directly from Firestore
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun deleteAllTaxDeadlineEvents(userId: String): Boolean {
        Log.d(TAG, "Starting DIRECT deletion of all tax deadline events in Firestore")
        try {
            // Get reference to Firestore
            val firestore = FirebaseManager.getAuthFirestore()
            val eventsCollection = firestore.collection("users").document(userId).collection("events")

            // Get all events directly
            val documents = eventsCollection.get().await().documents
            Log.d(TAG, "Found ${documents.size} total events in Firestore")

            // Find tax deadline events
            val taxDeadlineEvents = documents.filter { doc ->
                val title = doc.getString("title") ?: ""
                title.contains("Tax Filing Deadline", ignoreCase = true)
            }

            Log.d(TAG, "Found ${taxDeadlineEvents.size} tax deadline events to delete")

            // Delete each event directly
            var allSuccessful = true
            for (doc in taxDeadlineEvents) {
                try {
                    val docId = doc.id
                    Log.d(TAG, "Deleting tax event with ID: $docId, title: ${doc.getString("title")}")
                    eventsCollection.document(docId).delete().await()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting event document", e)
                    allSuccessful = false
                }
            }

            Log.d(TAG, "Deleted ${taxDeadlineEvents.size} tax deadline events with success: $allSuccessful")
            return allSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct Firestore deletion of tax events", e)
            return false
        }
    }

    /**
     * Create deadline events for employee filing (April 30th) for multiple years
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createEmployeeDeadlineEvents(userId: String, eventRepository: EventRepository): Boolean {
        val currentYear = Year.now().value
        var allSuccessful = true

        // Create events for the current year and next 3 years
        for (year in currentYear..currentYear + 3) {
            val deadlineDate = LocalDate.of(year, Month.APRIL, 30)

            // Skip past dates
            val today = LocalDate.now()
            if (deadlineDate.isBefore(today)) {
                Log.d(TAG, "Skipping past date: $deadlineDate")
                continue
            }

            Log.d(TAG, "Creating employee deadline event for $year-04-30")

            val event = Event(
                title = "Tax Filing Deadline (Employee) - $year",
                description = "Today is the deadline for filing your taxes for year ${year-1}. Make sure you've submitted all required forms to LHDN.",
                date = deadlineDate,
                startTime = "00:00",
                endTime = "23:59",
                hasReminder = true
            )

            try {
                // ADD DIRECT FIRESTORE CREATION - more reliable than repository
                val firestore = FirebaseManager.getAuthFirestore()
                val eventMap = event.toMap()

                // Create directly in Firestore
                firestore.collection("users").document(userId)
                    .collection("events")
                    .add(eventMap)
                    .await()

                Log.d(TAG, "Successfully created employee deadline event for $year DIRECTLY in Firestore")

                // Small delay to avoid overwhelming Firestore
                withContext(Dispatchers.IO) {
                    Thread.sleep(100)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating employee deadline event for $year", e)
                allSuccessful = false
            }
        }

        return allSuccessful
    }

    /**
     * Create deadline events for self-employed filing (June 30th) for multiple years
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createSelfEmployDeadlineEvents(userId: String, eventRepository: EventRepository): Boolean {
        val currentYear = Year.now().value
        var allSuccessful = true

        // Create events for the current year and next 3 years
        for (year in currentYear..currentYear + 3) {
            val deadlineDate = LocalDate.of(year, Month.JUNE, 30)

            // Skip past dates
            val today = LocalDate.now()
            if (deadlineDate.isBefore(today)) {
                Log.d(TAG, "Skipping past date: $deadlineDate")
                continue
            }

            Log.d(TAG, "Creating self-employed deadline event for $year-06-30")

            val event = Event(
                title = "Tax Filing Deadline (Self-Employed) - $year",
                description = "Today is the deadline for filing your taxes for year ${year-1}. Make sure you've submitted all required forms to LHDN.",
                date = deadlineDate,
                startTime = "00:00",
                endTime = "23:59",
                hasReminder = true
            )

            try {
                // ADD DIRECT FIRESTORE CREATION - more reliable than repository
                val firestore = FirebaseManager.getAuthFirestore()
                val eventMap = event.toMap()

                // Create directly in Firestore
                firestore.collection("users").document(userId)
                    .collection("events")
                    .add(eventMap)
                    .await()

                Log.d(TAG, "Successfully created self-employed deadline event for $year DIRECTLY in Firestore")

                // Small delay to avoid overwhelming Firestore
                withContext(Dispatchers.IO) {
                    Thread.sleep(100)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating self-employed deadline event for $year", e)
                allSuccessful = false
            }
        }

        return allSuccessful
    }

    /**
     * Check if the tax deadline is approaching and show a notification
     * Call this method from MainActivity or a worker for periodic checks
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun checkUpcomingDeadline(context: Context, scope: CoroutineScope) {
        scope.launch {
            try {
                val userId = FirebaseManager.getCurrentUserId() ?: return@launch

                // Get user's tax filing preference
                val firestore = FirebaseManager.getAuthFirestore()
                val userDoc = firestore.collection("users").document(userId).get().await()

                val employment = userDoc.getString("employment") ?: "employee"

                // Determine the deadline date based on preference
                val currentYear = Year.now().value
                val deadlineDate = when (employment) {
                    "employee" -> LocalDate.of(currentYear, Month.APRIL, 30)
                    "self-employed" -> LocalDate.of(currentYear, Month.JUNE, 30)
                    else -> LocalDate.of(currentYear, Month.APRIL, 30) // Default to employee
                }

                // Calculate days remaining until the deadline
                val today = LocalDate.now()
                val daysUntilDeadline = ChronoUnit.DAYS.between(today, deadlineDate)

                // Show a notification if the deadline is approaching (within 30 days)
                if (daysUntilDeadline in 0..30) {
                    withContext(Dispatchers.Main) {
                        val deadlineType = if (employment == "employee") "employee" else "self-employed"

                        if (daysUntilDeadline == 0L) {
                            Toast.makeText(
                                context,
                                "TODAY is the tax deadline for $deadlineType!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Your tax deadline for $deadlineType is in $daysUntilDeadline days!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                Log.d(TAG, "Checked for upcoming tax deadline. Days remaining: $daysUntilDeadline")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking upcoming deadline", e)
            }
        }
    }

    // Test function
    @RequiresApi(Build.VERSION_CODES.O)
    fun testAddDeadlineEvent(context: Context, scope: CoroutineScope) {
        Toast.makeText(context, "Testing tax deadline event creation...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val eventRepository = EventRepository.getInstance()
                val testDate = LocalDate.now().plusDays(1) // Tomorrow for testing

                val event = Event(
                    title = "TEST - Tax Filing Deadline",
                    description = "This is a test tax deadline event.",
                    date = testDate,
                    startTime = "12:00",
                    endTime = "13:00",
                    hasReminder = true
                )

                val success = eventRepository.addEvent(event)

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "Test event created successfully for ${testDate}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to create test event", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating test event", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Directly force update default tax events for a user
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun forceDefaultEvents(scope: CoroutineScope) {
        scope.launch {
            try {
                val userId = FirebaseManager.getCurrentUserId() ?: return@launch

                // Get user's employment status
                val firestore = FirebaseManager.getAuthFirestore()
                val userDoc = firestore.collection("users").document(userId).get().await()
                val employment = userDoc.getString("employment") ?: "employee"

                Log.d(TAG, "Forcing default tax events for: $employment")

                // Update tax deadline events
                updateTaxDeadlineEvents(employment, scope) { success ->
                    Log.d(TAG, "Force default events completed: $success")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forcing default events", e)
            }
        }
    }
}