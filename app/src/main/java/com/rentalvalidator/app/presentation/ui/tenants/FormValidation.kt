package com.rentalvalidator.app.presentation.ui.tenants

internal fun validOptionalPhone(value: String): Boolean = value.isBlank() ||
    (value.length in 10..11 && value.all(Char::isDigit) && value.take(2).all { it != '0' } &&
        (if (value.length == 11) value[2] == '9' else value[2] in '2'..'9'))

internal fun validOptionalCpf(value: String): Boolean {
    if (value.isBlank()) return true
    if (value.length != 11 || !value.all(Char::isDigit) || value.toSet().size == 1) return false
    fun digit(length: Int): Int {
        val sum = (0 until length).sumOf { value[it].digitToInt() * (length + 1 - it) }
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }
    return value[9].digitToInt() == digit(9) && value[10].digitToInt() == digit(10)
}

internal fun validMoney(value: String, allowZero: Boolean = false): Boolean {
    val number = value.replace(',', '.').toDoubleOrNull() ?: return false
    return number.isFinite() && (if (allowZero) number >= 0 else number > 0) &&
        value.matches(Regex("\\d+(?:[.,]\\d{1,2})?"))
}
