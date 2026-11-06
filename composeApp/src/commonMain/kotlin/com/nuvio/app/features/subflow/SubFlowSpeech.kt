package com.nuvio.app.features.subflow

internal expect object SubFlowSpeech {
    fun speak(text: String, languageTag: String?, rate: Float)
    fun stop()
}
