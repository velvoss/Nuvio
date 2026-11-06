package com.nuvio.app.features.subflow

import android.speech.tts.TextToSpeech
import java.util.Locale

actual object SubFlowSpeech {
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: PendingSpeech? = null

    private data class PendingSpeech(
        val text: String,
        val languageTag: String?,
        val rate: Float,
    )

    private fun ensureEngine(): TextToSpeech? {
        val context = SubFlowAndroidRuntime.applicationContext ?: return null
        if (engine != null) return engine
        engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                pending?.let { speak(it.text, it.languageTag, it.rate) }
                pending = null
            }
        }
        return engine
    }

    actual fun speak(text: String, languageTag: String?, rate: Float) {
        if (text.isBlank()) return
        val tts = ensureEngine() ?: return
        if (!ready) {
            pending = PendingSpeech(text, languageTag, rate)
            return
        }
        if (!languageTag.isNullOrBlank()) {
            val locale = Locale.forLanguageTag(languageTag)
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
            }
        } else {
            tts.setLanguage(Locale.getDefault())
        }
        tts.setSpeechRate(rate.coerceIn(0.1f, 2f))
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "subflow")
    }

    actual fun stop() {
        pending = null
        engine?.stop()
    }
}
