package com.ritune.ailab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.ritune.core.designsystem.theme.RituneTheme

/**
 * Debug-only playground for Firebase AI Logic. Not part of the release app.
 *
 * Timing runs without touching the screen:
 * adb shell am start -n com.ritune/com.ritune.ailab.AiLabActivity \
 *   --es prompt "hi" --es model FLASH_LITE_3_5 --es thinking LOW (unknown names fall back to the defaults) --ei runs 3
 */
class AiLabActivity : ComponentActivity() {

    private val viewModel: AiLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prints a debug token to logcat on first run; register it in the Firebase console under App Check.
        Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_PROMPT)?.let { prompt ->
                viewModel.runBatch(
                    prompt = prompt,
                    model = LabModel.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_MODEL) }
                        ?: LabModel.FLASH_LITE_3_5,
                    thinking = LabThinking.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_THINKING) }
                        ?: LabThinking.DEFAULT,
                    runs = intent.getIntExtra(EXTRA_RUNS, 1),
                )
            }
        }
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
    }
}

@Composable
private fun AiLabScreen(viewModel: AiLabViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("AI lab", style = MaterialTheme.typography.titleMedium)
        ChipRow("Model", LabModel.entries, state.model, { it.id.removePrefix("gemini-") }, viewModel::onModelChange)
        ChipRow("Thinking", LabThinking.entries, state.thinking, { it.name.lowercase() }, viewModel::onThinkingChange)
        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.rememberChat, onCheckedChange = viewModel::onRememberChatChange)
            Text("Remember conversation", Modifier.weight(1f))
            if (state.streaming) {
                Button(onClick = viewModel::stop) { Text("Stop") }
            } else {
                Button(onClick = viewModel::send, enabled = state.input.isNotBlank()) { Text("Send") }
            }
        }
        state.stats?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = state.reply,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun <T> ChipRow(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}
