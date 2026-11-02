package com.ritune

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.ritune.core.designsystem.TestTags
import com.ritune.core.domain.ports.DataResetter
import com.ritune.core.domain.repositories.AnchorEventRepository
import com.ritune.core.domain.repositories.AnchorRepository
import com.ritune.core.domain.repositories.RoutineRepository
import com.ritune.core.models.AnchorEvent
import com.ritune.core.models.EventId
import com.ritune.core.models.EventSource
import com.ritune.core.models.IdGenerator
import com.ritune.core.models.Routine
import com.ritune.core.models.RoutineId
import com.ritune.core.models.Step
import com.ritune.core.models.StepId
import com.ritune.core.models.Timekeeper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
@AndroidEntryPoint
class SeedActivity : ComponentActivity() {

    @Inject lateinit var resetter: DataResetter
    @Inject lateinit var anchors: AnchorRepository
    @Inject lateinit var routines: RoutineRepository
    @Inject lateinit var events: AnchorEventRepository
    @Inject lateinit var ids: IdGenerator
    @Inject lateinit var timekeeper: Timekeeper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val days = intent.getIntExtra(EXTRA_DAYS, DEFAULT_DAYS)
        val routineCount = intent.getIntExtra(EXTRA_ROUTINES, DEFAULT_ROUTINES)
        val withLongRoutine = intent.getBooleanExtra(EXTRA_LONG_ROUTINE, false)
        // Blocking keeps `am start -W` from returning until the data is written.
        runBlocking { seed(days, routineCount, withLongRoutine) }
        finish()
    }

    private suspend fun seed(days: Int, routineCount: Int, withLongRoutine: Boolean) {
        // A fixed seed gives every run, on both phones, the same data.
        val random = Random(RANDOM_SEED)
        resetter.resetAll()
        repeat(routineCount) { index -> routines.save(routine(index, random)) }
        logAnchors(days, random)
        // Last and without `random`, so the data above matches runs that don't ask for it.
        if (withLongRoutine) routines.save(longRoutine())
    }

    private fun longRoutine() = Routine(
        id = RoutineId(ids.newId()),
        name = TestTags.LONG_ROUTINE_NAME,
        steps = List(TestTags.LONG_ROUTINE_STEPS) { step ->
            Step(id = StepId(ids.newId()), title = "Step ${step + 1}", duration = 60.seconds)
        },
    )

    private fun routine(index: Int, random: Random) = Routine(
        id = RoutineId(ids.newId()),
        name = "Benchmark routine ${index + 1}",
        steps = List(random.nextInt(3, 9)) { step ->
            Step(
                id = StepId(ids.newId()),
                title = "Step ${step + 1}",
                duration = random.nextInt(30, 301).seconds,
            )
        },
    )

    private suspend fun logAnchors(days: Int, random: Random) {
        val zone = timekeeper.timeZone()
        val now = timekeeper.now().wall
        val today = now.toLocalDateTime(zone).date

        anchors.observeAll().first().forEachIndexed { index, anchor ->
            val usual = anchor.targetTime ?: LocalTime((7 + index * 3) % 24, 0)
            for (daysAgo in days downTo 0) {
                val jitter = random.nextInt(-JITTER_SECONDS, JITTER_SECONDS + 1)
                val second = (usual.toSecondOfDay() + jitter).coerceIn(0, 86_399)
                val date = today.minus(daysAgo, DateTimeUnit.DAY)
                val occurredAt = LocalDateTime(date, LocalTime.fromSecondOfDay(second)).toInstant(zone)
                if (occurredAt > now) continue

                events.append(
                    AnchorEvent(
                        id = EventId(ids.newId()),
                        anchorId = anchor.id,
                        occurredAt = occurredAt,
                        utcOffsetSeconds = zone.offsetAt(occurredAt).totalSeconds,
                        source = EventSource.MANUAL,
                    )
                )
            }
        }
    }

    private companion object {
        const val EXTRA_DAYS = "days"
        const val EXTRA_ROUTINES = "routines"
        const val EXTRA_LONG_ROUTINE = "longRoutine"
        const val DEFAULT_DAYS = 21
        const val DEFAULT_ROUTINES = 30
        const val RANDOM_SEED = 42
        const val JITTER_SECONDS = 30 * 60
    }
}
