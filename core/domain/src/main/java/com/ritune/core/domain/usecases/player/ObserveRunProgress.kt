//package com.ritune.core.domain.usecases.player
//
//import com.ritune.core.models.Timekeeper
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOf
//import kotlin.time.Duration
//import kotlin.time.Duration.Companion.milliseconds
//import kotlin.time.Duration.Companion.seconds
//
///** The player state plus the time left in the current step, rounded up to whole seconds. */
//data class RunProgress(val state: PlayerState, val remaining: Duration)
//
///**
// * Feeds the countdown. While a step runs it re-reads the monotonic clock about five times a second and emits
// * only when the displayed second changes, so the UI redraws once a second and never drifts.
// * Pausing or finishing stops the ticking.
// */
//@OptIn(ExperimentalCoroutinesApi::class)
//class ObserveRunProgress(
//    private val player: PlayerController,
//    private val timekeeper: Timekeeper,
//    private val tick: Duration = 200.milliseconds,
//) {
//    operator fun invoke(): Flow<RunProgress> = player.state
//        .flatMapLatest { state ->
//            when (state) {
//                is PlayerState.Running -> flow {
//                    while (true) {
//                        emit(
//                            RunProgress(
//                                state,
//                                state.remainingAt(timekeeper.now().monoMillis)
//                                    .roundUpToSeconds()
//                            )
//                        )
//                        delay(tick)
//                    }
//                }
//
//                else -> flowOf(RunProgress(state, state.remainingAt(0)))
//            }
//        }
//        .distinctUntilChanged()
//
//    private fun Duration.roundUpToSeconds(): Duration =
//        ((inWholeMilliseconds + 999) / 1000).seconds
//}