package com.ritune.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object RituneMotion {
    const val ShortMs = 150
    const val MediumMs = 200
}

/** True when the user turned animations off (system "Remove animations" sets the scale to 0). */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Short tween, or an instant change when reduced motion is on. */
fun <T> rituneTween(
    reducedMotion: Boolean,
    durationMs: Int = RituneMotion.MediumMs
): FiniteAnimationSpec<T> =
    if (reducedMotion) snap() else tween(durationMs)