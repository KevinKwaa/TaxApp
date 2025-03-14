package com.example.taxapp.user

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.taxapp.accessibility.LocalThemeColors
import java.util.*
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * A composable that wraps a text field with a date picker dialog
 * Uses the platform DatePickerDialog for consistency
 */
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    ttsManager: Any? = null
) {
    val context = LocalContext.current
    val accessibleColors = LocalThemeColors.current

    // Parse the current date value if possible
    val calendar = Calendar.getInstance()

    // Try to parse the existing date, or default to 18 years ago
    try {
        if (value.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val date = sdf.parse(value)
            if (date != null) {
                calendar.time = date
            } else {
                // Default to 18 years ago
                calendar.add(Calendar.YEAR, -18)
            }
        } else {
            // Default to 18 years ago
            calendar.add(Calendar.YEAR, -18)
        }
    } catch (e: Exception) {
        // Default to 18 years ago on any parsing error
        calendar.add(Calendar.YEAR, -18)
    }

    // Create date picker dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            // Format the date as MM/DD/YYYY
            val formattedDate = String.format("%02d/%02d/%04d", month + 1, dayOfMonth, year)
            onValueChange(formattedDate)

            // Announce via TTS if available
            ttsManager?.let {
                try {
                    val speakMethod = it::class.java.getMethod("speak", String::class.java)
                    speakMethod.invoke(it, "Selected date $formattedDate")
                } catch (e: Exception) {
                    // Handle if the method doesn't exist
                }
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Set a maximum date to today (no future dates)
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    OutlinedTextField(
        value = value,
        onValueChange = { /* Read-only, use dialog instead */ },
        label = label,
        modifier = modifier.clickable(enabled = enabled) {
            if (enabled) {
                ttsManager?.let {
                    try {
                        val speakMethod = it::class.java.getMethod("speak", String::class.java)
                        speakMethod.invoke(it, "Opening date picker")
                    } catch (e: Exception) {
                        // Handle if the method doesn't exist
                    }
                }
                datePickerDialog.show()
            }
        },
        readOnly = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        placeholder = {
            Text(
                text = "MM/DD/YYYY",
                color = accessibleColors.calendarText.copy(alpha = 0.5f)
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error else accessibleColors.calendarBorder,
            focusedTextColor = accessibleColors.calendarText,
            unfocusedTextColor = accessibleColors.calendarText
        )
    )
}