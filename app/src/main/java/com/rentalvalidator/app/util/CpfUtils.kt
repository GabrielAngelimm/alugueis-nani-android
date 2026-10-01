package com.rentalvalidator.app.util

object CpfUtils {
    fun digits(value: String): String = value.filter(Char::isDigit).take(11)

    fun format(value: String): String {
        val digits = digits(value)
        return buildString {
            digits.forEachIndexed { index, char ->
                if (index == 3 || index == 6) append('.')
                if (index == 9) append('-')
                append(char)
            }
        }
    }

    fun isCompleteOrBlank(value: String): Boolean {
        val digits = digits(value)
        return digits.isEmpty() || digits.length == 11
    }
}
