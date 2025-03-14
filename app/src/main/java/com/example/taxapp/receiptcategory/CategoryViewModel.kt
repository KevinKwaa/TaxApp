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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class CategoryViewModel : ViewModel() {
    private val repository = ReceiptRepository()

    // UI State
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var showDeleteConfirmation by mutableStateOf(false)
    var receiptToDelete by mutableStateOf<ReceiptModel?>(null)
    var expenseToDelete by mutableStateOf<ExpenseItem?>(null)

    // Year filter state
    var availableYears by mutableStateOf<List<Int>>(emptyList())
    var selectedYear by mutableStateOf<Int?>(null)

    // Edit receipt state (legacy support)
    var isEditingReceipt by mutableStateOf(false)
    var currentEditReceipt by mutableStateOf<ReceiptModel?>(null)
    var editMerchantName by mutableStateOf("")
    var editTotal by mutableStateOf("")
    var editDate by mutableStateOf("")
    var editCategory by mutableStateOf("")
    var editExpenseItems by mutableStateOf<List<ExpenseItem>>(emptyList())

    // Edit expense item state (new, focused on individual items)
    var isEditingExpenseItem by mutableStateOf(false)
    var currentEditExpenseItem by mutableStateOf<ExpenseItem?>(null)
    var editExpenseDescription by mutableStateOf("")
    var editExpenseAmount by mutableStateOf("")
    var editExpenseCategory by mutableStateOf("")
    var editExpenseMerchant by mutableStateOf("")
    var editExpenseDate by mutableStateOf("")
    var parentReceiptId by mutableStateOf("")

    // Validation state
    var descriptionError by mutableStateOf<String?>(null)
    var amountError by mutableStateOf<String?>(null)
    var dateError by mutableStateOf<String?>(null)
    var merchantError by mutableStateOf<String?>(null)
    var categoryError by mutableStateOf<String?>(null)
    var hasValidationErrors by mutableStateOf(false)

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

                // Extract all available years from the receipts
                val years = receipts.map { receipt ->
                    val calendar = Calendar.getInstance()
                    calendar.time = receipt.date
                    calendar.get(Calendar.YEAR)
                }.toSet().sorted()

                // Update available years
                availableYears = years

                // If selectedYear is null or not in the available years, default to the most recent year
                if (selectedYear == null || !years.contains(selectedYear)) {
                    selectedYear = years.lastOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                }

                // Filter receipts by the selected year
                val filteredReceipts = if (selectedYear != null) {
                    receipts.filter { receipt ->
                        val calendar = Calendar.getInstance()
                        calendar.time = receipt.date
                        calendar.get(Calendar.YEAR) == selectedYear
                    }
                } else {
                    receipts
                }

                // If no receipts in the selected year
                if (filteredReceipts.isEmpty()) {
                    categoryData = emptyMap()
                    categorySummary = emptyMap()
                    errorMessage = "No receipts found for $selectedYear. Try selecting a different year."
                    isLoading = false
                    return@launch
                }

                // Create a map to store items by category
                val itemsByCategory = mutableMapOf<String, MutableList<ExpenseItemWithReceipt>>()
                val categorySums = mutableMapOf<String, Double>()

                // Process each receipt and its expense items
                filteredReceipts.forEach { receipt ->
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
                            category = receipt.category,
                            merchantName = receipt.merchantName,
                            date = receipt.date
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
                    // Sort by date descending, then by merchant name
                    entry.value.sortedWith(
                        compareByDescending<ExpenseItemWithReceipt> { it.item.date }
                            .thenBy { it.item.merchantName }
                    )
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

    // Set the selected year and reload data
    @RequiresApi(Build.VERSION_CODES.N)
    fun setSelectedYear(year: Int) {
        if (selectedYear != year) {
            selectedYear = year
            loadCategoryData()
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
    fun parseDate(dateStr: String): Date? {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            Log.e("CategoryViewModel", "Error parsing date: $dateStr", e)
            null
        }
    }

    // Start editing an expense item (new, focused on individual item)
    fun startEditingExpenseItem(item: ExpenseItem) {
        currentEditExpenseItem = item
        editExpenseDescription = item.description
        editExpenseAmount = item.amount.toString()
        editExpenseCategory = item.category
        editExpenseMerchant = item.merchantName
        editExpenseDate = formatDate(item.date)

        // Find parent receipt for this item
        val parentReceipt = findParentReceipt(item)
        parentReceiptId = parentReceipt?.id ?: ""

        // Clear validation errors
        clearValidationErrors()

        isEditingExpenseItem = true
    }

    // Find which receipt contains this expense item
    private fun findParentReceipt(item: ExpenseItem): ReceiptModel? {
        for ((_, itemsWithReceipt) in categoryData) {
            for (itemWithReceipt in itemsWithReceipt) {
                if (itemWithReceipt.item.id == item.id) {
                    return itemWithReceipt.receipt
                }
            }
        }
        return null
    }

    // Cancel editing expense item
    fun cancelEditingExpenseItem() {
        isEditingExpenseItem = false
        currentEditExpenseItem = null
        clearEditExpenseFields()
        clearValidationErrors()
    }

    // Clear edit expense item fields
    private fun clearEditExpenseFields() {
        editExpenseDescription = ""
        editExpenseAmount = ""
        editExpenseCategory = ""
        editExpenseMerchant = ""
        editExpenseDate = ""
        parentReceiptId = ""
    }

    // Clear validation errors
    private fun clearValidationErrors() {
        descriptionError = null
        amountError = null
        dateError = null
        merchantError = null
        categoryError = null
        hasValidationErrors = false
    }

    // Validate expense item fields
    fun validateExpenseItemFields(): Boolean {
        var isValid = true

        // Validate description
        if (editExpenseDescription.trim().isEmpty()) {
            descriptionError = "Description cannot be empty"
            isValid = false
        } else {
            descriptionError = null
        }

        // Validate amount
        val amountValue = editExpenseAmount.replace(",", ".").toDoubleOrNull()
        if (amountValue == null) {
            amountError = "Please enter a valid amount"
            isValid = false
        } else if (amountValue <= 0) {
            amountError = "Amount must be greater than 0"
            isValid = false
        } else {
            amountError = null
        }

        // Validate date
        val parsedDate = parseDate(editExpenseDate)
        if (parsedDate == null) {
            dateError = "Please enter a valid date (DD/MM/YYYY)"
            isValid = false
        } else {
            dateError = null
        }

        // Validate merchant
        if (editExpenseMerchant.trim().isEmpty()) {
            merchantError = "Merchant name cannot be empty"
            isValid = false
        } else {
            merchantError = null
        }

        // Validate category
        if (editExpenseCategory.trim().isEmpty() || !availableCategories.contains(editExpenseCategory)) {
            categoryError = "Please select a valid category"
            isValid = false
        } else {
            categoryError = null
        }

        hasValidationErrors = !isValid
        return isValid
    }

    // Save edited expense item
    @RequiresApi(Build.VERSION_CODES.N)
    fun saveEditedExpenseItem(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Validate fields first
        if (!validateExpenseItemFields()) {
            onError("Please correct the validation errors")
            return
        }

        val parentReceipt = findParentReceipt(currentEditExpenseItem ?: return)
        if (parentReceipt == null) {
            onError("Could not find parent receipt for this expense item")
            return
        }

        viewModelScope.launch {
            isLoading = true

            try {
                // Parse values (already validated)
                val expenseAmount = editExpenseAmount.replace(",", ".").toDouble()
                val expenseDate = parseDate(editExpenseDate)!!

                // Create updated expense item
                val updatedItem = currentEditExpenseItem!!.copy(
                    description = editExpenseDescription.trim(),
                    amount = expenseAmount,
                    category = editExpenseCategory,
                    merchantName = editExpenseMerchant.trim(),
                    date = expenseDate
                )

                // Find and update the item in the receipt's items list
                val updatedItems = parentReceipt.items.map {
                    if (it.id == updatedItem.id) updatedItem else it
                }

                // Create updated receipt with the modified item
                val updatedReceipt = parentReceipt.copy(
                    items = updatedItems,
                    // Optionally update receipt total
                    total = updatedItems.sumOf { it.amount },
                    updatedAt = Timestamp.now()
                )

                // Update receipt in Firestore
                val result = repository.updateReceipt(updatedReceipt)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Failed to update expense item")
                }

                // Refresh data
                loadCategoryData()

                // Reset edit state
                isEditingExpenseItem = false
                currentEditExpenseItem = null
                clearEditExpenseFields()
                clearValidationErrors()

                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error updating expense item", e)
                onError(e.localizedMessage ?: "Failed to update expense item")
            } finally {
                isLoading = false
            }
        }
    }

    // Remaining methods stay the same...

    // Confirm delete expense item
    fun confirmDeleteExpenseItem(item: ExpenseItem) {
        expenseToDelete = item
        showDeleteConfirmation = true
    }

    // Delete expense item
    fun deleteExpenseItem() {
        val item = expenseToDelete ?: return
        val parentReceipt = findParentReceipt(item) ?: return

        viewModelScope.launch {
            isLoading = true

            try {
                // Remove the item from the receipt's items
                val updatedItems = parentReceipt.items.filter { it.id != item.id }

                // Create updated receipt with the item removed
                val updatedReceipt = parentReceipt.copy(
                    items = updatedItems,
                    // Update receipt total
                    total = updatedItems.sumOf { it.amount },
                    updatedAt = Timestamp.now()
                )

                // If there are no items left, delete the entire receipt
                if (updatedItems.isEmpty()) {
                    val deleteResult = repository.deleteReceipt(parentReceipt.id)
                    if (deleteResult.isFailure) {
                        throw deleteResult.exceptionOrNull() ?: Exception("Failed to delete receipt")
                    }
                } else {
                    // Otherwise update the receipt with the item removed
                    val updateResult = repository.updateReceipt(updatedReceipt)
                    if (updateResult.isFailure) {
                        throw updateResult.exceptionOrNull() ?: Exception("Failed to update receipt")
                    }
                }

                // Refresh data
                loadCategoryData()

                // Reset state
                showDeleteConfirmation = false
                expenseToDelete = null

            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error deleting expense item", e)
                // Show error but don't reset state to allow retry
                errorMessage = e.localizedMessage ?: "Failed to delete expense item"
            } finally {
                isLoading = false
            }
        }
    }

    // Legacy methods for receipt editing (supporting backward compatibility)

    // Start editing receipt
    fun startEditingReceipt(receipt: ReceiptModel) {
        currentEditReceipt = receipt
        editMerchantName = receipt.merchantName
        editTotal = receipt.total.toString()
        editDate = formatDate(receipt.date)
        editCategory = receipt.category
        editExpenseItems = receipt.items
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
                val receiptDate = parseDate(editDate) ?: Date()

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

    // Clear year state variables
    var showClearYearConfirmation by mutableStateOf(false)
    var yearToClear by mutableStateOf<Int?>(null)

    // Confirm clear year
    fun confirmClearYear(year: Int) {
        yearToClear = year
        showClearYearConfirmation = true
    }

    // Cancel clear year
    fun cancelClearYear() {
        showClearYearConfirmation = false
        yearToClear = null
    }

    // Clear all expenses for a specific year
    @RequiresApi(Build.VERSION_CODES.N)
    fun clearExpensesForYear(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val year = yearToClear ?: return

        viewModelScope.launch {
            isLoading = true

            try {
                // Get all receipts
                val receiptsResult = repository.getUserReceipts()
                if (receiptsResult.isFailure) {
                    throw receiptsResult.exceptionOrNull() ?: Exception("Failed to load receipts")
                }

                val receipts = receiptsResult.getOrNull() ?: emptyList()

                // Filter receipts from the selected year
                val receiptsToDelete = receipts.filter { receipt ->
                    val calendar = Calendar.getInstance()
                    calendar.time = receipt.date
                    calendar.get(Calendar.YEAR) == year
                }

                if (receiptsToDelete.isEmpty()) {
                    throw Exception("No receipts found for year $year")
                }

                // Delete all receipts for the selected year
                var successCount = 0
                var failureCount = 0

                for (receipt in receiptsToDelete) {
                    val result = repository.deleteReceipt(receipt.id)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        failureCount++
                    }
                }

                // Check if all deletions were successful
                if (failureCount > 0) {
                    throw Exception("Failed to delete $failureCount out of ${receiptsToDelete.size} receipts")
                }

                // Refresh the data
                loadCategoryData()

                // Reset state
                showClearYearConfirmation = false
                yearToClear = null

                onSuccess()
            } catch (e: Exception) {
                Log.e("CategoryViewModel", "Error clearing expenses for year $year", e)
                onError(e.localizedMessage ?: "Failed to clear expenses")
            } finally {
                isLoading = false
            }
        }
    }
}