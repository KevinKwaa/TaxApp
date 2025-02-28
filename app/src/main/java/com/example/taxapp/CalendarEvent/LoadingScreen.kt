package com.example.taxapp.CalendarEvent

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalThemeColors

@Composable
fun LoadingScreen(
    message: String = "Loading events...",
    accessibilityState: AccessibilityState = AccessibilityState()
) {
    // Get theme colors from accessibility settings
    val accessibleColors = LocalThemeColors.current

    // Create pulsating animation
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(accessibleColors.calendarBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // App logo or icon (can be replaced with your app icon)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(accessibleColors.selectedDay),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TA",
                    color = accessibleColors.selectedDayText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Circular progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = accessibleColors.selectedDay,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = accessibleColors.calendarText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}