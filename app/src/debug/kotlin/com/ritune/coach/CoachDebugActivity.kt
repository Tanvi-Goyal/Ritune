package com.ritune.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ritune.ailab.installDebugAppCheck
import com.ritune.core.designsystem.theme.RituneTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Debug-only coach that answers from the user's real rhythm data. Not part of the release app.
 *
 * Validation runs need no taps:
 * `adb shell am start -S -n com.ritune/com.ritune.coach.CoachDebugActivity --es ask "How was my week?"`
 */
@AndroidEntryPoint
class CoachDebugActivity : ComponentActivity() {

    private val viewModel: CoachDebugViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installDebugAppCheck()

        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_ASK)?.let { viewModel.onEvent(CoachDebugEvent.Ask(it)) }
        }

        enableEdgeToEdge()
        setContent {
            RituneTheme {
                Surface(Modifier.fillMaxSize()) { CoachDebugScreen(viewModel) }
            }
        }
    }

    private companion object {
        const val EXTRA_ASK = "ask"
    }
}
