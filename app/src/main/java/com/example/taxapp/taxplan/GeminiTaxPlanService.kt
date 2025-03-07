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
    suspend fun generateTaxPlan(income: String, employmentType: String, name: String = ""): Result<TaxPlan> = withContext(Dispatchers.IO) {
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

            // Build a comprehensive prompt
            val prompt = buildTaxPlanPrompt(incomeValue, employmentType)
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
            Log.d(TAG, "Raw AI response: ${response.text?.take(100)}...")

            // Parse the AI response into a tax plan
            val taxPlan = parseTaxPlanResponse(response, incomeValue, employmentType)

            Result.success(taxPlan)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in generateTaxPlan", e)
            Result.failure(e)
        }
    }

    /**
     * Build a prompt for the AI to generate a tax plan
     */
    private fun buildTaxPlanPrompt(income: Double, employmentType: String, name: String = ""): String {
        val greeting = if (name.isNotBlank()) "for $name" else ""
        return """
        You are a Tax Planning Assistant for a Malaysian tax app. Generate a detailed, personalized tax plan ${greeting} based on the following information:
        
        User's Annual Income: RM ${income}
        Employment Type: ${employmentType}
        
        Create a comprehensive tax plan with:
        1. A brief analysis of the user's tax situation based on their income level and employment type
        2. 5-7 actionable tax-saving suggestions organized by specific categories
        3. Realistic estimated potential savings for each suggestion in Malaysian Ringgit (RM)
        
        Malaysian Tax Context:
        - For employees: Tax filing deadline is April 30th
        - For self-employed: Tax filing deadline is June 30th
        - Individual income tax rates: Progressive from 0% to 30% based on income brackets
        - Tax relief categories include: lifestyle (RM2,500), medical expenses (RM8,000), education (RM7,000), EPF contributions, SOCSO, housing interest, insurance premiums
        
        Format each suggestion clearly with:
        - Category: [specific category name]
        - Suggestion: [detailed actionable advice]
        - Potential Savings: RM [realistic amount]
        
        Make all suggestions specific, practical, and compliant with Malaysian tax regulations. Ensure the estimated savings are realistic and proportional to the user's income level.
        
        For self-employed users, include suggestions about:
        - Business expense deductions
        - Home office deductions
        - Retirement contributions
        - Record-keeping best practices
        
        For employees, focus on:
        - Maximizing available tax reliefs
        - Employee benefits optimization
        - Additional deduction opportunities
        
        Make the language accessible and provide a clear potential total savings amount across all suggestions.
        """.trimIndent()
    }

    /**
     * Parse the AI response into a structured TaxPlan object
     */
    private fun parseTaxPlanResponse(response: GenerateContentResponse, income: Double, employmentType: String): TaxPlan {
        val responseText = response.text ?: ""

        // Default values in case parsing fails
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val planName = "Tax Plan ${dateFormat.format(Date())}"
        var planDescription = "AI-generated tax plan based on your income profile"

        val suggestions = mutableListOf<TaxPlanSuggestion>()
        var totalSavings = 0.0

        try {
            // Extract description (first paragraph)
            val paragraphs = responseText.split("\n\n")
            if (paragraphs.isNotEmpty()) {
                planDescription = paragraphs.first { it.isNotBlank() }.trim()
            }

            // Try to extract suggestions with different patterns
            val patterns = listOf(
                // Pattern 1: Category: Suggestion - RM XXX
                "(?i)(?:^|\\n)\\s*(?:Category|[\\w\\s]+):\\s*([\\w\\s]+)\\s*(?:-|:)\\s*(.+?)\\s*(?:Potential Savings|Savings|Saving):\\s*RM\\s*([\\d,.]+)",

                // Pattern 2: X. Category: Suggestion (RM XXX)
                "(?i)(?:^|\\n)\\s*(?:\\d+\\.)?\\s*([\\w\\s]+):\\s*(.+?)\\s*\\(\\s*RM\\s*([\\d,.]+)\\s*\\)",

                // Pattern 3: Category - Suggestion: RM XXX
                "(?i)(?:^|\\n)\\s*([\\w\\s]+)\\s*-\\s*(.+?):\\s*RM\\s*([\\d,.]+)"
            )

            for (pattern in patterns) {
                val regex = pattern.toRegex(RegexOption.DOT_MATCHES_ALL)
                val matches = regex.findAll(responseText)

                for (match in matches) {
                    if (match.groupValues.size >= 4) {
                        val category = match.groupValues[1].trim()
                        val suggestion = match.groupValues[2].trim()
                        // Parse the saving amount, removing commas and converting to Double
                        val savingText = match.groupValues[3].replace(",", "")
                        val saving = savingText.toDoubleOrNull() ?: 0.0

                        // Only add if we have meaningful data
                        if (category.isNotBlank() && suggestion.isNotBlank() && saving > 0) {
                            suggestions.add(TaxPlanSuggestion(
                                id = UUID.randomUUID().toString(),
                                category = category,
                                suggestion = suggestion,
                                potentialSaving = saving,
                                isImplemented = false
                            ))

                            totalSavings += saving
                        }
                    }
                }

                // If we found suggestions with this pattern, stop trying more patterns
                if (suggestions.isNotEmpty()) {
                    break
                }
            }

            // If no suggestions were found with regex patterns, try a simpler approach
            if (suggestions.isEmpty()) {
                // Look for "RM" mentions with numbers
                val rmPattern = "(?i)([\\w\\s]+)(?::|-)\\s*(.+?)\\s*RM\\s*([\\d,.]+)".toRegex()
                val rmMatches = rmPattern.findAll(responseText)

                for (match in rmMatches) {
                    if (match.groupValues.size >= 4) {
                        val category = match.groupValues[1].trim()
                        val suggestion = match.groupValues[2].trim()
                        val savingText = match.groupValues[3].replace(",", "")
                        val saving = savingText.toDoubleOrNull() ?: 0.0

                        if (category.isNotBlank() && suggestion.isNotBlank() && saving > 0) {
                            suggestions.add(TaxPlanSuggestion(
                                id = UUID.randomUUID().toString(),
                                category = category,
                                suggestion = suggestion,
                                potentialSaving = saving,
                                isImplemented = false
                            ))

                            totalSavings += saving
                        }
                    }
                }
            }

            // Generate default suggestions if still nothing found
            if (suggestions.isEmpty()) {
                Log.w(TAG, "Could not parse suggestions from AI response, using defaults")
                generateDefaultSuggestions(suggestions, income, employmentType)
                totalSavings = suggestions.sumOf { it.potentialSaving }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            // Fall back to default suggestions if parsing fails
            generateDefaultSuggestions(suggestions, income, employmentType)
            totalSavings = suggestions.sumOf { it.potentialSaving }
        }

        return TaxPlan(
            name = planName,
            description = planDescription,
            suggestions = suggestions,
            potentialSavings = totalSavings
        )
    }

    /**
     * Generate default suggestions if AI parsing fails
     */
    private fun generateDefaultSuggestions(suggestions: MutableList<TaxPlanSuggestion>, income: Double, employmentType: String) {
        // Clear existing suggestions
        suggestions.clear()

        // Calculate estimated savings based on income
        val estimatedTaxRate = when {
            income < 35000 -> 0.0
            income < 50000 -> 0.08
            income < 70000 -> 0.13
            income < 100000 -> 0.21
            else -> 0.24
        }

        // Standard reliefs and deductions with estimated savings
        suggestions.add(TaxPlanSuggestion(
            category = "Lifestyle",
            suggestion = "Maximize your RM2,500 lifestyle relief by keeping receipts for books, electronics, sports equipment, and internet subscriptions.",
            potentialSaving = 2500 * estimatedTaxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "Education",
            suggestion = "Claim education relief of up to RM7,000 for skills development courses or further education.",
            potentialSaving = 7000 * estimatedTaxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "Medical",
            suggestion = "Track medical expenses for yourself and dependents for relief up to RM8,000.",
            potentialSaving = 5000 * estimatedTaxRate
        ))

        suggestions.add(TaxPlanSuggestion(
            category = "EPF",
            suggestion = "Maximize your EPF contribution (11% for employees, voluntary for self-employed) for tax relief up to RM4,000.",
            potentialSaving = 4000 * estimatedTaxRate
        ))

        if (employmentType == "self-employed") {
            suggestions.add(TaxPlanSuggestion(
                category = "Business Expenses",
                suggestion = "Track and document all business-related expenses including office supplies, utilities, and professional services.",
                potentialSaving = income * 0.15 * estimatedTaxRate
            ))

            suggestions.add(TaxPlanSuggestion(
                category = "Home Office",
                suggestion = "If you work from home, allocate a portion of rent, utilities and internet as business expenses.",
                potentialSaving = 2400 * estimatedTaxRate
            ))
        } else {
            suggestions.add(TaxPlanSuggestion(
                category = "SSPN",
                suggestion = "Consider SSPN savings for children's education with tax relief up to RM8,000.",
                potentialSaving = 3000 * estimatedTaxRate
            ))
        }

        suggestions.add(TaxPlanSuggestion(
            category = "Donation",
            suggestion = "Make donations to approved organizations for tax deductions.",
            potentialSaving = 1000 * estimatedTaxRate
        ))
    }
}