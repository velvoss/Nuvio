package com.nuvio.app.features.subflow

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

actual object SubFlowSpeech {
    private val synthesizer = AVSpeechSynthesizer()

    actual fun speak(text: String, languageTag: String?, rate: Float) {
        if (text.isBlank()) return
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.rate = rate.coerceIn(0.1f, 1f)
        if (!languageTag.isNullOrBlank()) {
            AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)?.let { utterance.voice = it }
        }
        synthesizer.speakUtterance(utterance)
    }

    actual fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}
