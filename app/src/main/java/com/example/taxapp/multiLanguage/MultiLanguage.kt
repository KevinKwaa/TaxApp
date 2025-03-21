package com.example.taxapp.multiLanguage

import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taxapp.R
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.accessibility.AccessibilitySettings
import com.example.taxapp.accessibility.AccessibilityState
import com.example.taxapp.accessibility.LocalTtsManager
import com.example.taxapp.accessibility.ScreenReader
import kotlinx.coroutines.launch

data class Language(
    val code: String,
    val name: String
)

@Composable
fun LanguageSelector(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    activity: ComponentActivity? = null  // Add activity parameter
) {
    val context = LocalContext.current
    var tempSelection by remember { mutableStateOf(currentLanguageCode) }
    val languageManager = remember { AppLanguageManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    val activity = context as? ComponentActivity

    var showAccessibilitySettings by remember { mutableStateOf(false) }
    val accessibilityRepository = remember { AccessibilityRepository.getInstance(context) }
    // Observe accessibility settings
    val accessibilityState by accessibilityRepository.accessibilityStateFlow.collectAsState(
        initial = AccessibilityState()
    )
    // Create a TTS instance if text-to-speech is enabled
    val tts = remember(accessibilityState.textToSpeech) {
        if (accessibilityState.textToSpeech) {
            TextToSpeech(context) { status ->
                // Initialize TTS engine
            }
        } else null
    }

    //val ttsManager = LocalTtsManager.current

    val selectedLanguageName = getLocalizedLanguageName(tempSelection)

    // Define language options with localized names
    val languages = listOf(
        Language("zh", getLocalizedLanguageName("zh")),
        Language("en", getLocalizedLanguageName("en")),
        Language("ms", getLocalizedLanguageName("ms"))
    )

    ScreenReader("Multi-Language")

    LanguageProvider(languageCode = currentLanguageCode, key = currentLanguageCode) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.current_language),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        // Show the localized name of the current language
                        text = getLocalizedLanguageName(currentLanguageCode),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.select_language),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        languages.forEach { language ->
                            TextButton(
                                onClick = { tempSelection = language.code },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (tempSelection == language.code)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = language.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                // Add TTS feedback
                                if (accessibilityState.textToSpeech) {
                                    tts?.speak(
                                        "Cancelling",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        null
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(stringResource(id = R.string.cancel))
                        }

                        Button(
                            onClick = {
                                // Apply the language change without recreating the activity
                                languageManager.setLanguage(tempSelection, activity)
                                onLanguageSelected(tempSelection)
                                onDismiss()

                                // Add TTS feedback
                                if (accessibilityState.textToSpeech) {
                                    tts?.speak(
                                        "Language changed to $selectedLanguageName",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        null
                                    )
                                }
                            },
                            enabled = tempSelection != currentLanguageCode,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(stringResource(id = R.string.confirm))
                        }
                    }
                }
            }
        }

        if (showAccessibilitySettings) {
            AccessibilitySettings(
                currentSettings = accessibilityState,
                onSettingsChanged = { newSettings ->
                    coroutineScope.launch {
                        accessibilityRepository.updateSettings(newSettings)
                    }
                },
                onDismiss = { showAccessibilitySettings = false }
            )
        }
    }
}

// Helper function to get localized language names
@Composable
private fun getLocalizedLanguageName(code: String): String {
    val resourceId = when (code) {
        "zh" -> R.string.language_chinese
        "ms" -> R.string.language_malay
        else -> R.string.language_english
    }
    return stringResource(id = resourceId)
}