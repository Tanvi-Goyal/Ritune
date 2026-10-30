package com.ritune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import com.ritune.core.designsystem.theme.ThemeMode
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.models.ThemeSetting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    observeSettings: ObserveSettings
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode?> = observeSettings()
        .traceUntilFirst("ThemeRead", cookie = 0)
        .map { settings ->
            when (settings.theme) {
                ThemeSetting.SYSTEM -> ThemeMode.SYSTEM
                ThemeSetting.LIGHT -> ThemeMode.LIGHT
                ThemeSetting.DARK -> ThemeMode.DARK
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )
}

/** Settings keep emitting after startup; only the first read holds the splash, so trace just that. */
private fun <T> Flow<T>.traceUntilFirst(name: String, cookie: Int): Flow<T> = flow {
    Trace.beginAsyncSection(name, cookie)
    var ended = false
    try {
        collect { value ->
            if (!ended) {
                ended = true
                Trace.endAsyncSection(name, cookie)
            }
            emit(value)
        }
    } finally {
        if (!ended) Trace.endAsyncSection(name, cookie)
    }
}
