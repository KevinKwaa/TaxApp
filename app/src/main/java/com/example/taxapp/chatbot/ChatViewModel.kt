package com.example.taxapp.chatbot

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxapp.chatbot.database.ChatRepository
import com.example.taxapp.user.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // Initialize the AI service with application context
    private val aiService = GeminiAIService(application.applicationContext)

    // Initialize the chat repository
    private val chatRepository = ChatRepository(application.applicationContext)

    // Flag to track if we're in history view mode
    private val _isHistoryViewActive = MutableStateFlow(false)
    val isHistoryViewActive: StateFlow<Boolean> = _isHistoryViewActive.asStateFlow()

    // Get the current user ID
    private val currentUserId: String?
        get() = FirebaseManager.getCurrentUserId()

    val chatHistory = FirebaseManager.currentUserFlow
        .flatMapLatest { userId ->
            if (userId != null) {
                Log.d(TAG, "User changed, loading messages for: $userId")
                chatRepository.getAllMessages(userId)
                    .catch { e ->
                        Log.e(TAG, "Error in chat history flow", e)
                        emit(emptyList())
                    }
            } else {
                Log.d(TAG, "No user logged in, returning empty messages")
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    init {
        Log.d(TAG, "ChatViewModel initialized")
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val userId = currentUserId
        if (userId == null) {
            Log.w(TAG, "Cannot send message: No user logged in")
            return
        }

        // Add user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = message,
            type = MessageType.USER
        )

        val updatedMessages = _chatState.value.messages + userMessage
        _chatState.value = _chatState.value.copy(
            messages = updatedMessages,
            isProcessing = true,
            userInput = ""
        )

        // Save user message to database with user ID
        viewModelScope.launch {
            try {
                chatRepository.saveMessage(userMessage, userId)
                Log.d(TAG, "User message saved for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user message", e)
            }
        }

        // Get AI response
        viewModelScope.launch {
            try {
                val response = aiService.getResponse(message)
                val botMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = response,
                    type = MessageType.BOT
                )

                _chatState.value = _chatState.value.copy(
                    messages = _chatState.value.messages + botMessage,
                    isProcessing = false
                )

                // Save bot message to database with user ID
                chatRepository.saveMessage(botMessage, userId)
                Log.d(TAG, "Bot response saved for user: $userId")
            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e(TAG, "Error getting AI response", e)
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    text = "Sorry, I couldn't process your request. Please try again later.",
                    type = MessageType.BOT
                )
                _chatState.value = _chatState.value.copy(
                    messages = _chatState.value.messages + errorMessage,
                    isProcessing = false
                )

                // Save error message to database with user ID
                try {
                    chatRepository.saveMessage(errorMessage, userId)
                } catch (innerE: Exception) {
                    Log.e(TAG, "Error saving error message", innerE)
                }
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

    fun clearChatHistory() {
        val userId = currentUserId
        if (userId == null) {
            Log.w(TAG, "Cannot clear chat history: No user logged in")
            return
        }

        viewModelScope.launch {
            try {
                chatRepository.clearAllMessages(userId)
                Log.d(TAG, "Chat history cleared for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing chat history", e)
            }
        }
    }

    fun deleteMessage(messageId: String) {
        val userId = currentUserId
        if (userId == null) {
            Log.w(TAG, "Cannot delete message: No user logged in")
            return
        }

        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(messageId, userId)
                Log.d(TAG, "Message $messageId deleted for user: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting message", e)
            }
        }
    }
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val userInput: String = "",
    val isProcessing: Boolean = false,
    val isChatVisible: Boolean = false,
    val isHistoryView: Boolean = false
)