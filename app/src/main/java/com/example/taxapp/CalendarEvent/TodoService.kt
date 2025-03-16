package com.example.taxapp.CalendarEvent

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.taxapp.user.FirebaseManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Worker class for checking past due to-do items
 */
@RequiresApi(Build.VERSION_CODES.O)
class TodoReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TodoReminderWorker"
        private const val WORK_NAME = "todo_reminder_check"

        /**
         * Schedule the periodic work to check for past due to-do items
         */
        fun schedule(context: Context) {
            Log.d(TAG, "Scheduling periodic to-do reminder checks")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<TodoReminderWorker>(
                2, TimeUnit.HOURS, // Check every 2 hours
                15, TimeUnit.MINUTES // Flex interval of 15 minutes
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

            Log.d(TAG, "Todo reminder checks scheduled")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting todo reminder check")

        try {
            // Get current user
            val userId = FirebaseManager.getCurrentUserId() ?: Firebase.auth.currentUser?.uid
            if (userId == null) {
                Log.d(TAG, "No user logged in, skipping reminder check")
                return Result.success()
            }

            // Get all events for the current user
            val eventRepository = EventRepository.getInstance()

            var pastDueTodos: List<Event> = emptyList()
            var nearDueTodos: List<Event> = emptyList()

            try {
                withTimeout(10000) { // 10 second timeout
                    val events = eventRepository.getAllEvents(userId).first()

                    val today = LocalDate.now()
                    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    val allEvents = events.values.flatten()

                    // Find past due to-do events
                    pastDueTodos = allEvents.filter { event ->
                        event.isTodoEvent &&
                                !event.isCompleted &&
                                (event.date.isBefore(today) ||
                                        (event.date.isEqual(today) && event.endTime < currentTime))
                    }

                    // Find to-do events due in the next 2 hours
                    val twoHoursLater = LocalTime.now().plusHours(2)
                        .format(DateTimeFormatter.ofPattern("HH:mm"))

                    nearDueTodos = allEvents.filter { event ->
                        event.isTodoEvent &&
                                !event.isCompleted &&
                                event.date.isEqual(today) &&
                                event.endTime > currentTime &&
                                event.endTime <= twoHoursLater
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching events for reminder check", e)
                return Result.retry() // Retry on error
            }

            // Show notification for past due to-do items
            if (pastDueTodos.isNotEmpty()) {
                val pastDueCount = pastDueTodos.size
                Log.d(TAG, "Found $pastDueCount past due to-do items")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "You have $pastDueCount unfinished to-do tasks that are past due!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Here you would normally create a proper notification using NotificationManager
                    // For simplicity, we're just using Toast but you should implement real notifications
                }
            }

            // Show notification for soon-to-be-due to-do items
            if (nearDueTodos.isNotEmpty()) {
                val nearDueCount = nearDueTodos.size
                Log.d(TAG, "Found $nearDueCount to-do items due in the next 2 hours")

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "You have $nearDueCount to-do tasks due in the next 2 hours!",
                        Toast.LENGTH_LONG
                    ).show()

                    // Here you would normally create a proper notification using NotificationManager
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in todo reminder check", e)
            return Result.failure()
        }
    }
}