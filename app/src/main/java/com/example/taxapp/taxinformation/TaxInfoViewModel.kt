package com.example.taxapp.taxinformation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaxInfoViewModel : ViewModel() {

    private val _userIncome = MutableStateFlow(0.0)
    val userIncome: StateFlow<Double> = _userIncome.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadUserIncome() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val userId = FirebaseManager.getCurrentUserId()
                if (userId != null) {
                    val firestore = FirebaseManager.getAuthFirestore()
                    val userDocRef = firestore.collection("users").document(userId)

                    userDocRef.get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                // Get income as string and convert to double
                                val incomeStr = document.getString("income") ?: "0"
                                val income = try {
                                    incomeStr.toDouble()
                                } catch (e: NumberFormatException) {
                                    0.0
                                }

                                // If income is provided as a monthly figure, annualize it
                                // (Assuming monthly income for simplicity)
                                _userIncome.value = income * 12

                                Log.d("TaxInfoViewModel", "Loaded user income: ${_userIncome.value}")
                            } else {
                                Log.d("TaxInfoViewModel", "No user document found")
                                _userIncome.value = 0.0
                            }
                            _isLoading.value = false
                        }
                        .addOnFailureListener { e ->
                            Log.e("TaxInfoViewModel", "Error loading user data", e)
                            _userIncome.value = 0.0
                            _isLoading.value = false
                        }
                } else {
                    Log.d("TaxInfoViewModel", "No user logged in")
                    _userIncome.value = 0.0
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("TaxInfoViewModel", "Error in loadUserIncome", e)
                _userIncome.value = 0.0
                _isLoading.value = false
            }
        }
    }
}