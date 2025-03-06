package com.example.taxapp.taxplan

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.receiptcategory.GeminiService
import com.example.taxapp.receiptcategory.ReceiptRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaxPlanViewModel : ViewModel() {
    private val TAG = "TaxPlanViewModel"
    private val repository = TaxPlanRepository()
    private val receiptRepository = ReceiptRepository()
    private lateinit var geminiService: GeminiService

    // UI State
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var taxPlans by mutableStateOf<List<TaxPlan>>(emptyList())

    // Current plan details
    var currentPlanName by mutableStateOf("")
    var currentPlanDescription by mutableStateOf("")
    var currentPlanSuggestions by mutableStateOf<List<TaxPlanSuggestion>>(emptyList())
    var currentPlanSavings by mutableStateOf(0.0)

    // Delete confirmation
    var showDeleteConfirmation by mutableStateOf(false)
    var planToDelete by mutableStateOf<TaxPlan?>(null)

    // If viewing a specific plan
    var isViewingPlan by mutableStateOf(false)
    var currentPlan by mutableStateOf<TaxPlan?>(null)

    init {
        loadTaxPlans()
    }

    /**
     * Load all tax plans for the current user
     */
    fun loadTaxPlans() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = repository.getUserTaxPlans()
                if (result.isSuccess) {
                    taxPlans = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Loaded ${taxPlans.size} tax plans")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to load tax plans")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tax plans", e)
                errorMessage = e.message ?: "Error loading tax plans"
                taxPlans = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * View a specific tax plan
     */
    fun viewTaxPlan(planId: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val result = repository.getTaxPlanById(planId)
                if (result.isSuccess) {
                    val plan = result.getOrNull()
                    if (plan != null) {
                        currentPlan = plan
                        isViewingPlan = true
                        Log.d(TAG, "Viewing tax plan: ${plan.name}")
                    } else {
                        throw Exception("Tax plan not found")
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to load tax plan")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error viewing tax plan", e)
                errorMessage = e.message ?: "Error viewing tax plan"
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Generate a new tax plan using AI
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun generateTaxPlan(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Initialize GeminiService if needed
                if (!::geminiService.isInitialized) {
                    geminiService = GeminiService(context)
                }

                // Get user receipts for analysis
                val receiptsResult = receiptRepository.getUserReceipts()
                if (receiptsResult.isFailure) {
                    throw receiptsResult.exceptionOrNull()
                        ?: Exception("Failed to retrieve receipt data")
                }

                val receipts = receiptsResult.getOrNull() ?: emptyList()
                if (receipts.isEmpty()) {
                    throw Exception("No receipts found. Please add some receipts first to generate a tax plan.")
                }

                // Generate plan name
                val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val planName = "Tax Plan ${dateFormat.format(Date())}"

                // Use Gemini to analyze tax savings and generate suggestions
                val savingsResult = geminiService.analyzeTaxSavings(receipts)
                if (savingsResult.isFailure) {
                    throw savingsResult.exceptionOrNull()
                        ?: Exception("Failed to analyze tax savings")
                }

                val savingsMap = savingsResult.getOrNull() ?: emptyMap()

                // Create suggestions from the analysis
                val suggestions = savingsMap.map { (category, savings) ->
                    TaxPlanSuggestion(
                        category = category,
                        suggestion = generateSuggestionText(category, savings),
                        potentialSaving = savings
                    )
                }

                // Calculate total potential savings
                val totalSavings = savingsMap.values.sum()

                // Create the tax plan
                val taxPlan = TaxPlan(
                    name = planName,
                    description = "AI-generated tax plan based on your receipts",
                    suggestions = suggestions,
                    potentialSavings = totalSavings
                )

                // Save to repository
                val saveResult = repository.createTaxPlan(taxPlan)
                if (saveResult.isFailure) {
                    throw saveResult.exceptionOrNull() ?: Exception("Failed to save tax plan")
                }

                // Reload plans
                loadTaxPlans()

                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error generating tax plan", e)
                errorMessage = e.message ?: "Error generating tax plan"
                onError(errorMessage ?: "Unknown error")
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Generate suggestion text based on category and savings
     */
    private fun generateSuggestionText(category: String, savings: Double): String {
        return when (category) {
            "Lifestyle Expenses" ->
                "Continue tracking lifestyle expenses. You may qualify for relief up to RM2,500."
            "Childcare" ->
                "Keep receipts for childcare expenses. You can claim up to RM3,000 per child."
            "Sport Equipment" ->
                "Purchase sports equipment with proper documentation for tax relief up to RM500."
            "Donations" ->
                "Donations to approved organizations qualify for tax deductions."
            "Medical" ->
                "Medical expenses may qualify for relief up to RM8,000. Keep all receipts."
            "Education" ->
                "Education expenses can qualify for relief up to RM7,000. Maintain records."
            else -> "Track expenses in this category for potential tax benefits."
        }
    }

    /**
     * Delete a tax plan
     */
    fun deleteTaxPlan(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val plan = planToDelete ?: return

        viewModelScope.launch {
            isLoading = true

            try {
                // Delete from repository
                val result = repository.deleteTaxPlan(plan.id)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Failed to delete tax plan")
                }

                // Reset state
                showDeleteConfirmation = false
                planToDelete = null

                // Reload plans
                loadTaxPlans()

                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting tax plan", e)
                errorMessage = e.message ?: "Error deleting tax plan"
                onError(errorMessage ?: "Unknown error")
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Confirm deletion of a tax plan
     */
    fun confirmDeleteTaxPlan(plan: TaxPlan) {
        planToDelete = plan
        showDeleteConfirmation = true
    }

    /**
     * Cancel deletion
     */
    fun cancelDelete() {
        showDeleteConfirmation = false
        planToDelete = null
    }

    /**
     * Close plan view
     */
    fun closePlanView() {
        isViewingPlan = false
        currentPlan = null
    }

    /**
     * Format currency for display
     */
    fun formatCurrency(amount: Double): String {
        return String.format("RM %.2f", amount)
    }
}