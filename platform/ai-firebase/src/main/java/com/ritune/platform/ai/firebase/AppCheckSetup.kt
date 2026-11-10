package com.ritune.platform.ai.firebase

/**
 * Installs the App Check provider the build should use. Each build type binds its own: debug
 * builds use registered debug tokens, release builds Play Integrity.
 */
fun interface AppCheckSetup {
    fun install()
}
