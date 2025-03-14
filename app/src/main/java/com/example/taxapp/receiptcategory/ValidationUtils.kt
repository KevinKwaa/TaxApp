package com.example.taxapp.receiptcategory

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class with extension functions for validation in ViewModels
 */
object ValidationUtils {

    /**
     * Validates if a text field is not empty
     * @param text The text to validate
     * @return True if the text is not empty after trimming
     */
    fun isTextValid(text: String): Boolean {
        return text.trim().isNotEmpty()
    }

    /**
     * Validates if an amount is valid (can be parsed as a positive double)
     * @param amountStr The amount string to validate
     * @return True if the amount is a valid positive number
     */
    fun isAmountValid(amountStr: String): Boolean {
        val amount = amountStr.replace(",", ".").toDoubleOrNull()
        return amount != null && amount > 0
    }

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

    /**
     * Parses a date string into a Date object
     * @param dateStr The date string to parse (format: DD/MM/YYYY)
     * @return A Date object or null if the date string is invalid
     */
    fun parseDate(dateStr: String): Date? {
        return if (isDateValid(dateStr)) {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.parse(dateStr)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * Formats a Date object to a string using the format DD/MM/YYYY
     * @param date The Date object to format
     * @return A formatted date string
     */
    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Extension function to clean an amount string for parsing
     * @return A string ready for parsing as a Double or null if invalid
     */
    fun String.cleanAmount(): Double? {
        return this.replace(",", ".").toDoubleOrNull()
    }
}