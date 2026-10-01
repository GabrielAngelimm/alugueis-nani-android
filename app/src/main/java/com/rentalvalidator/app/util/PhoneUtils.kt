package com.rentalvalidator.app.util

object PhoneUtils {
    /**
     * Extracts only the digits from a phone number string.
     */
    fun getDigits(phone: String?): String {
        if (phone == null) return ""
        return phone.replace("\\D".toRegex(), "")
    }

    /**
     * Formats a 10 or 11 digit Brazilian phone number.
     * Returns the original string if not matching.
     */
    fun formatPhone(phone: String?): String {
        if (phone == null) return ""
        val digits = getDigits(phone)
        if (digits.length == 11) {
            return "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
        } else if (digits.length == 10) {
            return "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6)}"
        }
        return phone
    }
}
