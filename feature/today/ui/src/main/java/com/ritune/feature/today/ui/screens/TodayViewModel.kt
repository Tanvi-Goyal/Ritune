package com.ritune.feature.today.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritune.core.domain.models.anchor.LogResult
import com.ritune.core.domain.ports.StartResult
import com.ritune.core.domain.usecases.anchor.LogAnchor
import com.ritune.core.domain.usecases.anchor.ObserveToday
import com.ritune.core.domain.usecases.anchor.UndoLogAnchor
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.player.StartRoutine
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.AnchorId
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.RoutineId
import com.ritune.core.models.Timekeeper
import com.ritune.feature.today.ui.mapper.buildContent
import com.ritune.feature.today.ui.mapper.toLocalTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    observeToday: ObserveToday,
    observePlayerState: ObservePlayerState,
    private val observeRoutines: ObserveRoutines,
    private val logAnchor: LogAnchor,
    private val undoLogAnchor: UndoLogAnchor,
    private val timekeeper: Timekeeper,
    private val startRoutine: StartRoutine,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = currentDay()
        .flatMapLatest<LocalDate, TodayUiState> { day ->
            combine(
                observeToday(day),
                observeRoutines(),
                observePlayerState(),
            ) { statuses, allRoutines, playerState ->
                buildContent(
                    day,
                    statuses,
                    allRoutines,
                    playerState,
                    timekeeper.timeZone()
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TodayUiState.Loading
        )

    private val _effects = Channel<TodayEffect>(Channel.BUFFERED)
    val effects: Flow<TodayEffect> = _effects.receiveAsFlow()

    fun onEvent(event: TodayEvent) {
        when (event) {
            is TodayEvent.LogAnchor -> log(event.anchorId)
            is TodayEvent.StartRoutine -> startRoutine(event.routineId)
            is TodayEvent.UndoLog -> viewModelScope.launch {
                undoLogAnchor(EventId(event.eventId))
            }

            TodayEvent.OpenRunningRoutine -> sendEffect(TodayEffect.OpenPlayer)
        }
    }

    private fun log(anchorId: String) {
        viewModelScope.launch {
            when (val result = logAnchor(
                anchorId = AnchorId(anchorId),
                source = EventSource.MANUAL
            )) {
                is LogResult.Logged -> sendEffect(
                    TodayEffect.Logged(
                        anchorName = anchorName(anchorId),
                        loggedAt = result.event.occurredAt.toLocalTime(timekeeper.timeZone()),
                        eventId = result.event.id.value,
                    ),
                )

                is LogResult.Duplicate -> Unit
            }
        }
    }

    private fun startRoutine(routineId: String) {
        viewModelScope.launch {
            when (startRoutine(RoutineId(routineId))) {
                StartResult.Started, StartResult.AlreadyRunning ->
                    sendEffect(TodayEffect.OpenPlayer)

                StartResult.RoutineNotFound ->
                    sendEffect(TodayEffect.RoutineUnavailable)
            }
        }
    }

    private fun sendEffect(effect: TodayEffect) {
        _effects.trySend(effect)
    }

    private fun anchorName(id: String): String =
        (state.value as? TodayUiState.Content)?.anchors?.firstOrNull {
            it.id == id
        }?.name.orEmpty()

    private fun currentDay(): Flow<LocalDate> = flow {
        while (true) {
            val now = timekeeper.now().wall
            val zone = timekeeper.timeZone()
            val today = now.toLocalDateTime(zone).date
            emit(today)
            val nextMidnight = today.plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(zone)
            delay((nextMidnight - now).coerceAtLeast(1.seconds))
        }
    }.distinctUntilChanged()
}