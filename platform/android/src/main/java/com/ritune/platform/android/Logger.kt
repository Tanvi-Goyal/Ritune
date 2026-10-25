package com.ritune.platform.android

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface Logger {
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

@Singleton
class AndroidLogger @Inject constructor() : Logger {
    override fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
