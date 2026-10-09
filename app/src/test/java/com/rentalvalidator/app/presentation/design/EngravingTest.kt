package com.rentalvalidator.app.presentation.design

import org.junit.Assert.*
import org.junit.Test

class EngravingTest {
    private val names = listOf("Jardim das Oliveiras", "Esio 46", "Marina Oliveira", "Fabio Jose Santos",
        "Ana Beatriz de Albuquerque", "Geral", "Casa 2", "Kitnet Centro")

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
            assertTrue(it.wavelength in .55f..0.9f)
            assertTrue(it.amplitude in .12f..0.22f)
            assertTrue(it.phase in 0f..(2 * Math.PI).toFloat())
            assertTrue(it.twist in .2f..0.42f)
            assertTrue(it.tilt in -.18f..0.18f)
        }
    }

    @Test fun strandsCoverTheWholeCoverWithoutRunningAway() {
        val width = 360f
        val height = 140f
        names.map(::engravingOf).forEach { engraving ->
            val bases = (0 until EngravingStrands).map { engraving.strandY(it, width / 2, width, height) -
                engraving.amplitude * height * kotlin.math.sin(2 * Math.PI.toFloat() * .5f / engraving.wavelength +
                    engraving.phase + it * engraving.twist) }
            // Strands are spread evenly from just above the top edge to just below the bottom one.
            assertTrue(bases.first() < height * .05f)
            assertTrue(bases.last() > height * .95f)
            bases.zipWithNext { a, b -> assertEquals(height * 1.2f / EngravingStrands, b - a, 1e-2f) }
            for (index in 0 until EngravingStrands) for (x in listOf(0f, width / 3, width)) for (crossing in listOf(false, true)) {
                assertTrue(engraving.strandY(index, x, width, height, crossing) in -height * .5f..height * 1.5f)
            }
        }
    }

    @Test fun theCrossingSetTwistsTheOtherWay() {
        val engraving = engravingOf("Jardim das Oliveiras")
        // The first strand has no twist yet, so both sets start it at the same height.
        assertEquals(engraving.strandY(0, 10f, 360f, 140f), engraving.strandY(0, 10f, 360f, 140f, crossing = true), 1e-4f)
        assertNotEquals(engraving.strandY(5, 10f, 360f, 140f), engraving.strandY(5, 10f, 360f, 140f, crossing = true), 1e-2f)
    }
}
