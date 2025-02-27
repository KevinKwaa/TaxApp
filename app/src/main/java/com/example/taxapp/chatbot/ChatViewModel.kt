package com.example.taxapp.chatbot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.chatbot.database.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enhanced ViewModel that uses GeminiAIService and persists chat history
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // Initialize the AI service with application context
    private val aiService = GeminiAIService(application.applicationContext)

    // Initialize the chat repository
    private val chatRepository = ChatRepository(application.applicationContext)

    // Stored chat history for history view
    val chatHistory = chatRepository.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    // Flag to track if we're in history view mode
    private val _isHistoryViewActive = MutableStateFlow(false)
    val isHistoryViewActive: StateFlow<Boolean> = _isHistoryViewActive.asStateFlow()

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

        // Save user message to database
        viewModelScope.launch {
            chatRepository.saveMessage(userMessage)
        }

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

                // Save bot message to database
                chatRepository.saveMessage(botMessage)
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

                // Save error message to database
                chatRepository.saveMessage(errorMessage)
            }
        }
    }

    fun updateUserInput(input: String) {
        _chatState.value = _chatState.value.copy(userInput = input)
    }

    fun toggleChatVisibility() {
        _chatState.value = _chatState.value.copy(
            isChatVisible = !_chatState.value.isChatVisible,
            // Reset history view when toggling chat visibility
            isHistoryView = false
        )

        // Reset history view active state
        _isHistoryViewActive.value = false
    }

    fun toggleHistoryView() {
        _isHistoryViewActive.value = !_isHistoryViewActive.value
    }

    fun clearChat() {
        _chatState.value = _chatState.value.copy(messages = emptyList())
        // Also clear conversation history in the AI service
        aiService.clearConversationHistory()
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.clearAllMessages()
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    /**
     * Format a timestamp for display
     */
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault())
        return format.format(date)
    }
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isProcessing: Boolean = false,
    val isChatVisible: Boolean = false,
    val isHistoryView: Boolean = false
)