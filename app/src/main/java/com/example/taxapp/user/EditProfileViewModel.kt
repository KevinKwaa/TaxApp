package com.example.taxapp.user

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.get
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlin.io.path.exists

class EditProfileViewModel : ViewModel() {
    private val TAG = "EditProfileViewModel"

    // Use FirebaseManager to get the auth and firestore instances
    private val auth = FirebaseManager.getAuthInstance()
    private val firestore = FirebaseManager.getAuthFirestore()

    var email by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var dob by mutableStateOf("")
    var income by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        Log.d(TAG, "Initializing EditProfileViewModel")
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
                "income" to income
            )

            isLoading = true
            Log.d(TAG, "Saving user details: $userDetails")

            // Try to update first, if document doesn't exist, set it
            firestore.collection("users").document(userId)
                .set(userDetails) // Using set instead of update to handle new profiles too
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated successfully")
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
}