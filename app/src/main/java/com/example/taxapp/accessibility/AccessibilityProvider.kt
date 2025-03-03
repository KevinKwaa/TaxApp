package com.example.taxapp.accessibility

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.multiLanguage.LanguageProvider
import kotlin.math.pow

// Create composition locals for accessibility settings
val LocalColorBlindMode = compositionLocalOf { false }
val LocalHighContrastMode = compositionLocalOf { false }
val LocalDarkMode = compositionLocalOf { false }
val LocalThemeColors = compositionLocalOf<AccessibleColors> { error("No AccessibleColors provided") }

// Class to hold accessible color variations
class AccessibleColors(
    val calendarBackground: Color,
    val calendarSurface: Color,
    val calendarText: Color,
    val calendarBorder: Color,
    val selectedDay: Color,
    val selectedDayText: Color,
    val todayBackground: Color,
    val todayText: Color,
    val eventIndicator: Color,
    val buttonBackground: Color,
    val buttonText: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val headerText: Color
)

data class AccessibleThemeColors(
    // Background colors
    val lightBackground: Color = Color.White,
    val darkBackground: Color = Color(0xFF121212), // Dark mode standard background

    // Text colors - with high contrast for readability
    val lightText: Color = Color(0xFF000000),
    val darkText: Color = Color(0xFFEEEEEE),

    // Button colors with improved contrast
    val buttonBackground: Color = Color(0xFF0D47A1), // Deep blue with good contrast
    val buttonText: Color = Color.White,

    // Primary button with good contrast
    val primaryButtonBackground: Color = Color(0xFF2E7D32), // Green with sufficient contrast
    val primaryButtonText: Color = Color.White,

    // Secondary button
    val secondaryButtonBackground: Color = Color(0xFFD32F2F), // Red with good contrast
    val secondaryButtonText: Color = Color.White,

    // Calendar colors
    val calendarBackground: Color = Color(0xFFF5F5F5),
    val calendarDarkBackground: Color = Color(0xFF262626),
    val calendarText: Color = Color(0xFF212121),
    val calendarDarkText: Color = Color(0xFFEEEEEE),
    val calendarHighlight: Color = Color(0xFF1976D2),
    val calendarBorder: Color = Color(0xFFBDBDBD),

    // Error and success colors with good contrast for color blindness
    val errorColor: Color = Color(0xFFB71C1C), // Deep red
    val successColor: Color = Color(0xFF2E7D32), // Deep green
    val warningColor: Color = Color(0xFFE65100)  // Deep orange
)

@Composable
fun AccessibilityThemeProvider(
    accessibilityState: AccessibilityState,
    content: @Composable () -> Unit
) {
    // Get system dark mode as reference
    val isSystemDarkMode = isSystemInDarkTheme()

    // Use either user preference or system setting
    val effectiveDarkMode = accessibilityState.darkMode

    // Create color schemes based on accessibility settings
    val colorScheme = createAccessibleColorScheme(
        isDarkMode = effectiveDarkMode,
        isColorBlindMode = accessibilityState.colorBlindMode,
        isHighContrastMode = accessibilityState.highContrastMode
    )

    // Create specialized accessible colors for specific UI elements
    val accessibleColors = createAccessibleColors(
        isDarkMode = effectiveDarkMode,
        isColorBlindMode = accessibilityState.colorBlindMode,
        isHighContrastMode = accessibilityState.highContrastMode,
        baseScheme = colorScheme
    )

    CompositionLocalProvider(
        LocalColorBlindMode provides accessibilityState.colorBlindMode,
        LocalHighContrastMode provides accessibilityState.highContrastMode,
        LocalDarkMode provides effectiveDarkMode,
        LocalThemeColors provides accessibleColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
private fun createAccessibleColorScheme(
    isDarkMode: Boolean,
    isColorBlindMode: Boolean,
    isHighContrastMode: Boolean
): ColorScheme {
    // Define improved dark mode colors with better visual hierarchy
    val darkScheme = darkColorScheme(
        // Main colors
        primary = Color(0xFF81A9FF),        // Softer blue that works well in dark mode
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF263852), // Subtle blue-tinted container
        onPrimaryContainer = Color(0xFFD8E6FF),

        // Secondary colors
        secondary = Color(0xFF64B5F6),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF1E3047),
        onSecondaryContainer = Color(0xFFD1E4FF),

        // Background colors
        background = Color(0xFF121212),      // Material dark surface
        onBackground = Color(0xFFE1E1E1),    // Off-white for better eye comfort
        surface = Color(0xFF1E1E1E),         // Slightly lighter than background
        onSurface = Color(0xFFE1E1E1),
        surfaceVariant = Color(0xFF252525),  // For card backgrounds
        onSurfaceVariant = Color(0xFFD1D1D1),

        // Utility colors
        outline = Color(0xFF616161),
        outlineVariant = Color(0xFF3D3D3D),  // Subtle grid lines
        error = Color(0xFFFF5252),
        onError = Color.Black
    )

    // Define high contrast dark mode
    val highContrastDarkScheme = darkColorScheme(
        primary = Color(0xFF4D90FF),         // Brighter blue
        onPrimary = Color.White,
        primaryContainer = Color(0xFF003780),
        onPrimaryContainer = Color.White,

        secondary = Color(0xFF82B1FF),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF0D2D62),
        onSecondaryContainer = Color.White,

        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF0A0A0A),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = Color.White,

        outline = Color(0xFF9E9E9E),         // Much more visible outlines
        outlineVariant = Color(0xFF6E6E6E),
        error = Color(0xFFFF8A80),
        onError = Color.Black
    )

    // Define light scheme
    val lightScheme = lightColorScheme(
        primary = Color(0xFF1565C0),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF0A3977),

        secondary = Color(0xFF1976D2),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE3F2FD),
        onSecondaryContainer = Color(0xFF0D47A1),

        background = Color.White,
        onBackground = Color(0xFF121212),
        surface = Color(0xFFF5F5F5),
        onSurface = Color(0xFF121212),
        surfaceVariant = Color(0xFFE1E1E1),
        onSurfaceVariant = Color(0xFF505050),

        outline = Color(0xFF757575),
        outlineVariant = Color(0xFFBDBDBD)
    )

    // Define high contrast light scheme
    val highContrastLightScheme = lightColorScheme(
        primary = Color(0xFF0042A0),         // Darker blue for more contrast
        onPrimary = Color.White,
        primaryContainer = Color(0xFFA3C9FF),
        onPrimaryContainer = Color(0xFF001D45),

        secondary = Color(0xFF004BA0),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFB6D6FF),
        onSecondaryContainer = Color(0xFF002254),

        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF0F0F0),
        onSurfaceVariant = Color.Black,

        outline = Color.Black,
        outlineVariant = Color(0xFF505050)
    )

    // Color blind adjustments
    // Deuteranopia-friendly colors (avoiding red-green confusion)
    val colorBlindColors = if (isColorBlindMode) {
        if (isDarkMode) {
            darkScheme.copy(
                primary = Color(0xFF00A2E0),         // Blue
                secondary = Color(0xFFE6C619),       // Yellow
                error = Color(0xFF56B4E9),           // Light blue
                primaryContainer = Color(0xFF004B6B),
                secondaryContainer = Color(0xFF553F00)
            )
        } else {
            lightScheme.copy(
                primary = Color(0xFF0072B2),         // Blue
                secondary = Color(0xFFE69F00),       // Orange/Yellow
                error = Color(0xFF0092E0),           // Blue for errors instead of red
                primaryContainer = Color(0xFFB5E1F7),
                secondaryContainer = Color(0xFFF7E3B5)
            )
        }
    } else {
        if (isDarkMode) {
            if (isHighContrastMode) highContrastDarkScheme else darkScheme
        } else {
            if (isHighContrastMode) highContrastLightScheme else lightScheme
        }
    }

    return colorBlindColors
}

@Composable
private fun createAccessibleColors(
    isDarkMode: Boolean,
    isColorBlindMode: Boolean,
    isHighContrastMode: Boolean,
    baseScheme: ColorScheme
): AccessibleColors {
    // Create specialized colors for calendar UI elements
    return if (isDarkMode) {
        if (isHighContrastMode) {
            // High contrast dark mode
            AccessibleColors(
                calendarBackground = Color.Black,
                calendarSurface = Color(0xFF0D0D0D),
                calendarText = Color.White,
                calendarBorder = Color(0xFF5E5E5E),
                selectedDay = Color(0xFF4D90FF),
                selectedDayText = Color.White,
                todayBackground = Color(0xFF003780),
                todayText = Color.White,
                eventIndicator = Color(0xFFFFD600),
                buttonBackground = Color(0xFF003780),
                buttonText = Color.White,
                cardBackground = Color(0xFF1A1A1A),
                cardBorder = Color(0xFF4D90FF).copy(alpha = 0.5f),
                headerText = Color.White
            )
        } else {
            // Regular dark mode
            AccessibleColors(
                calendarBackground = Color(0xFF121212),
                calendarSurface = Color(0xFF1E1E1E),
                calendarText = Color(0xFFE1E1E1),
                calendarBorder = Color(0xFF2C2C2C),
                selectedDay = Color(0xFF81A9FF),
                selectedDayText = Color.Black,
                todayBackground = Color(0xFF263852),
                todayText = Color(0xFFD8E6FF),
                eventIndicator = Color(0xFF64B5F6),
                buttonBackground = Color(0xFF263852),
                buttonText = Color(0xFFD8E6FF),
                cardBackground = Color(0xFF252525),
                cardBorder = Color(0xFF81A9FF).copy(alpha = 0.2f),
                headerText = Color(0xFFE1E1E1)
            )
        }
    } else {
        if (isHighContrastMode) {
            // High contrast light mode
            AccessibleColors(
                calendarBackground = Color.White,
                calendarSurface = Color.White,
                calendarText = Color.Black,
                calendarBorder = Color.Black,
                selectedDay = Color(0xFF0042A0),
                selectedDayText = Color.White,
                todayBackground = Color(0xFFA3C9FF),
                todayText = Color(0xFF001D45),
                eventIndicator = Color(0xFFD50000),
                buttonBackground = Color(0xFF0042A0),
                buttonText = Color.White,
                cardBackground = Color.White,
                cardBorder = Color.Black,
                headerText = Color.Black
            )
        } else {
            // Regular light mode
            AccessibleColors(
                calendarBackground = Color.White,
                calendarSurface = Color(0xFFF5F5F5),
                calendarText = Color(0xFF121212),
                calendarBorder = Color(0xFFDDDDDD),
                selectedDay = Color(0xFF1565C0),
                selectedDayText = Color.White,
                todayBackground = Color(0xFFD1E4FF),
                todayText = Color(0xFF0A3977),
                eventIndicator = Color(0xFF1976D2),
                buttonBackground = Color(0xFF1565C0),
                buttonText = Color.White,
                cardBackground = Color(0xFFF5F5F5),
                cardBorder = Color(0xFF1565C0).copy(alpha = 0.2f),
                headerText = Color(0xFF121212)
            )
        }
    }
}

// Extension function to adjust colors for color blindness if needed
fun adjustForColorBlindness(color: Color, type: ColorBlindnessType): Color {
    return when (type) {
        ColorBlindnessType.DEUTERANOPIA -> {
            // Deuteranopia (red-green color blindness, affects ~6% of males)
            // Convert red/green colors to colors that are more distinguishable
            when {
                // If the color is in red spectrum
                color.red > 0.7f && color.green < 0.5f && color.blue < 0.5f ->
                    Color(0.1f, 0.5f, 0.9f) // Convert to blue
                // If the color is in green spectrum
                color.red < 0.5f && color.green > 0.7f && color.blue < 0.5f ->
                    Color(0.9f, 0.8f, 0.2f) // Convert to yellow
                // If the color is a mixed red-green that might be challenging
                color.red > 0.6f && color.green > 0.6f && color.blue < 0.5f ->
                    Color(0.2f, 0.6f, 0.8f) // Convert to teal/blue
                else -> color
            }
        }
        ColorBlindnessType.PROTANOPIA -> {
            // Protanopia (red-green color blindness, affects ~1% of males)
            // Similar to deuteranopia but with different red perception
            when {
                // If the color is in red spectrum
                color.red > 0.7f && color.green < 0.5f && color.blue < 0.5f ->
                    Color(0.2f, 0.6f, 0.9f) // Convert to blue-teal
                // If the color is in green spectrum
                color.red < 0.5f && color.green > 0.7f && color.blue < 0.5f ->
                    Color(0.95f, 0.9f, 0.25f) // Convert to bright yellow
                // Mixed colors
                color.red > 0.6f && color.green > 0.6f && color.blue < 0.5f ->
                    Color(0.1f, 0.7f, 0.9f) // Convert to bright blue
                else -> color
            }
        }
        ColorBlindnessType.TRITANOPIA -> {
            // Tritanopia (blue-yellow color blindness, rare)
            when {
                // If the color is in blue spectrum
                color.red < 0.5f && color.green < 0.5f && color.blue > 0.7f ->
                    Color(0.8f, 0.2f, 0.8f) // Convert to purple
                // If the color is in yellow spectrum
                color.red > 0.7f && color.green > 0.7f && color.blue < 0.3f ->
                    Color(0.9f, 0.4f, 0.1f) // Convert to orange
                // Teal colors can be problematic
                color.red < 0.5f && color.green > 0.6f && color.blue > 0.6f ->
                    Color(0.3f, 0.3f, 0.9f) // Convert to more blue
                else -> color
            }
        }
        ColorBlindnessType.ACHROMATOPSIA -> {
            // Complete color blindness - convert to grayscale
            val luminance = color.luminance()
            Color(luminance, luminance, luminance)
        }
        ColorBlindnessType.NONE -> color
    }
}

fun createColorBlindPalette(baseScheme: ColorScheme, type: ColorBlindnessType): ColorScheme {
    return when (type) {
        ColorBlindnessType.DEUTERANOPIA -> {
            // Deuteranopia-friendly palette - focus on blue/yellow contrast
            baseScheme.copy(
                primary = Color(0x0072B2), // Blue
                secondary = Color(0xE69F00), // Yellow/Orange
                error = Color(0x0092E0), // Blue for errors instead of red
                tertiary = Color(0x56B4E9), // Light blue
                primaryContainer = Color(0xB5E1F7),
                secondaryContainer = Color(0xF7E3B5)
            )
        }
        ColorBlindnessType.PROTANOPIA -> {
            // Protanopia-friendly palette
            baseScheme.copy(
                primary = Color(0x0080B3), // Blue
                secondary = Color(0xF0E442), // Yellow
                error = Color(0x56B4E9), // Light blue for errors
                tertiary = Color(0xCC79A7), // Pink/Purple
                primaryContainer = Color(0xBFE6F2),
                secondaryContainer = Color(0xFAF3AA)
            )
        }
        ColorBlindnessType.TRITANOPIA -> {
            // Tritanopia-friendly palette - focus on red/green contrast
            baseScheme.copy(
                primary = Color(0xD55E00), // Orange/Red
                secondary = Color(0x009E73), // Green
                error = Color(0xCC79A7), // Purple for errors
                tertiary = Color(0xF0E442), // Yellow
                primaryContainer = Color(0xFABD9B),
                secondaryContainer = Color(0x99D8C2)
            )
        }
        ColorBlindnessType.ACHROMATOPSIA -> {
            // Achromatopsia (monochromacy) - use high contrast grayscale
            val darkGray = Color(0x333333)
            val lightGray = Color(0xDDDDDD)
            val mediumGray = Color(0x777777)
            val veryLightGray = Color(0xF5F5F5)

            baseScheme.copy(
                primary = darkGray,
                onPrimary = veryLightGray,
                primaryContainer = mediumGray,
                onPrimaryContainer = veryLightGray,
                secondary = mediumGray,
                onSecondary = veryLightGray,
                secondaryContainer = lightGray,
                onSecondaryContainer = darkGray,
                error = darkGray,
                background = veryLightGray,
                onBackground = darkGray,
                surface = veryLightGray,
                onSurface = darkGray
            )
        }
        ColorBlindnessType.NONE -> baseScheme
    }
}

enum class ColorBlindnessType {
    NONE,
    DEUTERANOPIA,  // Most common (red-green)
    PROTANOPIA,    // Also red-green but different perception
    TRITANOPIA,    // Blue-yellow color blindness (rare)
    ACHROMATOPSIA  // Complete color blindness (very rare)
}

// Helper extension to check if a color scheme is light or dark
fun ColorScheme.isLight(): Boolean = surface.luminance() > 0.5f

// Helper extension to calculate luminance
fun Color.luminance(): Float {
    // Standard relative luminance calculation
    val r = if (red <= 0.03928f) red/12.92f else ((red+0.055f)/1.055f).pow(2.4f)
    val g = if (green <= 0.03928f) green/12.92f else ((green+0.055f)/1.055f).pow(2.4f)
    val b = if (blue <= 0.03928f) blue/12.92f else ((blue+0.055f)/1.055f).pow(2.4f)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

// Extension to get a color that ensures readable text on this background
fun Color.ensureContrast(darkColor: Color = Color.Black, lightColor: Color = Color.White): Color {
    return if (this.luminance() > 0.5f) darkColor else lightColor
}

@Composable
fun AppAccessibilityProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Get the accessibility repository instance
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }

    // Collect the accessibility state as a state object
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )

    // Apply all accessibility providers in the correct order
    AccessibilityThemeProvider(accessibilityState = accessibilityState) {
        FontSizeProvider(fontSizeAdjustment = accessibilityState.fontSize) {
            TtsProvider(isEnabled = accessibilityState.textToSpeech) {
                content()
            }
        }
    }
}