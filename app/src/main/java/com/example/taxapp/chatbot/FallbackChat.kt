package com.example.taxapp.chatbot

/**
 * Fallback service that provides hardcoded responses when the AI service is unavailable
 * This preserves the original functionality as a backup
 */
class FallbackChatService {
    // Simple implementation with hardcoded responses, keeping original logic
    fun getResponse(userMessage: String): String {
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