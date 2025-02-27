package com.example.taxapp.multiLanguage

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

// Create a composition local to provide the current locale throughout the app
val LocalAppLanguage = staticCompositionLocalOf { "en" }

class LanguageManager(private val context: Context) {

    // Get the shared preferences for language settings
    private val preferences = context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)

    private val _currentLanguageCode = MutableStateFlow(getCurrentLanguageCode())
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode

    private val _languageChanged = mutableStateOf(false)
    val languageChanged = _languageChanged

    // Change the app's language
    fun setLanguage(languageCode: String, activity: ComponentActivity? = null) {
        if (languageCode == getCurrentLanguageCode()) return

        val locale = when (languageCode) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ms" -> Locale("ms", "MY")
            else -> Locale.ENGLISH
        }

        // Save the language code to preferences
        preferences.edit().putString("language_code", languageCode).apply()

        // Update configuration
        updateResources(context, locale)

        // Update activity configuration without recreating it
        activity?.let {
            updateResources(it, locale)
        }

        // Update the state flow to trigger recomposition
        _currentLanguageCode.value = languageCode
        _languageChanged.value = true
    }

    // Update app resources with the new locale
    private fun updateResources(context: Context, locale: Locale) {
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        configuration.setLocale(locale)

        // For API 25 and below
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // For API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.applicationContext.createConfigurationContext(configuration)
        }
    }

    // Reset the language changed flag after handling
    fun resetLanguageChangedFlag() {
        _languageChanged.value = false
    }

    // Get the language code from preferences or default locale
    fun getCurrentLanguageCode(): String {
        return preferences.getString("language_code", Locale.getDefault().language) ?: "en"
    }

    // Get the current locale based on the language code
    fun getCurrentLocale(): Locale {
        val languageCode = getCurrentLanguageCode()
        return when (languageCode) {
            "zh" -> Locale.CHINA
            "ms" -> Locale("ms", "MY")
            else -> Locale.ENGLISH
        }
    }
}

object AppLanguageManager {
    private var instance: LanguageManager? = null

    fun getInstance(context: Context): LanguageManager {
        if (instance == null) {
            instance = LanguageManager(context.applicationContext)
        }
        return instance!!
    }
}

// Create a composable to provide the LocalAppLanguage to the entire app
@Composable
fun LanguageProvider(
    languageCode: String,
    key: Any? = null,
    content: @Composable () -> Unit
) {
    // This ensures all children will receive the language code
    CompositionLocalProvider(LocalAppLanguage provides languageCode) {
        key(key) { // Use the key to force recomposition
            content()
        }
    }
}