package com.example.taxapp.chatbot.database

import android.content.Context
import com.example.taxapp.chatbot.ChatMessage
import com.example.taxapp.chatbot.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing chat message persistence
 */
class ChatRepository(context: Context) {
    private val chatMessageDao = ChatDatabase.getInstance(context).chatMessageDao()

    /**
     * Save a new chat message to the database
     */
    suspend fun saveMessage(message: ChatMessage) {
        val entity = ChatMessageEntity(
            id = message.id,
            text = message.text,
            messageType = message.type.name,
            timestamp = message.timestamp
        )
        chatMessageDao.insert(entity)
    }

    /**
     * Get all chat messages as a Flow
     */
    fun getAllMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    type = MessageType.valueOf(entity.messageType),
                    timestamp = entity.timestamp
                )
            }
        }
    }

    /**
     * Get the most recent messages
     */
    fun getRecentMessages(limit: Int): Flow<List<ChatMessage>> {
        return chatMessageDao.getRecentMessages(limit).map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    text = entity.text,
                    type = MessageType.valueOf(entity.messageType),
                    timestamp = entity.timestamp
                )
            }
        }
    }

    /**
     * Clear all chat history
     */
    suspend fun clearAllMessages() {
        chatMessageDao.deleteAllMessages()
    }

    /**
     * Delete a specific message
     */
    suspend fun deleteMessage(messageId: String) {
        chatMessageDao.deleteMessage(messageId)
    }

    /**
     * Get the total count of saved messages
     */
    suspend fun getMessageCount(): Int {
        return chatMessageDao.getMessageCount()
    }
}