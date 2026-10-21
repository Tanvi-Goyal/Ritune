package com.ritune

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ritune.core.designsystem.theme.RituneTheme
import com.ritune.core.designsystem.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val themeViewModel: AppThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition {
            viewModel.startDestination.value == null || themeViewModel.themeMode.value == null
        }

        setContent {
            val mode by themeViewModel.themeMode.collectAsState()
            val startDestination by viewModel.startDestination.collectAsState()
            val themeMode = mode
            val destination = startDestination

            if (themeMode != null && destination != null) {
                val dark = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

                DisposableEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(
                            Color.TRANSPARENT,
                            Color.TRANSPARENT
                        ) { dark },
                        navigationBarStyle = SystemBarStyle.auto(
                            Color.argb(
                                0xe6,
                                0xff,
                                0xff,
                                0xff
                            ),
                            Color.argb(
                                0x80,
                                0x1b,
                                0x1b,
                                0x1b
                            )
                        ) { dark },
                    )
                    onDispose { }
                }

                RituneTheme(
                    themeMode = themeMode
                ) {
                    RituneApp(startDestination = destination)
                }
            }
        }
    }
}