package com.rentalvalidator.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.usecase.ValidationResult

object WhatsAppUtils {

    fun generateWhatsAppLink(phone: String, message: String): String {
        val cleanPhone = PhoneUtils.getDigits(phone)
        val encodedMessage = Uri.encode(message)
        // Ensure phone has country code. Assuming Brazil (55) if length is 10 or 11
        val fullPhone = if (cleanPhone.length in 10..11) "55$cleanPhone" else cleanPhone
        return "https://wa.me/$fullPhone?text=$encodedMessage"
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val link = generateWhatsAppLink(phone, message)
        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
        // Attempt to open specifically in WhatsApp if installed
        intent.setPackage("com.whatsapp")
        // Required when calling from outside an Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed, fallback to browser
            val fallbackIntent = Intent(Intent.ACTION_VIEW, link.toUri())
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                // Ignore if no browser found
            }
        }
    }

    fun createBillingMessage(
        result: ValidationResult,
        monthName: String = "mês atual",
        includePenalty: Boolean = false
    ): String {
        val base = createReminderMessage(result.tenant, monthName)
        if (!includePenalty || result.penaltyApplied <= 0.0) return base
        val remainingRent = (result.amountDue - result.amountPaid).coerceAtLeast(0.0)
        val total = remainingRent + result.penaltyApplied
        return base.replace(
            "\n#mensagemautomática",
            "\nValor restante do aluguel: ${CurrencyUtils.format(remainingRent)}. " +
                "Multa: ${CurrencyUtils.format(result.penaltyApplied)}. " +
                "Total: ${CurrencyUtils.format(total)}.\n#mensagemautomática"
        )
    }

    fun createReminderMessage(tenant: Tenant, monthName: String = "mês atual"): String {
        val tenantName = tenant.whatsappName.takeIf { it.isNotBlank() }
            ?: tenant.name.split(" ").firstOrNull()
            ?: "Inquilino"

        return "Oi, $tenantName, tudo bem? Gostaria de avisar que ainda não identificamos o pagamento do aluguel do mês de $monthName. Poderia verificar, por favor?\n#mensagemautomática"
    }
}
