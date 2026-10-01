package com.rentalvalidator.app.data.local

import com.rentalvalidator.app.data.local.converters.Converters
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testListToStringAndBack() {
        val list = listOf("Alias 1", "Alias 2", "Another Name")
        val json = converters.fromStringList(list)
        assertEquals("[\"Alias 1\",\"Alias 2\",\"Another Name\"]", json)

        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun testEmptyList() {
        val list = emptyList<String>()
        val json = converters.fromStringList(list)
        assertEquals("[]", json)

        val restored = converters.toStringList(json)
        assertEquals(list, restored)
    }

    @Test
    fun testInvalidJsonReturnsEmptyList() {
        val restored = converters.toStringList("invalid-json")
        assertEquals(emptyList<String>(), restored)
    }
}
