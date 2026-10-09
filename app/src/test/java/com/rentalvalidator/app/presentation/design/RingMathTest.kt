package com.rentalvalidator.app.presentation.design

import org.junit.Assert.*
import org.junit.Test

class RingMathTest {
    private val cap = 7f
    private val gap = 6f

    /** What the eye sees of an arc: the stroke plus its two round ends. */
    private fun visibleSpan(mark: RingMark?): Float = when (mark) {
        is RingMark.Arc -> if (mark.round) mark.sweep + 2 * cap else mark.sweep
        else -> 0f
    }

    @Test fun equalCountsBecomeEqualShares() {
        val shares = countShares(listOf(2, 2, 2))
        shares.forEach { assertEquals(1f / 3f, it, 1e-6f) }
        assertEquals(1f, shares.sum(), 1e-6f)
    }

    @Test fun sharesFollowCountsNotAmounts() {
        // 1 paid, 3 due and 0 late is a quarter and three quarters, whatever each rent is worth.
        assertEquals(listOf(.25f, .75f, 0f), countShares(listOf(1, 3, 0)))
        assertEquals(listOf(0f, 0f), countShares(listOf(0, 0)))
    }

    @Test fun equalSharesAreDrawnTheSameSizeAndInOrder() {
        val marks = ringMarks(countShares(listOf(2, 2, 2)), cap, gap)
        val spans = marks.map(::visibleSpan)
        spans.forEach { assertEquals(120f - gap, it, 1e-3f) }
        // Clockwise from twelve o'clock: each arc starts one third after the previous one.
        val starts = marks.map { (it as RingMark.Arc).start - cap }
        assertEquals(-90f + gap / 2, starts[0], 1e-3f)
        assertEquals(30f + gap / 2, starts[1], 1e-3f)
        assertEquals(150f + gap / 2, starts[2], 1e-3f)
    }

    @Test fun unequalSharesKeepTheirRatio() {
        val marks = ringMarks(countShares(listOf(1, 2, 3)), cap, gap)
        val spans = marks.map { visibleSpan(it) + gap }
        assertEquals(60f, spans[0], 1e-3f)
        assertEquals(120f, spans[1], 1e-3f)
        assertEquals(180f, spans[2], 1e-3f)
    }

    @Test fun emptySharesLeaveNoMarkAndAWholeShareClosesTheRing() {
        val marks = ringMarks(listOf(0f, 1f, 0f), cap, gap)
        assertNull(marks[0]); assertNull(marks[2])
        assertEquals(RingMark.Arc(-90f, 360f, round = false), marks[1])
    }

    @Test fun aTinyShareStaysVisibleAsADot() {
        val marks = ringMarks(listOf(.01f, .99f), cap, gap)
        assertTrue(marks[0] is RingMark.Dot)
        assertTrue(marks[1] is RingMark.Arc)
    }

    @Test fun dueStatesAreCountedOncePerTenant() {
        val counts = dueCounts(listOf(DueState.PAID, DueState.PAID, DueState.UPCOMING, DueState.DUE_TODAY,
            DueState.OVERDUE, DueState.OVERDUE, DueState.REVIEW))
        assertEquals(DueCounts(paid = 2, upcoming = 2, overdue = 2, review = 1), counts)
        assertEquals(7, counts.total)
    }

    @Test fun percentagesRoundToTheNearestWhole() {
        assertEquals(33, percentOf(2, 6))
        assertEquals(67, percentOf(2, 3))
        assertEquals(0, percentOf(0, 0))
        assertEquals(100, percentOf(3, 3))
    }
}
