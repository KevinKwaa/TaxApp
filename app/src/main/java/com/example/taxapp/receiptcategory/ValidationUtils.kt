package com.example.taxapp.receiptcategory

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility class with extension functions for validation in ViewModels
 */
object ValidationUtils {

    /**
     * Validates if a date string matches the format DD/MM/YYYY and is a valid date
     * @param dateStr The date string to validate
     * @return True if the date is valid
     */
    fun isDateValid(dateStr: String): Boolean {
        // Check format with regex first (DD/MM/YYYY)
        if (!dateStr.matches(Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$"))) {
            return false
        }

        // Then verify it's a valid date (e.g., not 31/02/2023)
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }

}