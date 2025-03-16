package com.example.taxapp.user

import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.CalendarEvent.EventRepository
import com.example.taxapp.CalendarEvent.TaxDeadlineHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {
    private val TAG = "EditProfileViewModel"

    // Use default Firebase instances
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    // Auth state listener to detect user changes
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    // Add this new variable for radio option
    var employment by mutableStateOf("employee")
    private var originalEmployment = "employee" // Track the original value

    var email by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var dob by mutableStateOf("")
    var income by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Add a flag to indicate if tax events need refresh
    private val _needsEventsRefresh = MutableStateFlow(false)

    // State for tracking tax event updates
    private val _taxEventUpdateState = MutableStateFlow<TaxEventUpdateState>(TaxEventUpdateState.NotStarted)

    // Enum to track tax event update state
    enum class TaxEventUpdateState {
        NotStarted,
        InProgress,
        Success,
        Failed
    }

    init {
        Log.d(TAG, "Initializing EditProfileViewModel")
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        // Remove any existing listener
        authStateListener?.let { auth.removeAuthStateListener(it) }

        // Create new listener
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            Log.d(TAG, "Auth state changed, current user: ${firebaseAuth.currentUser?.uid}")

            // Clear previous data when user changes
            if (firebaseAuth.currentUser?.uid != null) {
                // Give Firebase a moment to fully process the auth state change
                viewModelScope.launch {
                    delay(500) // Short delay to ensure auth state is fully updated
                    getUserData()
                }
            } else {
                // Reset data when logged out
                resetData()
            }
        }

        // Add the listener
        auth.addAuthStateListener(authStateListener!!)

        // Also try to get data immediately if user is already logged in
        if (auth.currentUser != null) {
            viewModelScope.launch {
                delay(500) // Short delay to ensure auth state is fully updated
                getUserData()
            }
        }
    }

    private fun resetData() {
        email = ""
        name = ""
        phone = ""
        dob = ""
        income = ""
        employment = "employee"
        originalEmployment = "employee"
        errorMessage = null
        _taxEventUpdateState.value = TaxEventUpdateState.NotStarted
        _needsEventsRefresh.value = false
    }

    // Public method to force refresh data (call this from UI when needed)

    fun getUserData() {
        val userId = FirebaseManager.getCurrentUserId()
        Log.d(TAG, "Getting user data for ID: $userId")

        if (userId == null) {
            errorMessage = "User not logged in"
            Log.e(TAG, "getUserData failed: User not logged in")
            return
        }

        isLoading = true
        errorMessage = null // Clear any previous errors

        Log.d(TAG, "Fetching user document from FireStore")
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Parse user data from document
                    Log.d(TAG, "User document exists: ${document.data}")
                    email = document.getString("email") ?: ""
                    name = document.getString("name") ?: ""
                    phone = document.getString("phone") ?: ""
                    dob = document.getString("dob") ?: ""
                    income = document.getString("income") ?: ""
                    //password = "••••••••" // Always show masked in UI
                    //actualPassword = document.getString("password") ?: "" // Store actual value

                    // Get tax filing preference with default value "employee"
                    val employmentFromDoc = document.getString("employment") ?: "employee"
                    Log.d(TAG, "Loaded employment status from FireStore: $employmentFromDoc")

                    // Store original for comparison
                    originalEmployment = employmentFromDoc

                    // Set current value - UI update
                    employment = employmentFromDoc

                } else {
                    // Document doesn't exist yet
                    Log.w(TAG, "User document does not exist for ID: $userId")
                    errorMessage = "Profile not found. Please save your details."
                    // Keep default empty values for fields
                    email = auth.currentUser?.email ?: ""

                    // Reset employment to default
                    employment = "employee"
                    originalEmployment = "employee"
                }
            }
            .addOnFailureListener { exception ->
                errorMessage = "Failed to load profile: ${exception.localizedMessage}"
                Log.e(TAG, "Error getting user data", exception)
            }
            .addOnCompleteListener {
                isLoading = false
                Log.d(TAG, "getUserData completed")
            }
    }

    fun updateUserProfile(onResult: (Boolean, String?) -> Unit) {
        // First validate all the fields
        val nameValidation = ValidationUtil.validateName(name)
        if (!nameValidation.isValid) {
            errorMessage = nameValidation.errorMessage
            onResult(false, nameValidation.errorMessage)
            return
        }

        val phoneValidation = ValidationUtil.validatePhone(phone)
        if (!phoneValidation.isValid) {
            errorMessage = phoneValidation.errorMessage
            onResult(false, phoneValidation.errorMessage)
            return
        }

        val dobValidation = ValidationUtil.validateDOB(dob)
        if (!dobValidation.isValid) {
            errorMessage = dobValidation.errorMessage
            onResult(false, dobValidation.errorMessage)
            return
        }

        val incomeValidation = ValidationUtil.validateIncome(income)
        if (!incomeValidation.isValid) {
            errorMessage = incomeValidation.errorMessage
            onResult(false, incomeValidation.errorMessage)
            return
        }

        // If we reach here, all validations have passed
        val userId = FirebaseManager.getCurrentUserId()
        Log.d(TAG, "Updating profile for user ID: $userId")

        if (userId != null) {
            val userDetails = mapOf(
                "email" to email,
                "name" to name,
                "phone" to phone,
                "dob" to dob,
                "income" to income,
                "employment" to employment, // This is the important field for tax deadlines
                //"password" to actualPassword // Keep the stored password
            )

            isLoading = true
            errorMessage = null // Clear any previous errors
            Log.d(TAG, "Saving user details: $userDetails")

            // Check if employment changed
            val employmentChanged = originalEmployment != employment
            Log.d(TAG, "Employment changed: $employmentChanged (from $originalEmployment to $employment)")

            // TRANSACTION APPROACH: First update user profile, then handle tax events
            firestore.collection("users").document(userId)
                .set(userDetails) // Using set instead of update to handle new profiles too
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated successfully")

                    // Store new employment as original immediately to prevent duplicate updates
                    val prevEmployment = originalEmployment
                    originalEmployment = employment

                    // Set flag for UI to know events need refresh
                    _needsEventsRefresh.value = employmentChanged

                    // Update tax deadline events if employment changed and on a supported version
                    if (employmentChanged && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        _taxEventUpdateState.value = TaxEventUpdateState.InProgress

                        viewModelScope.launch {
                            try {
                                Log.d(TAG, "Employment changed from $prevEmployment to $employment - updating tax deadline events")

                                // Wait a moment for FireStore to update profile
                                delay(500)

                                // IMPORTANT: First reset the repository to clear out any cached events
                                EventRepository.resetInstance()

                                // Wait a bit for cleanup
                                delay(300)

                                // Now update the tax deadlines using the helper
                                TaxDeadlineHelper.updateTaxDeadlineEvents(
                                    employment,  // Use the NEW employment type
                                    viewModelScope
                                ) { success ->
                                    _taxEventUpdateState.value = if (success) {
                                        TaxEventUpdateState.Success
                                    } else {
                                        TaxEventUpdateState.Failed
                                    }

                                    Log.d(TAG, "Tax event update completed: $success")

                                    // Allow time for FireStore operations to complete
                                    viewModelScope.launch {
                                        // Force reset repository to ensure fresh data on next calendar view
                                        EventRepository.resetInstance()
                                        delay(300)

                                        // Get a fresh instance to refresh data
                                        val repo = EventRepository.getInstance()
                                        repo.forceRefresh()

                                        // Give FireStore time to catch up
                                        delay(500)

                                        // Signal completion only after everything is done
                                        isLoading = false
                                        onResult(true, if (success) null else "Profile saved but tax events may not be updated")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating tax events", e)
                                _taxEventUpdateState.value = TaxEventUpdateState.Failed

                                // Force refresh anyway to ensure UI updates
                                EventRepository.resetInstance()
                                delay(300)
                                EventRepository.getInstance().forceRefresh()

                                isLoading = false
                                onResult(true, "Profile saved but tax events may not be updated")
                            }
                        }
                    } else {
                        // No employment change, just return success
                        isLoading = false
                        onResult(true, null)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error updating user profile", exception)
                    _taxEventUpdateState.value = TaxEventUpdateState.Failed
                    isLoading = false
                    errorMessage = exception.localizedMessage
                    onResult(false, exception.localizedMessage)
                }
        } else {
            Log.e(TAG, "updateUserProfile failed: User not logged in")
            _taxEventUpdateState.value = TaxEventUpdateState.Failed
            errorMessage = "User not logged in"
            onResult(false, "User not logged in")
        }
    }

    /**
     * Force update tax events based on current employment
     */


    override fun onCleared() {
        super.onCleared()
        // Clean up auth listener when view model is destroyed
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}