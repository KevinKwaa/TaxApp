package com.example.taxapp.taxplan

import android.content.Context
import android.util.Log
import com.example.taxapp.BuildConfig
import com.example.taxapp.user.FirebaseManager
import com.example.taxapp.utils.NetworkUtil
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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

            // Get existing plans to ensure uniqueness
            val existingPlanData = fetchExistingTaxPlans()

            // Build a comprehensive prompt with uniqueness requirements
            val prompt = buildTaxPlanPrompt(adjustedIncome, employmentType, name, planType, existingPlanData)
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
            val validatedPlan = validateTaxPlan(taxPlan, adjustedIncome, employmentType, planType, existingPlanData)

            Result.success(validatedPlan)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in generateTaxPlan", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch existing tax plans to ensure uniqueness of new plans
     */
    private suspend fun fetchExistingTaxPlans(): List<ExistingPlanData> {
        val result = mutableListOf<ExistingPlanData>()

        try {
            val userId = FirebaseManager.getCurrentUserId() ?: return result

            // Fetch plans from Firestore
            val snapshot = Firebase.firestore.collection("tax_plans")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Extract relevant data for uniqueness
            snapshot.documents.forEach { doc ->
                val plan = doc.toObject(TaxPlan::class.java) ?: return@forEach

                // Add details to analyze for uniqueness
                val planData = ExistingPlanData(
                    planType = plan.planType,
                    categories = plan.suggestions.map { it.category },
                    suggestionTexts = plan.suggestions.map { it.suggestion.take(100) } // Only need beginning for comparison
                )

                result.add(planData)
            }

            Log.d(TAG, "Found ${result.size} existing plans for uniqueness comparison")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching existing plans for uniqueness check", e)
            // Continue without existing data if there's an error
        }

        return result
    }

    /**
     * Data class to help track existing plan details for uniqueness
     */
    data class ExistingPlanData(
        val planType: String,
        val categories: List<String>,
        val suggestionTexts: List<String>
    )

    /**
     * Build a prompt for the AI to generate a tax plan
     */
    private fun buildTaxPlanPrompt(
        income: Double,
        employmentType: String,
        name: String = "",
        planType: String = "standard",
        existingPlans: List<ExistingPlanData> = emptyList()
    ): String {
        val greeting = if (name.isNotBlank()) "for $name" else ""
        val planTypeDesc = when (planType) {
            "future" -> "future income planning (assuming 20% income growth)"
            "business" -> "business venture planning"
            else -> "standard tax planning"
        }

        // Add uniqueness requirement based on existing plans
        val uniquenessRequirement = if (existingPlans.isNotEmpty()) {
            val planTypeCounts = existingPlans.groupingBy { it.planType }.eachCount()
            val commonCategories = existingPlans.flatMap { it.categories }
                .groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
                .keys
                .take(5)
                .joinToString(", ")

            // Create a uniqueness instruction
            """
            UNIQUENESS REQUIREMENT:
            - The user already has ${existingPlans.size} tax plans.
            - ${if (planTypeCounts[planType] ?: 0 > 0) "They already have ${planTypeCounts[planType]} plans of this type." else "This is their first plan of this type."}
            - Common categories in existing plans: ${if (commonCategories.isNotBlank()) commonCategories else "None"}
            - YOU MUST GENERATE A UNIQUE PLAN with different approaches and suggestions from existing plans.
            - Focus on DIFFERENT TAX STRATEGIES that weren't emphasized in previous plans.
            - Use creative and alternative approaches that achieve the same tax saving goals.
            - The VARIATION FOCUS for this specific plan should be: ${getVariationFocus(planType, existingPlans.size)}
            """
        } else {
            // First plan, no uniqueness needed yet
            ""
        }

        // Get a specialized focus for this plan for more variation
        val specializedFocus = getSpecializedFocus(planType, existingPlans.size)

        return """
        You are a Malaysian Tax Planning Expert AI for a tax app. Generate a detailed, personalized tax plan for ${planTypeDesc} ${greeting} with the following information:
        
        User's Annual Income: RM ${income}
        Employment Type: ${employmentType}
        
        TASK:
        Create a comprehensive tax plan with:
        1. A brief analysis of the user's tax situation based on their income level and employment type
        2. 5-7 actionable tax-saving suggestions organized by specific categories
        3. Realistic estimated potential savings for each suggestion in Malaysian Ringgit (RM)
        
        $uniquenessRequirement
        
        SPECIALIZED FOCUS FOR THIS PLAN:
        $specializedFocus
        
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
        - USE UNIQUE APPROACHES DIFFERENT FROM STANDARD TAX ADVICE when possible.
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
     * Get a specialized focus based on plan type and number of existing plans
     * This ensures that different plans emphasize different aspects
     */
    private fun getSpecializedFocus(planType: String, existingPlanCount: Int): String {
        // Primary focus areas by plan type
        val focusOptions = when (planType) {
            "future" -> listOf(
                "Focus on long-term tax efficiency strategies and investments that minimize future tax burden.",
                "Emphasize education and skill development investments for future income growth.",
                "Concentrate on retirement planning and tax-efficient wealth accumulation.",
                "Highlight property investment strategies and associated tax benefits.",
                "Focus on career advancement expenses that qualify for tax relief."
            )
            "business" -> listOf(
                "Emphasize business expense optimization and record-keeping strategies.",
                "Focus on tax-efficient business structure and registration options.",
                "Concentrate on capital investment strategies and associated tax incentives.",
                "Highlight employee benefits and compensation structures that optimize tax position.",
                "Focus on digital transformation expenses that qualify for tax incentives."
            )
            else -> listOf(
                "Focus on maximizing personal and family-related tax reliefs.",
                "Emphasize healthcare and medical expense optimization for tax purposes.",
                "Concentrate on education and continuous learning tax benefits.",
                "Highlight lifestyle and technology-related tax reliefs.",
                "Focus on charity and donation strategies for tax optimization."
            )
        }

        // Select different focus based on how many plans exist
        val focusIndex = existingPlanCount % focusOptions.size

        return focusOptions[focusIndex]
    }

    /**
     * Generate a variation focus based on plan type and existing plan count
     * This helps create uniqueness in the suggestions
     */
    private fun getVariationFocus(planType: String, existingPlanCount: Int): String {
        val variationOptions = listOf(
            "Optimization based on TIMING of expenses and contributions",
            "Focus on DOCUMENTATION and record-keeping for maximum deductions",
            "Emphasis on AUTOMATION of tax-saving strategies",
            "Concentration on FAMILY-BASED tax optimization strategies",
            "Focus on DIGITAL TRANSFORMATION tax incentives",
            "Emphasis on GREEN/SUSTAINABLE initiatives with tax benefits",
            "Focus on EDUCATION and PROFESSIONAL DEVELOPMENT",
            "Concentration on RETIREMENT and LONG-TERM planning",
            "Emphasis on HEALTHCARE and WELLNESS expense optimization"
        )

        // Select variation based on count to ensure different plans have different focus
        val variationIndex = (existingPlanCount + planType.hashCode()) % variationOptions.size

        return variationOptions[variationIndex]
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
            planType = planType,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
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
     * Enhanced to ensure uniqueness when compared to existing plans
     */
    private fun validateTaxPlan(
        plan: TaxPlan,
        income: Double,
        employmentType: String,
        planType: String,
        existingPlans: List<ExistingPlanData> = emptyList()
    ): TaxPlan {
        // Sanity checks
        val suggestions = plan.suggestions.toMutableList()
        var totalSavings = plan.potentialSavings

        // If we have no suggestions or total savings is zero, regenerate
        if (suggestions.isEmpty() || totalSavings <= 0) {
            suggestions.clear()
            generateComprehensiveSuggestions(suggestions, income, employmentType, planType, existingPlans)
            totalSavings = suggestions.sumOf { it.potentialSaving }
        }

        // Verify each suggestion has a reasonable saving amount
        // Tax bracket estimation
        val taxRate = calculateEstimatedTaxRate(income)

        // Check each suggestion for:
        // 1. Reasonable savings amount
        // 2. Uniqueness compared to existing plans
        for (i in suggestions.indices) {
            val suggestion = suggestions[i]
            var needsReplacement = false

            // Check if saving is unreasonably high (>30% of income) or zero
            if (suggestion.potentialSaving <= 0 || suggestion.potentialSaving > income * 0.3) {
                needsReplacement = true
            }

            // Check for duplicate suggestions in existing plans
            if (!needsReplacement && existingPlans.isNotEmpty()) {
                val suggestionStart = suggestion.suggestion.take(50).lowercase()

                // Look for similar suggestions in existing plans
                val isDuplicate = existingPlans.any { plan ->
                    plan.suggestionTexts.any { existingSuggestion ->
                        existingSuggestion.lowercase().contains(suggestionStart) ||
                                suggestionStart.contains(existingSuggestion.take(50).lowercase())
                    }
                }

                if (isDuplicate) {
                    needsReplacement = true
                }
            }

            // Replace if needed
            if (needsReplacement) {
                // Generate a more unique suggestion
                val (uniqueCategory, uniqueSuggestion) = generateUniqueSuggestion(
                    suggestion.category,
                    income,
                    employmentType,
                    planType,
                    existingPlans
                )

                val reasonableSaving = generateReasonableSaving(uniqueCategory, income, taxRate, employmentType)

                suggestions[i] = suggestion.copy(
                    category = uniqueCategory,
                    suggestion = uniqueSuggestion,
                    potentialSaving = reasonableSaving
                )
            }
        }

        // Recalculate total savings
        totalSavings = suggestions.sumOf { it.potentialSaving }

        // Ensure we have enough suggestions
        if (suggestions.size < 5) {
            // Get categories already in plan
            val existingCategories = suggestions.map { it.category }.toSet()

            // Get categories from other plans to avoid
            val existingPlanCategories = existingPlans.flatMap { it.categories }.toSet()

            // Find unique categories not yet used in this plan or common in other plans
            val additionalCategories = TAX_CATEGORIES
                .filter { it !in existingCategories && (it !in existingPlanCategories || Math.random() > 0.7) }
                .shuffled()
                .take(5 - suggestions.size)

            for (category in additionalCategories) {
                val (uniqueCategory, uniqueSuggestion) = generateUniqueSuggestion(
                    category, income, employmentType, planType, existingPlans
                )
                val saving = generateReasonableSaving(uniqueCategory, income, taxRate, employmentType)

                suggestions.add(TaxPlanSuggestion(
                    category = uniqueCategory,
                    suggestion = uniqueSuggestion,
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
     * Generate a truly unique suggestion for a category
     */
    private fun generateUniqueSuggestion(
        baseCategory: String,
        income: Double,
        employmentType: String,
        planType: String,
        existingPlans: List<ExistingPlanData>
    ): Pair<String, String> {
        // Sometimes modify the category name to create variety
        val uniqueCategory = if (Math.random() > 0.7) {
            when (baseCategory.lowercase()) {
                "lifestyle", "lifestyle relief" -> listOf(
                    "Tech & Entertainment Relief", "Lifestyle Optimization", "Personal Expenses Relief"
                ).random()

                "education", "education relief" -> listOf(
                    "Professional Development", "Skills Enhancement", "Education Investment"
                ).random()

                "medical", "medical relief", "medical expenses" -> listOf(
                    "Healthcare Optimization", "Wellness Benefits", "Medical Tax Planning"
                ).random()

                "donation", "donations" -> listOf(
                    "Charitable Giving", "Social Impact Relief", "Philanthropy"
                ).random()

                else -> baseCategory
            }
        } else {
            baseCategory
        }

        // Generate variations for the suggestion text
        val variations = getSuggestionVariations(uniqueCategory, income, employmentType, planType)

        // If we have existing plans, choose a suggestion that's not too similar to existing ones
        if (existingPlans.isNotEmpty()) {
            // Try to find a suggestion that doesn't closely match existing ones
            for (variation in variations.shuffled()) {
                val variationStart = variation.take(50).lowercase()
                val isDuplicate = existingPlans.any { plan ->
                    plan.suggestionTexts.any { existingSuggestion ->
                        existingSuggestion.lowercase().contains(variationStart) ||
                                variationStart.contains(existingSuggestion.take(50).lowercase())
                    }
                }

                if (!isDuplicate) {
                    return Pair(uniqueCategory, variation)
                }
            }
        }

        // If no unique match found or no existing plans, just use a random variation
        return Pair(uniqueCategory, variations.random())
    }

    /**
     * Get a list of varied suggestion texts for a category
     */
    private fun getSuggestionVariations(
        category: String,
        income: Double,
        employmentType: String,
        planType: String
    ): List<String> {
        // Calculate some values for personalizing suggestions
        val epfAmount = (income * 0.11).toInt()
        val isHighIncome = income > 100000

        return when (category.lowercase()) {
            "lifestyle", "lifestyle relief", "tech & entertainment relief", "lifestyle optimization", "personal expenses relief" -> listOf(
                "Maximize your RM2,500 lifestyle relief by keeping digital receipts for books, electronics, sports equipment, and internet subscriptions.",
                "Create a spreadsheet to track lifestyle expenses up to RM2,500 including smartphones, tablets, books, and sports equipment.",
                "Set up automatic monthly transfers of RM210 to a dedicated account for lifestyle purchases that qualify for the RM2,500 relief.",
                "Subscribe to apps that automatically categorize lifestyle purchases and remind you when you're approaching the RM2,500 relief limit.",
                "Strategically time larger lifestyle purchases (electronics, gym memberships) to fully utilize the RM2,500 annual relief."
            )

            "education", "education relief", "professional development", "skills enhancement", "education investment" -> listOf(
                "Claim education relief of up to RM7,000 for skills development courses related to emerging technologies or management training.",
                "Invest in certified professional courses with tax-deductible tuition up to RM7,000, focusing on skills that can increase your market value.",
                "Enroll in qualifying online learning platforms with annual subscriptions that can be claimed under the RM7,000 education relief.",
                "Consider part-time diploma or degree programs relevant to your industry that qualify for the full RM7,000 education relief.",
                "Create a 3-year education plan with courses spread strategically to maximize the RM7,000 relief each tax year."
            )

            "medical", "medical relief", "medical expenses", "healthcare optimization", "wellness benefits", "medical tax planning" -> listOf(
                "Maintain digital records of all medical expenses for yourself and dependents to easily claim relief up to RM8,000 annually.",
                "Schedule preventive healthcare check-ups strategically to maximize the RM8,000 medical relief each tax year.",
                "Consider a family healthcare account to consolidate and track all qualifying medical expenses up to the RM8,000 limit.",
                "Ensure you include often-forgotten medical expenses like specialized treatments, preventive screenings, and mobility aids toward your RM8,000 relief.",
                "Create a health spending strategy that optimizes timing of elective procedures to maximize tax relief across multiple years."
            )

            "epf", "epf contribution", "kwsp" -> {
                if (employmentType == "employee") {
                    listOf(
                        "Ensure you're not opting out of any portion of your 11% EPF contribution to maximize tax relief up to RM4,000 annually.",
                        "Check your EPF contribution statement quarterly to verify you're on track for the maximum RM4,000 tax relief.",
                        "Consider restructuring your compensation package to optimize the EPF-eligible portion, targeting the full RM4,000 relief.",
                        "If your income allows contributions over RM4,000 annually to EPF, ensure you claim the maximum tax relief of RM4,000.",
                        "Set up automatic notification alerts to track your EPF contributions and ensure you reach the optimal tax relief amount of RM4,000."
                    )
                } else {
                    listOf(
                        "Establish a systematic voluntary EPF contribution plan of RM${(4000/12).toInt()} monthly to reach the maximum RM4,000 tax relief.",
                        "Create a quarterly schedule for voluntary EPF contributions to reach the RM4,000 maximum while managing cash flow for your business.",
                        "Consider timing larger voluntary EPF contributions during your business's high-revenue months to reach the RM4,000 tax relief.",
                        "Set up automatic recurring transfers to EPF to ensure you consistently build toward the RM4,000 tax relief threshold.",
                        "Allocate a percentage of each client payment directly to voluntary EPF contributions to systematically reach the RM4,000 relief."
                    )
                }
            }

            "business expenses", "operating expenses" -> {
                if (employmentType == "self-employed") {
                    listOf(
                        "Implement a digital receipt management system to track and categorize all business expenses, which could represent approximately RM${(income * 0.15).toInt()} in deductions.",
                        "Schedule quarterly expense reviews with your accountant to identify overlooked business deductions, potentially worth RM${(income * 0.02).toInt()} annually.",
                        "Create dedicated business accounts and cards to automatically separate personal and business expenses for cleaner tax filing.",
                        "Develop a comprehensive business expense policy that includes often-overlooked categories like professional development, subscriptions, and industry memberships.",
                        "Implement a cloud-based accounting system that automatically categorizes expenses and flags tax-deductible items."
                    )
                } else {
                    listOf(
                        "Track employment-related expenses that your employer doesn't reimburse, which may qualify for deductions under certain circumstances.",
                        "Document transportation expenses related to work duties (excluding commuting) that may qualify for deductions.",
                        "Maintain records of professional subscriptions and memberships related to your employment that may be tax-deductible.",
                        "Keep receipts for work-specific equipment or tools you purchase for your job that your employer doesn't provide.",
                        "Track professional development expenses directly related to maintaining skills required in your current position."
                    )
                }
            }

            else -> {
                // For any other category, provide generic but somewhat tailored advice
                when {
                    planType == "future" -> listOf(
                        "Create a tax-optimized investment strategy focusing on vehicles with preferential tax treatment for long-term growth.",
                        "Establish automated systems to maximize tax advantages as your income grows over the next several years.",
                        "Develop a 5-year tax planning roadmap that adapts to projected income increases and changing tax brackets.",
                        "Consider tax-advantaged investment vehicles like unit trusts or retirement schemes that offer compound growth potential.",
                        "Establish tax-efficient income diversification strategies to optimize your tax position as your earnings increase."
                    )
                    planType == "business" -> listOf(
                        "Establish proper entity structure and documentation to optimize tax treatment for your business activities.",
                        "Create a comprehensive business expense tracking system with quarterly tax position reviews.",
                        "Develop a strategic reinvestment plan for business profits that optimizes both growth and tax efficiency.",
                        "Consider implementing a tax-optimized employee compensation structure as your business expands.",
                        "Establish a detailed capital expenditure schedule that maximizes available tax incentives and timing advantages."
                    )
                    isHighIncome -> listOf(
                        "Create a comprehensive tax planning calendar with specific action items for each quarter of the tax year.",
                        "Establish a systematic approach to tracking and maximizing all available tax reliefs relevant to your income level.",
                        "Consider tax-efficient wealth preservation strategies appropriate for your high-income bracket.",
                        "Develop a balanced portfolio of investments and charitable giving that optimizes both growth and tax efficiency.",
                        "Implement advanced tax planning techniques with regular reviews by a qualified tax professional."
                    )
                    else -> listOf(
                        "Create a simple but effective system to track all potential tax relief categories throughout the year.",
                        "Establish a monthly routine to organize and digitize receipts for all potential tax-deductible expenses.",
                        "Develop a basic tax planning calendar with reminders for key actions and deadlines.",
                        "Consider strategic timing of major purchases and investments to maximize available tax benefits.",
                        "Implement a systematic approach to identifying and utilizing all tax reliefs available at your income level."
                    )
                }
            }
        }
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
        // Add some randomization for more variety between plans
        val variationFactor = 0.8 + (Math.random() * 0.4) // Between 0.8 and 1.2

        val baseSaving = when (category.lowercase()) {
            "lifestyle", "lifestyle relief", "tech & entertainment relief", "lifestyle optimization", "personal expenses relief" ->
                2500 * taxRate

            "education", "education relief", "professional development", "skills enhancement", "education investment" ->
                7000 * taxRate

            "medical", "medical relief", "medical expenses", "healthcare optimization", "wellness benefits", "medical tax planning" ->
                5000 * taxRate

            "epf", "epf contribution", "kwsp" ->
                minOf(income * 0.11, 4000.0) * taxRate

            "insurance", "insurance premium" ->
                3000 * taxRate

            "donation", "donations", "charitable giving", "social impact relief", "philanthropy" ->
                1000 * taxRate

            "sspn", "sspn savings", "education savings" ->
                3000 * taxRate

            "business expenses", "operating expenses" ->
                if (employmentType == "self-employed") income * 0.15 * taxRate else 1000 * taxRate

            "home office", "workspace" ->
                if (employmentType == "self-employed") 2400 * taxRate else 800 * taxRate

            "socso", "perkeso" ->
                350 * taxRate

            "housing loan", "housing loan interest", "mortgage interest" ->
                3000 * taxRate

            "investment", "investments" ->
                income * 0.02 * taxRate

            "retirement planning", "retirement", "prs" ->
                3000 * taxRate

            "capital investment" ->
                income * 0.04 * taxRate

            "business structure" ->
                income * 0.035 * taxRate

            else -> income * 0.01 * taxRate // Default 1% of income as tax saving
        }

        // Apply variation and round to nearest 10
        // Fixed version: Convert Long to Double after rounding
        return Math.round((baseSaving * variationFactor) / 10).toDouble() * 10
    }

    // List of categories for generating unique tax plans
    private val TAX_CATEGORIES = listOf(
        "Lifestyle Relief",
        "Medical Relief",
        "Education Relief",
        "EPF Contribution",
        "Insurance Premium",
        "SSPN Savings",
        "Donation",
        "SOCSO Contribution",
        "Housing Loan Interest",
        "Business Expenses",
        "Home Office",
        "Investment",
        "Retirement Planning",
        "Capital Investment",
        "Business Structure",
        "Technology Adoption",
        "Green Initiatives",
        "Professional Memberships",
        "Work-Related Travel",
        "Family Tax Planning",
        "Zakat",
        "Child Relief",
        "Parental Care Relief",
        "Disability Relief",
        "Intellectual Property"
    )

    /**
     * Generate comprehensive default suggestions if AI parsing fails
     * Enhanced to create more varied suggestions for unique plans
     */
    private fun generateComprehensiveSuggestions(
        suggestions: MutableList<TaxPlanSuggestion>,
        income: Double,
        employmentType: String,
        planType: String,
        existingPlans: List<ExistingPlanData> = emptyList()
    ) {
        // Clear existing suggestions
        suggestions.clear()

        // Calculate estimated tax rate
        val taxRate = calculateEstimatedTaxRate(income)
        Log.d(TAG, "Generating default suggestions with income: $income, tax rate: $taxRate, employment: $employmentType")

        // Decide how many plans exist of this type to add variation
        val planTypeCount = existingPlans.count { it.planType == planType }

        // Choose categories based on plan type and what existing plans already have
        val existingPlanCategories = existingPlans.flatMap { it.categories }.groupingBy { it }.eachCount()

        // Select categories less commonly used in existing plans
        val selectedCategories = TAX_CATEGORIES
            .filter { category ->
                (existingPlanCategories[category] ?: 0) < 2 || Math.random() > 0.7
            }
            .shuffled()
            .take(7)

        // Generate unique suggestions
        for (category in selectedCategories) {
            val (uniqueCategory, uniqueSuggestion) = generateUniqueSuggestion(
                category, income, employmentType, planType, existingPlans
            )
            val saving = generateReasonableSaving(uniqueCategory, income, taxRate, employmentType)

            suggestions.add(TaxPlanSuggestion(
                category = uniqueCategory,
                suggestion = uniqueSuggestion,
                potentialSaving = saving
            ))
        }

        // Add some plan-type specific suggestions if we don't have enough
        if (suggestions.size < 5) {
            when (planType) {
                "future" -> {
                    // Future income plan - focus on tax brackets and investment strategies
                    suggestions.add(TaxPlanSuggestion(
                        category = "Investment Planning",
                        suggestion = "Develop a tax-efficient investment strategy targeting long-term growth with periodic reviews to optimize as your income increases.",
                        potentialSaving = income * 0.02 * taxRate
                    ))
                }
                "business" -> {
                    // Business venture plan - focus on business deductions
                    suggestions.add(TaxPlanSuggestion(
                        category = "Business Structure Optimization",
                        suggestion = "Review your business structure (sole proprietorship vs. LLC) to optimize tax treatment based on your projected growth and specific business activities.",
                        potentialSaving = income * 0.035 * taxRate
                    ))
                }
                else -> {
                    // Standard plan
                    suggestions.add(TaxPlanSuggestion(
                        category = "Tax Calendar Planning",
                        suggestion = "Create a comprehensive tax planning calendar with key dates and actions to optimize your tax position throughout the year.",
                        potentialSaving = income * 0.01 * taxRate
                    ))
                }
            }
        }

        // Log the generated suggestions
        Log.d(TAG, "Generated ${suggestions.size} default suggestions with total savings: ${suggestions.sumOf { it.potentialSaving }}")
    }
}