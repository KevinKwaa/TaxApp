package com.example.taxapp.accessibility

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.taxapp.R
import kotlinx.coroutines.launch

data class AccessibilityState(
    var fontSize: Int = 10,  // Changed default to match UI
    var textToSpeech: Boolean = false,
    var colorBlindMode: Boolean = false,
    var highContrastMode: Boolean = false,
    var darkMode: Boolean = false,
    var ttsSpeechRate: Float = 1.0f,
    var ttsPitch: Float = 1.0f
)

@Composable
fun AccessibilitySettings(
    currentSettings: AccessibilityState,
    onSettingsChanged: (AccessibilityState) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    var tempSettings by remember { mutableStateOf(currentSettings) }

    // Get the theme colors to ensure text is visible in dark mode
    val isDarkMode = LocalDarkMode.current
    val accessibleColors = LocalThemeColors.current

    // Determine text color based on mode
    val textColor = accessibleColors.calendarText
    val titleColor = accessibleColors.headerText

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = accessibleColors.cardBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.accessibility_features),
                    style = MaterialTheme.typography.headlineMedium,
                    color = titleColor
                )

                // Font Size with better spacing
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.font_size),
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (tempSettings.fontSize > 8) {
                                    tempSettings = tempSettings.copy(fontSize = tempSettings.fontSize - 1)
                                }
                            }
                        ) {
                            Text("-",
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor
                            )
                        }
                        Text(
                            text = tempSettings.fontSize.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        IconButton(
                            onClick = {
                                if (tempSettings.fontSize < 20) {
                                    tempSettings = tempSettings.copy(fontSize = tempSettings.fontSize + 1)
                                }
                            }
                        ) {
                            Text("+",
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor
                            )
                        }
                    }
                }

                // Text-to-Speech
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = stringResource(R.string.text_to_speech),
//                        style = MaterialTheme.typography.titleMedium,
//                        color = textColor
//                    )
//                    Switch(
//                        checked = tempSettings.textToSpeech,
//                        onCheckedChange = {
//                            tempSettings = tempSettings.copy(textToSpeech = it)
//                        },
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = accessibleColors.selectedDay,
//                            checkedTrackColor = accessibleColors.selectedDay.copy(alpha = 0.5f),
//                            uncheckedThumbColor = if (isDarkMode) accessibleColors.buttonBackground else MaterialTheme.colorScheme.outline,
//                            uncheckedTrackColor = if (isDarkMode) accessibleColors.calendarBorder else MaterialTheme.colorScheme.outlineVariant
//                        )
//                    )
//                }
                TtsControls(
                    isEnabled = tempSettings.textToSpeech,
                    onEnabledChange = { enabled ->
                        tempSettings = tempSettings.copy(textToSpeech = enabled)
                    }
                )

                // Visual Accessibility Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(R.string.visual_accessibility),
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor
                    )

                    // Color Blind Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.color_blind_mode),
                            color = textColor
                        )
                        Switch(
                            checked = tempSettings.colorBlindMode,
                            onCheckedChange = {
                                tempSettings = tempSettings.copy(colorBlindMode = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accessibleColors.selectedDay,
                                checkedTrackColor = accessibleColors.selectedDay.copy(alpha = 0.5f),
                                uncheckedThumbColor = if (isDarkMode) accessibleColors.buttonBackground else MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = if (isDarkMode) accessibleColors.calendarBorder else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // High Contrast Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.high_contrast_mode),
                            color = textColor
                        )
                        Switch(
                            checked = tempSettings.highContrastMode,
                            onCheckedChange = {
                                tempSettings = tempSettings.copy(highContrastMode = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accessibleColors.selectedDay,
                                checkedTrackColor = accessibleColors.selectedDay.copy(alpha = 0.5f),
                                uncheckedThumbColor = if (isDarkMode) accessibleColors.buttonBackground else MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = if (isDarkMode) accessibleColors.calendarBorder else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    // Dark Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.dark_mode),
                            color = textColor
                        )
                        Switch(
                            checked = tempSettings.darkMode,
                            onCheckedChange = {
                                tempSettings = tempSettings.copy(darkMode = it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accessibleColors.selectedDay,
                                checkedTrackColor = accessibleColors.selectedDay.copy(alpha = 0.5f),
                                uncheckedThumbColor = if (isDarkMode) accessibleColors.buttonBackground else MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = if (isDarkMode) accessibleColors.calendarBorder else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons in vertical arrangement
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            // Update both the local callback and the repository
                            onSettingsChanged(tempSettings)
                            coroutineScope.launch {
                                accessibilityRepository.updateSettings(tempSettings)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accessibleColors.buttonBackground,
                            contentColor = accessibleColors.buttonText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.apply_changes),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TtsControls(
    isEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use remember to avoid recreation on recomposition
    val context = LocalContext.current
    val ttsInstance = remember { TextToSpeech(context) { /* init */ } }

    // Clean up resources
    DisposableEffect(Unit) {
        onDispose {
            ttsInstance.shutdown()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main TTS toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.text_to_speech),
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = { enabled ->
                    onEnabledChange(enabled)
                    if (enabled) {
                        // Use the local ttsInstance for immediate feedback
                        ttsInstance.speak(
                            "Text to speech enabled",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "feedback"
                        )
                    }
                }
            )
        }

        // Only show additional controls if TTS is enabled
        if (isEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp), // Indent for hierarchy
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stop_speaking),
                    style = MaterialTheme.typography.bodyMedium
                )

                IconButton(onClick = { ttsInstance.stop() }) {
                    Text("🔇", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun SpeechRateControl(
    currentRate: Float,
    onRateChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val ttsManager = LocalTtsManager.current

    Column(modifier = modifier) {
        Text(
            text = "Speech Rate",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Slow")

            Slider(
                value = currentRate,
                onValueChange = { newRate ->
                    onRateChange(newRate)
                    ttsManager?.let { tts ->
                        // Update TTS engine speech rate
                        tts.stop()
                        tts.updateSpeechRate(newRate)
                        tts.speak("Speech rate is now ${(newRate * 100).toInt()} percent")
                    }
                },
                valueRange = 0.5f..2.0f,
                steps = 6,
                modifier = Modifier.weight(1f)
            )

            Text("Fast")
        }
    }
}