package com.example.taxapp.multiLanguage

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

    // Define language options with localized names
    val languages = listOf(
        Language("zh", getLocalizedLanguageName("zh")),
        Language("en", getLocalizedLanguageName("en")),
        Language("ms", getLocalizedLanguageName("ms"))
    )

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
                        onClick = onDismiss,
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

                            // REMOVED: activity?.recreate()
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