package com.example.taxapp.chatbot

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taxapp.R
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager

@Composable
fun ChatFAB(
    chatViewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val chatState by chatViewModel.chatState.collectAsState()
    val accessibleColors = LocalThemeColors.current
    val ttsManager = LocalTtsManager.current

    Box(modifier = modifier) {
        // Chat FAB
        FloatingActionButton(
            onClick = {
                chatViewModel.toggleChatVisibility()
                ttsManager?.speak("Opening chat assistant")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .semantics {
                    contentDescription = "Open AI chat assistant"
                },
            containerColor = accessibleColors.buttonBackground,
            contentColor = accessibleColors.buttonText
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = null
            )
        }

        // Chat Dialog
        if (chatState.isChatVisible) {
            ChatDialog(
                chatViewModel = chatViewModel
            )
        }
    }
}

@Composable
fun ChatDialog(
    chatViewModel: ChatViewModel
) {
    val chatState by chatViewModel.chatState.collectAsState()
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val ttsManager = LocalTtsManager.current

    Dialog(
        onDismissRequest = { chatViewModel.toggleChatVisibility() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxSize(0.7f),
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
                // Chat Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accessibleColors.headerText
                    )

                    IconButton(
                        onClick = {
                            chatViewModel.toggleChatVisibility()
                            ttsManager?.speak("Closing chat assistant")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close chat",
                            tint = accessibleColors.headerText
                        )
                    }
                }

                // Chat Messages
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    reverseLayout = false
                ) {
                    items(chatState.messages) { message ->
                        ChatMessageItem(message = message)
                    }
                }

                // Auto-scroll to the bottom when new messages are added
                LaunchedEffect(chatState.messages.size) {
                    if (chatState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(chatState.messages.size - 1)
                    }
                }

                // Input Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatState.userInput,
                        onValueChange = { chatViewModel.updateUserInput(it) },
                        placeholder = { Text("Type your question...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            chatViewModel.sendMessage(chatState.userInput)
                            ttsManager?.speak("Message sent")
                        },
                        enabled = chatState.userInput.isNotBlank() && !chatState.isProcessing,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (chatState.userInput.isNotBlank() && !chatState.isProcessing)
                                    accessibleColors.buttonBackground
                                else
                                    accessibleColors.buttonBackground.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send message",
                            tint = accessibleColors.buttonText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val ttsManager = LocalTtsManager.current

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.type == MessageType.USER) 16.dp else 0.dp,
                        bottomEnd = if (message.type == MessageType.USER) 0.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp)
                .semantics {
                    contentDescription = "${if (message.type == MessageType.USER) "You" else "Assistant"}: ${message.text}"
                }
                .clickable {
                    ttsManager?.speak(message.text)
                }
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}