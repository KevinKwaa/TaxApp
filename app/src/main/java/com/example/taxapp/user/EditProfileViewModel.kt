package com.example.taxapp.user

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.get
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlin.io.path.exists

class EditProfileViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    var email by mutableStateOf("")
    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var dob by mutableStateOf("")
    var income by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        getUserData()
    }

    fun getUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            errorMessage = "User not logged in"
            return
        }

        isLoading = true
        errorMessage = null // Clear any previous errors

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Parse user data from document
                    email = document.getString("email") ?: ""
                    name = document.getString("name") ?: ""
                    phone = document.getString("phone") ?: ""
                    dob = document.getString("dob") ?: ""
                    income = document.getString("income") ?: ""
                } else {
                    // Document doesn't exist yet
                    errorMessage = "Profile not found. Please save your details."
                    // Keep default empty values for fields
                }
            }
            .addOnFailureListener { exception ->
                errorMessage = "Failed to load profile: ${exception.localizedMessage}"
                Log.e("EditProfileViewModel", "Error getting user data", exception)
            }
            .addOnCompleteListener {
                isLoading = false
            }
    }

    fun updateUserProfile(onResult:(Boolean, String?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDetails = mapOf(
                "email" to email,
                "name" to name,
                "phone" to phone,
                "dob" to dob,
                "income" to income
            )

            isLoading = true
            firestore.collection("users").document(userId)
                .set(userDetails) // Use `.set()` instead of `.update()` to create/update
                .addOnSuccessListener {
                    onResult(true, null)
                }
                .addOnFailureListener { exception ->
                    onResult(false, exception.localizedMessage)
                    Log.e("EditProfileViewModel", "Error updating user profile", exception)
                }
                .addOnCompleteListener {
                    isLoading = false
                }
        } else {
            onResult(false, "User not logged in")
        }
    }

}