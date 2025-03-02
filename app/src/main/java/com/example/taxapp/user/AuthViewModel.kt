package com.example.taxapp.user

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taxapp.CalendarEvent.EventRepository
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val TAG = "AuthViewModel"

    private val auth = FirebaseManager.getAuthInstance()
    private val firestore = FirebaseManager.getAuthFirestore()

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

    fun userProfile(name: String, phone: String, dob: String, income: String, onResult: (Boolean, String?) -> Unit) {
        val userId = FirebaseManager.getCurrentUserId()
        Log.d(TAG, "Updating profile for user ID: $userId")

        if (userId != null) {
            val userDetails = hashMapOf(
                "name" to name,
                "phone" to phone,
                "dob" to dob,
                "income" to income
            )

            firestore.collection("users").document(userId)
                .update(userDetails as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d(TAG, "Profile updated successfully")
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
            onResult(false, "User not logged in")
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