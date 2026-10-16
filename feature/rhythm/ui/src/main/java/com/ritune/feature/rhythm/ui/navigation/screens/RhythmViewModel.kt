package com.ritune.feature.rhythm.ui.navigation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.rhythm.ObserveWeekRhythm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RhythmViewModel @Inject constructor(
    observeCurrentDay: ObserveCurrentDay,
    observeWeekRhythm: ObserveWeekRhythm,
) : ViewModel() {

    val state: StateFlow<RhythmUiState> = observeCurrentDay()
        .flatMapLatest<LocalDate, RhythmUiState> { day ->
            observeWeekRhythm(day).map { rhythm ->
                buildContent(day, rhythm)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RhythmUiState.Loading
        )
}
