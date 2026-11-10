package com.ritune.core.testing.coach

import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.domain.repositories.RunHistoryRepository
import com.ritune.core.domain.usecases.anchor.ObserveAnchors
import com.ritune.core.domain.usecases.coach.CoachToolExecutor
import com.ritune.core.domain.usecases.coach.tools.AnchorEventsTool
import com.ritune.core.domain.usecases.coach.tools.AnchorsTool
import com.ritune.core.domain.usecases.coach.tools.RoutinesTool
import com.ritune.core.domain.usecases.coach.tools.RunHistoryTool
import com.ritune.core.domain.usecases.coach.tools.WeekRhythmTool
import com.ritune.core.domain.usecases.rhythm.ObserveCurrentDay
import com.ritune.core.domain.usecases.rhythm.ObserveWeekRhythm
import com.ritune.core.domain.usecases.routine.ObserveRoutines
import com.ritune.core.models.Timekeeper
import com.ritune.core.testing.repository.FakeAnchorEventRepository
import com.ritune.core.testing.repository.FakeAnchorRepository
import com.ritune.core.testing.repository.FakeRoutineRepository
import com.ritune.core.testing.repository.FakeRunHistoryRepository

fun coachToolExecutor(
    timekeeper: Timekeeper,
    anchors: AnchorRepository = FakeAnchorRepository(),
    events: AnchorEventRepository = FakeAnchorEventRepository(),
    routines: RoutineRepository = FakeRoutineRepository(),
    runs: RunHistoryRepository = FakeRunHistoryRepository(),
): CoachToolExecutor {
    val observeAnchors = ObserveAnchors(anchors)
    return CoachToolExecutor(
        WeekRhythmTool(
            ObserveCurrentDay(timekeeper),
            ObserveWeekRhythm(anchors, events, timekeeper)
        ),
        AnchorsTool(observeAnchors),
        AnchorEventsTool(observeAnchors, events, timekeeper),
        RunHistoryTool(runs, timekeeper),
        RoutinesTool(ObserveRoutines(routines), observeAnchors),
    )
}
