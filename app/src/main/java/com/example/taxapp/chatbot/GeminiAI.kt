package com.example.taxapp.chatbot

import android.content.Context
import android.util.Log
import com.example.taxapp.BuildConfig
import com.example.taxapp.utils.NetworkUtil
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service class for interacting with Google's Gemini AI API.
 * Handles conversations with the AI model for the chatbot feature.
 * Includes fallback to hardcoded responses for offline scenarios.
 */
class GeminiAIService(private val context: Context) {
    // Initialize the Gemini GenerativeModel
    private val generativeModel by lazy {

        val requestOptions = RequestOptions(apiVersion = "v1")

        try {
            GenerativeModel(
                modelName = "gemini-2.0-flash",
                apiKey = BuildConfig.GEMINI_API_KEY,
                requestOptions = requestOptions,
            )
        } catch (e: Exception) {
            Log.e("GeminiAIService", "Error initializing GenerativeModel", e)
            null
        }
    }

    // Fallback service for offline or error scenarios
    private val fallbackService = FallbackChatService()

    // Keep track of conversation history for context
    private val chatHistory = mutableListOf<Pair<String, String>>()

    /**
     * Send a message to the Gemini AI model and get a response.
     * @param userMessage The message from the user
     * @return A response from the AI
     */
    suspend fun getResponse(userMessage: String): String = withContext(Dispatchers.IO) {
        // Build a comprehensive prompt
        val prompt = buildPrompt(userMessage)
        Log.d("GeminiAIService", "Full Prompt: $prompt")

        // Generate content with more explicit error handling
        val response = try {
            generativeModel!!.generateContent(prompt)
        } catch (e: Exception) {
            Log.e("GeminiAIService", "Error generating content (Ask Gemini)", e)

            // More detailed error reporting
            when (e) {
                is java.net.UnknownHostException ->
                    Log.e("GeminiAIService", "Network connectivity issue: ${e.message}")

                is java.io.IOException ->
                    Log.e("GeminiAIService", "API connection error: ${e.message}")

                is com.google.ai.client.generativeai.type.ServerException -> {
                    Log.e("GeminiAIService", "Server error details: ${e.message}")
                    // Extract status code if available
                    val errorMatch = Regex("\"code\":\\s*(\\d+)").find(e.message ?: "")
                    val statusCode = errorMatch?.groupValues?.get(1)
                    Log.e("GeminiAIService", "HTTP status code: $statusCode")

                    // Log model name being used
                    Log.e("GeminiAIService", "Model attempting to use: gemini-pro")
                }

                is IllegalArgumentException ->
                    Log.e("GeminiAIService", "Invalid input parameters: ${e.message}")

                else ->
                    Log.e("GeminiAIService", "Unknown error type: ${e.javaClass.simpleName}")
            }

            // Log SDK version if possible
            try {
                val sdkVersion = com.google.ai.client.generativeai.BuildConfig.VERSION_NAME
                Log.e("GeminiAIService", "Generative AI SDK version: $sdkVersion")
            } catch (ex: Exception) {
                Log.e("GeminiAIService", "Could not determine SDK version")
            }

            return@withContext fallbackService.getResponse(userMessage)
        }

        try {
            // Detailed logging for troubleshooting
            Log.d("GeminiAIService", "API Key: ${BuildConfig.GEMINI_API_KEY.take(5)}...")
            Log.d("GeminiAIService", "Network Status: ${NetworkUtil.isOnline(context)}")
            Log.d("GeminiAIService", "Generative Model: ${generativeModel != null}")

            // Validate model and API key
            if (generativeModel == null || BuildConfig.GEMINI_API_KEY.isBlank()) {
                Log.e("GeminiAIService", "GenerativeModel is null or API key is blank")
                return@withContext fallbackService.getResponse(userMessage)
            }

            // Build a comprehensive prompt
            val prompt = buildPrompt(userMessage)
            Log.d("GeminiAIService", "Full Prompt: $prompt")

            // Generate content with more explicit error handling
            val response = try {
                generativeModel!!.generateContent(prompt)
            } catch (e: Exception) {
                Log.e("GeminiAIService", "Error generating content", e)
                // Log specific exception details
                when (e) {
                    is IllegalArgumentException -> Log.e("GeminiAIService", "Invalid input: ${e.message}")
                    is java.io.IOException -> Log.e("GeminiAIService", "Network/IO error: ${e.message}")
                    else -> Log.e("GeminiAIService", "Unexpected error: ${e.message}")
                }
                return@withContext fallbackService.getResponse(userMessage)
            }

            // Process and log the response
            val responseText = processResponse(response)
            Log.d("GeminiAIService", "AI Response: $responseText")

            // Update chat history
            //updateChatHistory(userMessage, responseText)

            responseText
        } catch (e: Exception) {
            Log.e("GeminiAIService", "Unexpected error in getResponse", e)
            fallbackService.getResponse(userMessage)
        }
    }

    /**
     * Build a context-aware prompt including chat history and app-specific guidance
     */

    private fun buildPrompt(userMessage: String): String {
        return StringBuilder().apply {
            append("You are an AI assistant for a tax and scheduling app called TaxApp. ")
            append("Provide helpful, concise responses about app features, event scheduling, ")
            append("accessibility options, and general assistance. ")

            if (chatHistory.isNotEmpty()) {
                append("\nRecent conversation context:\n")
                chatHistory.takeLast(3).forEach { (user, bot) ->
                    append("User: ${user.take(100)}\n")
                    append("Assistant: ${bot.take(100)}\n")
                }
            }

            append("\nUser's latest message: $userMessage\n")
            append("Assistant: ")
        }.toString()
    }

    /**
     * Process the AI model's response to extract the text content
     */
    private fun processResponse(response: GenerateContentResponse): String {
        return response.text?.trim()?.takeIf { it.isNotBlank() }
            ?: run {
                Log.w("GeminiAIService", "Empty or null response from Gemini")
                "I'm sorry, I couldn't generate a meaningful response."
            }
    }
}