package com.example.taxapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Status of an operation
 */
enum class OperationStatus {
    SUCCESS,
    ERROR,
    LOADING
}

/**
 * A snackbar to show operation status feedback
 */
@Composable
fun StatusSnackbar(
    visible: Boolean,
    message: String,
    status: OperationStatus = OperationStatus.SUCCESS,
    onDismiss: () -> Unit,
    durationMillis: Long = 3000,
    modifier: Modifier = Modifier
) {
    var showSnackbar by remember(visible) { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            showSnackbar = true
            delay(durationMillis)
            showSnackbar = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = showSnackbar,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (status) {
                            OperationStatus.SUCCESS -> Color(0xFF4CAF50) // Green
                            OperationStatus.ERROR -> Color(0xFFF44336) // Red
                            OperationStatus.LOADING -> Color(0xFF2196F3) // Blue
                        }
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                when (status) {
                    OperationStatus.SUCCESS -> {
                        Text(
                            text = "✓",
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    OperationStatus.ERROR -> {
                        Text(
                            text = "✗",
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    OperationStatus.LOADING -> {
                        Text(
                            text = "⟳",
                            color = Color.White,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                // Message
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}