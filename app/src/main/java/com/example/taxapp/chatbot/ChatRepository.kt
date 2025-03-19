package com.example.taxapp.chatbot.database

import android.content.Context
import android.util.Log
import com.example.taxapp.chatbot.ChatMessage
import com.example.taxapp.chatbot.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ChatRepository(context: Context) {
    private val TAG = "ChatRepository"
    private val chatMessageDao: ChatMessageDao

    init {
        val db = ChatDatabase.getInstance(context)
        chatMessageDao = db.chatMessageDao()
        Log.d(TAG, "ChatRepository initialized")
    }

    /**
     * Save a new chat message to the database
     */
    suspend fun saveMessage(message: ChatMessage, userId: String) {
        try {
            if (userId.isBlank()) {
                Log.w(TAG, "Attempted to save message with blank userId")
                return
            }

            val entity = ChatMessageEntity(
                id = message.id,
                text = message.text,
                messageType = message.type.name,
                timestamp = message.timestamp,
                userId = userId
            )

            Log.d(TAG, "Saving message for user $userId: ${message.text.take(20)}...")
            chatMessageDao.insert(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message", e)
        }
    }

    /**
     * Get all chat messages as a Flow
     */
    fun getAllMessages(userId: String): Flow<List<ChatMessage>> {
        Log.d(TAG, "Getting all messages for user: $userId")
        return chatMessageDao.getAllMessages(userId)
            .map { entities ->
                Log.d(TAG, "Retrieved ${entities.size} messages for user $userId")
                entities.map { entity ->
                    ChatMessage(
                        id = entity.id,
                        text = entity.text,
                        type = MessageType.valueOf(entity.messageType),
                        timestamp = entity.timestamp
                    )
                }
            }
            .catch { e ->
                Log.e(TAG, "Error getting messages for user $userId", e)
                emit(emptyList())
            }
    }

    /**
     * Clear all chat history
     */
    suspend fun clearAllMessages(userId: String) {
        try {
            Log.d(TAG, "Clearing all messages for user: $userId")
            chatMessageDao.deleteAllMessages(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing messages for user $userId", e)
        }
    }

    /**
     * Delete a specific message
     */
    suspend fun deleteMessage(messageId: String, userId: String) {
        try {
            Log.d(TAG, "Deleting message $messageId for user: $userId")
            chatMessageDao.deleteMessage(messageId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message $messageId for user $userId", e)
        }
    }
}