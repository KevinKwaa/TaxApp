package com.example.taxapp.CalendarEvent

import android.os.Build
import android.text.Layout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibleColors
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect

/**
 * Custom Date Picker Dialog that follows the app's accessibility and theming patterns
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleDatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate = LocalDate.now(),
    //validateDate: ((LocalDate) -> Pair<Boolean, String?>)? = null
) {
    val isDarkMode = LocalDarkMode.current
    val accessibleColors = LocalThemeColors.current

    // State for validation error
    var dateError by remember { mutableStateOf<String?>(null) }

    // Convert LocalDate to milliseconds for DatePickerState
    val initialMillis = initialDate.atStartOfDay(ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayMode = DisplayMode.Picker
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        // Validate the selected date if a validator is provided
//                        if (validateDate != null) {
//                            val validation = validateDate(selectedDate)
//                            if (!validation.first) {
//                                // Show validation error
//                                dateError = validation.second
//                                return@Button
//                            }
//                        }

                        // If validation passes or no validator
                        onDateSelected(selectedDate)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accessibleColors.buttonBackground,
                    contentColor = accessibleColors.buttonText
                )
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(id = R.string.cancel),
                    color = accessibleColors.calendarText
                )
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = accessibleColors.cardBackground,
            titleContentColor = accessibleColors.headerText,
            headlineContentColor = accessibleColors.headerText,
            weekdayContentColor = accessibleColors.calendarText,
            subheadContentColor = accessibleColors.calendarText,
            yearContentColor = accessibleColors.calendarText,
            currentYearContentColor = accessibleColors.selectedDay,
            selectedYearContainerColor = accessibleColors.selectedDay.copy(alpha = 0.2f),
            selectedYearContentColor = accessibleColors.selectedDay,
            dayContentColor = accessibleColors.calendarText,
            selectedDayContainerColor = accessibleColors.selectedDay,
            selectedDayContentColor = accessibleColors.selectedDayText,
            todayContentColor = accessibleColors.selectedDay,
            todayDateBorderColor = accessibleColors.selectedDay
        ),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Column {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
                colors = DatePickerDefaults.colors(
                    containerColor = accessibleColors.cardBackground,
                    titleContentColor = accessibleColors.headerText,
                    headlineContentColor = accessibleColors.headerText,
                    weekdayContentColor = accessibleColors.calendarText,
                    subheadContentColor = accessibleColors.calendarText,
                    yearContentColor = accessibleColors.calendarText,
                    currentYearContentColor = accessibleColors.selectedDay,
                    selectedYearContainerColor = accessibleColors.selectedDay.copy(alpha = 0.2f),
                    selectedYearContentColor = accessibleColors.selectedDay,
                    dayContentColor = accessibleColors.calendarText,
                    selectedDayContainerColor = accessibleColors.selectedDay,
                    selectedDayContentColor = accessibleColors.selectedDayText,
                    todayContentColor = accessibleColors.selectedDay,
                    todayDateBorderColor = accessibleColors.selectedDay
                )
            )

            // Show error message if validation fails
            if (dateError != null) {
                Text(
                    text = dateError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * An improved time picker dialog with 5-minute increments and validation
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTimePickerDialog(
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialTime: String = "09:00",
    title: String = stringResource(id = R.string.select_time),
    validateTime: ((String) -> Pair<Boolean, String?>)? = null
) {
    val isDarkMode = LocalDarkMode.current
    val accessibleColors = LocalThemeColors.current

    // Parse initial time
    val initialHour = initialTime.substringBefore(":").toIntOrNull() ?: 9
    val initialMinute = initialTime.substringAfter(":").toIntOrNull() ?: 0

    // Round initial minute to nearest 5
    val roundedInitialMinute = (initialMinute / 5) * 5

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(roundedInitialMinute) }

    // Error state
    var validationError by remember { mutableStateOf<String?>(null) }

    // Get current formatted time string
    val currentTimeString = remember(selectedHour, selectedMinute) {
        "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}"
    }

    // Validate time whenever it changes
    LaunchedEffect(currentTimeString) {
        if (validateTime != null) {
            val validation = validateTime(currentTimeString)
            validationError = if (!validation.first) validation.second else null
        } else {
            validationError = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = accessibleColors.headerText
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time scroller
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour picker
                    ImprovedNumberScroller(
                        value = selectedHour,
                        onValueChange = { selectedHour = it },
                        range = 0..23,
                        formatNumber = { it.toString().padStart(2, '0') },
                        primaryColor = accessibleColors.selectedDay
                    )

                    // Separator
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.calendarText,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute picker - 5 minute steps
                    ImprovedNumberScroller(
                        value = selectedMinute,
                        onValueChange = { selectedMinute = it },
                        range = 0..55,
                        step = 5,
                        formatNumber = { it.toString().padStart(2, '0') },
                        primaryColor = accessibleColors.selectedDay
                    )
                }

                // Show selected time in 24-hour format
                Text(
                    text = currentTimeString,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (validationError != null) MaterialTheme.colorScheme.error else accessibleColors.selectedDay,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Show validation error if present
                if (validationError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            //textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Final validation check before returning
                    if (validateTime != null) {
                        val validation = validateTime(currentTimeString)
                        if (!validation.first) {
                            validationError = validation.second
                            return@Button
                        }
                    }

                    onTimeSelected(currentTimeString)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accessibleColors.buttonBackground,
                    contentColor = accessibleColors.buttonText
                ),
                shape = RoundedCornerShape(50),
                enabled = validationError == null
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(id = R.string.cancel),
                    color = accessibleColors.calendarText
                )
            }
        },
        containerColor = accessibleColors.cardBackground,
        titleContentColor = accessibleColors.headerText,
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    )
}

/**
 * Improved number scroller that matches the design in the screenshot
 */
@Composable
fun ImprovedNumberScroller(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int = 1,
    formatNumber: (Int) -> String = { it.toString() },
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val accessibleColors = LocalThemeColors.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Up arrow
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEEEEE))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val newValue = if (value >= range.last) range.first else value + step
                    onValueChange(newValue)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▲",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Current value
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(primaryColor)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatNumber(value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                //textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Down arrow
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEEEEE))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val newValue = if (value <= range.first) range.last else value - step
                    onValueChange(newValue)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▼",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }
    }
}

/**
 * Updated method that uses the improved time picker
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AccessibleTimePickerDialog(
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialTime: String = "09:00",
    title: String = stringResource(id = R.string.select_time),
    validateTime: ((String) -> Pair<Boolean, String?>)? = null
) {
    // Use our improved time picker implementation
    ImprovedTimePickerDialog(
        onTimeSelected = onTimeSelected,
        onDismiss = onDismiss,
        initialTime = initialTime,
        title = title,
        validateTime = validateTime
    )
}

/**
 * Helper function to format a date according to the user's locale
 */
@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(date: LocalDate, locale: java.util.Locale): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", locale)
    return date.format(formatter)
}

/**
 * Helper function to parse a time string into a LocalTime
 */
@RequiresApi(Build.VERSION_CODES.O)
fun parseTime(timeString: String): LocalTime {
    return try {
        LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        // Default to current time if parsing fails
        LocalTime.now()
    }
}