package com.example.taxapp.taxplan

import android.content.Context
import android.util.Log
import com.example.taxapp.BuildConfig
import com.example.taxapp.utils.NetworkUtil
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Service class for generating tax plans using Google's Gemini AI API.
 */
class GeminiTaxPlanService(private val context: Context) {
    private val TAG = "GeminiTaxPlanService"

    // Initialize the Gemini GenerativeModel
    private val generativeModel by lazy {
        val requestOptions = RequestOptions(apiVersion = "v1")

        try {
            Log.d(TAG, "Initializing Gemini model for tax planning")
            GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                requestOptions = requestOptions,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing GenerativeModel", e)
            null
        }
    }

    /**
     * Generate tax plan based on user income and other profile data
     *
     * @param income User's income (as string)
     * @param employmentType User's employment type ("employee" or "self-employed")
     * @return Result object containing either tax plan data or error message
     */
    suspend fun generateTaxPlan(income: String, employmentType: String, name: String = "", planType: String = "standard"): Result<TaxPlan> = withContext(Dispatchers.IO) {
        try {
            // Validate model and API key
            if (generativeModel == null || BuildConfig.GEMINI_API_KEY.isBlank()) {
                Log.e(TAG, "GenerativeModel is null or API key is blank")
                return@withContext Result.failure(Exception("AI service unavailable"))
            }

            // Check network availability
            if (!NetworkUtil.isOnline(context)) {
                Log.e(TAG, "Network unavailable")
                return@withContext Result.failure(Exception("Internet connection required"))
            }

            // Parse income as Double, defaulting to 0.0 if invalid
            val incomeValue = income.toDoubleOrNull() ?: 0.0

            // Adjust income based on plan type
            val adjustedIncome = when (planType) {
                "future" -> incomeValue * 1.2 // 20% higher for future planning
                else -> incomeValue
            }

            // Build a comprehensive prompt
            val prompt = buildTaxPlanPrompt(adjustedIncome, employmentType, name, planType)
            Log.d(TAG, "Tax plan prompt: $prompt")

            // Generate content with explicit error handling
            val response = try {
                Log.d(TAG, "Calling Gemini API for tax plan generation")
                generativeModel!!.generateContent(prompt)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating content from Gemini", e)
                return@withContext Result.failure(e)
            }

            // Log raw AI response
            Log.d(TAG, "Raw AI response: ${response.text?.take(500)}...")

            // Parse the AI response into a tax plan
            val taxPlan = parseTaxPlanResponse(response, adjustedIncome, employmentType, planType)

            // Validate the tax plan
            val validatedPlan = validateTaxPlan(taxPlan, adjustedIncome, employmentType, planType)

            Result.success(validatedPlan)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in generateTaxPlan", e)
            Result.failure(e)
        }
    }

    /**
     * Build a prompt for the AI to generate a tax plan
     */
    private fun buildTaxPlanPrompt(income: Double, employmentType: String, name: String = "", planType: String = "standard"): String {
        val greeting = if (name.isNotBlank()) "for $name" else ""
        val planTypeDesc = when (planType) {
            "future" -> "future income planning (assuming 20% income growth)"
            "business" -> "business venture planning"
            else -> "standard tax planning"
        }

        return """
        You are a Malaysian Tax Planning Expert AI for a tax app. Generate a detailed, personalized tax plan for ${planTypeDesc} ${greeting} with the following information:
        
        User's Annual Income: RM ${income}
        Employment Type: ${employmentType}
        
        TASK:
        Create a comprehensive tax plan with:
        1. A brief analysis of the user's tax situation based on their income level and employment type
        2. 5-7 actionable tax-saving suggestions organized by specific categories
        3. Realistic estimated potential savings for each suggestion in Malaysian Ringgit (RM)
        
        DETAILED MALAYSIAN TAX CONTEXT:
        - Individual income tax rates (2024):
          * First RM5,000: 0%
          * RM5,001-RM20,000: 1%
          * RM20,001-RM35,000: 3%
          * RM35,001-RM50,000: 8%
          * RM50,001-RM70,000: 13%
          * RM70,001-RM100,000: 21%
          * RM100,001-RM250,000: 24%
          * RM250,001-RM400,000: 24.5%
          * RM400,001-RM600,000: 25%
          * RM600,001-RM1,000,000: 26%
          * Above RM1,000,000: 30%
          
        - Important tax relief categories include:
          * Personal relief: RM9,000
          * EPF/KWSP contributions: Up to RM4,000
          * Life insurance premiums: Up to RM3,000
          * Medical and education insurance: Up to RM3,000
          * Lifestyle relief: Up to RM2,500
          * Medical expenses for self, spouse, or children: Up to RM8,000
          * Education fees (self): Up to RM7,000
          * SOCSO/PERKESO contributions: Up to RM350
          * SSPN education savings: Up to RM8,000
          * Housing loan interest: Up to RM10,000 (for first 3 years)
          * Donations to approved institutions: Varies
          
        - For self-employed:
          * Business expenses are deductible
          * Home office deductions are available
          * Voluntary EPF and SOCSO contributions are tax-deductible
          
        YOUR RESPONSE FORMAT:
        1. First, provide a 2-3 sentence personal tax analysis.
        2. Then provide tax-saving suggestions in this precise format:
           - Category: [specific category name]
           - Suggestion: [detailed actionable advice with specific amounts and steps]
           - Potential Savings: RM [realistic amount]
        3. After all suggestions, provide a total potential savings amount.
        
        IMPORTANT GUIDELINES:
        - MAKE ALL SUGGESTIONS SPECIFIC AND ACTIONABLE.
        - ENSURE EACH SUGGESTION HAS A REALISTIC SAVINGS AMOUNT IN RM.
        - Savings amounts must be proportional to income and tax bracket.
        - For employee (${employmentType == "employee"}), focus on tax reliefs.
        - For self-employed (${employmentType == "self-employed"}), focus more on business deductions.
        - ${if (planType == "future") "Focus on strategies for future income growth and tax efficiency with long-term planning." else ""}
        - ${if (planType == "business") "Focus on business tax optimization strategies, capital investments, and business expansion tax considerations." else ""}
        - ALWAYS CALCULATE TOTAL POTENTIAL SAVINGS AT THE END.
        - The total savings must make logical sense and never be zero.
        
        EXAMPLES OF GOOD SUGGESTIONS:
        - Category: EPF Contribution
          Suggestion: Maximize your EPF contribution to the 11% employee statutory rate. For your income level of RM${income}, this means contributing approximately RM${(income * 0.11).toInt()} annually to EPF.
          Potential Savings: RM${(income * 0.11 * 0.08).toInt()} (based on your tax bracket)
        
        - Category: Lifestyle Relief
          Suggestion: Fully utilize the RM2,500 lifestyle relief by keeping receipts for books, sports equipment, internet subscription, and electronic devices.
          Potential Savings: RM${(2500 * 0.08).toInt()} (based on your tax bracket)
        
        MAKE SURE ALL POTENTIAL SAVINGS ADD UP CORRECTLY IN THE FINAL TOTAL.
        """.trimIndent()
    }

    /**
     * Parse the AI response into a structured TaxPlan object
     */
    private fun parseTaxPlanResponse(response: GenerateContentResponse, income: Double, employmentType: String, planType: String): TaxPlan {
        val responseText = response.text ?: ""

        // Default values in case parsing fails
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val planName = when (planType) {
            "future" -> "Future Income Tax Plan (${dateFormat.format(Date())})"
            "business" -> "Business Tax Plan (${dateFormat.format(Date())})"
            else -> "Tax Plan (${dateFormat.format(Date())})"
        }

        // Extract the first paragraph as the description
        var planDescription = responseText.split("\n\n").firstOrNull { it.isNotBlank() } ?:
        "AI-generated tax plan based on your income profile"

        // Limit description length
        if (planDescription.length > 200) {
            planDescription = planDescription.take(197) + "..."
        }

        val suggestions = mutableListOf<TaxPlanSuggestion>()
        var totalSavings = 0.0

        try {
            // First, try to extract using structured pattern matching
            extractSuggestionsWithRegex(responseText, suggestions)

            // If that fails, try alternative parsing method
            if (suggestions.isEmpty()) {
                extractSuggestionsWithTextSearch(responseText, suggestions)
            }

            // Try to extract the total savings
            extractTotalSavings(responseText)?.let {
                totalSavings = it
            }

            // If still no suggestions or unreasonable total, create defaults
            if (suggestions.isEmpty() || totalSavings <= 0) {
                // Clear any partial results
                suggestions.clear()

                // Generate default suggestions
                generateComprehensiveSuggestions(suggestions, income, employmentType, planType)

                // Recalculate total savings
                totalSavings = suggestions.sumOf { it.potentialSaving }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            // Fall back to default suggestions if parsing fails
            suggestions.clear()
            generateComprehensiveSuggestions(suggestions, income, employmentType, planType)
            totalSavings = suggestions.sumOf { it.potentialSaving }
        }

        return TaxPlan(
            id = UUID.randomUUID().toString(),
            name = planName,
            description = planDescription,
            suggestions = suggestions,
            potentialSavings = totalSavings,
            planType = planType
        )
    }

    /**
     * Extract suggestions using regex patterns
     */
    private fun extractSuggestionsWithRegex(text: String, suggestions: MutableList<TaxPlanSuggestion>) {
        // More robust regex patterns
        val patterns = listOf(
            // Pattern 1: Category: [text] Suggestion: [text] Potential Savings: RM [number]
            "(?i)Category:[ \t]*([^\n]+)\\s+Suggestion:[ \t]*([^\n]+(?:\n(?!Category:|Potential)[^\n]+)*)\\s+Potential Savings:[ \t]*RM[ \t]*([$0-9,.]+)",

            // Pattern 2: Category: [text] - Suggestion: [text] - Potential Savings: RM [number]
            "(?i)Category:[ \t]*([^\n-]+)[ \t]*-[ \t]*Suggestion:[ \t]*([^-\n]+(?:\n(?!-[ \t]*Potential)[^-\n]+)*)[ \t]*-[ \t]*Potential Savings:[ \t]*RM[ \t]*([$0-9,.]+)",

            // Pattern 3: - Category: [text] - Suggestion: [text] - Potential Savings: RM [number]
            "(?i)-[ \t]*Category:[ \t]*([^\n-]+)[ \t]*-[ \t]*Suggestion:[ \t]*([^-\n]+(?:\n(?!-[ \t]*Potential)[^-\n]+)*)[ \t]*-[ \t]*Potential Savings:[ \t]*RM[ \t]*([$0-9,.]+)"
        )

        for (pattern in patterns) {
            val regex = pattern.toRegex(setOf(RegexOption.DOT_MATCHES_ALL))
            val matches = regex.findAll(text)

            for (match in matches) {
                if (match.groupValues.size >= 4) {
                    val category = match.groupValues[1].trim()
                    val suggestion = match.groupValues[2].trim()

                    // Parse the saving amount, removing commas and currency symbols
                    val savingText = match.groupValues[3].replace("[^0-9.]".toRegex(), "")
                    val saving = try {
                        savingText.toDoubleOrNull() ?: 0.0
                    } catch (e: Exception) {
                        0.0
                    }

                    // Only add if we have meaningful data and the saving is reasonable
                    if (category.isNotBlank() && suggestion.isNotBlank() && saving > 0) {
                        suggestions.add(TaxPlanSuggestion(
                            id = UUID.randomUUID().toString(),
                            category = category,
                            suggestion = suggestion,
                            potentialSaving = saving,
                            isImplemented = false
                        ))
                    }
                }
            }

            // If we found suggestions with this pattern, stop trying more patterns
            if (suggestions.isNotEmpty()) {
                Log.d(TAG, "Found ${suggestions.size} suggestions using pattern: $pattern")
                break
            }
        }
    }

    /**
     * Extract suggestions using simpler text search
     */
    private fun extractSuggestionsWithTextSearch(text: String, suggestions: MutableList<TaxPlanSuggestion>) {
        // Look for specific keywords and patterns that might indicate a suggestion
        val lines = text.split("\n")

        var currentCategory = ""
        var currentSuggestion = ""
        var inSuggestion = false

        for (line in lines) {
            val trimmedLine = line.trim()

            when {
                // Category line
                trimmedLine.startsWith("Category:", ignoreCase = true) -> {
                    if (inSuggestion && currentCategory.isNotBlank() && currentSuggestion.isNotBlank()) {
                        // Try to find a saving amount in the current suggestion
                        extractSavingAmount(currentSuggestion)?.let { saving ->
                            if (saving > 0) {
                                suggestions.add(TaxPlanSuggestion(
                                    category = currentCategory,
                                    suggestion = currentSuggestion,
                                    potentialSaving = saving
                                ))
                            }
                        }
                    }

                    currentCategory = trimmedLine.substringAfter("Category:", "").trim()
                    currentSuggestion = ""
                    inSuggestion = true
                }

                // Suggestion line
                trimmedLine.startsWith("Suggestion:", ignoreCase = true) -> {
                    currentSuggestion = trimmedLine.substringAfter("Suggestion:", "").trim()
                }

                // Potential Savings line
                trimmedLine.contains("Potential Savings:", ignoreCase = true) ||
                        trimmedLine.contains("Savings:", ignoreCase = true) -> {
                    val savingText = trimmedLine.substringAfter("RM", "").trim()
                    val saving = savingText.replace(",", "").toDoubleOrNull() ?: 0.0

                    if (currentCategory.isNotBlank() && currentSuggestion.isNotBlank() && saving > 0) {
                        suggestions.add(TaxPlanSuggestion(
                            category = currentCategory,
                            suggestion = currentSuggestion,
                            potentialSaving = saving
                        ))

                        // Reset for next suggestion
                        currentCategory = ""
                        currentSuggestion = ""
                        inSuggestion = false
                    }
                }

                // Continuation of suggestion text
                inSuggestion && !trimmedLine.startsWith("Potential Savings:", ignoreCase = true) &&
                        !trimmedLine.contains("Savings:", ignoreCase = true) && trimmedLine.isNotEmpty() -> {
                    currentSuggestion += " " + trimmedLine
                }
            }
        }
    }

    /**
     * Extract a saving amount from text
     */
    private fun extractSavingAmount(text: String): Double? {
        val regex = "RM\\s*([$0-9,.]+)".toRegex()
        val match = regex.find(text)

        return match?.let {
            try {
                it.groupValues[1].replace(",", "").toDoubleOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Extract the total potential savings from the response
     */
    private fun extractTotalSavings(text: String): Double? {
        val patterns = listOf(
            "Total[\\s\\w]*Savings:?\\s*RM\\s*([\\d,.]+)",
            "Total:?\\s*RM\\s*([\\d,.]+)",
            "total of\\s*RM\\s*([\\d,.]+)",
            "save up to\\s*RM\\s*([\\d,.]+)"
        )

        for (pattern in patterns) {
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(text)

            match?.let {
                try {
                    return it.groupValues[1].replace(",", "").toDoubleOrNull()
                } catch (e: Exception) {
                    // Continue to next pattern
                }
            }
        }

        return null
    }

    /**
     * Validate the tax plan and make any necessary adjustments
     */
    private fun validateTaxPlan(plan: TaxPlan, income: Double, employmentType: String, planType: String): TaxPlan {
        // Sanity checks
        val suggestions = plan.suggestions.toMutableList()
        var totalSavings = plan.potentialSavings

        // If we have no suggestions or total savings is zero, regenerate
        if (suggestions.isEmpty() || totalSavings <= 0) {
            suggestions.clear()
            generateComprehensiveSuggestions(suggestions, income, employmentType, planType)
            totalSavings = suggestions.sumOf { it.potentialSaving }
        }

        // Verify each suggestion has a reasonable saving amount
        // Tax bracket estimation
        val taxRate = calculateEstimatedTaxRate(income)

        // Check each suggestion
        for (i in suggestions.indices) {
            val suggestion = suggestions[i]

            // If saving is unreasonably high (>30% of income) or zero, recalculate
            if (suggestion.potentialSaving <= 0 || suggestion.potentialSaving > income * 0.3) {
                // Generate a more reasonable saving estimate based on category
                val reasonableSaving = generateReasonableSaving(suggestion.category, income, taxRate, employmentType)

                suggestions[i] = suggestion.copy(potentialSaving = reasonableSaving)
            }
        }

        // Recalculate total savings
        totalSavings = suggestions.sumOf { it.potentialSaving }

        // Ensure we have enough suggestions
        if (suggestions.size < 5) {
            // Add more suggestions to reach at least 5
            val additionalCategories = setOf(
                "Education Relief", "Medical Relief", "SSPN Savings", "Donation", "Insurance Premium"
            ).minus(suggestions.map { it.category }.toSet())

            for (category in additionalCategories) {
                if (suggestions.size >= 5) break

                val saving = generateReasonableSaving(category, income, taxRate, employmentType)
                val suggestion = generateDefaultSuggestion(category, income, employmentType, taxRate)

                suggestions.add(TaxPlanSuggestion(
                    category = category,
                    suggestion = suggestion,
                    potentialSaving = saving
                ))
            }

            // Recalculate total savings again
            totalSavings = suggestions.sumOf { it.potentialSaving }
        }

        return plan.copy(
            suggestions = suggestions,
            potentialSavings = totalSavings
        )
    }

    /**
     * Calculate estimated tax rate based on income
     */
    private fun calculateEstimatedTaxRate(income: Double): Double {
        return when {
            income < 5000 -> 0.0
            income < 20000 -> 0.01
            income < 35000 -> 0.03
            income < 50000 -> 0.08
            income < 70000 -> 0.13
            income < 100000 -> 0.21
            income < 250000 -> 0.24
            income < 400000 -> 0.245
            income < 600000 -> 0.25
            income < 1000000 -> 0.26
            else -> 0.30
        }
    }

    /**
     * Generate a reasonable saving amount based on category and income
     */
    private fun generateReasonableSaving(category: String, income: Double, taxRate: Double, employmentType: String): Double {
        return when (category.lowercase()) {
            "lifestyle", "lifestyle relief" -> 2500 * taxRate
            "education", "education relief" -> 7000 * taxRate
            "medical", "medical relief", "medical expenses" -> 5000 * taxRate
            "epf", "epf contribution", "kwsp" -> minOf(income * 0.11, 4000.0) * taxRate
            "insurance", "insurance premium" -> 3000 * taxRate
            "donation", "donations", "charitable donations" -> 1000 * taxRate
            "sspn", "sspn savings" -> 3000 * taxRate
            "business expenses", "operating expenses" ->
                if (employmentType == "self-employed") income * 0.15 * taxRate else 0.0
            "home office" ->
                if (employmentType == "self-employed") 2400 * taxRate else 0.0
            "socso", "perkeso" -> 350 * taxRate
            "housing loan", "housing loan interest" -> 3000 * taxRate
            else -> income * 0.01 * taxRate // Default 1% of income as tax saving
        }
    }

    /**
     * Generate default suggestion text based on category
     */
    private fun generateDefaultSuggestion(category: String, income: Double, employmentType: String, taxRate: Double): String {
        return when (category.lowercase()) {
            "lifestyle", "lifestyle relief" ->
                "Maximize your RM2,500 lifestyle relief by keeping receipts for books, electronics, sports equipment, and internet subscriptions."

            "education", "education relief" ->
                "Claim education relief of up to RM7,000 for skills development courses or further education."

            "medical", "medical relief", "medical expenses" ->
                "Track medical expenses for yourself and dependents for relief up to RM8,000. This includes medical check-ups, treatment, and special needs equipment."

            "epf", "epf contribution", "kwsp" -> {
                val epfAmount = minOf(income * 0.11, 4000.0).toInt()
                "Maximize your EPF contribution ${if (employmentType == "employee") "of 11%" else "through voluntary contributions"} for tax relief up to RM4,000. For your income level, this could be approximately RM$epfAmount."
            }

            "insurance", "insurance premium" ->
                "Ensure you claim relief for life insurance premiums up to RM3,000 and medical/education insurance premiums up to RM3,000."

            "donation", "donations", "charitable donations" ->
                "Make donations to approved organizations for tax deductions. These organizations include registered charities, educational institutions, and sports bodies approved by the Malaysian government."

            "sspn", "sspn savings" ->
                "Consider SSPN savings for children's education with tax relief up to RM8,000. This national education savings scheme offers both tax benefits and competitive returns."

            "business expenses", "operating expenses" ->
                "Track and document all business-related expenses including office supplies, utilities, professional services, and business travel. These can be directly deducted from your business income."

            "home office" ->
                "If you work from home, allocate a portion of rent, utilities and internet as business expenses. Calculate based on the percentage of your home used exclusively for business."

            "socso", "perkeso" ->
                "Ensure you claim the full SOCSO/PERKESO contributions of up to RM350 as a tax relief."

            "housing loan", "housing loan interest" ->
                "If you have a housing loan for your first home, claim interest payments as tax relief up to RM10,000 per year for the first three years."

            else -> "Implement tax-efficient strategies appropriate for your income level and employment status to maximize available tax benefits."
        }
    }

    /**
     * Generate comprehensive default suggestions if AI parsing fails
     */
    private fun generateComprehensiveSuggestions(suggestions: MutableList<TaxPlanSuggestion>, income: Double, employmentType: String, planType: String) {
        // Clear existing suggestions
        suggestions.clear()

        // Calculate estimated tax rate
        val taxRate = calculateEstimatedTaxRate(income)
        Log.d(TAG, "Generating default suggestions with income: $income, tax rate: $taxRate, employment: $employmentType")

        // Standard reliefs and deductions with estimated savings
        suggestions.add(TaxPlanSuggestion(
            category = "Lifestyle Relief",
            suggestion = "Maximize your RM2,500 lifestyle relief by keeping receipts for books, electronics, sports equipment, and internet subscriptions.",
            potentialSaving = 2500 * taxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "Education Relief",
            suggestion = "Claim education relief of up to RM7,000 for skills development courses or further education that can enhance your career prospects.",
            potentialSaving = 7000 * taxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "Medical Relief",
            suggestion = "Track medical expenses for yourself and dependents for relief up to RM8,000. This includes medical check-ups, treatment, and special needs equipment.",
            potentialSaving = 5000 * taxRate
        ))

        // EPF suggestion varies based on employment type
        val epfAmount = minOf(income * 0.11, 4000.0)
        suggestions.add(TaxPlanSuggestion(
            category = "EPF Contribution",
            suggestion = if (employmentType == "employee") {
                "Ensure you're maximizing your mandatory EPF contribution of 11% for tax relief up to RM4,000. For your income level of RM$income, this equals approximately RM${epfAmount.toInt()} annually."
            } else {
                "Make voluntary EPF contributions up to RM4,000 annually for tax relief. This not only reduces your tax but also builds your retirement savings."
            },
            potentialSaving = epfAmount * taxRate
        ))

        // Add employment-specific suggestions
        if (employmentType == "self-employed") {
            // Business expense suggestion
            val businessExpenseAmount = income * 0.15
            suggestions.add(TaxPlanSuggestion(
                category = "Business Expenses",
                suggestion = "Track and document all business-related expenses including office supplies, utilities, professional services, and business travel. For your income level, this could represent approximately RM${businessExpenseAmount.toInt()} in deductions.",
                potentialSaving = businessExpenseAmount * taxRate
            ))

            // Home office suggestion
            suggestions.add(TaxPlanSuggestion(
                category = "Home Office",
                suggestion = "If you work from home, allocate a portion of rent, utilities and internet as business expenses. Calculate based on the percentage of your home used exclusively for business.",
                potentialSaving = 2400 * taxRate
            ))

            // Professional development
            if (planType == "future") {
                suggestions.add(TaxPlanSuggestion(
                    category = "Professional Development",
                    suggestion = "Invest in professional courses and certifications that can be categorized as business expenses. This improves both your skills and tax position.",
                    potentialSaving = 3500 * taxRate
                ))
            }
        } else {
            // Employee-specific suggestions
            suggestions.add(TaxPlanSuggestion(
                category = "SSPN Savings",
                suggestion = "Consider SSPN savings for children's education with tax relief up to RM8,000. This national education savings scheme offers both tax benefits and competitive returns.",
                potentialSaving = 3000 * taxRate
            ))

            // SOCSO/PERKESO
            suggestions.add(TaxPlanSuggestion(
                category = "SOCSO Contribution",
                suggestion = "Ensure you claim the full SOCSO/PERKESO contributions of up to RM350 as a tax relief.",
                potentialSaving = 350 * taxRate
            ))
        }

        // Plan type specific suggestions
        when (planType) {
            "future" -> {
                suggestions.add(TaxPlanSuggestion(
                    category = "Long-term Investment",
                    suggestion = "Consider tax-efficient investment vehicles like unit trusts with tax incentives to prepare for future income growth.",
                    potentialSaving = income * 0.03 * taxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Retirement Planning",
                    suggestion = "Supplement your EPF with Private Retirement Scheme (PRS) contributions for additional tax relief up to RM3,000.",
                    potentialSaving = 3000 * taxRate
                ))
            }
            "business" -> {
                suggestions.add(TaxPlanSuggestion(
                    category = "Capital Investment",
                    suggestion = "Plan capital investments to take advantage of capital allowances and incentives for business expansion.",
                    potentialSaving = income * 0.04 * taxRate
                ))

                suggestions.add(TaxPlanSuggestion(
                    category = "Business Structure",
                    suggestion = "Review your business structure (sole proprietorship vs. LLC) to optimize tax treatment based on your projected growth.",
                    potentialSaving = income * 0.035 * taxRate
                ))
            }
            else -> {
                // Common suggestion for all plans
                suggestions.add(TaxPlanSuggestion(
                    category = "Donation",
                    suggestion = "Make donations to approved organizations for tax deductions. These organizations include registered charities, educational institutions, and sports bodies approved by the Malaysian government.",
                    potentialSaving = 1000 * taxRate
                ))
            }
        }

        // Insurance suggestion for all
        suggestions.add(TaxPlanSuggestion(
            category = "Insurance Premium",
            suggestion = "Claim relief for life insurance premiums up to RM3,000 and medical/education insurance premiums up to RM3,000.",
            potentialSaving = 3000 * taxRate
        ))

        // Log the generated suggestions
        Log.d(TAG, "Generated ${suggestions.size} default suggestions with total savings: ${suggestions.sumOf { it.potentialSaving }}")
    }
}