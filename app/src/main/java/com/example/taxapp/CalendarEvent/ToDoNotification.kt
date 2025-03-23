package com.example.taxapp.CalendarEvent

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Manages notification states and acknowledgments for the app
 */
class NotificationManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if the past due notification was already acknowledged today
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun wasPastDueAcknowledgedRecently(): Boolean {
        val lastAckTimeMillis = prefs.getLong(KEY_LAST_PAST_DUE_ACK, 0)
        if (lastAckTimeMillis == 0L) return false

        // Convert to LocalDateTime
        val lastAckTime = Instant.ofEpochMilli(lastAckTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        // Check if acknowledgment was today or if it's been less than the specified hours
        val now = LocalDateTime.now()
        val hoursSinceAck = ChronoUnit.HOURS.between(lastAckTime, now)

        // Return true if acknowledged today and within the minimum hours
        return (hoursSinceAck < HOURS_BETWEEN_NOTIFICATIONS)
    }

    /**
     * Save the current time as the last acknowledgment time for past due notifications
     */
    fun acknowledgePastDueNotification() {
        prefs.edit()
            .putLong(KEY_LAST_PAST_DUE_ACK, System.currentTimeMillis())
            .apply()
    }

    /**
     * Reset the past due notification acknowledgment
     * Use this sparingly, mainly for testing or in case of user preference reset
     */
    fun resetPastDueAcknowledgement() {
        prefs.edit()
            .remove(KEY_LAST_PAST_DUE_ACK)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "taxapp_notification_prefs"
        private const val KEY_LAST_PAST_DUE_ACK = "last_past_due_acknowledgment"
        private const val HOURS_BETWEEN_NOTIFICATIONS = 6L  // Only show notification every 6 hours

        @Volatile
        private var INSTANCE: NotificationManager? = null

        fun getInstance(context: Context): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}