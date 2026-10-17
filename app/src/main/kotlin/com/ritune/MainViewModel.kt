package com.ritune

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.usecases.InitializeApp
import com.ritune.feature.today.ui.navigation.TodayRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val initializeApp: InitializeApp,
) : ViewModel() {
    private val _startDestination = MutableStateFlow<Any?>(null)
    val startDestination: StateFlow<Any?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            initializeApp()
            _startDestination.value = TodayRoute
        }
    }
}
