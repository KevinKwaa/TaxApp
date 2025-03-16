package com.example.taxapp.taxplan

import android.util.Log
import com.example.taxapp.user.FirebaseManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TaxPlanRepository {
    private val TAG = "TaxPlanRepository"
    private val firestore: FirebaseFirestore = FirebaseManager.getAuthFirestore()
    private val COLLECTION_NAME = "tax_plans"

    /**
     * Create a new tax plan
     */
    suspend fun createTaxPlan(taxPlan: TaxPlan): Result<String> {
        return try {
            val currentUserId = FirebaseManager.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Add user ID to the tax plan
            val taxPlanWithUserId = taxPlan.copy(userId = currentUserId)

            // Save to Firestore
            val documentRef = firestore.collection(COLLECTION_NAME).document(taxPlan.id)
            documentRef.set(taxPlanWithUserId).await()

            Log.d(TAG, "Tax plan created with ID: ${taxPlan.id}")
            Result.success(taxPlan.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating tax plan", e)
            Result.failure(e)
        }
    }

    /**
     * Get all tax plans for the current user
     */
    suspend fun getUserTaxPlans(): Result<List<TaxPlan>> {
        return try {
            val currentUserId = FirebaseManager.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Query Firestore for user's tax plans
            val snapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", currentUserId)
                .get()
                .await()

            val taxPlans = snapshot.documents.mapNotNull { document ->
                document.toObject(TaxPlan::class.java)
            }

            Log.d(TAG, "Retrieved ${taxPlans.size} tax plans for user: $currentUserId")
            Result.success(taxPlans)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tax plans", e)
            Result.failure(e)
        }
    }

    /**
     * Get a specific tax plan by ID
     */
    suspend fun getTaxPlanById(planId: String): Result<TaxPlan?> {
        return try {
            val currentUserId = FirebaseManager.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Get the document
            val document = firestore.collection(COLLECTION_NAME)
                .document(planId)
                .get()
                .await()

            val taxPlan = document.toObject(TaxPlan::class.java)

            // Verify ownership
            if (taxPlan != null && taxPlan.userId != currentUserId) {
                return Result.failure(Exception("Unauthorized access to tax plan"))
            }

            Result.success(taxPlan)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving tax plan: $planId", e)
            Result.failure(e)
        }
    }

    /**
     * Update a tax plan
     */
    suspend fun updateTaxPlan(taxPlan: TaxPlan): Result<Unit> {
        return try {
            val currentUserId = FirebaseManager.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Verify ownership
            if (taxPlan.userId != currentUserId) {
                return Result.failure(Exception("Unauthorized access to tax plan"))
            }

            // Update in Firestore
            firestore.collection(COLLECTION_NAME)
                .document(taxPlan.id)
                .set(taxPlan)
                .await()

            Log.d(TAG, "Tax plan updated: ${taxPlan.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tax plan", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a tax plan
     */
    suspend fun deleteTaxPlan(planId: String): Result<Unit> {
        return try {
            val currentUserId = FirebaseManager.getCurrentUserId()
                ?: return Result.failure(Exception("User not authenticated"))

            // Get the plan to verify ownership
            val planResult = getTaxPlanById(planId)
            if (planResult.isFailure) {
                return Result.failure(planResult.exceptionOrNull() ?: Exception("Tax plan not found"))
            }

            val plan = planResult.getOrNull()
            if (plan == null) {
                return Result.failure(Exception("Tax plan not found"))
            }

            // Verify ownership
            if (plan.userId != currentUserId) {
                return Result.failure(Exception("Unauthorized access to tax plan"))
            }

            // Delete from Firestore
            firestore.collection(COLLECTION_NAME)
                .document(planId)
                .delete()
                .await()

            Log.d(TAG, "Tax plan deleted: $planId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting tax plan", e)
            Result.failure(e)
        }
    }
}