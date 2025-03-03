package com.example.taxapp.receiptcategory

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CategoryViewModel : ViewModel() {
    private val repository = ReceiptRepository()

    // UI State
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var showDeleteConfirmation by mutableStateOf(false)
    var receiptToDelete by mutableStateOf<ReceiptModel?>(null)
    var expenseToDelete by mutableStateOf<ExpenseItem?>(null)

    // Edit state
    var isEditingReceipt by mutableStateOf(false)
    var currentEditReceipt by mutableStateOf<ReceiptModel?>(null)
    var editMerchantName by mutableStateOf("")
    var editTotal by mutableStateOf("")
    var editDate by mutableStateOf("")
    var editCategory by mutableStateOf("")
    var editExpenseItems by mutableStateOf<List<ExpenseItem>>(emptyList())

    // Data state
    var categoryData by mutableStateOf<Map<String, List<ExpenseItemWithReceipt>>>(emptyMap())
    var categorySummary by mutableStateOf<Map<String, Double>>(emptyMap())
    var expandedCategories by mutableStateOf<Set<String>>(emptySet())

    // Available categories
    val availableCategories = listOf(
        "Lifestyle Expenses",
        "Childcare",
        "Sport Equipment",
        "Donations",
        "Medical",
        "Education"
    )

    // Data class to link expense items with their parent receipt
    data class ExpenseItemWithReceipt(
        val item: ExpenseItem,
        val receipt: ReceiptModel
    )

    init {
        loadCategoryData()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun loadCategoryData() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Get all receipts
                val receiptsResult = repository.getUserReceipts()
                if (receiptsResult.isFailure) {
                    throw receiptsResult.exceptionOrNull() ?: Exception("Failed to load receipts")
                }

                val receipts = receiptsResult.getOrNull() ?: emptyList()

                if (receipts.isEmpty()) {
                    categoryData = emptyMap()
                    categorySummary = emptyMap()
                    errorMessage = "No receipts found. Add some receipts to see categories."
                    return@launch
                }

                // Create a map to store items by category
                val itemsByCategory = mutableMapOf<String, MutableList<ExpenseItemWithReceipt>>()
                val categorySums = mutableMapOf<String, Double>()

                // Process each receipt and its expense items
                receipts.forEach { receipt ->
                    // If receipt has specific expense items, process each item's category
                    if (receipt.items.isNotEmpty()) {
                        receipt.items.forEach { item ->
                            // Use the item's category, or fall back to receipt category if empty
                            val itemCategory = if (item.category.isNotEmpty()) item.category else receipt.category

                            // Add item to its category list
                            val itemsList = itemsByCategory.getOrDefault(itemCategory, mutableListOf())
                            itemsList.add(ExpenseItemWithReceipt(item, receipt))
                            itemsByCategory[itemCategory] = itemsList

                            // Add to category sum
                            val currentSum = categorySums.getOrDefault(itemCategory, 0.0)
                            categorySums[itemCategory] = currentSum + item.amount
                        }
                    } else {
                        // If no specific items, create a default item from receipt info
                        val defaultItem = ExpenseItem(
                            description = receipt.merchantName,
                            amount = receipt.total,
                            category = receipt.category
                        )

                        val itemsList = itemsByCategory.getOrDefault(receipt.category, mutableListOf())
                        itemsList.add(ExpenseItemWithReceipt(defaultItem, receipt))
                        itemsByCategory[receipt.category] = itemsList

                        // Add to category sum
                        val currentSum = categorySums.getOrDefault(receipt.category, 0.0)
                        categorySums[receipt.category] = currentSum + receipt.total
                    }
                }

                // Update state
                categoryData = itemsByCategory.mapValues { entry ->
                    entry.value.sortedByDescending { it.receipt.date }
                }
                categorySummary = categorySums

                // If this is the first load, expand the first category by default
                if (expandedCategories.isEmpty() && itemsByCategory.isNotEmpty()) {
                    expandedCategories = setOf(itemsByCategory.keys.first())
                }

            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error loading category data", e)
                errorMessage = e.localizedMessage ?: "Failed to load category data"
                categoryData = emptyMap()
                categorySummary = emptyMap()
            } finally {
                isLoading = false
            }
        }
    }

    // Toggle category expansion
    fun toggleCategoryExpansion(category: String) {
        expandedCategories = if (expandedCategories.contains(category)) {
            expandedCategories - category
        } else {
            expandedCategories + category
        }
    }

    // Format date for display
    fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

    // Format currency for display
    fun formatCurrency(amount: Double): String {
        return String.format("RM %.2f", amount)
    }

    // Parse date from string
    fun parseDate(dateStr: String): Date {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Log.e("CategoryViewModel", "Error parsing date: $dateStr", e)
            Date()
        }
    }

    // Start editing receipt
    fun startEditingReceipt(receipt: ReceiptModel) {
        currentEditReceipt = receipt
        editMerchantName = receipt.merchantName
        editTotal = receipt.total.toString()
        editDate = formatDate(receipt.date)
        editCategory = receipt.category
        editExpenseItems = receipt.items ?: emptyList()
        isEditingReceipt = true
    }

    // Cancel editing
    fun cancelEditing() {
        isEditingReceipt = false
        currentEditReceipt = null
        clearEditFields()
    }

    // Clear edit fields
    private fun clearEditFields() {
        editMerchantName = ""
        editTotal = ""
        editDate = ""
        editCategory = ""
        editExpenseItems = emptyList()
    }

    // Save edited receipt
    @RequiresApi(Build.VERSION_CODES.N)
    fun saveEditedReceipt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentReceipt = currentEditReceipt ?: return

        viewModelScope.launch {
            isLoading = true

            try {
                // Parse values
                val totalAmount = editTotal.replace(",", ".").toDoubleOrNull() ?: 0.0
                val receiptDate = parseDate(editDate)

                // Create updated receipt
                val updatedReceipt = currentReceipt.copy(
                    merchantName = editMerchantName,
                    total = totalAmount,
                    date = receiptDate,
                    category = editCategory,
                    items = editExpenseItems,
                    updatedAt = Timestamp.now()
                )

                // Update receipt in Firestore
                val result = repository.updateReceipt(updatedReceipt)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Failed to update receipt")
                }

                // Refresh data
                loadCategoryData()

                // Reset edit state
                isEditingReceipt = false
                currentEditReceipt = null
                clearEditFields()

                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error updating receipt", e)
                onError(e.localizedMessage ?: "Failed to update receipt")
            } finally {
                isLoading = false
            }
        }
    }

    // Confirm delete receipt
    fun confirmDeleteReceipt(receipt: ReceiptModel) {
        receiptToDelete = receipt
        showDeleteConfirmation = true
    }

    // Delete receipt
    fun deleteReceipt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val receipt = receiptToDelete ?: return

        viewModelScope.launch {
            isLoading = true

            try {
                // Delete receipt from Firestore
                val result = repository.deleteReceipt(receipt.id)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Failed to delete receipt")
                }

                // Refresh data
                loadCategoryData()

                // Reset delete state
                showDeleteConfirmation = false
                receiptToDelete = null

                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error deleting receipt", e)
                onError(e.localizedMessage ?: "Failed to delete receipt")
            } finally {
                isLoading = false
            }
        }
    }

    // Cancel delete
    fun cancelDelete() {
        showDeleteConfirmation = false
        receiptToDelete = null
        expenseToDelete = null
    }

    // Update expense item name
    fun updateExpenseItemName(expenseItem: ExpenseItem, newName: String) {
        editExpenseItems = editExpenseItems.map {
            if (it.id == expenseItem.id) it.copy(description = newName) else it
        }
    }

    // Update expense item amount
    fun updateExpenseItemAmount(expenseItem: ExpenseItem, newAmount: Double) {
        editExpenseItems = editExpenseItems.map {
            if (it.id == expenseItem.id) it.copy(amount = newAmount) else it
        }

        // Recalculate total
        updateTotalFromItems()
    }

    // Update expense item category
    fun updateExpenseItemCategory(expenseItem: ExpenseItem, newCategory: String) {
        editExpenseItems = editExpenseItems.map {
            if (it.id == expenseItem.id) it.copy(category = newCategory) else it
        }
    }

    // Recalculate total amount from all expense items
    private fun updateTotalFromItems() {
        val sum = editExpenseItems.sumOf { it.amount }
        editTotal = String.format(Locale.getDefault(), "%.2f", sum)
    }

    // Confirm delete expense item
    fun confirmDeleteExpenseItem(expenseItem: ExpenseItem) {
        expenseToDelete = expenseItem
        showDeleteConfirmation = true
    }

    // Delete expense item
    fun deleteExpenseItem() {
        val expense = expenseToDelete ?: return

        // Remove expense from list
        editExpenseItems = editExpenseItems.filter { it.id != expense.id }

        // Recalculate total
        updateTotalFromItems()

        // Reset delete state
        showDeleteConfirmation = false
        expenseToDelete = null
    }
}