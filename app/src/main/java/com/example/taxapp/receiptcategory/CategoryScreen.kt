package com.example.taxapp.receiptcategory

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.user.AppUtil

@RequiresApi(Build.VERSION_CODES.N)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    categoryViewModel: CategoryViewModel = viewModel()
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.categories),
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },

            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    //home
                    IconButton(onClick = { navController.navigate("home") }) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //upload receipt
                    IconButton(onClick = { navController.navigate("uploadReceipt") }) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Upload Receipt",
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //category
                    IconButton(onClick = { navController.navigate("category") }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Profile",
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    //profile
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(
                            Icons.Filled.Face,
                            contentDescription = "Profile",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        CategoryScreenContent(
            modifier = modifier.padding(innerPadding),
            categoryViewModel = categoryViewModel,
            navController = navController
        )
    }



    // Delete confirmation dialog
    if (categoryViewModel.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            isExpenseItem = categoryViewModel.expenseToDelete != null,
            onConfirm = {
                if (categoryViewModel.expenseToDelete != null) {
                    categoryViewModel.deleteExpenseItem()
                } else {
                    categoryViewModel.deleteReceipt(
                        onSuccess = {
                            AppUtil.showToast(context, "Receipt deleted successfully")
                        },
                        onError = { error ->
                            AppUtil.showToast(context, error)
                        }
                    )
                }
            },
            onDismiss = {
                categoryViewModel.cancelDelete()
            }
        )
    }

    // Edit receipt dialog
    if (categoryViewModel.isEditingReceipt) {
        EditReceiptDialog(
            categoryViewModel = categoryViewModel,
            onSave = {
                categoryViewModel.saveEditedReceipt(
                    onSuccess = {
                        AppUtil.showToast(context, "Receipt updated successfully")
                    },
                    onError = { error ->
                        AppUtil.showToast(context, error)
                    }
                )
            },
            onCancel = {
                categoryViewModel.cancelEditing()
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun CategoryScreenContent(
    modifier: Modifier = Modifier,
    categoryViewModel: CategoryViewModel,
    navController: NavHostController? = null
) {
    val isLoading = categoryViewModel.isLoading
    val errorMessage = categoryViewModel.errorMessage
    val categoryData = categoryViewModel.categoryData
    val categorySummary = categoryViewModel.categorySummary
    val expandedCategories = categoryViewModel.expandedCategories

    // Create scroll state for scrollable content
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        // Show loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Show error message if any
        else if (errorMessage != null && categoryData.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.error_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                ElevatedButton(
                    onClick = { categoryViewModel.loadCategoryData() }
                ) {
                    Text(text = stringResource(id = R.string.retry),)
                }

                if (navController != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    ElevatedButton(
                        onClick = { navController.navigate("uploadReceipt") }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Receipt",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.add_receipt),)
                    }
                }
            }
        }
        // Show category data
        else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.categories_summary),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(
                                id = R.string.categories_summary,
                                categoryViewModel.formatCurrency(categorySummary.values.sum())),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = R.string.number_of_categories, categoryData.size),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(id = R.string.number_of_receipts,categoryData.values.sumOf { it.size }),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Category Items
                categoryData.forEach { (category, expenseItems) ->
                    val isExpanded = expandedCategories.contains(category)
                    CategoryItemsSection(
                        category = category,
                        expenseItems = expenseItems,
                        totalAmount = categorySummary[category] ?: 0.0,
                        isExpanded = isExpanded,
                        onToggleExpand = { categoryViewModel.toggleCategoryExpansion(category) },
                        formatDate = { date -> categoryViewModel.formatDate(date) },
                        formatCurrency = { amount -> categoryViewModel.formatCurrency(amount) },
                        onEditReceipt = { receipt -> categoryViewModel.startEditingReceipt(receipt) },
                        onDeleteReceipt = { receipt -> categoryViewModel.confirmDeleteReceipt(receipt) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Space to ensure bottom items are visible
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CategoryItemsSection(
    category: String,
    expenseItems: List<CategoryViewModel.ExpenseItemWithReceipt>,
    totalAmount: Double,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    formatDate: (java.util.Date) -> String,
    formatCurrency: (Double) -> String,
    onEditReceipt: (ReceiptModel) -> Unit,
    onDeleteReceipt: (ReceiptModel) -> Unit
) {
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Category Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category name and count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${expenseItems.size} ${if (expenseItems.size == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Total amount
                Text(
                    text = formatCurrency(totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Expand/collapse icon
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationState)
                    )
                }
            }

            // Expandable expense items list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Divider()

                    Spacer(modifier = Modifier.height(8.dp))

                    if (expenseItems.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.no_item),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Expense items
                        expenseItems.forEach { expenseWithReceipt ->
                            ExpenseItemCard(
                                expenseItem = expenseWithReceipt.item,
                                receipt = expenseWithReceipt.receipt,
                                formatDate = formatDate,
                                formatCurrency = formatCurrency,
                                onEdit = { onEditReceipt(expenseWithReceipt.receipt) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expenseItem: ExpenseItem,
    receipt: ReceiptModel,
    formatDate: (java.util.Date) -> String,
    formatCurrency: (Double) -> String,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Expense item details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expenseItem.description.ifEmpty { "Unnamed Item" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.from ,receipt.merchantName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatDate(receipt.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Amount
                Text(
                    text = formatCurrency(expenseItem.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Edit button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Receipt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    isExpenseItem: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(id = R.string.delete_confirmation),)
        },
        text = {
            Text(
                text = if (isExpenseItem)
                    "Are you sure you want to delete this expense item?"
                else
                    "Are you sure you want to delete this receipt? This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(id = R.string.delete),)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel),)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptDialog(
    categoryViewModel: CategoryViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        val scrollState = rememberScrollState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 600.dp), // Set maximum height for the card
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    .verticalScroll(scrollState) // Make the entire content scrollable
            ) {
                // Dialog title with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.edit_receipt),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Merchant name field
                OutlinedTextField(
                    value = categoryViewModel.editMerchantName,
                    onValueChange = { categoryViewModel.editMerchantName = it },
                    label = { Text(text = stringResource(id = R.string.merchant_name),) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date field
                OutlinedTextField(
                    value = categoryViewModel.editDate,
                    onValueChange = { categoryViewModel.editDate = it },
                    label = { Text(text = stringResource(id = R.string.date),) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Total amount field
                OutlinedTextField(
                    value = categoryViewModel.editTotal,
                    onValueChange = { categoryViewModel.editTotal = it },
                    label = { Text(text = stringResource(id = R.string.total_amount),) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category dropdown
                var categoryMenuExpanded by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = categoryViewModel.editCategory,
                    onValueChange = { },
                    label = { Text(text = stringResource(id = R.string.category),) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { categoryMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Choose Category"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    categoryViewModel.availableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                categoryViewModel.editCategory = category
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Items section
                Text(
                    text = stringResource(id = R.string.items),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of expense items
                val expenses = categoryViewModel.editExpenseItems

                if (expenses.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_item_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        expenses.forEachIndexed { index, item ->
                            ExpenseItemRow(
                                item = item,
                                availableCategories = categoryViewModel.availableCategories,
                                onNameChange = { newName ->
                                    categoryViewModel.updateExpenseItemName(item, newName)
                                },
                                onAmountChange = { newAmount ->
                                    categoryViewModel.updateExpenseItemAmount(item, newAmount)
                                },
                                onCategoryChange = { newCategory ->
                                    categoryViewModel.updateExpenseItemCategory(item, newCategory)
                                },
                                onDelete = {
                                    categoryViewModel.confirmDeleteExpenseItem(item)
                                }
                            )

                            if (index < expenses.size - 1) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save button - with bottom padding to ensure it's not cut off
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.save_changes),)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseItemRow(
    item: ExpenseItem,
    availableCategories: List<String>,
    onNameChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(item.description) }
    var amount by remember { mutableStateOf(item.amount.toString()) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Item name field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onNameChange(it)
                },
                label = { Text(text = stringResource(id = R.string.description),) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Item amount field
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    it.toDoubleOrNull()?.let { validAmount ->
                        onAmountChange(validAmount)
                    }
                },
                label = { Text(text = stringResource(id = R.string.amount),) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Category selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.cate, item.category),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { categoryMenuExpanded = true }
                )

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false }
                ) {
                    availableCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onCategoryChange(category)
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Delete button
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Item",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}