package com.example.taxapp.taxinformation

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.SpeakableContent
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import com.example.taxapp.multiLanguage.LanguageSelector
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxInformationScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    taxInfoViewModel: TaxInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? androidx.activity.ComponentActivity
    var showAccessibilitySettings by remember { mutableStateOf(false) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    // Access shared repositories
    val languageManager = remember { AppLanguageManager.getInstance(context) }

    // Observe the current language
    var currentLanguageCode by remember(languageManager.currentLanguageCode) {
        mutableStateOf(languageManager.getCurrentLanguageCode())
    }

    var showLanguageSelector by remember { mutableStateOf(false) }

    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val ttsManager = LocalTtsManager.current

    // Get user data
    LaunchedEffect(Unit) {
        taxInfoViewModel.loadUserIncome()
    }

    val userIncome = taxInfoViewModel.userIncome.collectAsState().value
    val isLoading = taxInfoViewModel.isLoading.collectAsState().value

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.tax_information),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
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

                    IconButton(onClick = {
                        ttsManager?.speak("Categories")
                        navController.navigate("category")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Category,
                            contentDescription = "Categories"
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
        LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
            Box(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    TaxInformationContent(
                        userIncome = userIncome,
                        isDarkMode = isDarkMode,
                        accessibleColors = accessibleColors
                    )
                }
            }

            // Language selector dialog
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
}

@Composable
fun TaxInformationContent(
    userIncome: Double,
    isDarkMode: Boolean,
    accessibleColors: com.example.taxapp.accessibility.AccessibleColors
) {
    val scrollState = rememberScrollState()
    val ttsManager = LocalTtsManager.current
    val context = LocalContext.current

    // Calculate tax based on user income
    val taxCalculation = calculateIncomeTax(userIncome)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User's Income Section
        SpeakableContent(text = stringResource(R.string.your_income_tax)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.your_income_tax),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.your_annual_income),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "RM ${String.format("%,.2f", userIncome)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.estimated_tax),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "RM ${String.format("%,.2f", taxCalculation.totalTaxAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.effective_tax_rate),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = String.format("%.2f%%", taxCalculation.effectiveTaxRate * 100),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tax Rate Table
        SpeakableContent(text = stringResource(R.string.malaysia_income_tax_rates)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.malaysia_income_tax_rates),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = accessibleColors.buttonBackground.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.chargeable_income),
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.buttonText
                        )
                        Text(
                            text = stringResource(R.string.rate),
                            modifier = Modifier.weight(0.5f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.buttonText,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.tax_rm),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.buttonText,
                            textAlign = TextAlign.End
                        )
                    }

                    // Tax brackets
                    getTaxBrackets().forEachIndexed { index, bracket ->
                        // Highlight user's tax bracket
                        val isUserBracket = userIncome > bracket.min &&
                                (bracket.max == null || userIncome <= bracket.max)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isUserBracket)
                                        accessibleColors.selectedDay.copy(alpha = 0.2f)
                                    else if (index % 2 == 0)
                                        accessibleColors.calendarBackground
                                    else
                                        accessibleColors.cardBackground.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            // Income range
                            Text(
                                text = formatIncomeRange(bracket.min, bracket.max),
                                modifier = Modifier.weight(1.5f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isUserBracket) FontWeight.Bold else FontWeight.Normal
                            )

                            // Rate
                            Text(
                                text = "${bracket.rate}%",
                                modifier = Modifier.weight(0.5f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isUserBracket) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )

                            // Tax amount on this bracket
                            Text(
                                text = if (isUserBracket && bracket.max != null) {
                                    val incomeInBracket = minOf(userIncome, bracket.max) - bracket.min
                                    val taxInBracket = incomeInBracket * bracket.rate / 100
                                    "RM ${String.format("%,.2f", taxInBracket)}"
                                } else if (isUserBracket) {
                                    val incomeInBracket = userIncome - bracket.min
                                    val taxInBracket = incomeInBracket * bracket.rate / 100
                                    "RM ${String.format("%,.2f", taxInBracket)}"
                                } else {
                                    "-"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isUserBracket) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // Tax Regulations Section
        SpeakableContent(text = stringResource(R.string.malaysia_tax_regulations)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors =
                    CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.malaysia_tax_regulations),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(R.string.tax_regulations_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Filing deadline
                    TaxRegulationItem(
                        title = stringResource(R.string.filing_deadline),
                        description = stringResource(R.string.filing_deadline_desc)
                    )

                    // Tax Reliefs
                    TaxRegulationItem(
                        title = stringResource(R.string.tax_reliefs),
                        description = stringResource(R.string.tax_reliefs_desc)
                    )

                    // Deductions
                    TaxRegulationItem(
                        title = stringResource(R.string.tax_deductions),
                        description = stringResource(R.string.tax_deductions_desc)
                    )

                    // Payment Methods
                    TaxRegulationItem(
                        title = stringResource(R.string.payment_methods),
                        description = stringResource(R.string.payment_methods_desc)
                    )

                    // PCB (Monthly Tax Deduction)
                    TaxRegulationItem(
                        title = stringResource(R.string.pcb),
                        description = stringResource(R.string.pcb_desc)
                    )
                }
            }
        }

        // Tax Relief Categories
        SpeakableContent(text = stringResource(R.string.tax_relief_categories)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tax_relief_categories),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(R.string.relief_categories_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Major tax relief categories
                    TaxReliefCategoryItem(
                        category = stringResource(R.string.lifestyle_expenses),
                        limit = "RM 2,500",
                        description = stringResource(R.string.lifestyle_expenses_desc)
                    )

                    TaxReliefCategoryItem(
                        category = stringResource(R.string.medical_expenses),
                        limit = "RM 8,000",
                        description = stringResource(R.string.medical_expenses_desc)
                    )

                    TaxReliefCategoryItem(
                        category = stringResource(R.string.education_fees),
                        limit = "RM 7,000",
                        description = stringResource(R.string.education_fees_desc)
                    )

                    TaxReliefCategoryItem(
                        category = stringResource(R.string.epf_contributions),
                        limit = "RM 4,000",
                        description = stringResource(R.string.epf_contributions_desc)
                    )

                    TaxReliefCategoryItem(
                        category = stringResource(R.string.life_insurance),
                        limit = "RM 3,000",
                        description = stringResource(R.string.life_insurance_desc)
                    )

                    TaxReliefCategoryItem(
                        category = stringResource(R.string.housing_loan_interest),
                        limit = "RM 10,000",
                        description = stringResource(R.string.housing_loan_interest_desc)
                    )
                }
            }
        }

        // Important notice at the bottom
        Text(
            text = stringResource(R.string.tax_information_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 16.dp, bottom = 1.dp) // Extra padding at bottom for scrollability
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.visit_official_tax_portal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        // Open URL in browser
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hasil.gov.my"))
                        context.startActivity(intent)
                        // Optional: Add a toast notification
                        Toast.makeText(context, "Opening official tax portal...", Toast.LENGTH_SHORT).show()
                    }
                    .padding(5.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TaxRegulationItem(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun TaxReliefCategoryItem(
    category: String,
    limit: String,
    description: String
) {
    val accessibleColors = LocalThemeColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = accessibleColors.cardBackground.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = limit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper function to format income range
fun formatIncomeRange(min: Double, max: Double?): String {
    return if (max == null) {
        "RM ${String.format("%,.2f", min)} and above"
    } else {
        "RM ${String.format("%,.2f", min)} - RM ${String.format("%,.2f", max)}"
    }
}

// Data class for tax brackets
data class TaxBracket(
    val min: Double,
    val max: Double?,
    val rate: Double
)

// Function to get Malaysia's income tax brackets for 2023
fun getTaxBrackets(): List<TaxBracket> {
    return listOf(
        TaxBracket(0.0, 5000.0, 0.0),
        TaxBracket(5000.0, 20000.0, 1.0),
        TaxBracket(20000.0, 35000.0, 3.0),
        TaxBracket(35000.0, 50000.0, 8.0),
        TaxBracket(50000.0, 70000.0, 13.0),
        TaxBracket(70000.0, 100000.0, 21.0),
        TaxBracket(100000.0, 250000.0, 24.0),
        TaxBracket(250000.0, 400000.0, 24.5),
        TaxBracket(400000.0, 600000.0, 25.0),
        TaxBracket(600000.0, 1000000.0, 26.0),
        TaxBracket(1000000.0, 2000000.0, 28.0),
        TaxBracket(2000000.0, null, 30.0)
    )
}

// Data class for tax calculation results
data class TaxCalculationResult(
    val totalTaxAmount: Double,
    val effectiveTaxRate: Double,
    val taxByBracket: Map<TaxBracket, Double>
)

// Function to calculate income tax based on the provided income
fun calculateIncomeTax(income: Double): TaxCalculationResult {
    var totalTaxAmount = 0.0
    val taxByBracket = mutableMapOf<TaxBracket, Double>()

    val brackets = getTaxBrackets()

    for (i in brackets.indices) {
        val bracket = brackets[i]

        if (income > bracket.min) {
            val taxableAmountInBracket = if (bracket.max != null && income > bracket.max) {
                bracket.max - bracket.min
            } else {
                income - bracket.min
            }

            val taxForBracket = taxableAmountInBracket * bracket.rate / 100
            totalTaxAmount += taxForBracket
            taxByBracket[bracket] = taxForBracket

            if (bracket.max == null || income <= bracket.max) {
                break
            }
        }
    }

    val effectiveTaxRate = if (income > 0) totalTaxAmount / income else 0.0

    return TaxCalculationResult(
        totalTaxAmount = totalTaxAmount,
        effectiveTaxRate = effectiveTaxRate,
        taxByBracket = taxByBracket
    )
}