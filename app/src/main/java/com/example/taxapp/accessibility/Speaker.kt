package com.example.taxapp.accessibility

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

// A button that speaks text when clicked
@Composable
fun SpeakButton(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Blue,
    contentDescription: String = "Read aloud"
) {
    val ttsManager = LocalTtsManager.current ?: return
    val isSpeaking by ttsManager.isSpeaking.collectAsState()

    IconButton(
        onClick = {
            if (isSpeaking) {
                ttsManager.stop()
            } else {
                ttsManager.speak(text)
            }
        },
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(contentDescription) {
                    ttsManager.speak(text)
                    true
                }
            )
        }
    ) {
        // Use a simple text icon instead of the vector icon
        Text(
            text = "🔊", // Unicode speaker symbol
            color = if (isSpeaking) tint.copy(alpha = 0.5f) else tint
        )
    }
}