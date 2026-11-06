package com.nuvio.app.features.subflow

import android.content.Context
import android.content.SharedPreferences

actual object SubFlowStorage {
    private const val preferencesName = "nuvio_subflow"
    private const val preferencesKey = "subflow_preferences"
    private const val wordsKey = "subflow_words"

    private fun preferences(): SharedPreferences? =
        SubFlowAndroidRuntime.applicationContext
            ?.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    actual fun loadPreferencesJson(): String? =
        preferences()?.getString(preferencesKey, null)

    actual fun savePreferencesJson(value: String) {
        preferences()?.edit()?.putString(preferencesKey, value)?.apply()
    }

    actual fun loadWordsJson(): String? =
        preferences()?.getString(wordsKey, null)

    actual fun saveWordsJson(value: String) {
        preferences()?.edit()?.putString(wordsKey, value)?.apply()
    }

    actual fun clear() {
        preferences()?.edit()?.clear()?.apply()
    }
}
