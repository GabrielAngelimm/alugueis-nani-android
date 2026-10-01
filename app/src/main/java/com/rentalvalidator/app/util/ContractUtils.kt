package com.rentalvalidator.app.util

import androidx.compose.ui.graphics.Color
import com.rentalvalidator.app.presentation.theme.SuccessGreen
import com.rentalvalidator.app.presentation.theme.WarningAmber
import com.rentalvalidator.app.presentation.theme.md_theme_dark_error
import com.rentalvalidator.app.presentation.theme.md_theme_dark_onSurfaceVariant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

enum class ContractStatus {
    VIGENTE,
    VENCE_EM_BREVE,
    VENCIDO,
    DATA_PENDENTE,
    SEM_CONTRATO
}

data class ContractStatusInfo(
    val status: ContractStatus,
    val label: String,
    val color: Color
)

fun getContractStatus(expirationDate: String, hasFile: Boolean): ContractStatusInfo {
    if (!hasFile) {
        return ContractStatusInfo(
            status = ContractStatus.SEM_CONTRATO,
            label = "Sem Contrato",
            color = md_theme_dark_onSurfaceVariant
        )
    }

    if (expirationDate.isBlank()) {
        return ContractStatusInfo(
            status = ContractStatus.DATA_PENDENTE,
            label = "Data Pendente",
            color = WarningAmber
        )
    }

    val parsedDate = parseContractDate(expirationDate)
        ?: return ContractStatusInfo(
            status = ContractStatus.DATA_PENDENTE,
            label = "Data Pendente",
            color = WarningAmber
        )

    val today = LocalDate.now()
    val daysUntilExpiration = ChronoUnit.DAYS.between(today, parsedDate)

    return when {
        daysUntilExpiration < 0 -> ContractStatusInfo(
            status = ContractStatus.VENCIDO,
            label = "Vencido",
            color = md_theme_dark_error
        )
        daysUntilExpiration <= 45 -> ContractStatusInfo(
            status = ContractStatus.VENCE_EM_BREVE,
            label = "Vence em breve",
            color = WarningAmber
        )
        else -> ContractStatusInfo(
            status = ContractStatus.VIGENTE,
            label = "Vigente",
            color = SuccessGreen
        )
    }
}

private val ddmmyyyyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val yyyymmddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun parseContractDate(dateStr: String): LocalDate? {
    val trimmed = dateStr.trim()
    return try {
        if (trimmed.contains("-")) {
            LocalDate.parse(trimmed, yyyymmddFormatter)
        } else {
            LocalDate.parse(trimmed, ddmmyyyyFormatter)
        }
    } catch (e: DateTimeParseException) {
        null
    }
}
