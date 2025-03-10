package com.example.taxapp.user

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ValidationUtil {

    // Name validation - Check if it's not empty and contains only valid characters
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Name cannot be empty")
            name.length < 2 -> ValidationResult(false, "Name is too short")
            !name.all { it.isLetter() || it.isWhitespace() || it == '.' || it == '-' || it == '\'' } ->
                ValidationResult(false, "Name contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    // Phone validation - Check if it follows a valid phone number pattern
    fun validatePhone(phone: String): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult(false, "Phone number cannot be empty")
            phone.length < 10 -> ValidationResult(false, "Phone number is too short")
            !phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' } ->
                ValidationResult(false, "Phone number contains invalid characters")
            else -> ValidationResult(true)
        }
    }

    // Date of Birth validation - Check if it's a valid date in the format MM/DD/YYYY
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

    // Income validation - Check if it's a valid number
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

    // Employment validation - Check if it's one of the valid options
    fun validateEmployment(employment: String): ValidationResult {
        return when (employment) {
            "employee", "self-employed" -> ValidationResult(true)
            else -> ValidationResult(false, "Please select a valid employment type")
        }
    }

    // Password validation - Check if it follows required format
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

    // Password confirmation validation - Check if passwords match
    fun validatePasswordConfirmation(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Please confirm your password")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }

    // Combined validation for all fields
    fun validateAllFields(
        name: String,
        phone: String,
        dob: String,
        income: String,
        employment: String
    ): Boolean {
        val nameResult = validateName(name)
        val phoneResult = validatePhone(phone)
        val dobResult = validateDOB(dob)
        val incomeResult = validateIncome(income)
        val employmentResult = validateEmployment(employment)

        return nameResult.isValid && phoneResult.isValid &&
                dobResult.isValid && incomeResult.isValid && employmentResult.isValid
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)