package com.example.taxapp.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

// Announce screen changes automatically
@Composable
fun ScreenReader(
    screenName: String,
    autoAnnounce: Boolean = true
) {
    val ttsManager = LocalTtsManager.current ?: return

    LaunchedEffect(screenName) {
        if (autoAnnounce) {
            // Small delay to ensure other UI elements are ready
            delay(300)
            ttsManager.speak("$screenName screen")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.stop()
        }
    }
}