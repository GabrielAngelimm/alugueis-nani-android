package com.rentalvalidator.app.util

import java.text.Normalizer

object TextNormalizer {
    /**
     * Removes accents, lowercases and trims extra spaces.
     * Example: " João  da Silva " -> "joao da silva"
     */
    fun normalize(text: String?): String {
        if (text == null) return ""
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .lowercase()
            .trim()
            .replace("\\s+".toRegex(), " ")
    }
}
