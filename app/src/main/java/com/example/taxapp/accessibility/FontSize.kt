package com.example.taxapp.accessibility

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Create a composition local to hold the font size scaling factor
val LocalFontSizeAdjustment = compositionLocalOf { 1.0f }

@Composable
fun FontSizeProvider(
    fontSizeAdjustment: Int,
    content: @Composable () -> Unit
) {
    // Convert the numeric setting (8-20) to a scaling factor (0.8 to 2.0)
    val scaleFactor = remember(fontSizeAdjustment) {
        when {
            fontSizeAdjustment <= 10 -> 1.0f  // Default (10) maps to scale factor 1.0
            else -> 1.0f + (fontSizeAdjustment - 10) * 0.1f  // Each step above 10 adds 10%
        }
    }

    // Provide the scale factor through composition local
    CompositionLocalProvider(LocalFontSizeAdjustment provides scaleFactor) {
        // Create a custom typography that scales all font sizes
        val currentTypography = MaterialTheme.typography
        val scaledTypography = remember(scaleFactor) {
            Typography(
                // Scale each text style in the typography
                displayLarge = currentTypography.displayLarge.scaled(scaleFactor),
                displayMedium = currentTypography.displayMedium.scaled(scaleFactor),
                displaySmall = currentTypography.displaySmall.scaled(scaleFactor),
                headlineLarge = currentTypography.headlineLarge.scaled(scaleFactor),
                headlineMedium = currentTypography.headlineMedium.scaled(scaleFactor),
                headlineSmall = currentTypography.headlineSmall.scaled(scaleFactor),
                titleLarge = currentTypography.titleLarge.scaled(scaleFactor),
                titleMedium = currentTypography.titleMedium.scaled(scaleFactor),
                titleSmall = currentTypography.titleSmall.scaled(scaleFactor),
                bodyLarge = currentTypography.bodyLarge.scaled(scaleFactor),
                bodyMedium = currentTypography.bodyMedium.scaled(scaleFactor),
                bodySmall = currentTypography.bodySmall.scaled(scaleFactor),
                labelLarge = currentTypography.labelLarge.scaled(scaleFactor),
                labelMedium = currentTypography.labelMedium.scaled(scaleFactor),
                labelSmall = currentTypography.labelSmall.scaled(scaleFactor)
            )
        }

        // Use a custom MaterialTheme with our scaled typography
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            shapes = MaterialTheme.shapes,
            typography = scaledTypography,
            content = content
        )
    }
}

// Extension function to scale a TextStyle's fontSize
private fun TextStyle.scaled(factor: Float): TextStyle {
    return this.copy(
        fontSize = this.fontSize * factor
    )
}

// Extension function to make font scaling more convenient
@Composable
fun scaledSp(sp: Int): TextUnit {
    val scaleFactor = LocalFontSizeAdjustment.current
    return (sp * scaleFactor).sp
}