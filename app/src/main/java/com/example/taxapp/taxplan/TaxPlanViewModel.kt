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
import com.example.taxapp.user.FirebaseManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaxPlanViewModel : ViewModel() {
    private val TAG = "TaxPlanViewModel"
    private val repository = TaxPlanRepository()

    // UI State
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var taxPlans by mutableStateOf<List<TaxPlan>>(emptyList())

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
                    val loadedPlans = result.getOrNull() ?: emptyList()

                    // Validate each plan to ensure no zeros
                    val validatedPlans = loadedPlans.map { plan ->
                        validatePlan(plan)
                    }

                    taxPlans = validatedPlans
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
                        // Validate the plan before viewing
                        currentPlan = validatePlan(plan)
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
     * Validate a tax plan to ensure it has proper values
     */
    private fun validatePlan(plan: TaxPlan): TaxPlan {
        // Check for zero potential savings
        if (plan.potentialSavings <= 0) {
            Log.w(TAG, "Plan has zero potential savings, recalculating: ${plan.id}")

            // Calculate proper savings from suggestions
            val suggestions = plan.suggestions.map { suggestion ->
                // If suggestion has zero saving, estimate a reasonable amount
                if (suggestion.potentialSaving <= 0) {
                    val estimatedSaving = estimateSavingForCategory(suggestion.category)
                    suggestion.copy(potentialSaving = estimatedSaving)
                } else {
                    suggestion
                }
            }

            // Calculate new total savings
            val newTotalSavings = suggestions.sumOf { it.potentialSaving }

            // If still zero or no suggestions, generate default suggestions
            if (newTotalSavings <= 0 || suggestions.isEmpty()) {
                Log.w(TAG, "Creating default suggestions for plan: ${plan.id}")
                val defaultSuggestions = createDefaultSuggestions()
                val defaultSavings = defaultSuggestions.sumOf { it.potentialSaving }

                return plan.copy(
                    suggestions = defaultSuggestions,
                    potentialSavings = defaultSavings
                )
            }

            return plan.copy(
                suggestions = suggestions,
                potentialSavings = newTotalSavings
            )
        }

        // Check for zero savings in suggestions
        val hasZeroSuggestions = plan.suggestions.any { it.potentialSaving <= 0 }
        if (hasZeroSuggestions) {
            Log.w(TAG, "Plan has suggestions with zero savings, fixing: ${plan.id}")

            val fixedSuggestions = plan.suggestions.map { suggestion ->
                if (suggestion.potentialSaving <= 0) {
                    val estimatedSaving = estimateSavingForCategory(suggestion.category)
                    suggestion.copy(potentialSaving = estimatedSaving)
                } else {
                    suggestion
                }
            }

            // Recalculate total savings
            val fixedTotalSavings = fixedSuggestions.sumOf { it.potentialSaving }

            return plan.copy(
                suggestions = fixedSuggestions,
                potentialSavings = fixedTotalSavings
            )
        }

        return plan
    }

    /**
     * Estimate reasonable saving amount for a category
     */
    private fun estimateSavingForCategory(category: String): Double {
        // Default values based on typical Malaysian tax relief categories
        return when (category.lowercase()) {
            "lifestyle" -> 200.0
            "medical" -> 640.0
            "education" -> 560.0
            "epf" -> 480.0
            "sspn" -> 240.0
            "donation" -> 80.0
            "insurance" -> 240.0
            "business expenses" -> 1200.0
            "home office" -> 192.0
            else -> 150.0 // Default value for unknown categories
        }
    }

    /**
     * Create default suggestions when none available
     */
    private fun createDefaultSuggestions(): List<TaxPlanSuggestion> {
        return listOf(
            TaxPlanSuggestion(
                category = "Lifestyle",
                suggestion = "Maximize your RM2,500 lifestyle relief by keeping receipts for books, electronics, sports equipment, and internet subscriptions.",
                potentialSaving = 200.0
            ),
            TaxPlanSuggestion(
                category = "Medical",
                suggestion = "Track medical expenses for yourself and dependents for relief up to RM8,000.",
                potentialSaving = 640.0
            ),
            TaxPlanSuggestion(
                category = "EPF",
                suggestion = "Maximize your EPF contribution (11% for employees, voluntary for self-employed) for tax relief up to RM4,000.",
                potentialSaving = 480.0
            ),
            TaxPlanSuggestion(
                category = "Education",
                suggestion = "Claim education relief of up to RM7,000 for skills development courses or further education.",
                potentialSaving = 560.0
            ),
            TaxPlanSuggestion(
                category = "Donation",
                suggestion = "Make donations to approved organizations for tax deductions.",
                potentialSaving = 80.0
            )
        )
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

                // Don't stop the process for income validation, use a default
                val incomeValue = income.toDoubleOrNull() ?: 50000.0

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

                // Create the tax plan - two approaches: try AI first, then fallback

                // Create GeminiAIService for AI generation
                val geminiService = GeminiTaxPlanService(context)

                try {
                    // Try to generate with AI first
                    Log.d(TAG, "Attempting AI tax plan generation")
                    val aiResult = geminiService.generateTaxPlan(
                        income.toString(),
                        employment,
                        name,
                        planType
                    )

                    if (aiResult.isSuccess) {
                        Log.d(TAG, "AI successfully generated tax plan")
                        val aiTaxPlan = aiResult.getOrNull()

                        if (aiTaxPlan != null) {
                            // Validate the AI-generated plan
                            val validatedPlan = validateAIGeneratedPlan(aiTaxPlan, finalPlanName, planType)

                            // Save to repository
                            val saveResult = repository.createTaxPlan(validatedPlan)
                            if (saveResult.isFailure) {
                                throw saveResult.exceptionOrNull() ?: Exception("Failed to save tax plan")
                            }

                            // Reload plans
                            loadTaxPlans()

                            // Hide the create dialog
                            hideCreatePlanDialog()

                            onSuccess()
                            return@launch
                        }
                    }

                    // If we reached here, AI generation failed
                    Log.w(TAG, "AI tax plan generation failed, using fallback generation")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in AI tax plan generation, using fallback", e)
                }

                // Fallback: Generate tax suggestions manually
                Log.d(TAG, "Using fallback tax plan generation")
                val suggestions = generateTaxSuggestions(incomeValue, employment, planType)

                // Calculate total savings
                val totalSavings = suggestions.sumOf { it.potentialSaving }

                // Create description based on plan type
                val planDescription = when (planType) {
                    "future" -> "Tax plan optimized for future income growth and long-term tax efficiency"
                    "business" -> "Tax plan designed for business ventures with focus on business deductions"
                    else -> "Personalized tax plan based on your current income and employment status"
                }

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

                // Final validation
                val validatedPlan = validatePlan(taxPlan)

                // Save to repository
                val saveResult = repository.createTaxPlan(validatedPlan)
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
     * Ensure an AI-generated plan meets our requirements
     */
    private fun validateAIGeneratedPlan(plan: TaxPlan, name: String, planType: String): TaxPlan {
        // Check for issues that need fixing
        val needsFixing = plan.potentialSavings <= 0 ||
                plan.suggestions.isEmpty() ||
                plan.suggestions.any { it.potentialSaving <= 0 }

        if (!needsFixing) {
            // Just update name if needed
            return if (plan.name != name) {
                plan.copy(name = name)
            } else {
                plan
            }
        }

        Log.d(TAG, "Fixing AI-generated plan with issues")

        // Fix suggestions with zero savings
        val fixedSuggestions = if (plan.suggestions.isEmpty()) {
            // Create new suggestions if none exist
            generateTaxSuggestions(50000.0, "employee", planType)
        } else {
            // Fix existing suggestions
            plan.suggestions.map { suggestion ->
                if (suggestion.potentialSaving <= 0) {
                    val estimatedSaving = estimateSavingForCategory(suggestion.category)
                    suggestion.copy(potentialSaving = estimatedSaving)
                } else {
                    suggestion
                }
            }
        }

        // Recalculate total savings
        val fixedTotalSavings = fixedSuggestions.sumOf { it.potentialSaving }

        // Create description based on plan type if needed
        val planDescription = if (plan.description.isBlank()) {
            when (planType) {
                "future" -> "Tax plan optimized for future income growth and long-term tax efficiency"
                "business" -> "Tax plan designed for business ventures with focus on business deductions"
                else -> "Personalized tax plan based on your current income and employment status"
            }
        } else {
            plan.description
        }

        return plan.copy(
            name = name,
            description = planDescription,
            suggestions = fixedSuggestions,
            potentialSavings = fixedTotalSavings
        )
    }

    /**
     * Generate tax suggestions based on income, employment type and plan type
     */
    private fun generateTaxSuggestions(income: Double, employmentType: String, planType: String): List<TaxPlanSuggestion> {
        val suggestions = mutableListOf<TaxPlanSuggestion>()

        // Calculate estimated tax rate based on income
        val estimatedTaxRate = when {
            income < 5000 -> 0.0
            income < 20000 -> 0.01
            income < 35000 -> 0.03
            income < 50000 -> 0.08
            income < 70000 -> 0.13
            income < 100000 -> 0.21
            income < 250000 -> 0.24
            income < 400000 -> 0.245
            income < 600000 -> 0.25
            income < 1000000 -> 0.26
            else -> 0.30
        }

        Log.d(TAG, "Generating suggestions with income: $income, rate: $estimatedTaxRate, type: $planType")

        // Add common suggestions for all plan types
        suggestions.add(TaxPlanSuggestion(
            category = "Lifestyle",
            suggestion = "Maximize your RM2,500 lifestyle relief by keeping receipts for books, electronics, sports equipment, and internet subscriptions.",
            potentialSaving = 2500 * estimatedTaxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "Medical",
            suggestion = "Track medical expenses for yourself and dependents for relief up to RM8,000.",
            potentialSaving = 8000 * estimatedTaxRate
        ))

        // Add EPF suggestion
        val epfAmount = minOf(income * 0.11, 4000.0)
        suggestions.add(TaxPlanSuggestion(
            category = "EPF",
            suggestion = if (employmentType == "employee") {
                "Ensure you're maximizing your mandatory EPF contribution of 11% for tax relief up to RM4,000."
            } else {
                "Make voluntary EPF contributions up to RM4,000 annually for tax relief."
            },
            potentialSaving = epfAmount * estimatedTaxRate
        ))

        // Add plan-type specific suggestions
        when (planType) {
            "future" -> {
                // Future income plan - focus on tax brackets and investment strategies
                suggestions.add(TaxPlanSuggestion(
                    category = "Investment",
                    suggestion = "Consider tax-efficient investment vehicles like unit trusts with tax incentives.",
                    potentialSaving = income * 0.02 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Education",
                    suggestion = "Invest in skill development courses with tax relief up to RM7,000 to increase future earning potential.",
                    potentialSaving = 7000 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Retirement Planning",
                    suggestion = "Supplement your EPF with Private Retirement Scheme (PRS) contributions for additional tax relief up to RM3,000.",
                    potentialSaving = 3000 * estimatedTaxRate
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
                    category = "Capital Investment",
                    suggestion = "Plan capital investments to take advantage of capital allowances and incentives for business expansion.",
                    potentialSaving = income * 0.04 * estimatedTaxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Business Structure",
                    suggestion = "Review your business structure (sole proprietorship vs. LLC) to optimize tax treatment based on your projected growth.",
                    potentialSaving = income * 0.035 * estimatedTaxRate
                ))
            }
            else -> {
                // Standard plan - focus on common deductions
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
            category = "Donation",
            suggestion = "Make donations to approved organizations for tax deductions.",
            potentialSaving = 1000 * estimatedTaxRate
        ))

        // Safety check: ensure no zero savings
        val finalSuggestions = suggestions.map { suggestion ->
            if (suggestion.potentialSaving <= 0) {
                // Minimum 50 RM per suggestion if calculation resulted in zero
                suggestion.copy(potentialSaving = 50.0)
            } else {
                suggestion
            }
        }

        Log.d(TAG, "Generated ${finalSuggestions.size} suggestions with total: ${finalSuggestions.sumOf { it.potentialSaving }}")

        return finalSuggestions
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
        // Ensure amount is never negative or zero
        val positiveAmount = if (amount <= 0) 0.01 else amount
        return String.format("RM %.2f", positiveAmount)
    }
}