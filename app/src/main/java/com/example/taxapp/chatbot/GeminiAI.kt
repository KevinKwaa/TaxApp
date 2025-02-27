package com.example.taxapp.chatbot

import android.content.Context
import android.util.Log
import com.example.taxapp.BuildConfig
import com.example.taxapp.utils.NetworkUtil
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Service class for interacting with Google's Gemini AI API.
 * Handles conversations with the AI model for the chatbot feature.
 * Includes fallback to hardcoded responses for offline scenarios.
 */
class GeminiAIService(private val context: Context) {
    // Initialize the Gemini GenerativeModel
    private val generativeModel by lazy {
        try {
            Log.d("GeminiAIService", "API Key: ${BuildConfig.GEMINI_API_KEY.take(5)}...")
            Log.d("GeminiAIService", "Network Status: ${NetworkUtil.isOnline(context)}")
            GenerativeModel(
                modelName = "gemini-pro",
                apiKey = BuildConfig.GEMINI_API_KEY,
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
//    private fun buildPrompt(userMessage: String): String {
//        val promptBuilder = StringBuilder()
//
//        // Add app-specific context and guidelines
//        promptBuilder.append("You are an AI assistant for a scheduling and calendar app called TaxApp. ")
//        promptBuilder.append("Your role is to help users with features like creating events, setting reminders, ")
//        promptBuilder.append("changing language settings (the app supports English, Chinese, and Malay), ")
//        promptBuilder.append("and using accessibility features (text-to-speech, high contrast mode, ")
//        promptBuilder.append("font size adjustment, color blind mode, and dark mode). ")
//        promptBuilder.append("Keep your responses concise, helpful, and friendly. ")
//
//        // Add chat history for context
//        if (chatHistory.isNotEmpty()) {
//            promptBuilder.append("\n\nPrevious conversation:\n")
//            chatHistory.forEach { (userMsg, botMsg) ->
//                promptBuilder.append("User: $userMsg\n")
//                promptBuilder.append("Assistant: $botMsg\n")
//            }
//        }
//
//        // Add the current user message
//        promptBuilder.append("\nUser: $userMessage\n")
//        promptBuilder.append("Assistant: ")
//
//        return promptBuilder.toString()
//    }
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

    /**
     * Clear the conversation history
     */
    fun clearConversationHistory() {
        chatHistory.clear()
    }
}