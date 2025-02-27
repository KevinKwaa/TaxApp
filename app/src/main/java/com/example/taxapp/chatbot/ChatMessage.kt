package com.example.taxapp.chatbot

import java.util.UUID

enum class MessageType {
    USER,
    BOT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis()
)