package com.rentalvalidator.app.presentation.ui.tenants

import com.rentalvalidator.app.util.CpfUtils

class PhoneVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = text.text.filter(Char::isDigit)
        val breakAt = if (digits.length <= 10) 6 else 7
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                when (index) { 0 -> append('('); 2 -> append(") ") }
                if (index == breakAt) append('-')
                append(c)
            }
        }
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                var count = 0
                formatted.forEachIndexed { index, char ->
                    if (char.isDigit() && ++count == offset) return index + 1
                }
                return formatted.length
            }
            override fun transformedToOriginal(offset: Int): Int = formatted.take(offset).count(Char::isDigit).coerceAtMost(digits.length)
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(formatted), mapping)
    }
}

class CpfVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = CpfUtils.digits(text.text)
        val formatted = CpfUtils.format(digits)
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> offset + 1
                offset <= 8 -> offset + 2
                else -> offset + 3
            }.coerceIn(0, formatted.length)

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 7 -> offset - 1
                offset <= 11 -> offset - 2
                else -> offset - 3
            }.coerceIn(0, digits.length)
        }
        return androidx.compose.ui.text.input.TransformedText(
            androidx.compose.ui.text.AnnotatedString(formatted),
            mapping
        )
    }
}

/**
 * Pre-fills a money field the way it is written in Brazil ("1250,00"). A stored value with more
 * than two decimals is shown as-is, so opening and saving a form never rounds it silently.
 */
internal fun moneyInputText(value: Double): String {
    val exact = runCatching { java.math.BigDecimal(value.toString()).stripTrailingZeros() }.getOrNull() ?: return value.toString()
    return if (exact.scale() <= 2) exact.setScale(2).toPlainString().replace('.', ',') else value.toString()
}

fun formatPhoneForDB(digits: String): String {
    val value = digits.filter(Char::isDigit).take(11)
    return when (value.length) {
        11 -> "(${value.take(2)}) ${value.substring(2, 7)}-${value.takeLast(4)}"
        10 -> "(${value.take(2)}) ${value.substring(2, 6)}-${value.takeLast(4)}"
        else -> value
    }
}
