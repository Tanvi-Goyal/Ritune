package com.ritune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tracing.traceAsync
import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.feature.today.ui.navigation.TodayRoute
import com.ritune.platform.android.di.Dispatcher
import com.ritune.platform.android.di.RituneDispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val initializeApp: InitializeApp,
    @Dispatcher(RituneDispatchers.Default) private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _startDestination = MutableStateFlow<Any?>(null)
    val startDestination: StateFlow<Any?> = _startDestination.asStateFlow()

    init {
        // Off the main thread: during startup main is busy with onResume and the first frame, and
        // the splash would wait for it just to resume this coroutine and set the destination.
        viewModelScope.launch(dispatcher) {
            traceAsync("InitializeApp", cookie = 0) { initializeApp() }
            _startDestination.value = TodayRoute
        }
    }
}
