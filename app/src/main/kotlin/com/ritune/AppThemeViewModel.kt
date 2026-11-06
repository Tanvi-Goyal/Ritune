package com.ritune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import com.ritune.core.designsystem.theme.ThemeMode
import com.ritune.core.domain.usecases.settings.ObserveSettings
import com.ritune.core.models.ThemeSetting
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    observeSettings: ObserveSettings,
    @Dispatcher(RituneDispatchers.Default) dispatcher: CoroutineDispatcher,
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
        // The splash waits for the first value. On main it would queue behind onResume and the
        // first frame, like InitializeApp did, so the whole collection runs off main.
        .stateIn(
            viewModelScope + dispatcher,
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
