package com.example.taxapp.user

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.CalendarEvent.TaxDeadlineHelper
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
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

    var email by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var dob by mutableStateOf("")
    var income by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

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
        errorMessage = null
    }

    // Public method to force refresh data (call this from UI when needed)
    fun refreshData() {
        getUserData()
    }

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

        Log.d(TAG, "Fetching user document from Firestore")
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

                    // Get tax filing preference with default value "self"
                    employment = document.getString("employment") ?: "employee"

                } else {
                    // Document doesn't exist yet
                    Log.w(TAG, "User document does not exist for ID: $userId")
                    errorMessage = "Profile not found. Please save your details."
                    // Keep default empty values for fields
                    email = auth.currentUser?.email ?: ""
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
        val userId = FirebaseManager.getCurrentUserId()
        Log.d(TAG, "Updating profile for user ID: $userId")

        if (userId != null) {
            val userDetails = mapOf(
                "email" to email,
                "name" to name,
                "phone" to phone,
                "dob" to dob,
                "income" to income,
                "employment" to employment // Add this field to the map
            )

            isLoading = true
            Log.d(TAG, "Saving user details: $userDetails")

            // Try to update first, if document doesn't exist, set it
            firestore.collection("users").document(userId)
                .set(userDetails) // Using set instead of update to handle new profiles too
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated successfully")

                    // Create or update tax deadline events based on preference
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        //val context = getApplication<Application>().applicationContext
                        TaxDeadlineHelper.updateTaxDeadlineEvents(
                            employment,
                            viewModelScope
                        )
                    }

                    onResult(true, null)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error updating user profile", exception)
                    onResult(false, exception.localizedMessage)
                }
                .addOnCompleteListener {
                    isLoading = false
                }
        } else {
            Log.e(TAG, "updateUserProfile failed: User not logged in")
            onResult(false, "User not logged in")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up auth listener when view model is destroyed
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}