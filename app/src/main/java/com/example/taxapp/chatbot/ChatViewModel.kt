package com.example.taxapp.chatbot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enhanced ViewModel that uses GeminiAIService
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // Initialize the AI service with application context
    private val aiService = GeminiAIService(application.applicationContext)

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(
            text = message,
            type = MessageType.USER
        )

        val updatedMessages = _chatState.value.messages + userMessage
        _chatState.value = _chatState.value.copy(
            messages = updatedMessages,
            isProcessing = true,
            userInput = ""
        )

        // Get AI response
        viewModelScope.launch {
            try {
                val response = aiService.getResponse(message)
                val botMessage = ChatMessage(
                    text = response,
                    type = MessageType.BOT
                )

                _chatState.value = _chatState.value.copy(
                    messages = _chatState.value.messages + botMessage,
                    isProcessing = false
                )
            } catch (e: Exception) {
                // Handle errors gracefully
                val errorMessage = ChatMessage(
                    text = "Sorry, I couldn't process your request. Please try again later.",
                    type = MessageType.BOT
                )
                _chatState.value = _chatState.value.copy(
                    messages = _chatState.value.messages + errorMessage,
                    isProcessing = false
                )
            }
        }
    }

    fun updateUserInput(input: String) {
        _chatState.value = _chatState.value.copy(userInput = input)
    }

    fun toggleChatVisibility() {
        _chatState.value = _chatState.value.copy(isChatVisible = !_chatState.value.isChatVisible)
    }

    fun clearChat() {
        _chatState.value = _chatState.value.copy(messages = emptyList())
        // Also clear conversation history in the AI service
        aiService.clearConversationHistory()
    }
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isProcessing: Boolean = false,
    val isChatVisible: Boolean = false
)