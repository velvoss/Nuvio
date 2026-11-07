package com.nuvio.app.features.subflow

import platform.NaturalLanguage.NLLanguageRecognizer

actual object SubFlowLanguageDetector {
    actual fun detect(text: String): String? {
        val sample = text.trim()
        if (sample.length < 2) return null
        return runCatching {
            val recognizer = NLLanguageRecognizer()
            recognizer.processString(sample)
            recognizer.dominantLanguage
        }.getOrNull()
    }
}
