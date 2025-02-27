package com.example.taxapp.chatbot

import kotlinx.coroutines.delay

class AIChatService {

    // Simple implementation with hardcoded responses
    suspend fun getResponse(userMessage: String): String {
        // Simulate network delay
        delay(500)

        return when {
            userMessage.contains("event", ignoreCase = true) ->
                "To add an event, tap the 'Add Event' button on the calendar screen and fill in the event details."

            userMessage.contains("reminder", ignoreCase = true) ->
                "You can toggle reminders for events by using the switch on the event creation screen."

            userMessage.contains("language", ignoreCase = true) ->
                "To change the language, tap the globe icon in the top-left corner of the screen."

            userMessage.contains("accessibility", ignoreCase = true) ->
                "Our app includes text-to-speech, high contrast mode, and adjustable font sizes. Access these settings by tapping the gear icon."

            userMessage.contains("hello", ignoreCase = true) ||
                    userMessage.contains("hi", ignoreCase = true) ->
                "Hello! I'm your scheduling assistant. How can I help you today?"

            userMessage.contains("thank", ignoreCase = true) ->
                "You're welcome! Is there anything else I can help you with?"

            else -> "I'm not sure I understand. You can ask me about creating events, setting reminders, changing language, or accessibility features."
        }
    }
}