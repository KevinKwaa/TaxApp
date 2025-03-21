package com.example.taxapp.receiptcategory

import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import com.example.taxapp.user.AppUtil
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    categoryViewModel: CategoryViewModel = viewModel()
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
    //ScreenReader("Home Screen")
    val ttsManager = LocalTtsManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.category),
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
                            tint = accessibleColors.headerText,
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
                            tint = accessibleColors.headerText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = {
                        ttsManager?.speak("Home")
                        navController.navigate("home")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Calendar")
                        navController.navigate("calendar")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Upload Receipt")
                        navController.navigate("uploadReceipt")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Receipt,
                            contentDescription = "Upload Receipt"
                        )
                    }

                    IconButton(onClick = { /* Already on Category */ }) {
                        Icon(
                            imageVector = Icons.Filled.Category,
                            contentDescription = "Categories",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Account")
                        navController.navigate("editProfile")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Account"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        CategoryScreenContent(
            modifier = modifier.padding(innerPadding),
            categoryViewModel = categoryViewModel,
            navController = navController
        )

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

    // Edit expense item dialog
    if (categoryViewModel.isEditingExpenseItem) {
        EditExpenseItemDialog(
            categoryViewModel = categoryViewModel,
            onSave = {
                categoryViewModel.saveEditedExpenseItem(
                    onSuccess = {
                        AppUtil.showToast(context, "Expense item updated successfully")
                    },
                    onError = { error ->
                        AppUtil.showToast(context, error)
                    }
                )
            },
            onCancel = {
                categoryViewModel.cancelEditingExpenseItem()
            }
        )
    }
}

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
    val availableYears = categoryViewModel.availableYears
    val selectedYear = categoryViewModel.selectedYear

    // Create scroll state for scrollable content
    val scrollState = rememberScrollState()

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
    //ScreenReader("Category Screen")
    val ttsManager = LocalTtsManager.current

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground)
                .padding(20.dp), // Match HomeScreen padding
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Year Selection Tabs
            if (availableYears.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = availableYears.indexOf(selectedYear).takeIf { it >= 0 } ?: 0,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = accessibleColors.headerText,
                        divider = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        availableYears.forEach { year ->
                            Tab(
                                selected = year == selectedYear,
                                onClick = {
                                    ttsManager?.speak("Selected year $year")
                                    categoryViewModel.setSelectedYear(year)
                                },
                                text = {
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Show loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
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
                    Image(
                        painter = painterResource(id = R.drawable.no_item_found),
                        contentDescription = "no item found",
                    )
                    Text(
                        text = stringResource(id = R.string.no_item_found),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        color = accessibleColors.headerText
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Define a common width for both buttons - can adjust the fraction as needed
                    val buttonModifier = Modifier
                        .fillMaxWidth(0.7f)  // Both buttons will use 70% of available width
                        .height(48.dp)       // Fixed height for consistency

                    ElevatedButton(
                        modifier = buttonModifier,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = accessibleColors.cardBackground),
                        onClick = { categoryViewModel.loadCategoryData() }
                    ) {
                        Text(
                            text = stringResource(id = R.string.retry),
                            color = accessibleColors.headerText
                        )
                    }

                    if (navController != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        ElevatedButton(
                            modifier = buttonModifier,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = accessibleColors.cardBackground),
                            onClick = { navController.navigate("uploadReceipt") }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Receipt",
                                modifier = Modifier.size(20.dp),
                                tint = accessibleColors.headerText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = R.string.add_receipt), color = accessibleColors.headerText)
                        }
                    }
                }
            }
            // Show category data
            else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Summary Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = Color.Black
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.categories_summary),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accessibleColors.headerText
                                )

                                // Display selected year
                                selectedYear?.let {
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "$it",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accessibleColors.headerText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(id = R.string.total_expenses,categoryViewModel.formatCurrency(categorySummary.values.sum())),
                                style = MaterialTheme.typography.titleMedium,
                                color = accessibleColors.headerText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(id = R.string.number_of_categories,categoryData.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accessibleColors.headerText
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val totalItems = categoryData.values.sumOf { it.size }
                            Text(
                                text = stringResource(id = R.string.number_of_expense_items, totalItems),
                                style = MaterialTheme.typography.bodyMedium,
                                color = accessibleColors.headerText
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
                            onToggleExpand = {
                                categoryViewModel.toggleCategoryExpansion(
                                    category
                                )
                            },
                            onEditExpenseItem = { item ->
                                categoryViewModel.startEditingExpenseItem(item)
                            },
                            onDeleteExpenseItem = { item ->
                                categoryViewModel.confirmDeleteExpenseItem(item)
                            },
                            formatCurrency = { amount -> categoryViewModel.formatCurrency(amount) },
                            formatDate = { date -> categoryViewModel.formatDate(date) }
                        )

                        //Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Clear Year Button (only show if there are categories to clear)
                    if (categoryData.isNotEmpty() && selectedYear != null) {
                        Button(
                            onClick = {
                                ttsManager?.speak("Clear all expenses for year $selectedYear")
                                categoryViewModel.confirmClearYear(selectedYear)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.clear_year_expenses, selectedYear),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Space to ensure bottom items are visible
                    Spacer(modifier = Modifier.height(24.dp))
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

        // Clear Year confirmation dialog
        if (categoryViewModel.showClearYearConfirmation) {
            // Prepare the success message here (in the composable context)
            val successMessage = stringResource(
                id = R.string.clear_year_success,
                categoryViewModel.yearToClear?.toString() ?: ""
            )

            AlertDialog(
                onDismissRequest = { categoryViewModel.cancelClearYear() },
                title = {
                    Text(text = stringResource(id = R.string.clear_year_confirm, categoryViewModel.yearToClear ?: ""))
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.clear_year_message, categoryViewModel.yearToClear ?: "")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            categoryViewModel.clearExpensesForYear(
                                onSuccess = {
                                    // Use the pre-created success message
                                    AppUtil.showToast(context, successMessage)
                                },
                                onError = { error ->
                                    AppUtil.showToast(context, error)
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(text = stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { categoryViewModel.cancelClearYear() }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            )
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

@Composable
fun CategoryItemsSection(
    category: String,
    expenseItems: List<CategoryViewModel.ExpenseItemWithReceipt>,
    totalAmount: Double,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditExpenseItem: (ExpenseItem) -> Unit,
    onDeleteExpenseItem: (ExpenseItem) -> Unit,
    formatCurrency: (Double) -> String,
    formatDate: (Date) -> String
) {
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)
    val accessibleColors = LocalThemeColors.current
    val ttsManager = LocalTtsManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor = Color.Black
        )

    ) {
        // Category Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ttsManager?.speak(if (isExpanded) "Collapsing category $category" else "Expanding category $category")
                        onToggleExpand()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category name and count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText
                    )

                    Text(
                        text = "${expenseItems.size} ${if (expenseItems.size == 1) stringResource(id = R.string.item) else stringResource(id = R.string.items)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = accessibleColors.headerText,
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
                        modifier = Modifier.rotate(rotationState),
                        tint = accessibleColors.headerText
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
                    HorizontalDivider()

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
                                formatCurrency = formatCurrency,
                                formatDate = formatDate,
                                onEdit = { onEditExpenseItem(expenseWithReceipt.item) },
                                onDelete = { onDeleteExpenseItem(expenseWithReceipt.item) }
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
    formatCurrency: (Double) -> String,
    formatDate: (Date) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accessibleColors = LocalThemeColors.current
    val ttsManager = LocalTtsManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = accessibleColors.headerText
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.from_colon, expenseItem.merchantName),
                            style = MaterialTheme.typography.bodySmall,
                            color = accessibleColors.headerText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatDate(expenseItem.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = accessibleColors.headerText
                        )
                    }
                }

                // Amount
                Text(
                    text = formatCurrency(expenseItem.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = accessibleColors.headerText
                )

                // Action buttons
                Row {
                    // Edit button
                    IconButton(
                        onClick = {
                            ttsManager?.speak("Editing expense item ${expenseItem.description}")
                            onEdit()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = {
                            ttsManager?.speak("Deleting expense item ${expenseItem.description}")
                            onDelete()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
            Text(text = stringResource(id = R.string.delete_confirm))
        },
        text = {
            Text(
                text = if (isExpenseItem)
                    stringResource(id = R.string.delete_item_message)
                //"Are you sure you want to delete this expense item? This action cannot be undone."
                else
                    stringResource(id = R.string.delete_receipt_message)
                //"Are you sure you want to delete this receipt? This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseItemDialog(
    categoryViewModel: CategoryViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        val scrollState = rememberScrollState()

        // Validation states
        var isDescriptionValid by remember { mutableStateOf(true) }
        var isMerchantValid by remember { mutableStateOf(true) }
        var isDateValid by remember { mutableStateOf(true) }
        var isAmountValid by remember { mutableStateOf(true) }
        var isCategoryValid by remember { mutableStateOf(true) }

        // Error messages
        var descriptionError by remember { mutableStateOf("") }
        var merchantError by remember { mutableStateOf("") }
        var dateError by remember { mutableStateOf("") }
        var amountError by remember { mutableStateOf("") }
        var categoryError by remember { mutableStateOf("") }

        // Pre-load string resources
        val errorEmptyDescription = stringResource(id = R.string.error_empty_description)
        val errorEmptyMerchant = stringResource(id = R.string.error_empty_merchant)
        val errorInvalidDateFormat = stringResource(id = R.string.error_invalid_date_format)
        val errorInvalidAmount = stringResource(id = R.string.error_invalid_amount)
        val errorNegativeAmount = stringResource(id = R.string.error_negative_amount)
        val errorEmptyCategory = stringResource(id = R.string.error_empty_category)

        // Validation functions
        val validateDescription = {
            val valid = categoryViewModel.editExpenseDescription.trim().isNotEmpty()
            isDescriptionValid = valid
            descriptionError = if (!valid) errorEmptyDescription else ""
            valid
        }

        val validateMerchant = {
            val valid = categoryViewModel.editExpenseMerchant.trim().isNotEmpty()
            isMerchantValid = valid
            merchantError = if (!valid) errorEmptyMerchant else ""
            valid
        }

        val validateDate = {
            val valid = ValidationUtils.isDateValid(categoryViewModel.editExpenseDate)
            isDateValid = valid
            dateError = if (!valid) errorInvalidDateFormat else ""
            valid
        }

        val validateAmount = {
            val amountDouble = categoryViewModel.editExpenseAmount.replace(",", ".").toDoubleOrNull()
            val valid = amountDouble != null && amountDouble > 0
            isAmountValid = valid
            amountError = if (!valid) {
                if (amountDouble == null) errorInvalidAmount else errorNegativeAmount
            } else ""
            valid
        }

        val validateCategory = {
            val valid = categoryViewModel.editExpenseCategory.trim().isNotEmpty()
            isCategoryValid = valid
            categoryError = if (!valid) errorEmptyCategory else ""
            valid
        }

        val ttsManager = LocalTtsManager.current

        // Initial validation
        LaunchedEffect(Unit) {
            validateDescription()
            validateMerchant()
            validateDate()
            validateAmount()
            validateCategory()
        }

        // Validate all fields
        fun validateAllFields(): Boolean {
            return validateDescription() &&
                    validateMerchant() &&
                    validateDate() &&
                    validateAmount() &&
                    validateCategory()
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 450.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Dialog title
                Text(
                    text = stringResource(id = R.string.edit_expense_item),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Item Description
                OutlinedTextField(
                    value = categoryViewModel.editExpenseDescription,
                    onValueChange = {
                        categoryViewModel.editExpenseDescription = it
                        validateDescription()
                    },
                    label = { Text(text = stringResource(id = R.string.item_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isDescriptionValid,
                    supportingText = {
                        if (!isDescriptionValid) {
                            Text(text = descriptionError)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Merchant Name
                OutlinedTextField(
                    value = categoryViewModel.editExpenseMerchant,
                    onValueChange = {
                        categoryViewModel.editExpenseMerchant = it
                        validateMerchant()
                    },
                    label = { Text(stringResource(id = R.string.merchant)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isMerchantValid,
                    supportingText = {
                        if (!isMerchantValid) {
                            Text(text = merchantError)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date
                OutlinedTextField(
                    value = categoryViewModel.editExpenseDate,
                    onValueChange = {
                        categoryViewModel.editExpenseDate = it
                        validateDate()
                    },
                    label = { Text(stringResource(id = R.string.date)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isDateValid,
                    supportingText = {
                        if (!isDateValid) {
                            Text(text = dateError)
                        } else {
                            Text(text = stringResource(id = R.string.date_format_hint))
                        }
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date Format"
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Amount
                OutlinedTextField(
                    value = categoryViewModel.editExpenseAmount,
                    onValueChange = {
                        categoryViewModel.editExpenseAmount = it
                        validateAmount()
                    },
                    label = { Text(stringResource(id = R.string.amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isAmountValid,
                    supportingText = {
                        if (!isAmountValid) {
                            Text(text = amountError)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Dropdown
                var isExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = categoryViewModel.editExpenseCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.receipt_category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = !isCategoryValid,
                        supportingText = {
                            if (!isCategoryValid) {
                                Text(text = categoryError)
                            }
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false },
                        modifier = Modifier.exposedDropdownSize()
                    ) {
                        categoryViewModel.availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    categoryViewModel.editExpenseCategory = category
                                    validateCategory()
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (validateAllFields()) {
                                ttsManager?.speak("Saving expense item changes")
                                onSave()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isDescriptionValid && isMerchantValid && isDateValid && isAmountValid && isCategoryValid
                    ) {
                        Text(stringResource(id = R.string.save))
                    }
                }

                // Add bottom spacing for scrollable content
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}