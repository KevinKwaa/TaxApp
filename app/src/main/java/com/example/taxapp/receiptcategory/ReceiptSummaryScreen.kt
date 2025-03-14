package com.example.taxapp.receiptcategory

import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import com.example.taxapp.user.AppUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptSummaryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    receiptViewModel: ReceiptViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }

    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe the current language
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    // Create a TTS instance if text-to-speech is enabled
    val tts = remember(accessibilityState.textToSpeech) {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }

    // Clean up TTS when not needed
    DisposableEffect(accessibilityState.textToSpeech) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    ScreenReader("Home Screen")
    val ttsManager = LocalTtsManager.current

    // Today's date
    val today = LocalDate.now()
    val formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.receipt_summary),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    //titleContentColor = MaterialTheme.colorScheme.onTertiary
                ),
                actions = {
                    // Language button with improved styling
                    IconButton(
                        onClick = { showLanguageSelector = true },
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language, // Use the standard language icon
                            contentDescription = "Change Language",
                            //tint = accessibleColors.buttonText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Accessibility button with improved styling
                    IconButton(
                        onClick = { showAccessibilitySettings = true },
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,  // Standard settings icon
                            contentDescription = "Accessibility Settings",
                            //tint = accessibleColors.buttonText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        ReceiptSummaryContent(
            modifier = modifier.padding(innerPadding),
            navController = navController,
            receiptViewModel = receiptViewModel
        )
    }

    if (showLanguageSelector) {
        LanguageSelector(
            currentLanguageCode = currentLanguageCode,
            onLanguageSelected = { languageCode ->
                currentLanguageCode = languageCode
            },
            onDismiss = { showLanguageSelector = false },
            activity = activity
        )
    }

    if (showAccessibilitySettings) {
        AccessibilitySettings(
            currentSettings = accessibilityState,
            onSettingsChanged = { newSettings ->
                coroutineScope.launch {
                    accessibilityRepository.updateSettings(newSettings)
                }
            },
            onDismiss = { showAccessibilitySettings = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReceiptSummaryContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    receiptViewModel: ReceiptViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }

    // Total sum of all expense items
    val totalAmount by remember {
        derivedStateOf {
            receiptViewModel.expenseItems.sumOf { it.amount }
        }
    }

    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe the current language
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    // Create a TTS instance if text-to-speech is enabled
    val tts = remember(accessibilityState.textToSpeech) {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }

    // Clean up TTS when not needed
    DisposableEffect(accessibilityState.textToSpeech) {
        onDispose {
            tts?.shutdown()
        }
    }

    // Add debug logging to verify data is available
    LaunchedEffect(Unit) {
        Log.d("ReceiptSummaryContent", "Starting receipt summary screen with viewModel: $receiptViewModel")
        Log.d("ReceiptSummaryContent", "Receipt data available: URI=${receiptViewModel.currentReceiptUri}, merchant=${receiptViewModel.merchantName}")
    }

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    ScreenReader("Receipt Items Screen")
    val ttsManager = LocalTtsManager.current

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            receiptViewModel.errorMessage?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error: $error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            ttsManager?.speak("Going Back")
                            navController.navigateUp()
                        }) {
                            Text(text = "Go Back")
                        }
                    }
                }
                return@LanguageProvider
            }

            // Loading indicator
            if (receiptViewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
                return@LanguageProvider
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Receipt image
                item {
                    receiptViewModel.currentReceiptUri?.let { uri ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accessibleColors.calendarText
                            )
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Receipt Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Common merchant info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.receipt_details),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.merchant_name),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = receiptViewModel.merchantName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.date_colon),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = receiptViewModel.purchaseDate,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.receipt_total),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "RM ${String.format("%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Expense items section header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.expense_items,
                                receiptViewModel.expenseItems.size
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Add new item button
                        Button(
                            onClick = {
                                receiptViewModel.addExpenseItem()
                                ttsManager?.speak("Added new expense item")
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Item")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(id = R.string.add_item))
                        }
                    }
                }

                // List of expense items
                items(receiptViewModel.expenseItems) { item ->
                    EditableExpenseItemCard(
                        item = item,
                        availableCategories = receiptViewModel.availableCategories,
                        onCategoryChange = { newCategory ->
                            // Update the category for this item
                            receiptViewModel.updateExpenseItemCategory(item, newCategory)
                        },
                        onNameChange = { newName ->
                            // Update the name/description of this item
                            receiptViewModel.updateExpenseItemName(item, newName)
                        },
                        onAmountChange = { newAmount ->
                            // Update the amount of this item
                            receiptViewModel.updateExpenseItemAmount(item, newAmount)
                        },
                        onMerchantChange = { newMerchant ->
                            // Update the merchant name for this item
                            receiptViewModel.updateExpenseItemMerchant(item, newMerchant)
                        },
                        onDateChange = { newDate ->
                            // Update the date for this item
                            receiptViewModel.updateExpenseItemDate(item, newDate)
                        },
                        onDeleteItem = { expenseItem ->
                            // Delete this expense item
                            receiptViewModel.deleteExpenseItem(expenseItem)
                        }
                    )
                }

                // Save and Cancel buttons
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        // Save button
                        Button(
                            onClick = {
                                ttsManager?.speak("Saving Expenses")
                                receiptViewModel.saveReceipt(
                                    onSuccess = { receiptId ->
                                        AppUtil.showToast(context, "Expenses saved successfully")
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    },
                                    onError = { error ->
                                        AppUtil.showToast(context, error)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = receiptViewModel.expenseItems.isNotEmpty() && !receiptViewModel.hasAnyValidationErrors(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = if (receiptViewModel.expenseItems.size > 1)
                                    stringResource(id = R.string.save_num_items, receiptViewModel.expenseItems.size)
                                else
                                    stringResource(id = R.string.save_item),
                                fontSize = 18.sp
                            )
                        }




                        Spacer(modifier = Modifier.height(12.dp))

                        // Cancel button
                        Button(
                            onClick = {
                                ttsManager?.speak("Cancelling")
                                // Show confirmation dialog if there are items to prevent accidental cancellation
                                if (receiptViewModel.expenseItems.isNotEmpty()) {
                                    // Set up the confirmation dialog state
                                    showCancelConfirmation = true
                                } else {
                                    // If no items, just navigate back directly
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(
                                text = stringResource(id = R.string.cancel),
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showCancelConfirmation) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmation = false },
                title = { Text(stringResource(id = R.string.discard_changes)) },
                text = { Text(stringResource(id = R.string.cancel_confirmation_message)
                    //"Are you sure you want to cancel? All expense items will be lost."
                ) },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelConfirmation = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(id = R.string.discard))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showCancelConfirmation = false }
                    ) {
                        Text(stringResource(id = R.string.keep_editing))
                    }
                }
            )
        }

        if (showLanguageSelector) {
            LanguageSelector(
                currentLanguageCode = currentLanguageCode,
                onLanguageSelected = { languageCode ->
                    currentLanguageCode = languageCode
                },
                onDismiss = { showLanguageSelector = false },
                activity = activity  // Pass the activity
            )
        }

        if (showAccessibilitySettings) {
            AccessibilitySettings(
                currentSettings = accessibilityState,
                onSettingsChanged = { newSettings ->
                    coroutineScope.launch {
                        accessibilityRepository.updateSettings(newSettings)
                    }
                },
                onDismiss = { showAccessibilitySettings = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableExpenseItemCard(
    item: ExpenseItem,
    availableCategories: List<String>,
    onCategoryChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
    onMerchantChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onDeleteItem: (ExpenseItem) -> Unit,
    receiptViewModel: ReceiptViewModel = viewModel() // Directly reference the viewModel to access validation state
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(item.description) { mutableStateOf(item.description) }
    var editedAmount by remember(item.amount) { mutableStateOf(item.amount.toString()) }
    var editedMerchant by remember(item.merchantName) { mutableStateOf(item.merchantName) }
    var editedDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(item.date)) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Access validation error states for this specific item
    val descriptionError = receiptViewModel.itemDescriptionErrors[item.id]
    val amountError = receiptViewModel.itemAmountErrors[item.id]
    val merchantError = receiptViewModel.itemMerchantErrors[item.id]
    val dateError = receiptViewModel.itemDateErrors[item.id]
    val hasErrors = receiptViewModel.itemHasErrors[item.id] ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row with edit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isEditing) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row {
                    // Delete button
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Edit button
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save Edits" else "Edit Item",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Delete confirmation dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text(text = stringResource(id = R.string.delete_item)) },
                    text = { Text(text = stringResource(id = R.string.delete_item_confirmation)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteItem(item)
                                showDeleteConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(text = stringResource(id = R.string.delete))
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = false }
                        ) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                    }
                )
            }

            if (isEditing) {
                // Merchant field
                OutlinedTextField(
                    value = editedMerchant,
                    onValueChange = {
                        editedMerchant = it
                        // Validate as user types
                        receiptViewModel.validateExpenseItem(item, null, null, it, null)
                    },
                    label = { Text(text = stringResource(id = R.string.merchant)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    isError = merchantError != null,
                    supportingText = {
                        merchantError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Date field
                OutlinedTextField(
                    value = editedDate,
                    onValueChange = {
                        editedDate = it
                        // Validate as user types
                        receiptViewModel.validateExpenseItem(item, null, null, null, it)
                    },
                    label = { Text(text = stringResource(id = R.string.date)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date format"
                        )
                    },
                    isError = dateError != null,
                    supportingText = {
                        dateError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        } ?: Text("Format: DD/MM/YYYY")
                    }
                )

                // Item name field
                OutlinedTextField(
                    value = editedName,
                    onValueChange = {
                        editedName = it
                        // Validate as user types
                        receiptViewModel.validateExpenseItem(item, it, null, null, null)
                    },
                    label = { Text(text = stringResource(id = R.string.item_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    isError = descriptionError != null,
                    supportingText = {
                        descriptionError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Amount field
                OutlinedTextField(
                    value = editedAmount,
                    onValueChange = {
                        editedAmount = it
                        // Validate as user types
                        receiptViewModel.validateExpenseItem(item, null, it, null, null)
                    },
                    label = { Text(text = stringResource(id = R.string.amount)) },
                    isError = amountError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    prefix = { Text("RM ") },
                    supportingText = {
                        amountError?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Category dropdown
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.cate, item.category),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { categoryMenuExpanded = true }
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

                // Save button
                Button(
                    onClick = {
                        // Perform final validation before saving
                        val isValid = receiptViewModel.validateExpenseItem(
                            item,
                            editedName,
                            editedAmount,
                            editedMerchant,
                            editedDate
                        )

                        if (isValid) {
                            onNameChange(editedName)
                            onMerchantChange(editedMerchant)
                            onDateChange(editedDate)
                            editedAmount.toDoubleOrNull()?.let { onAmountChange(it) }
                            isEditing = false
                        }
                    },
                    enabled = !hasErrors,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.save_changes))
                }
            } else {
                // Display mode - show item details
                // Merchant and date info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.from_colon, item.merchantName),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(item.date),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box {
                    Text(
                        text = stringResource(id = R.string.cate, item.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { categoryMenuExpanded = true }
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

                Spacer(modifier = Modifier.height(4.dp))

                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.subtotal),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "RM ${String.format("%.2f", item.amount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}