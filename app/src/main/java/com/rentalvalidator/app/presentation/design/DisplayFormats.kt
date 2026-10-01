package com.rentalvalidator.app.presentation.design

import com.rentalvalidator.app.util.DateUtils

/** Display only: persisted dates retain their existing ISO representation. */
fun displayDate(value: String, empty: String): String =
    DateUtils.formatDate(DateUtils.parseDate(value)).ifBlank { value.ifBlank { empty } }
