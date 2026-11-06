package com.nuvio.app.features.subflow

import android.content.Context

internal object SubFlowAndroidRuntime {
    var applicationContext: Context? = null
        private set

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}
