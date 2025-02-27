package com.example.taxapp

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import com.example.taxapp.multiLanguage.AppLanguageManager

open class BaseActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val languageManager = AppLanguageManager.getInstance(newBase)
        val locale = languageManager.getCurrentLocale()
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}