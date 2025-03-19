package com.example.taxapp.chatbot

import android.app.Application
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taxapp.R
import com.example.taxapp.accessibility.LocalDarkMode
import com.example.taxapp.accessibility.LocalThemeColors
import com.example.taxapp.accessibility.LocalTtsManager

@Composable
fun ChatFAB(
    modifier: Modifier = Modifier
) {
    Log.d("ChatFAB", "Rendering ChatFAB")

    // Get the ViewModel using the viewModel() function
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )

    val chatState by chatViewModel.chatState.collectAsState()
    val isHistoryViewActive by chatViewModel.isHistoryViewActive.collectAsState()
    val accessibleColors = LocalThemeColors.current
    val ttsManager = LocalTtsManager.current

    // Get message count for badge
    val chatHistory by chatViewModel.chatHistory.collectAsState()
    val messageCount = chatHistory.size

    Box(modifier = modifier.fillMaxSize()) {
        // Just the draggable FAB with the badge
        SimpleDraggableBox {
            BadgedBox(
                badge = {
                    if (messageCount > 0) {
                        Badge {
                            val displayCount = if (messageCount > 99) "99+" else messageCount.toString()
                            Text(text = displayCount)
                        }
                    }
                }
            ) {
                FloatingActionButton(
                    onClick = {
                        chatViewModel.toggleChatVisibility()
                        ttsManager?.speak("Opening chat assistant")
                    },
                    modifier = Modifier.semantics {
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
            }
        }

        // Chat Dialog - positioned separately
        if (chatState.isChatVisible) {
            if (isHistoryViewActive) {
                ChatHistoryScreen(
                    chatViewModel = chatViewModel,
                    onClose = { chatViewModel.toggleHistoryView() }
                )
            } else {
                ChatDialog(
                    chatViewModel = chatViewModel,
                    isProcessing = chatState.isProcessing
                )
            }
        }
    }
}

// Factory for creating the ChatViewModel with an Application parameter
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun ChatDialog(
    chatViewModel: ChatViewModel,
    isProcessing: Boolean = false
) {
    val chatState by chatViewModel.chatState.collectAsState()
    val accessibleColors = LocalThemeColors.current
    val ttsManager = LocalTtsManager.current

    // Get message count for badge and history button
    val chatHistory by chatViewModel.chatHistory.collectAsState()
    val historyCount = chatHistory.size

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_assistant),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = accessibleColors.headerText
                        )

                        // Show loading indicator when processing
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = accessibleColors.selectedDay,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Row {
                        // History button with badge
                        BadgedBox(
                            badge = {
                                if (historyCount > 0) {
                                    Badge {
                                        val displayCount = if (historyCount > 99) "99+" else historyCount.toString()
                                        Text(text = displayCount)
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = {
                                    chatViewModel.toggleHistoryView()
                                    ttsManager?.speak("Opening chat history")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "View chat history",
                                    tint = accessibleColors.headerText
                                )
                            }
                        }

                        // Close button
                        IconButton(
                            onClick = {
                                chatViewModel.toggleChatVisibility()
                                ttsManager?.speak("Closing chat assistant")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close_chat),
                                tint = accessibleColors.headerText
                            )
                        }
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

                    // Show typing indicator if processing
                    if (isProcessing) {
                        item {
                            TypingIndicator()
                        }
                    }
                }

                // Auto-scroll to the bottom when new messages are added
                LaunchedEffect(chatState.messages.size, isProcessing) {
                    if (chatState.messages.isNotEmpty() || isProcessing) {
                        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
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
                        placeholder = { Text(stringResource(R.string.type_your_question)) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        enabled = !isProcessing
                    )

                    IconButton(
                        onClick = {
                            chatViewModel.sendMessage(chatState.userInput)
                            ttsManager?.speak("Message sent")
                        },
                        enabled = chatState.userInput.isNotBlank() && !isProcessing,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (chatState.userInput.isNotBlank() && !isProcessing)
                                    accessibleColors.buttonBackground
                                else
                                    accessibleColors.buttonBackground.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = stringResource(R.string.send_message),
                            tint = accessibleColors.buttonText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val accessibleColors = LocalThemeColors.current
    val dotSize = 8.dp
    val animationDuration = 1000

    // Animated dots
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val dotOneAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dotTwoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dotThreeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    // Typing indicator container
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .widthIn(max = 100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accessibleColors.cardBackground.copy(alpha = 0.7f))
            .padding(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(accessibleColors.selectedDay.copy(alpha = dotOneAlpha))
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(accessibleColors.selectedDay.copy(alpha = dotTwoAlpha))
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(accessibleColors.selectedDay.copy(alpha = dotThreeAlpha))
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val accessibleColors = LocalThemeColors.current
    val isDarkMode = LocalDarkMode.current
    val ttsManager = LocalTtsManager.current

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
        contentAlignment = if (message.type == MessageType.USER) Alignment.CenterEnd else Alignment.CenterStart
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
                .clickable { ttsManager?.speak(message.text) }
                .padding(12.dp)
                .semantics {
                    contentDescription = "${if (message.type == MessageType.USER) "You" else "Assistant"}: ${message.text}"
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