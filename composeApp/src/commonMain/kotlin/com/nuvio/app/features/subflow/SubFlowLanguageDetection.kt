package com.nuvio.app.features.subflow

internal expect object SubFlowLanguageDetector {
    fun detect(text: String): String?
}
