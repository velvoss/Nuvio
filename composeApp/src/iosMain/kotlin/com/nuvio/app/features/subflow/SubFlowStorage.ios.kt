package com.nuvio.app.features.subflow

import platform.Foundation.NSUserDefaults

actual object SubFlowStorage {
    private const val preferencesKey = "subflow_preferences"
    private const val wordsKey = "subflow_words"

    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual fun loadPreferencesJson(): String? = defaults.stringForKey(preferencesKey)

    actual fun savePreferencesJson(value: String) {
        defaults.setObject(value, forKey = preferencesKey)
    }

    actual fun loadWordsJson(): String? = defaults.stringForKey(wordsKey)

    actual fun saveWordsJson(value: String) {
        defaults.setObject(value, forKey = wordsKey)
    }

    actual fun clear() {
        defaults.removeObjectForKey(preferencesKey)
        defaults.removeObjectForKey(wordsKey)
    }
}
