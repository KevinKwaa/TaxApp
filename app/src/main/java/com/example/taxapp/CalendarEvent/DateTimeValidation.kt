package com.example.taxapp.CalendarEvent

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TimeValidator {
    fun validateTimeString(timeString: String): Pair<Boolean, String?> {
        val timeRegex = Regex("^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$")

        if (!timeRegex.matches(timeString)) {
            return Pair(false, "Invalid time format. Use HH:MM (24-hour).")
        }

        return Pair(true, null)
    }

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
}

@RequiresApi(Build.VERSION_CODES.O)
object DateValidator {
    fun isPastDate(date: LocalDate): Boolean {
        val today = LocalDate.now()
        return date.isBefore(today)
    }
}