package com.ritune.ailab

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// Debug builds can't pass Play Integrity, so they prove themselves with a debug token.
// It's printed to logcat on first run and must be registered in the Firebase console.
fun installDebugAppCheck() {
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
