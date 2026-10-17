package com.ritune.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ritune.core.designsystem.theme.Ritune
import com.ritune.core.designsystem.theme.RituneTheme
import kotlin.time.Duration.Companion.seconds

@PreviewLightDark
@Composable
private fun ComponentGalleryPreview() {
    RituneTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(Ritune.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
            ) {
                PrimaryButton("Start", onClick = {})
                SecondaryButton("Log", onClick = {}, compact = true)
                RituneCard {
                    SwitchRow(
                        "Haptics",
                        checked = true,
                        onCheckedChange = {}
                    )
                    RituneDivider()
                    SwitchRow(
                        "Sounds",
                        checked = false,
                        onCheckedChange = {},
                        supportingText = "Soft bell at each step"
                    )
                }
                RituneCard {
                    AnchorRow(
                        "Wake",
                        "6:42",
                        logged = true,
                        statusDescription = "Logged at 6:42"
                    )
                    RituneDivider()
                    AnchorRow(

                        "Last meal",
                        "~19:30",
                        logged = false,
                        statusDescription = "Not logged yet",
                        actionLabel = "Log", onAction = {},
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Ritune.spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConsistencyRing(
                        done = 3,
                        total = 5,
                        description = "3 of 5 anchors logged"
                    )
                    TimerText(
                        134.seconds,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                StepProgress(total = 4, currentIndex = 1, description = "Step 2 of 4")
                WeekBars(
                    values = listOf(0.7f, 0.85f, 0.6f, 0.9f, 0.75f, 0.4f, null),
                    labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                    descriptions = listOf(
                        "Monday, 70 percent",
                        "Tuesday, 85 percent",
                        "Wednesday, 60 percent",
                        "Thursday, 90 percent",
                        "Friday, 75 percent",
                        "Saturday, 40 percent",
                        "Sunday, no data",
                    ),
                    highlightIndex = 5,
                )
                RituneSnackbar(
                    message = "Wake logged at 6:42",
                    actionLabel = "Undo",
                    onAction = {}
                )
            }
        }
    }
}