package com.example.taxapp.user

import android.content.Context
import android.util.Patterns
import com.example.taxapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ValidationUtil {

    // Email validation - Check if it's a valid email format
    fun validateEmail(email: String, context: Context): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, context.getString(R.string.error_email_empty))
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> ValidationResult(false, context.getString(R.string.error_email_format))
            else -> ValidationResult(true)
        }
    }

    // Name validation - Check if it's not empty and contains only valid characters
    fun validateName(name: String, context: Context): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, context.getString(R.string.error_name_empty))
            name.length < 2 -> ValidationResult(false, context.getString(R.string.error_name_too_short))
            !name.all { it.isLetter() || it.isWhitespace() || it == '.' || it == '-' || it == '\'' } ->
                ValidationResult(false, context.getString(R.string.error_name_invalid_chars))
            else -> ValidationResult(true)
        }
    }

    // Phone validation - Check if it follows a valid phone number pattern
    fun validatePhone(phone: String, context: Context): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult(false, context.getString(R.string.error_phone_empty))
            phone.length < 10 -> ValidationResult(false, context.getString(R.string.error_phone_too_short))
            !phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' } ->
                ValidationResult(false, context.getString(R.string.error_phone_invalid_chars))
            else -> ValidationResult(true)
        }
    }

    // Date of Birth validation - Check if it's a valid date in the format MM/DD/YYYY
    fun validateDOB(dob: String, context: Context): ValidationResult {
        if (dob.isBlank()) {
            return ValidationResult(false, context.getString(R.string.error_dob_empty))
        }

        // First, check the format manually to provide more specific error messages
        val parts = dob.split("/")
        if (parts.size != 3) {
            return ValidationResult(false, context.getString(R.string.error_dob_format))
        }

        try {
            // Extract day, month, year
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            // Validate month (1-12)
            if (month < 1 || month > 12) {
                return ValidationResult(false, context.getString(R.string.error_dob_invalid_month))
            }

            // Validate day based on month
            val maxDays = when(month) {
                2 -> if (isLeapYear(year)) 29 else 28 // February
                4, 6, 9, 11 -> 30 // April, June, September, November
                else -> 31 // All other months
            }

            if (day < 1 || day > maxDays) {
                if (month == 2 && day == 29 && !isLeapYear(year)) {
                    return ValidationResult(false, context.getString(R.string.error_dob_not_leap_year))
                } else {
                    return ValidationResult(false, context.getString(R.string.error_dob_invalid_day))
                }
            }

            // Now use SimpleDateFormat for additional validations (creates a Date object)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            sdf.isLenient = false
            val parsedDate = sdf.parse(dob)

            // Check if the date is in the future
            if (parsedDate != null && parsedDate.after(Date())) {
                return ValidationResult(false, context.getString(R.string.error_dob_future))
            }

            // Check if the date is unreasonably old
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -120) // 120 years ago
            if (parsedDate != null && parsedDate.before(calendar.time)) {
                return ValidationResult(false, context.getString(R.string.error_dob_too_old))
            }

            return ValidationResult(true)
        } catch (e: NumberFormatException) {
            return ValidationResult(false, context.getString(R.string.error_dob_format))
        } catch (e: Exception) {
            return ValidationResult(false, context.getString(R.string.error_dob_format))
        }
    }

    // Helper function to check if a year is a leap year
    private fun isLeapYear(year: Int): Boolean {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
    // Income validation - Check if it's a valid number
    fun validateIncome(income: String, context: Context): ValidationResult {
        return when {
            income.isBlank() -> ValidationResult(false, context.getString(R.string.error_income_empty))
            !income.all { it.isDigit() || it == '.' || it == ',' } ->
                ValidationResult(false, context.getString(R.string.error_income_invalid_chars))
            else -> {
                try {
                    // Remove any commas and try to parse as double
                    val parsedIncome = income.replace(",", "").toDouble()
                    if (parsedIncome < 0) {
                        ValidationResult(false, context.getString(R.string.error_income_negative))
                    } else {
                        ValidationResult(true)
                    }
                } catch (e: Exception) {
                    ValidationResult(false, context.getString(R.string.error_income_invalid_format))
                }
            }
        }
    }

    // Employment validation - Check if it's one of the valid options
    fun validateEmployment(employment: String, context: Context): ValidationResult {
        return when (employment) {
            "employee", "self-employed" -> ValidationResult(true)
            else -> ValidationResult(false, context.getString(R.string.error_employment_invalid))
        }
    }

    // Password validation - Check if it follows required format
    fun validatePassword(password: String, context: Context): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, context.getString(R.string.error_password_empty))
            password.length < 8 -> ValidationResult(false, context.getString(R.string.error_password_too_short))
            !password.any { it.isUpperCase() } -> ValidationResult(false, context.getString(R.string.error_password_no_uppercase))
            !password.any { it.isDigit() } -> ValidationResult(false, context.getString(R.string.error_password_no_digit))
            !password.any { !it.isLetterOrDigit() } -> ValidationResult(false, context.getString(R.string.error_password_no_special))
            else -> ValidationResult(true)
        }
    }

    // Password confirmation validation - Check if passwords match
    fun validatePasswordConfirmation(password: String, confirmPassword: String, context: Context): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, context.getString(R.string.error_password_confirmation_empty))
            password != confirmPassword -> ValidationResult(false, context.getString(R.string.error_password_mismatch))
            else -> ValidationResult(true)
        }
    }

    // Combined validation for all fields
    fun validateAllFields(
        name: String,
        phone: String,
        dob: String,
        income: String,
        employment: String,
        context: Context
    ): Boolean {
        val nameResult = validateName(name, context)
        val phoneResult = validatePhone(phone, context)
        val dobResult = validateDOB(dob, context)
        val incomeResult = validateIncome(income, context)
        val employmentResult = validateEmployment(employment, context)

        return nameResult.isValid && phoneResult.isValid &&
                dobResult.isValid && incomeResult.isValid && employmentResult.isValid
    }

    // For backward compatibility - these methods use the app context to get strings
    // You should migrate all calls to the context versions above

    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Name cannot be empty")
            name.length < 2 -> ValidationResult(false, "Name is too short")
            !name.all { it.isLetter() || it.isWhitespace() || it == '.' || it == '-' || it == '\'' } ->
                ValidationResult(false, "Name contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult(false, "Phone number cannot be empty")
            phone.length < 10 -> ValidationResult(false, "Phone number is too short")
            !phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' } ->
                ValidationResult(false, "Phone number contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    fun validateDOB(dob: String): ValidationResult {
        if (dob.isBlank()) {
            return ValidationResult(false, "Date of birth cannot be empty")
        }

        return try {
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            sdf.isLenient = false
            val parsedDate = sdf.parse(dob)

            // Check if the date is in the past
            if (parsedDate != null && parsedDate.after(Date())) {
                return ValidationResult(false, "Date of birth cannot be in the future")
            }

            ValidationResult(true)
        } catch (e: Exception) {
            ValidationResult(false, "Invalid date format. Use MM/DD/YYYY")
        }
    }

    fun validateIncome(income: String): ValidationResult {
        return when {
            income.isBlank() -> ValidationResult(false, "Income cannot be empty")
            !income.all { it.isDigit() || it == '.' || it == ',' } ->
                ValidationResult(false, "Income can only contain numbers, commas, and periods")
            else -> {
                try {
                    // Remove any commas and try to parse as double
                    val parsedIncome = income.replace(",", "").toDouble()
                    if (parsedIncome < 0) {
                        ValidationResult(false, "Income cannot be negative")
                    } else {
                        ValidationResult(true)
                    }
                } catch (e: Exception) {
                    ValidationResult(false, "Invalid income format")
                }
            }
        }
    }

    fun validateEmployment(employment: String): ValidationResult {
        return when (employment) {
            "employee", "self-employed" -> ValidationResult(true)
            else -> ValidationResult(false, "Please select a valid employment type")
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "Password cannot be empty")
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters long")
            !password.any { it.isUpperCase() } -> ValidationResult(false, "Password must contain at least one uppercase letter")
            !password.any { it.isDigit() } -> ValidationResult(false, "Password must contain at least one number")
            !password.any { !it.isLetterOrDigit() } -> ValidationResult(false, "Password must contain at least one special character")
            else -> ValidationResult(true)
        }
    }

    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Please confirm your password")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)