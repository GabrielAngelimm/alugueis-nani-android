package com.rentalvalidator.app.util

import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType

/** Pure interpretation of extracted PDF text; deliberately preserves the existing matching grammar. */
internal object PdfTransactionExtractor {
    fun extract(text: String, referenceYear: Int): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        // Regex for Date (DD/MM or DD/MM/YYYY) + Description + Amount
        // Examples:
        // 12/05 Pix Recebido Joao 1.000,00
        // 12/05/2026 Transferencia 500,50
        val regex = Regex("(\\d{2}/\\d{2}(?:/\\d{4})?)\\s+(.+?)\\s+([+-]?\\d{1,3}(?:\\.\\d{3})*,\\d{2})")

        val lines = text.split('\n')
        
        for (line in lines) {
            val match = regex.find(line.trim())
            if (match != null) {
                val dateStr = match.groupValues[1]
                val descStr = match.groupValues[2].trim()
                val valStr = match.groupValues[3]

                val amount = CurrencyUtils.parse(valStr)
                val date = DateUtils.parseDate(dateStr, referenceYear)

                if (date != null && amount != 0.0) {
                    val type = if (amount >= 0) TransactionType.CREDIT else TransactionType.DEBIT
                    
                    transactions.add(
                        Transaction(
                            date = date,
                            description = descStr,
                            amount = amount,
                            type = type
                        )
                    )
                }
            }
        }

        return transactions
    }

}
