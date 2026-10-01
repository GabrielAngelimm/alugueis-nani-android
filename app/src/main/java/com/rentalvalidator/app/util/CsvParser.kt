package com.rentalvalidator.app.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType
import java.io.InputStream
import java.time.LocalDate

object CsvParser {
    fun parse(inputStream: InputStream, referenceYear: Int = LocalDate.now().year): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        // Read all rows with header
        val rows: List<Map<String, String>> = csvReader {
            skipEmptyLine = true
        }.readAllWithHeader(inputStream)
        
        for (row in rows) {
            val transaction = transformRow(row, referenceYear)
            if (transaction != null) {
                transactions.add(transaction)
            }
        }
        
        return transactions
    }

    private fun transformRow(row: Map<String, String>, referenceYear: Int): Transaction? {
        // Normalize keys to lowercase to find columns flexibly
        val keys = row.keys.associateBy { it.lowercase() }
        
        fun findKey(candidates: List<String>): String? {
            for (candidate in candidates) {
                if (keys.containsKey(candidate)) return keys[candidate]
            }
            return null
        }

        val dateKey = findKey(listOf("data", "date", "dt", "data movimento"))
        val descKey = findKey(listOf("descrição", "descricao", "description", "histórico", "historico", "memo"))
        val valKey = findKey(listOf("valor", "amount", "value", "valor (r$)"))

        if (dateKey == null || descKey == null || valKey == null) return null

        val dateStr = row[dateKey]
        val descStr = row[descKey]
        val valStr = row[valKey]

        val amount = CurrencyUtils.parse(valStr)
        if (amount == 0.0 && valStr.isNullOrBlank()) return null

        val date = DateUtils.parseDate(dateStr, referenceYear) ?: return null

        // In most Brazilian bank CSVs, positive values are credits
        val type = if (amount >= 0) TransactionType.CREDIT else TransactionType.DEBIT

        return Transaction(
            date = date,
            description = descStr?.trim() ?: "",
            amount = amount,
            type = type
        )
    }
}
