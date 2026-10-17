package com.ritune.core.domain.usecases.player

import com.ritune.core.domain.models.player.RunProgress
import com.ritune.core.domain.ports.PlayerController
import com.ritune.core.models.PlayerState
import com.ritune.core.models.Timekeeper
import com.ritune.core.models.remainingAt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/*
 * Feeds the countdown. While a step runs it re-reads the monotonic clock about five times a second and emits
 * only when the displayed second changes, so the UI redraws once a second and never drifts.
 * Pausing or finishing stops the ticking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveRunProgress @Inject constructor(
    private val player: PlayerController,
    private val timekeeper: Timekeeper,
) {
    operator fun invoke(): Flow<RunProgress> = player.state
        .flatMapLatest { state ->
            when (state) {
                is PlayerState.Running -> flow {
                    while (true) {
                        emit(
                            RunProgress(
                                state,
                                state
                                    .remainingAt(timekeeper.now().monoMillis)
                                    .roundUpToSeconds()
                            )
                        )
                        delay(200.milliseconds)
                    }
                }

                else -> flowOf(
                    RunProgress(
                        state, state
                            .remainingAt(0)
                    )
                )
            }
        }
        .distinctUntilChanged()

    private fun Duration.roundUpToSeconds(): Duration =
        ((inWholeMilliseconds + 999) / 1000).seconds
}