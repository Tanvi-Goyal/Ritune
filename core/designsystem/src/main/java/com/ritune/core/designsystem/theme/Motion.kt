package com.ritune.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object RituneMotion {
    const val SHORT_MS = 150
    const val MEDIUM_MS = 200
}

@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
}

fun <T> rituneTween(
    reducedMotion: Boolean,
    durationMs: Int = RituneMotion.MEDIUM_MS
): FiniteAnimationSpec<T> =
    if (reducedMotion) snap() else tween(durationMs)