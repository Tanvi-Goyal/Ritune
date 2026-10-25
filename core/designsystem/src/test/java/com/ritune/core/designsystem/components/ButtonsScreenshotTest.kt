package com.ritune.core.designsystem.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.screenshottesting.captureComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ButtonsScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun buttons_allStates() {
        composeTestRule.captureComponent("buttons") {
            RituneTheme {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton("Start routine", onClick = {})
                        PrimaryButton("Start routine", onClick = {}, enabled = false)
                        SecondaryButton("Log", onClick = {})
                        SecondaryButton("Log", onClick = {}, enabled = false)
                        SecondaryButton("Undo", onClick = {}, compact = true)
                    }
                }
            }
        }
    }
}
