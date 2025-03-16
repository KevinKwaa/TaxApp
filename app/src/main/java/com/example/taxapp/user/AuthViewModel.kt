package com.example.taxapp.user

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.CalendarEvent.EventRepository
import com.example.taxapp.CalendarEvent.TaxDeadlineHelper
import com.example.taxapp.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    //var context = LocalContext.current

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Attempting login for email: $email")

        // First, ensure we're starting fresh
        EventRepository.resetInstance()

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Login successful for email: $email")

                // Force refresh the user ID in FirebaseManager
                FirebaseManager.refreshCurrentUser()

                onResult(true, null)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Login failed for email: $email", exception)
                onResult(false, exception.localizedMessage)
            }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        Log.d(TAG, "Attempting registration for email: $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                Log.d(TAG, "Registration successful, creating user document for ID: $userId")

                // Force refresh the user ID in FirebaseManager
                FirebaseManager.refreshCurrentUser()

                if (userId != null) {
                    // Store the password in the user model for later display in profile
                    val userModel = UserModel(email, userId)
                    firestore.collection("users").document(userId)
                        .set(userModel)
                        .addOnSuccessListener {
                            Log.d(TAG, "User document created successfully")
                            onResult(true, null)
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to create user document", exception)
                            onResult(false, "Registration successful but failed to create profile: ${exception.localizedMessage}")
                        }
                } else {
                    Log.e(TAG, "Registration successful but user ID is null")
                    onResult(false, "Registration successful but failed to get user ID")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Registration failed", exception)
                onResult(false, exception.localizedMessage)
            }
    }

    fun userProfile(name: String, phone: String, dob: String, income: String,
                    employment: String = "employee", context: Context,
                    onResult: (Boolean, String?) -> Unit) {

        // First perform validation before proceeding with saving
        val nameValidation = ValidationUtil.validateName(name, context)
        if (!nameValidation.isValid) {
            onResult(false, nameValidation.errorMessage)
            return
        }

        val phoneValidation = ValidationUtil.validatePhone(phone, context)
        if (!phoneValidation.isValid) {
            onResult(false, phoneValidation.errorMessage)
            return
        }

        val dobValidation = ValidationUtil.validateDOB(dob, context)
        if (!dobValidation.isValid) {
            onResult(false, dobValidation.errorMessage)
            return
        }

        val incomeValidation = ValidationUtil.validateIncome(income, context)
        if (!incomeValidation.isValid) {
            onResult(false, incomeValidation.errorMessage)
            return
        }

        // If we reach here, all validations have passed
        val userId = FirebaseManager.getCurrentUserId()
        Log.d(TAG, "Updating profile for user ID: $userId")

        // Rest of method stays the same
        if (userId != null) {
            val userDetails = hashMapOf(
                "name" to name,
                "phone" to phone,
                "dob" to dob,
                "income" to income,
                "employment" to employment // Add this field
            )

            firestore.collection("users").document(userId)
                .update(userDetails as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated successfully")

                    // Create or update tax deadline events based on preference
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // FIX: Launch a coroutine to call the suspend function
                        viewModelScope.launch {
                            try {
                                Log.d(TAG, "Updating tax deadline events from userProfile")
                                TaxDeadlineHelper.updateTaxDeadlineEvents(
                                    employment,
                                    viewModelScope
                                ) { success ->
                                    Log.d(TAG, "Tax event update from userProfile completed: $success")
                                    // We already called onResult, so we don't need to call it again
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating tax events from userProfile", e)
                                // We already called onResult, so we don't need to call it again
                            }
                        }
                    }

                    // Call onResult immediately, don't wait for tax events
                    onResult(true, null)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error updating user profile", exception)

                    // If the error is because the document doesn't exist, try to create it
                    if (exception.message?.contains("NOT_FOUND") == true) {
                        Log.d(TAG, "Document not found, trying to create it")
                        firestore.collection("users").document(userId)
                            .set(userDetails)
                            .addOnSuccessListener {
                                Log.d(TAG, "Created new user profile")

                                // Create or update tax deadline events based on preference
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // FIX: Launch a coroutine to call the suspend function
                                    viewModelScope.launch {
                                        try {
                                            Log.d(TAG, "Updating tax deadline events after new profile creation")
                                            TaxDeadlineHelper.updateTaxDeadlineEvents(
                                                employment,
                                                viewModelScope
                                            ) { success ->
                                                Log.d(TAG, "Tax event update after profile creation completed: $success")
                                                // We already called onResult, so we don't need to call it again
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error updating tax events after profile creation", e)
                                            // We already called onResult, so we don't need to call it again
                                        }
                                    }
                                }

                                // Call onResult immediately, don't wait for tax events
                                onResult(true, null)
                            }
                            .addOnFailureListener { setException ->
                                Log.e(TAG, "Failed to create new user profile", setException)
                                onResult(false, setException.localizedMessage)
                            }
                    } else {
                        onResult(false, exception.localizedMessage)
                    }
                }
        } else {
            Log.e(TAG, "userProfile failed: User not logged in")
            onResult(false, context.getString(R.string.error_user_not_logged_in))
        }
    }

    // Add a logout function to handle cleanup properly
    fun logout(onComplete: () -> Unit) {
        Log.d(TAG, "Performing logout with cleanup")

        // First reset any repositories that depend on user data
        EventRepository.resetInstance()

        // Then sign out from Firebase
        auth.signOut()

        // Force refresh the user ID state
        FirebaseManager.refreshCurrentUser()

        // Callback
        onComplete()
    }
}