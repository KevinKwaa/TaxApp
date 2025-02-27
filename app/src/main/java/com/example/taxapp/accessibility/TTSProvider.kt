package com.example.taxapp.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Create a composition local for TTS manager
val LocalTtsManager = compositionLocalOf<AccessibilityTtsManager?> { null }

@Composable
fun TtsProvider(
    isEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Create or remember the TTS manager if enabled
    val ttsManager = remember(isEnabled) {
        if (isEnabled) AccessibilityTtsManager.getInstance(context) else null
    }

    // Clean up resources when the composable leaves composition
    DisposableEffect(isEnabled) {
        onDispose {
            if (!isEnabled && ttsManager != null) {
                AccessibilityTtsManager.getInstance(context).shutdown()
            }
        }
    }

    CompositionLocalProvider(LocalTtsManager provides ttsManager) {
        content()
    }
}