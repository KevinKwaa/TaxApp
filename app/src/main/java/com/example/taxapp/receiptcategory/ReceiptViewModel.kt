package com.example.taxapp.receiptcategory

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptViewModel : ViewModel() {
    private val repository = ReceiptRepository()
    private lateinit var geminiService: GeminiService

    // UI state
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var currentReceiptUri by mutableStateOf<Uri?>(null)

    // Receipt data
    var merchantName by mutableStateOf("")
    var total by mutableStateOf("")
    var date by mutableStateOf("")
    var category by mutableStateOf("")
    var expenseItems by mutableStateOf<List<ExpenseItem>>(emptyList())

    // Available categories
    val availableCategories = listOf(
        "Lifestyle Expenses",
        "Childcare",
        "Sport Equipment",
        "Donations",
        "Medical",
        "Education"
    )

    // Process receipt using Gemini AI
    fun processReceiptImage(uri: Uri, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Initialize GeminiService if not already done
        if (!::geminiService.isInitialized) {
            geminiService = GeminiService(context)
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            currentReceiptUri = uri

            try {
                // Use Gemini AI to analyze the receipt
                val result = geminiService.processReceiptImage(uri)

                if (result.isSuccess) {
                    val receiptData = result.getOrNull()
                    if (receiptData != null) {
                        // Update the UI state with extracted data
                        merchantName = receiptData.merchantName
                        total = receiptData.total.toString()
                        date = formatDate(receiptData.date)
                        category = receiptData.category
                        expenseItems = receiptData.items

                        onSuccess()
                    } else {
                        throw Exception("Failed to extract receipt data")
                    }
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error processing receipt")
                }
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error processing receipt", e)
                errorMessage = "Failed to process the receipt: ${e.localizedMessage}"
                onError(errorMessage ?: "Unknown error")
            } finally {
                isLoading = false
            }
        }
    }

    // Format date for display
    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    // Save the processed receipt
    fun saveReceipt(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                if (currentReceiptUri == null) {
                    throw Exception("No receipt image available")
                }

                // Convert total to Double
                val totalAmount = try {
                    total.replace(",", ".").toDouble()
                } catch (e: NumberFormatException) {
                    Log.w("ReceiptViewModel", "Error parsing total amount: $total", e)
                    0.00
                }

                // Parse date
                val receiptDate = try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.parse(date) ?: Date()
                } catch (e: Exception) {
                    Log.w("ReceiptViewModel", "Error parsing date: $date", e)
                    Date()
                }

                // First save the receipt without the image URL
                val receiptWithoutImage = ReceiptModel(
                    merchantName = merchantName,
                    total = totalAmount,
                    date = receiptDate,
                    category = category,
                    imageUrl = "", // Will be updated after image upload
                    items = expenseItems
                )

                // Save initial receipt to get an ID
                val saveResult = repository.saveReceipt(receiptWithoutImage)
                if (saveResult.isFailure) {
                    throw saveResult.exceptionOrNull() ?: Exception("Failed to save receipt")
                }

                val receiptId = saveResult.getOrNull() ?: ""

                // Upload the image
                val uploadResult = repository.uploadReceiptImage(currentReceiptUri!!)
                var imageUrl = ""

                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull() ?: ""

                    // Update the receipt with the image URL
                    val updatedReceipt = receiptWithoutImage.copy(
                        id = receiptId,
                        imageUrl = imageUrl
                    )

                    // Update the receipt in Firestore
                    repository.updateReceipt(updatedReceipt).getOrThrow()
                } else {
                    // If image upload fails, log it but continue with receipt saved
                    Log.w("ReceiptViewModel", "Image upload failed: ${uploadResult.exceptionOrNull()?.message}")
                    // We'll still return success but with a warning
                    errorMessage = "Receipt saved but image upload failed"
                }

                onSuccess(receiptId)

                // Reset state
                resetState()
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error saving receipt", e)
                errorMessage = e.message ?: "An error occurred"
                onError(errorMessage ?: "Unknown error")
            } finally {
                isLoading = false
            }
        }
    }

    // Update expense item description/name
    fun updateExpenseItemName(item: ExpenseItem, newName: String) {
        expenseItems = expenseItems.map {
            if (it == item) it.copy(description = newName) else it
        }
        Log.d("ReceiptViewModel", "Updated item name to: $newName")
    }

    // Update expense item amount
    fun updateExpenseItemAmount(item: ExpenseItem, newAmount: Double) {
        expenseItems = expenseItems.map {
            if (it == item) it.copy(amount = newAmount) else it
        }

        // Recalculate total
        updateTotalFromItems()
        Log.d("ReceiptViewModel", "Updated item amount to: $newAmount, new total: $total")
    }

    // Update expense item category
    fun updateExpenseItemCategory(item: ExpenseItem, newCategory: String) {
        expenseItems = expenseItems.map {
            if (it == item) it.copy(category = newCategory) else it
        }

        // If this is the only item, also update the receipt main category
        if (expenseItems.size <= 1) {
            category = newCategory
        }

        Log.d("ReceiptViewModel", "Updated item category to: $newCategory")
    }

    // Delete expense item
    fun deleteExpenseItem(item: ExpenseItem) {
        expenseItems = expenseItems.filter { it != item }

        // Recalculate total
        updateTotalFromItems()

        // If all items are deleted, set total to 0
        if (expenseItems.isEmpty()) {
            total = "0.00"
        }

        Log.d("ReceiptViewModel", "Deleted expense item: ${item.description}, remaining items: ${expenseItems.size}")
    }

    // Recalculate total amount from all expense items
    private fun updateTotalFromItems() {
        val sum = expenseItems.sumOf { it.amount }
        total = String.format(Locale.getDefault(), "%.2f", sum)
    }

    // Update receipt data
    fun updateReceiptData(
        newMerchantName: String,
        newTotal: String,
        newDate: String,
        newCategory: String,
        newItems: List<ExpenseItem>
    ) {
        merchantName = newMerchantName
        total = newTotal
        date = newDate
        category = newCategory
        expenseItems = newItems
    }

    // Reset the view model state
    fun resetState() {
        currentReceiptUri = null
        merchantName = ""
        total = ""
        date = ""
        category = ""
        expenseItems = emptyList()
        errorMessage = null
    }

    // Analyze all user receipts for tax insights
    @RequiresApi(Build.VERSION_CODES.N)
    fun analyzeTaxSavings(context: Context, onResult: (Map<String, Double>) -> Unit) {
        if (!::geminiService.isInitialized) {
            geminiService = GeminiService(context)
        }

        viewModelScope.launch {
            isLoading = true

            try {
                Log.d("ReceiptViewModel", "Starting tax savings analysis")
                // Get all user receipts
                val receiptsResult = repository.getUserReceipts()
                if (receiptsResult.isFailure) {
                    Log.e("ReceiptViewModel", "Failed to fetch receipts", receiptsResult.exceptionOrNull())
                    throw receiptsResult.exceptionOrNull() ?: Exception("Failed to fetch receipts")
                }

                val receipts = receiptsResult.getOrNull() ?: emptyList()
                Log.d("ReceiptViewModel", "Retrieved ${receipts.size} receipts for analysis")

                // No receipts to analyze
                if (receipts.isEmpty()) {
                    Log.d("ReceiptViewModel", "No receipts to analyze, returning empty map")
                    onResult(emptyMap())
                    return@launch
                }

                // Use Gemini to analyze tax savings
                Log.d("ReceiptViewModel", "Calling geminiService.analyzeTaxSavings with ${receipts.size} receipts")
                val savingsResult = geminiService.analyzeTaxSavings(receipts)

                if (savingsResult.isFailure) {
                    Log.e("ReceiptViewModel", "Failed to analyze tax savings", savingsResult.exceptionOrNull())
                    throw savingsResult.exceptionOrNull() ?: Exception("Failed to analyze tax savings")
                }

                val savings = savingsResult.getOrNull() ?: emptyMap()
                Log.d("ReceiptViewModel", "Analysis complete, returning results: $savings")
                onResult(savings)

            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error analyzing tax savings", e)
                errorMessage = "Failed to analyze tax savings: ${e.localizedMessage}"
                onResult(emptyMap())
            } finally {
                isLoading = false
            }
        }
    }
}