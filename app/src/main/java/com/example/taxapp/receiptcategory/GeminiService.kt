package com.example.taxapp.receiptcategory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.taxapp.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GeminiService(private val context: Context) {

    // Get API key from credentials.json in assets folder
    private val apiKey = ApiKeyHelper.getGeminiApiKey(context)

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )
    }

    /**
     * Process a receipt image using Gemini AI
     */
    suspend fun processReceiptImage(imageUri: Uri): Result<ReceiptModel> {
        return try {
            // Convert URI to bitmap for Gemini
            val bitmap = uriToBitmap(context, imageUri)
            if (bitmap == null) {
                return Result.failure(Exception("Failed to load image"))
            }

            // Create the prompt for Gemini - updated to focus on detailed item extraction
            val prompt = """
                You are a specialized receipt analyzer for a tax application. 
                Extract the following information from this receipt image:
                1. Merchant/Business name
                2. Date of purchase (format as DD/MM/YYYY)
                
                Most importantly, extract EACH individual item on the receipt with:
                - Item name/description
                - Item price/amount
                
                Then, categorize EACH item into one of these Malaysian tax relief categories:
                - Lifestyle Expenses
                - Childcare
                - Sport Equipment 
                - Donations
                - Medical
                - Education
                
                Return the data in a JSON format with these fields:
                {
                    "merchantName": "",
                    "date": "DD/MM/YYYY",
                    "totalAmount": 0.0,
                    "items": [
                        {
                            "description": "",
                            "amount": 0.0,
                            "category": ""
                        },
                        {
                            "description": "",
                            "amount": 0.0,
                            "category": ""
                        }
                    ]
                }
                
                Be as detailed as possible with the item descriptions. If you cannot see individual items, create at least one item with the receipt's total amount.
            """.trimIndent()

            // Query Gemini
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(
                    content {
                        text(prompt)
                        image(bitmap)
                    }
                ).text
            }

            // Parse the JSON response from Gemini
            val receiptData = parseGeminiResponse(response, imageUri)
            Result.success(receiptData)

        } catch (e: Exception) {
            Log.e("GeminiService", "Error processing receipt image", e)
            Result.failure(e)
        }
    }

    /**
     * Parse Gemini's response to extract structured receipt data
     */
    private fun parseGeminiResponse(response: String?, imageUri: Uri): ReceiptModel {
        // Handle null response case
        if (response == null) {
            Log.w("GeminiService", "Received null response from Gemini")
            throw Exception("NOT_A_RECEIPT: " + context.getString(R.string.error_unable_to_analyze_image))
        }

        // Extract JSON object from response (Gemini might wrap JSON in markdown code blocks)
        val jsonString = extractJsonFromResponse(response)

        try {
            val jsonObject = JSONObject(jsonString)

            // Extract basic receipt info
            val merchantName = jsonObject.optString("merchantName", "")
            val totalAmount = jsonObject.optDouble("totalAmount", 0.0)
            val dateStr = jsonObject.optString("date", "")
            // Set a default category for the receipt, items will have their own categories
            val category = "Lifestyle Expenses"

            // Check if this is likely not a receipt (missing key receipt information)
            if (merchantName.isBlank() && totalAmount == 0.0 && dateStr.isBlank()) {
                throw Exception("NOT_A_RECEIPT: " + context.getString(R.string.error_not_a_receipt))
            }

            // Parse date or use current date if not valid
            val date = parseDate(dateStr) ?: Date()

            // Extract items with enhanced details
            val itemsArray = jsonObject.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<ExpenseItem>()

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val description = itemObj.optString("description", "").takeIf { it.isNotEmpty() }
                    ?: "Unnamed Item"
                val amount = itemObj.optDouble("amount", 0.0)
                // Get category for this specific item
                val itemCategory = itemObj.optString("category", category)

                items.add(
                    ExpenseItem(
                        id = UUID.randomUUID().toString(),
                        description = description,
                        amount = amount,
                        category = itemCategory,
                        merchantName = merchantName,
                        date = date
                    )
                )
            }

            // If no items were extracted, create a default one with the total amount
            if (items.isEmpty() && totalAmount > 0) {
                items.add(
                    ExpenseItem(
                        id = UUID.randomUUID().toString(),
                        description = "Complete purchase",
                        amount = totalAmount,
                        category = category,
                        merchantName = merchantName,
                        date = date
                    )
                )
            }

            return ReceiptModel(
                merchantName = merchantName,
                total = totalAmount,
                date = date,
                category = category,
                imageUrl = imageUri.toString(),
                items = items
            )

        } catch (e: Exception) {
            // Check if it's our special error type and rethrow it
            if (e.message?.startsWith("NOT_A_RECEIPT:") == true) {
                throw e
            }

            Log.e("GeminiService", "Error parsing Gemini response: ${e.message}", e)
            // If JSON parsing fails, it's likely not a proper receipt
            throw Exception("NOT_A_RECEIPT: " + context.getString(R.string.error_image_not_recognized))
        }
    }


    /**
     * Extract JSON from Gemini response, which might be wrapped in markdown code blocks
     */
    private fun extractJsonFromResponse(response: String?): String {
        // Handle null response
        if (response == null) {
            Log.w("GeminiService", "Received null response from Gemini")
            return "{}"
        }

        // Check if response contains a JSON code block
        val codeBlockRegex = "```json\\s*([\\s\\S]*?)\\s*```".toRegex()
        val match = codeBlockRegex.find(response)

        return if (match != null) {
            // Extract the JSON from the code block
            match.groupValues[1].trim()
        } else {
            // Try to find a JSON object anywhere in the text
            val jsonRegex = "\\{[\\s\\S]*\\}".toRegex()
            val jsonMatch = jsonRegex.find(response)

            if (jsonMatch != null) {
                jsonMatch.value
            } else {
                // Return empty JSON object if no JSON found
                "{}"
            }
        }
    }

    /**
     * Parse date string in various formats
     */
    private fun parseDate(dateStr: String): Date? {
        if (dateStr.isEmpty()) return Date()

        val formats = arrayOf(
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "MM-dd-yyyy",
            "dd.MM.yyyy",
            "MM.dd.yyyy"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                return sdf.parse(dateStr)
            } catch (e: Exception) {
                // Try next format
            }
        }

        return null
    }

    /**
     * Convert Uri to Bitmap for Gemini API
     */
    private suspend fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                // Compress bitmap if needed (Gemini has size limits)
                if (bitmap != null && (bitmap.width > 1024 || bitmap.height > 1024)) {
                    val ratio = minOf(1024.0 / bitmap.width, 1024.0 / bitmap.height)
                    val width = (bitmap.width * ratio).toInt()
                    val height = (bitmap.height * ratio).toInt()
                    return@withContext Bitmap.createScaledBitmap(bitmap, width, height, true)
                }

                bitmap
            } catch (e: Exception) {
                Log.e("GeminiService", "Error converting URI to bitmap", e)
                null
            }
        }
    }

    /**
     * Analyze tax savings opportunities based on receipt data
     * Takes a list of ReceiptModel and returns potential tax savings by category
     */
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun analyzeTaxSavings(receipts: List<ReceiptModel>): Result<Map<String, Double>> {
        try {
            Log.d("GeminiService", "Analyzing tax savings for ${receipts.size} receipts")

            // Create a map to collect all expense items by category
            val categoryTotals = mutableMapOf<String, Double>()
            var totalSpending = 0.0

            // Calculate totals by item category instead of receipt category
            for (receipt in receipts) {
                for (item in receipt.items) {
                    val currentTotal = categoryTotals.getOrDefault(item.category, 0.0)
                    categoryTotals[item.category] = currentTotal + item.amount
                    totalSpending += item.amount
                }
            }

            // Prepare prompt for Gemini
            val categoriesText = categoryTotals.entries.joinToString("\n") {
                "- ${it.key}: RM ${String.format("%.2f", it.value)}"
            }

            val prompt = """
                Based on the following spending in Malaysian tax relief categories:
                $categoriesText
                
                Total spending: RM ${String.format("%.2f", totalSpending)}
                
                Please analyze and provide:
                1. Which categories qualify for tax relief in Malaysia
                2. The estimated tax savings for each category
                3. Any additional spending suggested to maximize tax benefits
                
                Return the result as a JSON object with category names as keys and potential savings as values.
            """.trimIndent()

            Log.d("GeminiService", "Sending prompt to Gemini: $prompt")

            // Query Gemini (text-only model is sufficient here)
            val textModel = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = apiKey
            )

            val response = withContext(Dispatchers.IO) {
                textModel.generateContent(prompt).text
            }

            Log.d("GeminiService", "Received response from Gemini: $response")

            // Parse the JSON response
            val jsonString = extractJsonFromResponse(response)
            Log.d("GeminiService", "Extracted JSON: $jsonString")

            val jsonObject = JSONObject(jsonString)

            val result = mutableMapOf<String, Double>()

            // Extract all keys from JSON
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                result[key] = jsonObject.optDouble(key, 0.0)
            }

            Log.d("GeminiService", "Analysis result: $result")
            return Result.success(result)

        } catch (e: Exception) {
            Log.e("GeminiService", "Error analyzing tax savings", e)
            return Result.failure(e)
        }
    }
}