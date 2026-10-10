package com.rentalvalidator.app.presentation.design

import org.junit.Assert.*
import org.junit.Test

class EngravingTest {
    private val names = listOf("Jardim das Oliveiras", "Esio 46", "Marina Oliveira", "Fabio Jose Santos",
        "Ana Beatriz de Albuquerque", "Geral", "Casa 2", "Kitnet Centro")

    /** Angles across the quarter the cover draws, from just before straight down to just past straight left. */
    private val angles = (0..140).map { (.47f + it * .004f) * Math.PI.toFloat() }

    @Test fun aNameKeepsItsEngravingAcrossVisits() {
        assertEquals(engravingOf("Marina Oliveira"), engravingOf("Marina Oliveira"))
        // Spacing and case typed differently still belong to the same record.
        assertEquals(engravingOf("Marina Oliveira"), engravingOf("  marina oliveira "))
    }

    @Test fun differentNamesGetDifferentEngravings() {
        assertEquals(names.size, names.map(::engravingOf).toSet().size)
    }

    @Test fun everyEngravingStaysWithinItsDesignedRange() {
        (names + (1..200).map { "Unidade $it" }).map(::engravingOf).forEach {
            assertTrue(it.waves in 4f..6f)
            assertTrue(it.amplitude in .016f..0.028f)
            assertTrue(it.phase in 0f..(2 * Math.PI).toFloat())
            assertTrue(it.twist in .05f..0.3f)
        }
    }

    @Test fun ringsOpenUpFromTheLitCornerAndNeverTouch() {
        // From the shortest cover to one holding a three-line name, on a phone and on a wide window.
        for ((width, height) in listOf(360f to 132f, 360f to 200f, 320f to 140f, 920f to 132f)) {
            names.map(::engravingOf).forEach { engraving ->
                assertEquals(height * .3f, engraving.ringRadius(0, angles[0], width, height), height * .03f)
                for (angle in angles) {
                    val radii = (0 until EngravingRings).map { engraving.ringRadius(it, angle, width, height) }
                    radii.zipWithNext { inner, outer -> assertTrue(outer - inner > 2f) }
                }
                // Each gap is wider than the one before it, measured without the ripples.
                val gaps = (0 until EngravingRings).map { ring ->
                    angles.map { engraving.ringRadius(ring, it, width, height) }.average()
                }.zipWithNext { inner, outer -> outer - inner }
                assertTrue(gaps.last() > gaps.first())
            }
        }
    }

    @Test fun theRingsStayClearOfTheMark() {
        // The mark ends 84dp from the left edge of the cover (20dp padding and a 64dp mark).
        for (width in listOf(320f, 360f, 412f, 600f, 920f)) {
            names.map(::engravingOf).forEach { engraving ->
                val outermost = angles.minOf { angle ->
                    width * EngravingCenterX + engraving.ringRadius(EngravingRings - 1, angle, width, 140f) * kotlin.math.cos(angle)
                }
                assertTrue("width $width reaches $outermost", outermost > 84f)
            }
        }
    }

    @Test fun ripplesTurnFromOneRingToTheNext() {
        val engraving = engravingOf("Jardim das Oliveiras")
        // A ring's base radius is the same all around, so its widest point is the crest of its ripple.
        fun crest(ring: Int) = angles.maxBy { engraving.ringRadius(ring, it, 360f, 140f) }
        // Crests of the inner and outer rings sit at different angles, so the lines flow instead of stacking.
        assertNotEquals(crest(0), crest(EngravingRings - 1), 1e-3f)
    }
}
