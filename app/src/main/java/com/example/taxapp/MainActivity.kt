package com.example.taxapp

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.taxapp.accessibility.AppAccessibilityProvider
import com.example.taxapp.multiLanguage.AppLanguageManager
import com.example.taxapp.ui.theme.TaxAppTheme

class MainActivity : BaseActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val languageManager = AppLanguageManager.getInstance(this)
        val langCode = languageManager.getCurrentLanguageCode()
        languageManager.setLanguage(langCode, this)
        enableEdgeToEdge()
        setContent {
            TaxAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AppAccessibilityProvider {
                        SchedulerApp()
                    }
                }
            }
        }
    }
}


