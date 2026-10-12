package com.rentalvalidator.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val localeBR = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(localeBR)

    @Synchronized
    fun format(amount: Double): String {
        return currencyFormat.format(amount)
    }

    /**
     * Reads an amount as banks print it, in Brazilian ("1.450,00") or international ("1450.00",
     * "1,450.00") notation. The decimal separator is the last dot or comma followed by one or two
     * digits; any other dot or comma groups thousands. A minus sign before or after the number makes it negative.
     */
    fun parse(amountStr: String?): Double {
        if (amountStr == null) return 0.0
        val clean = amountStr.replace("[^\\d,.-]".toRegex(), "")
        val negative = clean.startsWith('-') || clean.endsWith('-')
        val number = clean.trim('-')
        val separator = number.indexOfLast { it == '.' || it == ',' }
        val decimals = number.substring(separator + 1)
            .takeIf { separator >= 0 && it.length in 1..2 && it.all(Char::isDigit) }
        val integer = (if (decimals != null) number.substring(0, separator) else number).filter(Char::isDigit)
        if (integer.isEmpty() && decimals == null) return 0.0
        val value = "${integer.ifEmpty { "0" }}.${decimals ?: "0"}".toDouble()
        return if (negative) -value else value
    }
}
