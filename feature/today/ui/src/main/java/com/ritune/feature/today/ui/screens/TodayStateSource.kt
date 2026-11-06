package com.ritune.feature.today.ui.screens

import com.ritune.core.domain.usecases.anchor.ObserveToday
import com.ritune.core.domain.usecases.player.ObservePlayerState
import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.Timekeeper
import com.ritune.feature.today.ui.mapper.buildContent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Today's state, and a copy of its first value read during the splash. Today is the start
 * destination, so with that copy its first frame draws the content instead of an empty Loading
 * frame.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TodayStateSource @Inject constructor(
    private val observeToday: ObserveToday,
    private val observeCurrentDay: ObserveCurrentDay,
    private val observePlayerState: ObservePlayerState,
    private val observeRoutines: ObserveRoutines,
    private val timekeeper: Timekeeper,
) {
    private val preloaded = AtomicReference<TodayUiState?>(null)

    fun observe(): Flow<TodayUiState> = observeCurrentDay()
        .flatMapLatest { day ->
            combine(
                observeToday(day),
                observeRoutines(),
                observePlayerState(),
            ) { statuses, allRoutines, playerState ->
                buildContent(day, statuses, allRoutines, playerState, timekeeper.timeZone())
            }
        }

    suspend fun preload() {
        preloaded.set(observe().first())
    }

    /**
     * The preloaded state, handed out once: only the first Today screen opens right after the
     * splash. Any later one starts from Loading, so it never shows an old copy.
     */
    fun takePreloaded(): TodayUiState = preloaded.getAndSet(null) ?: TodayUiState.Loading
}
