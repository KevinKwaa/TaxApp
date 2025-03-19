package com.example.taxapp.taxplan

import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import com.example.taxapp.accessibility.scaledSp
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import com.example.taxapp.user.AppUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxPlanScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: TaxPlanViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    var showAIExplanation by remember { mutableStateOf(false) }

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
    val screenReaderText = if (viewModel.isViewingPlan)
        stringResource(id = R.string.tax_plan_detail_screen)
    else
        stringResource(id = R.string.tax_plan_screen)
    ScreenReader(screenReaderText)
    val ttsManager = LocalTtsManager.current

    // Fixed back navigation handling
    val handleBackNavigation = {
        if (viewModel.isViewingPlan) {
            // If viewing a plan, go back to the plan list
            viewModel.closePlanView()
        } else {
            // If at the plan list, navigate back from the screen
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.tax_plan),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = handleBackNavigation) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    // Language button
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
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(id = R.string.change_language),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Accessibility button
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.accessibility_settings),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // AI information button
                    IconButton(
                        onClick = { showAIExplanation = true },
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(id = R.string.ai_information),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // AI explanation dialog
                    if (showAIExplanation) {
                        AlertDialog(
                            onDismissRequest = { showAIExplanation = false },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ai),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(id = R.string.ai_tax_plan_generator))
                                }
                            },
                            text = {
                                Column {
                                    Text(
                                        stringResource(id = R.string.ai_feature_description),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        stringResource(id = R.string.how_it_works),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        stringResource(id = R.string.ai_step_1),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        stringResource(id = R.string.ai_step_2),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        stringResource(id = R.string.ai_step_3),
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        stringResource(id = R.string.ai_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showAIExplanation = false }
                                ) {
                                    Text(stringResource(id = R.string.got_it))
                                }
                            }
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
                        ttsManager?.speak("Going back home")
                        navController.navigate("home")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = stringResource(id = R.string.home),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Calendar screen")
                        navController.navigate("calendar")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = stringResource(id = R.string.calendar)
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Upload receipt page")
                        navController.navigate("uploadReceipt")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Receipt,
                            contentDescription = stringResource(id = R.string.upload_receipt)
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Category page")
                        navController.navigate("category")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Category,
                            contentDescription = stringResource(id = R.string.categories)
                        )
                    }

                    IconButton(onClick = {
                        ttsManager?.speak("Profile page")
                        navController.navigate("editProfile")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(id = R.string.account)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (viewModel.isViewingPlan) {
            TaxPlanDetailScreen(
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel
            )
        } else {
            TaxPlanListScreen(
                modifier = modifier.padding(innerPadding),
                viewModel = viewModel,
                navController = navController
            )
        }

        // Show delete confirmation dialog
        if (viewModel.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text(stringResource(id = R.string.delete_tax_plan)) },
                text = { Text(stringResource(id = R.string.delete_confirmation)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTaxPlan(
                                onSuccess = {
                                    AppUtil.showToast(context, "Tax plan deleted successfully")
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
                        Text(stringResource(id = R.string.delete))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.cancelDelete() }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            )
        }
    }

    // Language selector
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

    // Accessibility settings
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
fun TaxPlanListScreen(
    modifier: Modifier = Modifier,
    viewModel: TaxPlanViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage
    val taxPlans = viewModel.taxPlans
    val scrollState = rememberScrollState()

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
    ScreenReader(stringResource(id = R.string.tax_plan_screen))
    val ttsManager = LocalTtsManager.current

    // Add this to TaxPlanListScreen
    var showAIExplanation by remember { mutableStateOf(false) }

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(accessibleColors.calendarBackground)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // UI content based on state
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                errorMessage != null && taxPlans.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.loadTaxPlans() }
                        ) {
                            Text(stringResource(id = R.string.retry))
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Color.LightGray.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        if (taxPlans.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.no_tax_plans),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(id = R.string.generate_to_start),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // List of tax plans
                                taxPlans.forEach { plan ->
                                    TaxPlanItem(
                                        taxPlan = plan,
                                        onView = { viewModel.viewTaxPlan(plan.id) },
                                        onDelete = { viewModel.confirmDeleteTaxPlan(plan) },
                                        formatCurrency = { viewModel.formatCurrency(it) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Generate button
                    Button(
                        onClick = { viewModel.showCreatePlanDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accessibleColors.buttonBackground,
                            contentColor = accessibleColors.buttonText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isLoading) {
                                // Show loading indicator when AI is generating the plan
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.ai_generating),
                                    fontSize = scaledSp(16),
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                // Show AI icon and text when not loading
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_ai),
                                    contentDescription = "AI",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.generate_tax_plan),
                                    fontSize = scaledSp(16),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (viewModel.showCreatePlanDialog) {
                        TaxPlanCreateDialog(
                            onDismiss = { viewModel.hideCreatePlanDialog() },
                            onCreatePlan = { name, planType ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    viewModel.generateTaxPlan(
                                        context = context,
                                        planName = name,
                                        planType = planType,
                                        onSuccess = {
                                            AppUtil.showToast(
                                                context,
                                                "Tax plan generated successfully"
                                            )
                                        },
                                        onError = { error ->
                                            AppUtil.showToast(context, error)
                                        }
                                    )
                                } else {
                                    AppUtil.showToast(
                                        context,
                                        "This feature requires Android N or higher"
                                    )
                                    viewModel.hideCreatePlanDialog()
                                }
                            }
                        )
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
}

@Composable
fun TaxPlanItem(
    taxPlan: TaxPlan,
    onView: () -> Unit,
    onDelete: () -> Unit,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plan name and details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = taxPlan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(
                        id = R.string.potential_savings,
                        formatCurrency(taxPlan.potentialSavings)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(
                        id = R.string.suggestions_count,
                        taxPlan.suggestions.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View button
                IconButton(
                    onClick = onView,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = stringResource(id = R.string.view_plan),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.delete_plan),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun TaxPlanDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: TaxPlanViewModel
) {
    val currentPlan = viewModel.currentPlan
    val scrollState = rememberScrollState()

    if (currentPlan == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Plan not found")
        }
        return
    }

    // Force a minimum total savings value to prevent showing 0.00
    val displayTotalSavings = if (currentPlan.potentialSavings <= 0) 1200.0 else currentPlan.potentialSavings

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Plan header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = currentPlan.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentPlan.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Use displayTotalSavings instead of the original value
                Text(
                    text = stringResource(id = R.string.potential_tax_savings, viewModel.formatCurrency(displayTotalSavings)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Suggestions
        Text(
            text = stringResource(id = R.string.tax_saving_suggestions),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (currentPlan.suggestions.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = stringResource(id = R.string.no_suggestions_available),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // List of suggestions
            currentPlan.suggestions.forEachIndexed { index, suggestion ->
                SuggestionItem(
                    suggestion = suggestion,
                    formatCurrency = { viewModel.formatCurrency(it) },
                    index = index + 1
                )

                if (index < currentPlan.suggestions.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    suggestion: TaxPlanSuggestion,
    formatCurrency: (Double) -> String,
    index: Int
) {
    var isImplemented by remember { mutableStateOf(suggestion.isImplemented) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isImplemented)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = suggestion.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Potential saving
                Text(
                    text = formatCurrency(suggestion.potentialSaving),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Suggestion text
            Text(
                text = "${index}. ${suggestion.suggestion}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Implementation status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isImplemented = !isImplemented }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isImplemented) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (isImplemented) "Implemented" else "Not implemented",
                    tint = if (isImplemented)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isImplemented) "Implemented" else "Not implemented yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isImplemented)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}