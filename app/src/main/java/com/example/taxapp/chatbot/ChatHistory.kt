package com.example.taxapp.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taxapp.R
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager
import java.util.Calendar
import java.util.Date

@Composable
fun ChatHistoryScreen(
    chatViewModel: ChatViewModel,
    onClose: () -> Unit
) {
    val chatHistory by chatViewModel.chatHistory.collectAsState()
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val ttsManager = LocalTtsManager.current

    var showClearConfirmation by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = accessibleColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onClose()
                                ttsManager?.speak("Closing chat history")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to chat",
                                tint = accessibleColors.headerText
                            )
                        }

                        Text(
                            text = "Chat History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.headerText
                        )
                    }

                    IconButton(
                        onClick = { showClearConfirmation = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all history",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Chat history content
                if (chatHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No chat history yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = accessibleColors.calendarText.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Group messages by date
                        val groupedMessages = chatHistory.groupBy { message ->
                            getDateString(message.timestamp)
                        }

                        groupedMessages.forEach { (date, messages) ->
                            item {
                                DateHeader(date = date)
                            }

                            items(messages) { message ->
                                HistoryChatMessageItem(
                                    message = message,
                                    formattedTime = formatTime(message.timestamp),
                                    onDelete = { messageToDelete = message },
                                    onSpeakMessage = { ttsManager?.speak(message.text) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing all history
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Chat History") },
            text = { Text("Are you sure you want to delete all chat history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.clearChatHistory()
                        showClearConfirmation = false
                        ttsManager?.speak("Chat history cleared")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for deleting a single message
    messageToDelete?.let { message ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete Message") },
            text = { Text("Delete this message from your chat history?") },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.deleteMessage(message.id)
                        messageToDelete = null
                        ttsManager?.speak("Message deleted")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { messageToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = LocalThemeColors.current.calendarText.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun HistoryChatMessageItem(
    message: ChatMessage,
    formattedTime: String,
    onDelete: () -> Unit,
    onSpeakMessage: () -> Unit
) {
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current

    val alignment = if (message.type == MessageType.USER)
        Alignment.End else Alignment.Start

    val backgroundColor = if (message.type == MessageType.USER)
        accessibleColors.selectedDay
    else
        if (isDarkMode)
            accessibleColors.cardBackground.copy(alpha = 0.7f)
        else
            accessibleColors.calendarBorder.copy(alpha = 0.2f)

    val textColor = if (message.type == MessageType.USER)
        accessibleColors.selectedDayText
    else
        accessibleColors.calendarText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.type == MessageType.USER) 16.dp else 0.dp,
                        bottomEnd = if (message.type == MessageType.USER) 0.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .clickable { onSpeakMessage() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete message",
                    tint = if (message.type == MessageType.USER)
                        accessibleColors.selectedDayText.copy(alpha = 0.7f)
                    else
                        accessibleColors.calendarText.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Show message timestamp
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = accessibleColors.calendarText.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// Helper functions for date and time formatting
private fun getDateString(timestamp: Long): String {
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val yesterday = Calendar.getInstance()
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    yesterday.set(Calendar.HOUR_OF_DAY, 0)
    yesterday.set(Calendar.MINUTE, 0)
    yesterday.set(Calendar.SECOND, 0)
    yesterday.set(Calendar.MILLISECOND, 0)

    val messageDate = Calendar.getInstance()
    messageDate.timeInMillis = timestamp

    return when {
        messageDate.timeInMillis >= today.timeInMillis -> "Today"
        messageDate.timeInMillis >= yesterday.timeInMillis -> "Yesterday"
        else -> {
            val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return timeFormat.format(Date(timestamp))
}