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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
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

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.receipt_summary),
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
        Log.d("ReceiptSummaryContent", "Receipt data available: URI=${receiptViewModel.currentReceiptUri}, merchant=${receiptViewModel.merchantName}, total=${receiptViewModel.total}")
    }

    // Get the custom colors
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    ScreenReader("Receipt Screen")
    val ttsManager = LocalTtsManager.current
    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // Language button with improved styling
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        accessibleColors.buttonBackground.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = accessibleColors.calendarBorder,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { showLanguageSelector = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🌐",
                    style = MaterialTheme.typography.titleMedium,
                    color = accessibleColors.buttonText
                )
            }

            // Accessibility button with improved styling
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        accessibleColors.buttonBackground.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = accessibleColors.calendarBorder,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { showAccessibilitySettings = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚙️",
                    style = MaterialTheme.typography.titleMedium,
                    color = accessibleColors.buttonText
                )
            }
        }

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
                        text = stringResource(id = R.string.error, error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        ttsManager?.speak("Going Back")
                        navController.navigateUp() }
                    ) {
                        Text(text = stringResource(id = R.string.back),)
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
            modifier = modifier
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
                            .height(200.dp)
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

            // Receipt details
            item {
                ReceiptDetailsSection(receiptViewModel)
            }

            // Expense items
            item {
                Text(
                    text = stringResource(id = R.string.expense_items),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
                    onDeleteItem = { expenseItem ->
                        // Delete this expense item
                        receiptViewModel.deleteExpenseItem(expenseItem)
                    }
                )
            }

            // Save button
            item {
                Button(
                    onClick = {
                        ttsManager?.speak("Saving Receipt")
                        receiptViewModel.saveReceipt(
                            onSuccess = { receiptId ->
                                AppUtil.showToast(context, "Receipt saved successfully")
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
                        .padding(vertical = 16.dp)
                        .height(56.dp)
                ) {
                    Text(text = stringResource(id = R.string.confirm_save), fontSize = 18.sp)
                }
            }
        }

        if (isDarkMode) {
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.bodyMedium,
                color = accessibleColors.calendarText.copy(alpha = 0.7f)
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
fun ReceiptDetailsSection(receiptViewModel: ReceiptViewModel) {
    var isEditingMerchant by remember { mutableStateOf(false) }
    var isEditingTotal by remember { mutableStateOf(false) }
    var isEditingDate by remember { mutableStateOf(false) }
    var isEditingCategory by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

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
    ScreenReader("Receipt Screen")
    val ttsManager = LocalTtsManager.current

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // Language button with improved styling
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        accessibleColors.buttonBackground.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = accessibleColors.calendarBorder,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { showLanguageSelector = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🌐",
                    style = MaterialTheme.typography.titleMedium,
                    color = accessibleColors.buttonText
                )
            }

            // Accessibility button with improved styling
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        accessibleColors.buttonBackground.copy(alpha = 0.8f),
                        CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = accessibleColors.calendarBorder,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { showAccessibilitySettings = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⚙️",
                    style = MaterialTheme.typography.titleMedium,
                    color = accessibleColors.buttonText
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.receipt_details),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Merchant Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Merchant",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isEditingMerchant) {
                        OutlinedTextField(
                            value = receiptViewModel.merchantName,
                            onValueChange = { receiptViewModel.merchantName = it },
                            label = { Text(text = stringResource(id = R.string.merchant_name),) },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingMerchant = false }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(
                                id = R.string.merchant_colon,
                                receiptViewModel.merchantName
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingMerchant = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Merchant",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isEditingDate) {
                        OutlinedTextField(
                            value = receiptViewModel.date,
                            onValueChange = { receiptViewModel.date = it },
                            label = { Text(text = stringResource(id = R.string.date),) },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingDate = false }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(id = R.string.date_colon, receiptViewModel.date),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingDate = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.rm),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    if (isEditingTotal) {
                        OutlinedTextField(
                            value = receiptViewModel.total,
                            onValueChange = { receiptViewModel.total = it },
                            label = { Text(text = stringResource(id = R.string.total_amount),) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        IconButton(onClick = { isEditingTotal = false }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(
                                id = R.string.total_colon,
                                receiptViewModel.total
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = { isEditingTotal = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Total",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (isEditingCategory) {
                            OutlinedTextField(
                                value = receiptViewModel.category,
                                onValueChange = {},
                                label = { Text(text = stringResource(id = R.string.category),) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { categoryMenuExpanded = true }) {
                                        Icon(Icons.Default.Edit, "Select Category")
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                receiptViewModel.availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            receiptViewModel.category = category
                                            categoryMenuExpanded = false
                                            isEditingCategory = false
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(
                                    id = R.string.cate,
                                    receiptViewModel.category
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    if (!isEditingCategory) {
                        IconButton(onClick = { isEditingCategory = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Category",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = { isEditingCategory = false }) {
                            Text(text = stringResource(id = R.string.done),)
                        }
                    }
                }
            }
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
    onDeleteItem: (ExpenseItem) -> Unit
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(item.description) { mutableStateOf(item.description) }
    var editedAmount by remember(item.amount) { mutableStateOf(item.amount.toString()) }
    var isValidAmount by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

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
                            imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Cancel Editing" else "Edit Item",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Delete confirmation dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text(text = stringResource(id = R.string.delete_item),) },
                    text = { Text(text = stringResource(id = R.string.delete_confirmation),) },
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
                            Text(text = stringResource(id = R.string.delete),)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = false }
                        ) {
                            Text(text = stringResource(id = R.string.cancel),)
                        }
                    }
                )
            }

            if (isEditing) {
                // Edit mode - show text fields
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text(text = stringResource(id = R.string.item_description),) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = editedAmount,
                    onValueChange = {
                        editedAmount = it
                        isValidAmount = it.toDoubleOrNull() != null
                    },
                    label = { Text(text = stringResource(id = R.string.amount),) },
                    isError = !isValidAmount,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                if (!isValidAmount) {
                    Text(
                        text = stringResource(id = R.string.valid_amount), //please enter a valid amount
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Category dropdown
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.cate,item.category),
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
                        if (isValidAmount) {
                            onNameChange(editedName)
                            editedAmount.toDoubleOrNull()?.let { onAmountChange(it) }
                            isEditing = false
                        }
                    },
                    enabled = isValidAmount,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.save_changes),)
                }
            } else {
                // Display mode - show item details
                Box {
                    Text(
                        text = stringResource(id = R.string.cate,item.category),
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