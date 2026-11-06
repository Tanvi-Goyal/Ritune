package com.ritune.core.designsystem.components

import org.junit.Test
import kotlin.test.assertEquals

class CardSegmentTest {

    @Test
    fun singleRow_isTheWholeCard() {
        assertEquals(
            CardSegment.Only,
            CardSegment.of(index = 0, count = 1)
        )
    }

    @Test
    fun rows_splitIntoTopMiddleAndBottom() {
        assertEquals(
            listOf(
                CardSegment.Top,
                CardSegment.Middle, CardSegment.Middle, CardSegment.Bottom
            ),
            (0 until 4).map {
                CardSegment.of(it, count = 4)
            },
        )
    }

    @Test
    fun twoRows_haveNoMiddle() {
        assertEquals(
            listOf(CardSegment.Top, CardSegment.Bottom),
            (0 until 2).map {
                CardSegment.of(it, count = 2)
            })
    }
}
