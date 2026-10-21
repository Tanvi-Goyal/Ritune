package com.ritune.core.domain.usecases.settings

import com.ritune.core.models.AppSettings
import com.ritune.core.models.Cue

fun AppSettings.allows(cue: Cue): Boolean = when (cue) {
    is Cue.Haptic -> hapticsEnabled
    is Cue.Sound -> soundEnabled
    is Cue.Speech -> speechEnabled
}
