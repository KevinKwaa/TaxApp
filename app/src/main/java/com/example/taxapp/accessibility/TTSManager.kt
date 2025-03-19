package com.example.taxapp.accessibility

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class AccessibilityTtsManager private constructor(context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    init {
        initializeTts(context)
    }

    private fun initializeTts(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Default to device language
                textToSpeech?.language = Locale.getDefault()

                // Set up utterance progress listener to track speaking state
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (text.isBlank()) return

        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, queueMode, null, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AccessibilityTtsManager? = null

        fun getInstance(context: Context): AccessibilityTtsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AccessibilityTtsManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}