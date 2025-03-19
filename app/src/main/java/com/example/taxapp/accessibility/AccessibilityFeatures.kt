package com.example.taxapp.accessibility

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.taxapp.R
import kotlinx.coroutines.launch

data class AccessibilityState(
    var fontSize: Int = 10,  // Changed default to match UI
    var textToSpeech: Boolean = false,
    var colorBlindMode: Boolean = false,
    var colorBlindnessType: ColorBlindnessType = ColorBlindnessType.NONE,
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
                    .verticalScroll(rememberScrollState())
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

                    // Color Blind Mode with Type Selector
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
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
                                    // If disabling, also reset to NONE
                                    if (!it) {
                                        tempSettings = tempSettings.copy(colorBlindnessType = ColorBlindnessType.NONE)
                                    } else if (tempSettings.colorBlindnessType == ColorBlindnessType.NONE) {
                                        // Default to the most common type when enabling
                                        tempSettings = tempSettings.copy(colorBlindnessType = ColorBlindnessType.DEUTERANOPIA)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accessibleColors.selectedDay,
                                    checkedTrackColor = accessibleColors.selectedDay.copy(alpha = 0.5f),
                                    uncheckedThumbColor = if (isDarkMode) accessibleColors.buttonBackground else MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = if (isDarkMode) accessibleColors.calendarBorder else MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        // Color Blindness Type Selector (only shown when colorBlindMode is enabled)
                        AnimatedVisibility(
                            visible = tempSettings.colorBlindMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode)
                                        Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = accessibleColors.calendarBorder.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Type of Color Blindness:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = textColor,
                                        fontWeight = FontWeight.Medium
                                    )

                                    // Deuteranopia option
                                    ColorBlindTypeOption(
                                        title = "Deuteranopia (Red-Green)",
                                        description = "Most common form, affects ~6% of males",
                                        isSelected = tempSettings.colorBlindnessType == ColorBlindnessType.DEUTERANOPIA,
                                        onClick = {
                                            tempSettings = tempSettings.copy(colorBlindnessType = ColorBlindnessType.DEUTERANOPIA)
                                        },
                                        accessibleColors = accessibleColors
                                    )

                                    // Protanopia option
                                    ColorBlindTypeOption(
                                        title = "Protanopia (Red-Green)",
                                        description = "Less common, affects ~1% of males",
                                        isSelected = tempSettings.colorBlindnessType == ColorBlindnessType.PROTANOPIA,
                                        onClick = {
                                            tempSettings = tempSettings.copy(colorBlindnessType = ColorBlindnessType.PROTANOPIA)
                                        },
                                        accessibleColors = accessibleColors
                                    )

                                    // Tritanopia option
                                    ColorBlindTypeOption(
                                        title = "Tritanopia (Blue-Yellow)",
                                        description = "Rare, affects ~0.01% of population",
                                        isSelected = tempSettings.colorBlindnessType == ColorBlindnessType.TRITANOPIA,
                                        onClick = {
                                            tempSettings = tempSettings.copy(colorBlindnessType = ColorBlindnessType.TRITANOPIA)
                                        },
                                        accessibleColors = accessibleColors
                                    )

                                    // Achromatopsia option
                                    ColorBlindTypeOption(
                                        title = "Achromatopsia (Monochromacy)",
                                        description = "Very rare, complete color blindness",
                                        isSelected = tempSettings.colorBlindnessType == ColorBlindnessType.ACHROMATOPSIA,
                                        onClick = {
                                            tempSettings = tempSettings.copy(colorBlindnessType = ColorBlindnessType.ACHROMATOPSIA)
                                        },
                                        accessibleColors = accessibleColors
                                    )
                                }
                            }
                        }
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

                // Color Preview Section
                AnimatedVisibility(
                    visible = tempSettings.colorBlindMode && tempSettings.colorBlindnessType != ColorBlindnessType.NONE,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Color Preview",
                            style = MaterialTheme.typography.titleMedium,
                            color = titleColor
                        )

                        ColorPreviewPalette(
                            colorBlindnessType = tempSettings.colorBlindnessType,
                            isDarkMode = isDarkMode
                        )
                    }
                }

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
fun ColorBlindTypeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accessibleColors: AccessibleColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) accessibleColors.selectedDay.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) accessibleColors.selectedDay else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = accessibleColors.calendarText,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = accessibleColors.calendarText.copy(alpha = 0.7f)
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = accessibleColors.selectedDay,
                unselectedColor = accessibleColors.calendarBorder
            )
        )
    }
}

@Composable
fun ColorPreviewPalette(
    colorBlindnessType: ColorBlindnessType,
    isDarkMode: Boolean
) {
    // Sample set of colors to demonstrate the effects
    val originalColors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Magenta,
        Color.Cyan
    )

    // Create modified colors based on the selected color blindness type
    val modifiedColors = originalColors.map { color ->
        adjustForColorBlindness(color, colorBlindnessType)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF8F8F8)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Original colors row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Original Colors",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    originalColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .border(
                                    width = 1.dp,
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.3f)
                                    else Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Divider(
                color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
            )

            // Simulated view for people with this type of color blindness
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Simulated View with ${colorBlindnessType.name.lowercase().capitalize()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    modifiedColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .border(
                                    width = 1.dp,
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.3f)
                                    else Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Divider(
                color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
            )

            // Adapted color scheme used by your app
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Adapted Color Scheme",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Display a sample of the adapted color scheme
                    val baseScheme = MaterialTheme.colorScheme
                    val adaptedScheme = createColorBlindPalette(baseScheme, colorBlindnessType)

                    val adaptedColors = listOf(
                        adaptedScheme.primary,
                        adaptedScheme.secondary,
                        adaptedScheme.error,
                        adaptedScheme.tertiary,
                        adaptedScheme.primaryContainer,
                        adaptedScheme.secondaryContainer
                    )

                    adaptedColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .border(
                                    width = 1.dp,
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.3f)
                                    else Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

// Extension function to capitalize the first letter of a string
fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this.substring(0, 1).uppercase() + this.substring(1)
    } else {
        this
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