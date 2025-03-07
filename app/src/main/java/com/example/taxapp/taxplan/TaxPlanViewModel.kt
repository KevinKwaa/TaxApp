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
import com.example.taxapp.firebase.FirebaseManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaxPlanViewModel : ViewModel() {
    private val TAG = "TaxPlanViewModel"
    private val repository = TaxPlanRepository()

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

    // Create plan dialog
    var showCreatePlanDialog by mutableStateOf(false)

    // If viewing a specific plan
    var isViewingPlan by mutableStateOf(false)
    var currentPlan by mutableStateOf<TaxPlan?>(null)

    init {
        loadTaxPlans()
    }

    /**
     * Show the create plan dialog
     */
    fun showCreatePlanDialog() {
        showCreatePlanDialog = true
    }

    /**
     * Hide the create plan dialog
     */
    fun hideCreatePlanDialog() {
        showCreatePlanDialog = false
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
     * Generate a new tax plan with custom options
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun generateTaxPlan(
        context: Context,
        planName: String,
        planType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Get user ID
                val userId = FirebaseManager.getCurrentUserId()
                    ?: throw Exception("Please sign in to generate a tax plan")

                // Get user profile data from Firestore
                val userDoc = Firebase.firestore.collection("users").document(userId).get().await()

                if (!userDoc.exists()) {
                    throw Exception("Please complete your profile with income information first")
                }

                // Extract user information
                val name = userDoc.getString("name") ?: ""
                val income = userDoc.getString("income") ?: "0"
                val employment = userDoc.getString("employment") ?: "employee"

                if (income.toDoubleOrNull() ?: 0.0 <= 0) {
                    throw Exception("Please update your profile with valid income information")
                }

                // Adjust income based on plan type
                val adjustedIncome = when (planType) {
                    "future" -> (income.toDoubleOrNull() ?: 0.0) * 1.2 // 20% higher
                    else -> income.toDoubleOrNull() ?: 0.0
                }

                // Generate plan name with date if not provided
                val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val finalPlanName = if (planName.isBlank()) {
                    when (planType) {
                        "future" -> "Future Income Plan ${dateFormat.format(Date())}"
                        "business" -> "Business Plan ${dateFormat.format(Date())}"
                        else -> "Tax Plan ${dateFormat.format(Date())}"
                    }
                } else {
                    planName
                }

                // Create description based on plan type
                val planDescription = when (planType) {
                    "future" -> "AI-generated tax plan based on projected future income"
                    "business" -> "AI-generated tax plan for business ventures"
                    else -> "AI-generated tax plan based on your current income"
                }

                // Create tax suggestions based on plan type and income
                val suggestions = generateTaxSuggestions(adjustedIncome, employment, planType)

                // Calculate total potential savings
                val totalSavings = suggestions.sumOf { it.potentialSaving }

                // Create the tax plan
                val taxPlan = TaxPlan(
                    name = finalPlanName,
                    description = planDescription,
                    suggestions = suggestions,
                    potentialSavings = totalSavings,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    planType = planType
                )

                // Save to repository
                val saveResult = repository.createTaxPlan(taxPlan)
                if (saveResult.isFailure) {
                    throw saveResult.exceptionOrNull() ?: Exception("Failed to save tax plan")
                }

                // Reload plans
                loadTaxPlans()

                // Hide the create dialog
                hideCreatePlanDialog()

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
     * Generate tax suggestions based on income, employment type and plan type
     */
    private fun generateTaxSuggestions(income: Double, employmentType: String, planType: String): List<TaxPlanSuggestion> {
        val suggestions = mutableListOf<TaxPlanSuggestion>()

        // Calculate estimated tax rate based on income
        val estimatedTaxRate = when {
            income < 35000 -> 0.0
            income < 50000 -> 0.08
            income < 70000 -> 0.13
            income < 100000 -> 0.21
            else -> 0.24
        }

        // Add common suggestions for all plan types
        suggestions.add(TaxPlanSuggestion(
            category = "Lifestyle",
            suggestion = "Maximize your RM2,500 lifestyle relief by keeping receipts for books, electronics, sports equipment, and internet subscriptions.",
            potentialSaving = 2500 * estimatedTaxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "Medical",
            suggestion = "Track medical expenses for yourself and dependents for relief up to RM8,000.",
            potentialSaving = 5000 * estimatedTaxRate
        ))

        // Add plan-type specific suggestions
        when (planType) {
            "future" -> {
                // Future income plan - focus on tax brackets and investment strategies
                suggestions.add(TaxPlanSuggestion(
                    category = "Tax Bracket Planning",
                    suggestion = "With your projected income, consider maximizing deductible retirement contributions to manage your tax bracket.",
                    potentialSaving = income * 0.03 // Higher potential savings for future planning
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Investment",
                    suggestion = "Consider tax-efficient investment vehicles like unit trusts with tax incentives.",
                    potentialSaving = income * 0.02
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Education",
                    suggestion = "Invest in skill development courses with tax relief up to RM7,000 to increase future earning potential.",
                    potentialSaving = 7000 * estimatedTaxRate
                ))
            }
            "business" -> {
                // Business venture plan - focus on business deductions
                suggestions.add(TaxPlanSuggestion(
                    category = "Business Expenses",
                    suggestion = "Track and document all business-related expenses including office supplies, utilities, and professional services.",
                    potentialSaving = income * 0.15 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Home Office",
                    suggestion = "If you work from home, allocate a portion of rent, utilities and internet as business expenses.",
                    potentialSaving = 2400 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Business Tools",
                    suggestion = "Equipment and software purchased for business use may be eligible for capital allowances.",
                    potentialSaving = 3000 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Business Registration",
                    suggestion = "Consider formal business registration to access additional tax benefits and deductions.",
                    potentialSaving = income * 0.05 * estimatedTaxRate
                ))
            }
            else -> {
                // Standard plan - focus on common deductions
                suggestions.add(TaxPlanSuggestion(
                    category = "EPF",
                    suggestion = "Maximize your EPF contribution (11% for employees, voluntary for self-employed) for tax relief up to RM4,000.",
                    potentialSaving = 4000 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Education",
                    suggestion = "Claim education relief of up to RM7,000 for skills development courses or further education.",
                    potentialSaving = 7000 * estimatedTaxRate
                ))

                if (employmentType == "self-employed") {
                    suggestions.add(TaxPlanSuggestion(
                        category = "Business Expenses",
                        suggestion = "Keep records of all business-related expenses for deduction from business income.",
                        potentialSaving = income * 0.1 * estimatedTaxRate
                    ))
                } else {
                    suggestions.add(TaxPlanSuggestion(
                        category = "SSPN",
                        suggestion = "Consider SSPN savings for children's education with tax relief up to RM8,000.",
                        potentialSaving = 3000 * estimatedTaxRate
                    ))
                }
            }
        }

        // Add common suggestion for all plans
        suggestions.add(TaxPlanSuggestion(
            category = "Donations",
            suggestion = "Make donations to approved organizations for tax deductions.",
            potentialSaving = 1000 * estimatedTaxRate
        ))

        return suggestions
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

    /**
     * Get an emoji icon for a plan type
     */
    fun getPlanTypeIcon(plan: TaxPlan): String {
        return when (plan.planType) {
            "future" -> "📈" // Chart increasing
            "business" -> "💼" // Briefcase
            else -> "📋" // Default clipboard
        }
    }

    /**
     * Get a description for a plan type
     */
    fun getPlanTypeDescription(plan: TaxPlan): String {
        return when (plan.planType) {
            "future" -> "Future Income Plan"
            "business" -> "Business Plan"
            else -> "Standard Plan"
        }
    }
}