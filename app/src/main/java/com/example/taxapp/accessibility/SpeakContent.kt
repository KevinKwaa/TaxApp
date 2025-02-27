package com.example.taxapp.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import android.speech.tts.TextToSpeech

// A content wrapper that can be read aloud on click
@Composable
fun SpeakableContent(
    text: String,
    modifier: Modifier = Modifier,
    enableClickToSpeak: Boolean = true,
    content: @Composable () -> Unit
) {
    val ttsManager = LocalTtsManager.current

    Box(
        modifier = if (enableClickToSpeak && ttsManager != null) {
            modifier
                .clickable { ttsManager.speak(text) }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Read aloud") {
                            ttsManager.speak(text)
                            true
                        }
                    )
                }
        } else {
            modifier
        }
    ) {
        content()
    }
}