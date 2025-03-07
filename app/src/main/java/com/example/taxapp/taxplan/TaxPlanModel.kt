package com.example.taxapp.taxplan

import com.google.firebase.Timestamp
import java.util.UUID

/**
 * Data model for a Tax Plan
 */
data class TaxPlan(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val suggestions: List<TaxPlanSuggestion> = emptyList(),
    val potentialSavings: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val planType: String = "standard" // Add this field to distinguish plan types
)

/**
 * Represents a suggestion within a tax plan
 */
data class TaxPlanSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val category: String = "",
    val suggestion: String = "",
    val potentialSaving: Double = 0.0,
    val isImplemented: Boolean = false
)