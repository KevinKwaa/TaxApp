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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReceiptViewModel : ViewModel() {
    private val repository = ReceiptRepository()
    private lateinit var geminiService: GeminiService

    // UI state
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var currentReceiptUri by mutableStateOf<Uri?>(null)

    // Receipt extraction metadata - temporary to support the extraction process
    var merchantName by mutableStateOf("")
    var purchaseDate by mutableStateOf("")

    // Direct expense items list - this is our main focus now
    var expenseItems by mutableStateOf<List<ExpenseItem>>(emptyList())

    // Validation state maps - track validation errors for each expense item
    var itemDescriptionErrors by mutableStateOf<Map<String, String?>>(emptyMap())
    var itemAmountErrors by mutableStateOf<Map<String, String?>>(emptyMap())
    var itemMerchantErrors by mutableStateOf<Map<String, String?>>(emptyMap())
    var itemDateErrors by mutableStateOf<Map<String, String?>>(emptyMap())
    var itemHasErrors by mutableStateOf<Map<String, Boolean>>(emptyMap())

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
                        // Store only the common receipt metadata
                        merchantName = receiptData.merchantName
                        purchaseDate = formatDate(receiptData.date)

                        // Extract expense items, ensuring they have the merchant and date info
                        expenseItems = if (receiptData.items.isNotEmpty()) {
                            // Items already exist from extraction, make sure they have merchant and date
                            receiptData.items.map { item ->
                                item.copy(
                                    // Generate a new ID for each item
                                    id = UUID.randomUUID().toString(),
                                    merchantName = receiptData.merchantName,
                                    date = receiptData.date
                                )
                            }
                        } else {
                            // No items found, create a default one using the receipt total
                            listOf(
                                ExpenseItem(
                                    id = UUID.randomUUID().toString(),
                                    description = "Receipt item",
                                    amount = receiptData.total,
                                    category = receiptData.category,
                                    merchantName = receiptData.merchantName,
                                    date = receiptData.date
                                )
                            )
                        }

                        onSuccess()
                    } else {
                        throw Exception("Failed to extract receipt data")
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMsg = exception?.message ?: "Unknown error processing receipt"

                    // Handle our special error type separately
                    if (errorMsg.startsWith("NOT_A_RECEIPT:")) {
                        // Extract the clean error message without the prefix
                        errorMessage = errorMsg.substringAfter("NOT_A_RECEIPT:").trim()
                        onError("INVALID_RECEIPT_IMAGE: $errorMessage")
                    } else {
                        throw exception ?: Exception("Unknown error processing receipt")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error processing receipt", e)
                if (e.message?.startsWith("INVALID_RECEIPT_IMAGE:") == true) {
                    errorMessage = "Failed to process the receipt: ${e.localizedMessage}"
                    onError(errorMessage ?: "Unknown error")
                } else {
                    // Pass through our special error type
                    onError(e.message ?: "Unknown error")
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Initialize validation state for all items
    private fun initializeValidationState() {
        val descErrors = mutableMapOf<String, String?>()
        val amountErrors = mutableMapOf<String, String?>()
        val merchantErrors = mutableMapOf<String, String?>()
        val dateErrors = mutableMapOf<String, String?>()
        val hasErrors = mutableMapOf<String, Boolean>()

        expenseItems.forEach { item ->
            descErrors[item.id] = null
            amountErrors[item.id] = null
            merchantErrors[item.id] = null
            dateErrors[item.id] = null
            hasErrors[item.id] = false
        }

        itemDescriptionErrors = descErrors
        itemAmountErrors = amountErrors
        itemMerchantErrors = merchantErrors
        itemDateErrors = dateErrors
        itemHasErrors = hasErrors
    }

    // Format date for display
    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    // Parse date from string
    fun parseDate(dateString: String): Date? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(dateString)
        } catch (e: Exception) {
            Log.e("ReceiptViewModel", "Error parsing date: $dateString", e)
            null
        }
    }

    // Validate expense item fields
    fun validateExpenseItem(item: ExpenseItem,
                            newName: String?,
                            newAmount: String?,
                            newMerchant: String?,
                            newDate: String?): Boolean {
        var isValid = true
        val errors = mutableMapOf<String, String?>()

        // Create temp maps to hold updated validation state
        val updatedDescErrors = itemDescriptionErrors.toMutableMap()
        val updatedAmountErrors = itemAmountErrors.toMutableMap()
        val updatedMerchantErrors = itemMerchantErrors.toMutableMap()
        val updatedDateErrors = itemDateErrors.toMutableMap()
        val updatedHasErrors = itemHasErrors.toMutableMap()

        // Validate description if provided
        if (newName != null) {
            if (newName.trim().isEmpty()) {
                updatedDescErrors[item.id] = "Description cannot be empty"
                isValid = false
            } else {
                updatedDescErrors[item.id] = null
            }
        }

        // Validate amount if provided
        if (newAmount != null) {
            val amountValue = newAmount.replace(",", ".").toDoubleOrNull()
            if (amountValue == null) {
                updatedAmountErrors[item.id] = "Please enter a valid amount"
                isValid = false
            } else if (amountValue <= 0) {
                updatedAmountErrors[item.id] = "Amount must be greater than 0"
                isValid = false
            } else {
                updatedAmountErrors[item.id] = null
            }
        }

        // Validate merchant if provided
        if (newMerchant != null) {
            if (newMerchant.trim().isEmpty()) {
                updatedMerchantErrors[item.id] = "Merchant name cannot be empty"
                isValid = false
            } else {
                updatedMerchantErrors[item.id] = null
            }
        }

        // Validate date if provided
        if (newDate != null) {
            val parsedDate = parseDate(newDate)
            if (parsedDate == null) {
                updatedDateErrors[item.id] = "Please enter a valid date (DD/MM/YYYY)"
                isValid = false
            } else {
                updatedDateErrors[item.id] = null
            }
        }

        // Update validation state
        updatedHasErrors[item.id] = !isValid

        // Apply updated validation state
        itemDescriptionErrors = updatedDescErrors
        itemAmountErrors = updatedAmountErrors
        itemMerchantErrors = updatedMerchantErrors
        itemDateErrors = updatedDateErrors
        itemHasErrors = updatedHasErrors

        return isValid
    }

    // Check if any items have validation errors
    fun hasAnyValidationErrors(): Boolean {
        return itemHasErrors.values.any { it }
    }

    // Save all extracted expense items as individual entities
    fun saveReceipt(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        // Validate all items before saving
        if (hasAnyValidationErrors()) {
            onError("Please correct all validation errors before saving")
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                if (currentReceiptUri == null) {
                    throw Exception("No receipt image available")
                }

                if (expenseItems.isEmpty()) {
                    throw Exception("No expense items to save")
                }

                // Upload the image once to get a URL
                val uploadResult = repository.uploadReceiptImage(currentReceiptUri!!)
                var imageUrl = ""

                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull() ?: ""
                } else {
                    // Log warning but continue - image is not critical
                    Log.w("ReceiptViewModel", "Image upload failed: ${uploadResult.exceptionOrNull()?.message}")
                }

                // Track IDs of saved items for return
                val savedItemIds = mutableListOf<String>()

                // Save each expense item individually
                for (item in expenseItems) {
                    // Create an expense-only receipt model for this item
                    val expenseReceipt = ReceiptModel(
                        merchantName = item.merchantName,
                        total = item.amount,  // Individual item amount
                        date = item.date,     // Using the item's date
                        category = item.category,
                        imageUrl = imageUrl,  // All items share the same receipt image
                        items = listOf(item)  // Only one item per receipt now
                    )

                    // Save this individual expense
                    val saveResult = repository.saveReceipt(expenseReceipt)
                    if (saveResult.isSuccess) {
                        val receiptId = saveResult.getOrNull() ?: ""
                        savedItemIds.add(receiptId)
                    } else {
                        // If any item fails, throw exception
                        throw saveResult.exceptionOrNull() ?: Exception("Failed to save expense item")
                    }
                }

                // Success - all items were saved
                onSuccess(savedItemIds.firstOrNull() ?: "")

                // Reset state
                resetState()
            } catch (e: Exception) {
                Log.e("ReceiptViewModel", "Error saving expense items", e)
                errorMessage = e.message ?: "An error occurred"
                onError(errorMessage ?: "Unknown error")
            } finally {
                isLoading = false
            }
        }
    }

    // Add new expense item
    fun addExpenseItem() {
        // Extract date from first item or use current date
        val itemDate = if (expenseItems.isNotEmpty()) {
            expenseItems.first().date
        } else {
            parseDate(purchaseDate) ?: Date()
        }

        // Create new expense item with current merchant name
        val newItem = ExpenseItem(
            id = UUID.randomUUID().toString(),
            description = "New item",
            amount = 0.0,
            category = if (expenseItems.isNotEmpty()) expenseItems.first().category else "Lifestyle Expenses",
            merchantName = merchantName,
            date = itemDate
        )

        // Add to list
        expenseItems = expenseItems + newItem

        // Add new item to validation maps
        val descErrors = itemDescriptionErrors.toMutableMap()
        val amountErrors = itemAmountErrors.toMutableMap()
        val merchantErrors = itemMerchantErrors.toMutableMap()
        val dateErrors = itemDateErrors.toMutableMap()
        val hasErrors = itemHasErrors.toMutableMap()

        descErrors[newItem.id] = null
        amountErrors[newItem.id] = null
        merchantErrors[newItem.id] = null
        dateErrors[newItem.id] = null
        hasErrors[newItem.id] = false

        itemDescriptionErrors = descErrors
        itemAmountErrors = amountErrors
        itemMerchantErrors = merchantErrors
        itemDateErrors = dateErrors
        itemHasErrors = hasErrors
    }

    // Update expense item description/name
    fun updateExpenseItemName(item: ExpenseItem, newName: String) {
        // Validate first
        validateExpenseItem(item, newName, null, null, null)

        // Only update if valid
        if (itemDescriptionErrors[item.id] == null) {
            expenseItems = expenseItems.map {
                if (it.id == item.id) it.copy(description = newName) else it
            }
            Log.d("ReceiptViewModel", "Updated item name to: $newName")
        }
    }

    // Update expense item amount
    fun updateExpenseItemAmount(item: ExpenseItem, newAmount: Double) {
        // Validate using string representation
        validateExpenseItem(item, null, newAmount.toString(), null, null)

        // Only update if valid
        if (itemAmountErrors[item.id] == null) {
            expenseItems = expenseItems.map {
                if (it.id == item.id) it.copy(amount = newAmount) else it
            }
            Log.d("ReceiptViewModel", "Updated item amount to: $newAmount")
        }
    }

    // Update expense item category
    fun updateExpenseItemCategory(item: ExpenseItem, newCategory: String) {
        expenseItems = expenseItems.map {
            if (it.id == item.id) it.copy(category = newCategory) else it
        }
        Log.d("ReceiptViewModel", "Updated item category to: $newCategory")
    }

    // Update expense item merchant name
    fun updateExpenseItemMerchant(item: ExpenseItem, newMerchant: String) {
        // Validate first
        validateExpenseItem(item, null, null, newMerchant, null)

        // Only update if valid
        if (itemMerchantErrors[item.id] == null) {
            expenseItems = expenseItems.map {
                if (it.id == item.id) it.copy(merchantName = newMerchant) else it
            }
            // Update the common merchant name if this is the first item
            if (expenseItems.isNotEmpty() && expenseItems.first().id == item.id) {
                merchantName = newMerchant
            }
            Log.d("ReceiptViewModel", "Updated item merchant to: $newMerchant")
        }
    }

    // Update expense item date
    fun updateExpenseItemDate(item: ExpenseItem, newDateString: String) {
        // Validate first
        validateExpenseItem(item, null, null, null, newDateString)

        // Only update if valid
        if (itemDateErrors[item.id] == null) {
            val newDate = parseDate(newDateString)
            if (newDate != null) {
                expenseItems = expenseItems.map {
                    if (it.id == item.id) it.copy(date = newDate) else it
                }
                // Update the common date if this is the first item
                if (expenseItems.isNotEmpty() && expenseItems.first().id == item.id) {
                    purchaseDate = formatDate(newDate)
                }
                Log.d("ReceiptViewModel", "Updated item date to: $newDateString")
            }
        }
    }

    // Delete expense item
    fun deleteExpenseItem(item: ExpenseItem) {
        expenseItems = expenseItems.filter { it.id != item.id }

        // Remove from validation maps
        val descErrors = itemDescriptionErrors.toMutableMap()
        val amountErrors = itemAmountErrors.toMutableMap()
        val merchantErrors = itemMerchantErrors.toMutableMap()
        val dateErrors = itemDateErrors.toMutableMap()
        val hasErrors = itemHasErrors.toMutableMap()

        descErrors.remove(item.id)
        amountErrors.remove(item.id)
        merchantErrors.remove(item.id)
        dateErrors.remove(item.id)
        hasErrors.remove(item.id)

        itemDescriptionErrors = descErrors
        itemAmountErrors = amountErrors
        itemMerchantErrors = merchantErrors
        itemDateErrors = dateErrors
        itemHasErrors = hasErrors

        Log.d("ReceiptViewModel", "Deleted expense item: ${item.description}, remaining items: ${expenseItems.size}")
    }

    // Reset the view model state
    fun resetState() {
        currentReceiptUri = null
        merchantName = ""
        purchaseDate = ""
        expenseItems = emptyList()
        errorMessage = null

        // Reset validation state
        itemDescriptionErrors = emptyMap()
        itemAmountErrors = emptyMap()
        itemMerchantErrors = emptyMap()
        itemDateErrors = emptyMap()
        itemHasErrors = emptyMap()
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