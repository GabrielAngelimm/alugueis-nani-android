package com.rentalvalidator.app.presentation.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverGeometryTest {
    /** From the shortest cover to one holding a three-line name, on a phone, a narrow phone and a wide window. */
    private val covers = listOf(372f to 132f, 372f to 200f, 280f to 140f, 920f to 132f)

    /** A glyph is square; initials run from one narrow letter to two wide ones. */
    private val aspects = listOf(.6f, 1f, 1.4f, 1.9f)

    @Test fun theReliefNeverComesInPastHalfTheCover() {
        for ((width, height) in covers) for (aspect in aspects) {
            val frame = reliefFrame(width, height, aspect)
            assertTrue("$width x $height, aspect $aspect", frame.left >= width * ReliefReach - .01f)
        }
    }

    @Test fun theReliefRunsOffTheRightEdgeByTheSameShare() {
        for ((width, height) in covers) for (aspect in aspects) {
            val frame = reliefFrame(width, height, aspect)
            assertEquals(width + frame.width * ReliefBleed, frame.left + frame.width, .01f)
        }
    }

    @Test fun theReliefKeepsTheMarksProportions() {
        for ((width, height) in covers) for (aspect in aspects) {
            val frame = reliefFrame(width, height, aspect)
            assertEquals(aspect, frame.width / frame.height, .001f)
        }
    }

    @Test fun onAPhoneTheReliefIsAboutAsTallAsTheCoverAndSitsALittleLow() {
        val frame = reliefFrame(372f, 132f, 1f)
        assertEquals(132f * 1.04f, frame.height, .01f)
        val middle = frame.top + frame.height / 2
        assertTrue(middle > 132f / 2 && middle < 132f * .6f)
    }

    @Test fun aWideMarkOnANarrowCoverIsSetSmallerInsteadOfReachingTheName() {
        val wide = reliefFrame(280f, 140f, 1.9f)
        val square = reliefFrame(280f, 140f, 1f)
        assertTrue(wide.height < square.height)
    }
}
