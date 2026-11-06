package com.nuvio.app.features.subflow

internal expect object SubFlowStorage {
    fun loadPreferencesJson(): String?
    fun savePreferencesJson(value: String)
    fun loadWordsJson(): String?
    fun saveWordsJson(value: String)
    fun clear()
}
