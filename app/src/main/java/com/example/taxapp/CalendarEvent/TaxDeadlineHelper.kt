package com.example.taxapp.CalendarEvent

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.taxapp.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.time.Year
import java.time.temporal.ChronoUnit

/**
 * Helper class to manage tax deadline events in the calendar
 */
object TaxDeadlineHelper {
    private const val TAG = "TaxDeadlineHelper"

    // Event IDs for tax deadline events
    private const val EMPLOYEE_ID = "tax_deadline_employee"
    private const val SELF_EMPLOY_ID = "tax_deadline_self_employ"

    /**
     * Update tax deadline events based on the user's tax filing preference
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTaxDeadlineEvents(employment: String, scope: CoroutineScope, onComplete: () -> Unit = {}) {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        Log.d(TAG, "Starting tax deadline update with employment type: $employment")

        scope.launch(Dispatchers.IO) {
            try {
                val eventRepository = EventRepository.getInstance()

                // First, find and delete any existing tax deadline events
                val deadlineKeywords = listOf("Tax Filing Deadline", "tax deadline")

                // Get all events
                val allEvents = mutableMapOf<LocalDate, MutableList<Event>>()
                try {
                    // Use collectLatest to get just one emission
                    eventRepository.getAllEvents(userId).collectLatest { eventsMap ->
                        allEvents.putAll(eventsMap)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting events", e)
                }

                // Find all deadline events
                val eventsToDelete = mutableListOf<Event>()
                for ((_, events) in allEvents) {
                    for (event in events) {
                        if (deadlineKeywords.any { event.title.contains(it, ignoreCase = true) }) {
                            eventsToDelete.add(event)
                        }
                    }
                }

                Log.d(TAG, "Found ${eventsToDelete.size} existing tax deadline events to remove")

                // Delete each event
                for (event in eventsToDelete) {
                    val success = eventRepository.deleteEvent(event)
                    Log.d(TAG, "Deleted tax deadline event: ${event.title} - success: $success")
                }

                // Create new deadline based on employment type
                Log.d(TAG, "Creating new tax deadline for employment type: $employment")
                when (employment) {
                    "employee" -> createEmployeeDeadlineEvent(eventRepository)
                    "self-employed" -> createSelfEmployDeadlineEvent(eventRepository)
                    else -> {
                        Log.d(TAG, "Unknown employment type: $employment, defaulting to employee")
                        createEmployeeDeadlineEvent(eventRepository)
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating tax deadline events", e)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    /**
     * Remove existing tax deadline events to avoid duplicates
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun removeExistingTaxDeadlineEvents(eventRepository: EventRepository) {
        val userId = FirebaseManager.getCurrentUserId() ?: return

        try {
            // Get events once (not as a flow) for a more direct approach
            val allEvents = mutableListOf<Event>()
            eventRepository.getAllEvents(userId).first().forEach { (_, events) ->
                allEvents.addAll(events)
            }

            // Find tax deadline events
            val deadlineEvents = allEvents.filter {
                it.title.contains("Tax Filing Deadline", ignoreCase = true)
            }

            Log.d(TAG, "Found ${deadlineEvents.size} tax deadline events to remove")

            // Delete each event and wait for completion
            var successCount = 0
            for (event in deadlineEvents) {
                val success = eventRepository.deleteEvent(event)
                if (success) successCount++
            }

            Log.d(TAG, "Successfully deleted $successCount tax deadline events")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing tax deadline events", e)
        }
    }

    /**
     * Create a deadline event for self-filing (April 30th)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createEmployeeDeadlineEvent(eventRepository: EventRepository) {
        val currentYear = Year.now().value
        val deadlineDate = LocalDate.of(currentYear, Month.APRIL, 30)

        val event = Event(
            title = "Tax Filing Deadline (Employee)",
            description = "Today is the deadline for filing your taxes. Make sure you've submitted all required forms to LHDN.",
            date = deadlineDate,
            startTime = "00:00",
            endTime = "23:59",
            hasReminder = true
        )

        Log.d(TAG, "Attempting to add employee deadline event for $deadlineDate")
        val success = eventRepository.addEvent(event)
        if (success) {
            Log.d(TAG, "Created employee deadline event for $deadlineDate")
        } else {
            Log.e(TAG, "Failed to create employee deadline event")
        }
    }

    /**
     * Create a deadline event for agent-filing (June 30th)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun createSelfEmployDeadlineEvent(eventRepository: EventRepository) {
        val currentYear = Year.now().value
        val deadlineDate = LocalDate.of(currentYear, Month.JUNE, 30)

        val event = Event(
            title = "Tax Filing Deadline (Self-Employed)",
            description = "Today is the deadline for filing your taxes. Make sure you had submitted all required forms to LHDN.",
            date = deadlineDate,
            startTime = "00:00",
            endTime = "23:59",
            hasReminder = true
        )

        val success = eventRepository.addEvent(event)
        if (success) {
            Log.d(TAG, "Created self-employed deadline event for $deadlineDate")
        } else {
            Log.e(TAG, "Failed to create self-employed deadline event")
        }
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
                    else -> LocalDate.of(currentYear, Month.APRIL, 30) // Default to self-filing
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

    // In TaxDeadlineHelper.kt
    @RequiresApi(Build.VERSION_CODES.O)
    fun testAddDeadlineEvent(context: Context, scope: CoroutineScope) {
        Toast.makeText(context, "Testing tax deadline event creation...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val eventRepository = EventRepository.getInstance()
                val currentYear = Year.now().value
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
}