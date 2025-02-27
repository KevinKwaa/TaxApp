package com.example.taxapp.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Simple ViewModel that doesn't use Room
class ChatViewModel : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val aiService = AIChatService()

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
            val response = aiService.getResponse(message)
            val botMessage = ChatMessage(
                text = response,
                type = MessageType.BOT
            )

            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages + botMessage,
                isProcessing = false
            )
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
    }
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isProcessing: Boolean = false,
    val isChatVisible: Boolean = false
)