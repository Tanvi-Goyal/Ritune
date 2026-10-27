package com.ritune.feature.rhythm.ui.mapper

import com.ritune.core.domain.models.rhythm.AnchorRhythm
import com.ritune.core.domain.models.rhythm.DayConsistency
import com.ritune.core.domain.models.rhythm.WeekRhythm
import com.ritune.core.models.DefaultCatalog
import com.ritune.feature.rhythm.ui.screens.DayBar
import com.ritune.feature.rhythm.ui.screens.RhythmSummary
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.assertEquals

class RhythmUiStateMapperTest {

    private val weekEnd = LocalDate(2026, 1, 11)
    private val days = listOf(
        DayConsistency(LocalDate(2026, 1, 10), 0.5f),
        DayConsistency(weekEnd, null),
    )

    private fun rhythm(
        consistency: Float? = 0.666f,
        hasEnoughData: Boolean = true,
        anchors: List<AnchorRhythm> = emptyList(),
        steadiest: Boolean = false,
    ) = WeekRhythm(
        days = days,
        consistency = consistency,
        steadiest = if (steadiest) DefaultCatalog.lightsOut else null,
        anchors = anchors,
        loggedDays = 2,
        hasEnoughData = hasEnoughData,
    )

    @Test
    fun buildContent_notEnoughDays_isEarlyWithProgress() {
        val content = buildContent(weekEnd, rhythm(hasEnoughData = false))

        assertEquals(RhythmSummary.Early(loggedDays = 2, requiredDays = 3), content.summary)
    }

    @Test
    fun buildContent_enoughDaysButNothingScored_isBuilding() {
        assertEquals(RhythmSummary.Building, buildContent(weekEnd, rhythm(consistency = null)).summary)
    }

    @Test
    fun buildContent_scored_roundsPercentAndNamesWakeDriftAndSteadiest() {
        val anchors = listOf(
            AnchorRhythm(DefaultCatalog.lightsOut, medianMinutes = 1_350, driftMinutes = 4, logsThisWeek = 3),
            AnchorRhythm(DefaultCatalog.wake, medianMinutes = 420, driftMinutes = 12, logsThisWeek = 3),
        )

        val content = buildContent(weekEnd, rhythm(anchors = anchors, steadiest = true))

        assertEquals(RhythmSummary.Scored(67, wakeDriftMinutes = 12, steadiestName = "Lights out"), content.summary)
        assertEquals(weekEnd, content.weekEnd)
        assertEquals(listOf(DayBar(LocalDate(2026, 1, 10), 0.5f), DayBar(weekEnd, null)), content.days)
    }

    @Test
    fun buildContent_scoredWithoutWakeOrSteadiest_leavesThemEmpty() {
        val content = buildContent(weekEnd, rhythm())

        assertEquals(RhythmSummary.Scored(67, wakeDriftMinutes = null, steadiestName = null), content.summary)
    }
}
