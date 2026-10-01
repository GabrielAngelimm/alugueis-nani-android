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

    fun parse(amountStr: String?): Double {
        if (amountStr == null) return 0.0
        val cleanString = amountStr.replace("[^\\d,-]".toRegex(), "").replace(",", ".")
        return cleanString.toDoubleOrNull() ?: 0.0
    }
}
