package com.example.taxapp.CalendarEvent

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Helper class for time and date validation
 */
object TimeValidator {
    /**
     * Validate a time string in HH:MM format
     * @return pair of (isValid, errorMessage)
     */
    fun validateTimeString(timeString: String): Pair<Boolean, String?> {
        val timeRegex = Regex("^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$")

        if (!timeRegex.matches(timeString)) {
            return Pair(false, "Invalid time format. Use HH:MM (24-hour).")
        }

        return Pair(true, null)
    }

    /**
     * Validate that end time is after start time
     * @return pair of (isValid, errorMessage)
     */
    fun validateTimeOrder(startTime: String, endTime: String, errorMessage: String): Pair<Boolean, String?> {
        // First check if both times are valid
        val startValid = validateTimeString(startTime)
        val endValid = validateTimeString(endTime)

        if (!startValid.first) return startValid
        if (!endValid.first) return endValid

        // Split the times and compare
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")

        val startHour = startParts[0].toInt()
        val startMinute = startParts[1].toInt()
        val endHour = endParts[0].toInt()
        val endMinute = endParts[1].toInt()

        // Compare hours first, then minutes
        if (endHour < startHour || (endHour == startHour && endMinute <= startMinute)) {
            return Pair(false, errorMessage)
        }

        return Pair(true, null)
    }

    /**
     * Normalize a time string to ensure it's in proper HH:MM format
     */
    fun normalizeTimeString(timeString: String): String {
        val valid = validateTimeString(timeString)
        if (!valid.first) return "00:00" // Default to midnight if invalid

        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Round minutes to nearest 5 minutes
     */
    fun roundToNearestFiveMinutes(timeString: String): String {
        val valid = validateTimeString(timeString)
        if (!valid.first) return "00:00" // Default to midnight if invalid

        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        // Round to nearest 5 minutes
        val roundedMinute = ((minute + 2) / 5) * 5
        var adjustedHour = hour

        // Handle overflow
        if (roundedMinute == 60) {
            adjustedHour = (hour + 1) % 24
            return String.format("%02d:%02d", adjustedHour, 0)
        }

        return String.format("%02d:%02d", hour, roundedMinute)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
object DateValidator {
    /**
     * Validate a date string in yyyy-MM-dd format
     * @return pair of (isValid, errorMessage)
     */
    fun validateDateString(dateString: String): Pair<Boolean, String?> {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            LocalDate.parse(dateString, formatter)
            return Pair(true, null)
        } catch (e: DateTimeParseException) {
            return Pair(false, "Invalid date format. Use YYYY-MM-DD.")
        }
    }

    /**
     * Check if a date is in the past
     */
    fun isPastDate(date: LocalDate): Boolean {
        val today = LocalDate.now()
        return date.isBefore(today)
    }

    /**
     * Check if a date and time combination is in the past
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isDateTimePast(date: LocalDate, timeString: String): Boolean {
        val today = LocalDate.now()
        val now = LocalTime.now()

        if (date.isBefore(today)) {
            return true
        }

        if (date.isEqual(today)) {
            try {
                val time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
                return time.isBefore(now)
            } catch (e: Exception) {
                return false
            }
        }

        return false
    }
}