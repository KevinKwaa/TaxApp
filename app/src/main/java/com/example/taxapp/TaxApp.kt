package com.example.taxapp

import android.app.Application
import com.example.taxapp.accessibility.AccessibilityRepository
import com.example.taxapp.multiLanguage.AppLanguageManager

class TaxApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize language manager
        val languageManager = AppLanguageManager.getInstance(this)

        // Initialize accessibility repository (this ensures early creation of singleton)
        AccessibilityRepository.getInstance(this)

        // Set the default locale based on saved preferences
        val locale = languageManager.getCurrentLocale()
    }
}