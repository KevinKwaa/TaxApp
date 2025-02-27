package com.example.taxapp.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
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

// Describe element interactions to aid navigation
@Composable
fun ElementDescription(
    elementDescription: String,
    elementType: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    autoAnnounce: Boolean = false
) {
    val ttsManager = LocalTtsManager.current ?: return

    val fullDescription = buildString {
        append(elementDescription)
        append(", ")
        append(elementType)
        if (!enabled) {
            append(", disabled")
        }
        if (selected) {
            append(", selected")
        }
    }

    if (autoAnnounce) {
        LaunchedEffect(fullDescription) {
            ttsManager.speak(fullDescription)
        }
    }
}