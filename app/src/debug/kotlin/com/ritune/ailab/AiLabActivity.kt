package com.ritune.ailab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.ritune.core.designsystem.theme.RituneTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug-only playground for Firebase AI Logic. Not part of the release app.
 *
 * Timing runs need no taps; unknown names fall back to the defaults:
 * `adb shell am start -n com.ritune/com.ritune.ailab.AiLabActivity
 *   --es prompt "hi" --es model FLASH_LITE_3_5 --es thinking LOW --ei runs 3`
 */
@AndroidEntryPoint
class AiLabActivity : ComponentActivity() {

    private val viewModel: AiLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug builds can't pass Play Integrity, so they prove themselves with a debug token.
        // It's printed to logcat on first run and must be registered in the Firebase console.
        Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())

        if (savedInstanceState == null) intent.toRunBatch()?.let(viewModel::onEvent)

        enableEdgeToEdge()
        setContent {
            RituneTheme {
                Surface(Modifier.fillMaxSize()) { AiLabScreen(viewModel) }
            }
        }
    }

    private companion object {
        const val EXTRA_PROMPT = "prompt"
        const val EXTRA_MODEL = "model"
        const val EXTRA_THINKING = "thinking"
        const val EXTRA_RUNS = "runs"

        fun Intent.toRunBatch(): AiLabEvent.RunBatch? {
            val prompt = getStringExtra(EXTRA_PROMPT) ?: return null
            val model = getStringExtra(EXTRA_MODEL)
            val thinking = getStringExtra(EXTRA_THINKING)
            return AiLabEvent.RunBatch(
                prompt = prompt,
                model = LabModel.entries.firstOrNull { it.name == model } ?: LabModel.FLASH_LITE_3_5,
                thinking = LabThinking.entries.firstOrNull { it.name == thinking }
                    ?: LabThinking.DEFAULT,
                runs = getIntExtra(EXTRA_RUNS, 1),
            )
        }
    }
}
