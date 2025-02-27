package com.example.taxapp.chatbot

import android.content.Context
import com.example.taxapp.BuildConfig
import com.example.taxapp.utils.NetworkUtil
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
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
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
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
        // Check if the device is online
        if (!NetworkUtil.isOnline(context)) {
            // Use fallback if offline
            return@withContext fallbackService.getResponse(userMessage)
        }

        try {
            // Build context-aware prompt with chat history and app-specific guidance
            val prompt = buildPrompt(userMessage)

            // Use the generative model to get a response
            val response = generativeModel.generateContent(prompt)

            // Extract and process the response text
            val responseText = processResponse(response)

            // Add the exchange to history (limit to last 5 exchanges for context window)
            chatHistory.add(Pair(userMessage, responseText))
            if (chatHistory.size > 5) {
                chatHistory.removeAt(0)
            }

            responseText
        } catch (e: Exception) {
            // Use the fallback service when there's an error with the AI service
            fallbackService.getResponse(userMessage)
        }
    }

    /**
     * Build a context-aware prompt including chat history and app-specific guidance
     */
    private fun buildPrompt(userMessage: String): String {
        val promptBuilder = StringBuilder()

        // Add app-specific context and guidelines
        promptBuilder.append("You are an AI assistant for a scheduling and calendar app called TaxApp. ")
        promptBuilder.append("Your role is to help users with features like creating events, setting reminders, ")
        promptBuilder.append("changing language settings (the app supports English, Chinese, and Malay), ")
        promptBuilder.append("and using accessibility features (text-to-speech, high contrast mode, ")
        promptBuilder.append("font size adjustment, color blind mode, and dark mode). ")
        promptBuilder.append("Keep your responses concise, helpful, and friendly. ")

        // Add chat history for context
        if (chatHistory.isNotEmpty()) {
            promptBuilder.append("\n\nPrevious conversation:\n")
            chatHistory.forEach { (userMsg, botMsg) ->
                promptBuilder.append("User: $userMsg\n")
                promptBuilder.append("Assistant: $botMsg\n")
            }
        }

        // Add the current user message
        promptBuilder.append("\nUser: $userMessage\n")
        promptBuilder.append("Assistant: ")

        return promptBuilder.toString()
    }

    /**
     * Process the AI model's response to extract the text content
     */
    private fun processResponse(response: GenerateContentResponse): String {
        return response.text?.trim() ?: "I'm sorry, I couldn't generate a response."
    }

    /**
     * Clear the conversation history
     */
    fun clearConversationHistory() {
        chatHistory.clear()
    }
}