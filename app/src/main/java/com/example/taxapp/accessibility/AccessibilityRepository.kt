package com.example.taxapp.accessibility

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
val Context.accessibilityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "accessibility_settings"
)

/**
 * Repository to manage app-wide accessibility settings
 * Uses DataStore for persistence
 */
class AccessibilityRepository(private val context: Context) {

    // Define keys for each setting
    companion object {
        private val FONT_SIZE = intPreferencesKey("font_size")
        private val TEXT_TO_SPEECH = booleanPreferencesKey("text_to_speech")
        private val COLOR_BLIND_MODE = booleanPreferencesKey("color_blind_mode")
        private val COLOR_BLINDNESS_TYPE = stringPreferencesKey("color_blindness_type")
        private val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        private val TTS_PITCH = floatPreferencesKey("tts_pitch")

        // Singleton pattern
        @Volatile
        private var INSTANCE: AccessibilityRepository? = null

        fun getInstance(context: Context): AccessibilityRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccessibilityRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Flow of the current settings
    val accessibilityStateFlow: Flow<AccessibilityState> = context.accessibilityDataStore.data
        .map { preferences ->
            AccessibilityState(
                fontSize = preferences[FONT_SIZE] ?: 10,
                textToSpeech = preferences[TEXT_TO_SPEECH] ?: false,
                colorBlindMode = preferences[COLOR_BLIND_MODE] ?: false,
                colorBlindnessType = preferences[COLOR_BLINDNESS_TYPE]?.let {
                    try {
                        ColorBlindnessType.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        ColorBlindnessType.NONE
                    }
                } ?: ColorBlindnessType.NONE,
                highContrastMode = preferences[HIGH_CONTRAST_MODE] ?: false,
                darkMode = preferences[DARK_MODE] ?: false,
                ttsSpeechRate = preferences[TTS_SPEECH_RATE] ?: 1.0f,
                ttsPitch = preferences[TTS_PITCH] ?: 1.0f
            )
        }

    // Update settings
    suspend fun updateSettings(accessibilityState: AccessibilityState) {
        context.accessibilityDataStore.edit { preferences ->
            preferences[FONT_SIZE] = accessibilityState.fontSize
            preferences[TEXT_TO_SPEECH] = accessibilityState.textToSpeech
            preferences[COLOR_BLIND_MODE] = accessibilityState.colorBlindMode
            preferences[COLOR_BLINDNESS_TYPE] = accessibilityState.colorBlindnessType.name
            preferences[HIGH_CONTRAST_MODE] = accessibilityState.highContrastMode
            preferences[DARK_MODE] = accessibilityState.darkMode
            preferences[TTS_SPEECH_RATE] = accessibilityState.ttsSpeechRate
            preferences[TTS_PITCH] = accessibilityState.ttsPitch
        }
    }
}