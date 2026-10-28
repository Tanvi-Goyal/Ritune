package com.ritune.platform.android

class FakeLogger : Logger {

    val warnings = mutableListOf<String>()
    val errors = mutableListOf<Throwable?>()

    override fun warn(tag: String, message: String) {
        warnings += message
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        errors += throwable
    }
}
