package com.example.taxapp.receiptcategory

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReceiptRepository {
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private val auth = Firebase.auth

    // Collection name for receipts
    private val RECEIPTS_COLLECTION = "receipts"

    // Save receipt data to Firestore
    suspend fun saveReceipt(receipt: ReceiptModel): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Create a receipt with the current user ID
            val receiptWithUserId = receipt.copy(userId = currentUser.uid)

            // Add the receipt to Firestore
            val documentRef = if (receipt.id.isEmpty()) {
                firestore.collection(RECEIPTS_COLLECTION).document()
            } else {
                firestore.collection(RECEIPTS_COLLECTION).document(receipt.id)
            }

            // Save the receipt with the document ID
            val receiptToSave = if (receipt.id.isEmpty()) {
                receiptWithUserId.copy(id = documentRef.id)
            } else {
                receiptWithUserId
            }

            documentRef.set(receiptToSave).await()

            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error saving receipt", e)
            Result.failure(e)
        }
    }

    // Upload receipt image to Firebase Storage
    suspend fun uploadReceiptImage(imageUri: Uri): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Create a unique filename with proper path structure
            val filename = "receipts/${currentUser.uid}/${UUID.randomUUID()}.jpg"
            Log.d("ReceiptRepository", "Uploading to path: $filename")

            // Get a reference to the storage location
            val imageRef = storageRef.child(filename)

            // Log the URI to debug
            Log.d("ReceiptRepository", "Image URI: $imageUri")

            // Upload the image - using putFile instead of putBytes
            val uploadTask = imageRef.putFile(imageUri).await()

            // Get download URL only after successful upload
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Log.d("ReceiptRepository", "Upload successful, download URL: $downloadUrl")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error uploading image", e)
            Result.failure(e)
        }
    }

    // Get all receipts for the current user
    suspend fun getUserReceipts(): Result<List<ReceiptModel>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val snapshot = firestore.collection(RECEIPTS_COLLECTION)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val receipts = snapshot.documents.mapNotNull { document ->
                document.toObject(ReceiptModel::class.java)
            }

            Result.success(receipts)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error getting user receipts", e)
            Result.failure(e)
        }
    }

    // Get a specific receipt by ID
    suspend fun getReceiptById(receiptId: String): Result<ReceiptModel?> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            val document = firestore.collection(RECEIPTS_COLLECTION)
                .document(receiptId)
                .get()
                .await()

            val receipt = document.toObject(ReceiptModel::class.java)

            // Verify the receipt belongs to the current user
            if (receipt != null && receipt.userId != currentUser.uid) {
                return Result.failure(Exception("Unauthorized access to receipt"))
            }

            Result.success(receipt)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error getting receipt by ID", e)
            Result.failure(e)
        }
    }

    // Update receipt data
    suspend fun updateReceipt(receipt: ReceiptModel): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Verify the receipt belongs to the current user
            if (receipt.userId != currentUser.uid) {
                return Result.failure(Exception("Unauthorized access to receipt"))
            }

            firestore.collection(RECEIPTS_COLLECTION)
                .document(receipt.id)
                .set(receipt)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error updating receipt", e)
            Result.failure(e)
        }
    }

    // Delete a receipt
    suspend fun deleteReceipt(receiptId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }

            // Get the receipt to verify ownership
            val receiptResult = getReceiptById(receiptId)
            if (receiptResult.isFailure) {
                return Result.failure(receiptResult.exceptionOrNull() ?: Exception("Receipt not found"))
            }

            val receipt = receiptResult.getOrNull()
            if (receipt == null) {
                return Result.failure(Exception("Receipt not found"))
            }

            // Verify the receipt belongs to the current user
            if (receipt.userId != currentUser.uid) {
                return Result.failure(Exception("Unauthorized access to receipt"))
            }

            // Delete from Firestore
            firestore.collection(RECEIPTS_COLLECTION)
                .document(receiptId)
                .delete()
                .await()

            // Delete the image if it exists
            if (receipt.imageUrl.isNotEmpty()) {
                try {
                    // Handle both http and gs URLs
                    if (receipt.imageUrl.startsWith("http")) {
                        val httpsReference = storage.getReferenceFromUrl(receipt.imageUrl)
                        httpsReference.delete().await()
                    } else {
                        // If direct path
                        storageRef.child(receipt.imageUrl).delete().await()
                    }
                } catch (e: Exception) {
                    // Log but don't fail if image deletion fails
                    Log.w("ReceiptRepository", "Failed to delete image: ${e.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ReceiptRepository", "Error deleting receipt", e)
            Result.failure(e)
        }
    }
}