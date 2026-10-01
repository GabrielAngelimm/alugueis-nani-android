package com.rentalvalidator.app.util

import com.rentalvalidator.app.domain.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StatementParsingTest {
    @Test fun pdfRetainsCreditDebitAndExplicitDates() {
        val entries = PdfTransactionExtractor.extract("""
            Extrato bancário
            12/05 Pix Recebido Maria 1.250,50
            13/05/2025 Tarifa bancária -12,00
            14/05 Movimento zerado 0,00
        """.trimIndent(), 2026)
        assertEquals(2, entries.size)
        assertEquals(LocalDate.of(2026, 5, 12), entries[0].date)
        assertEquals("Pix Recebido Maria", entries[0].description)
        assertEquals(1250.50, entries[0].amount, 0.0)
        assertEquals(TransactionType.CREDIT, entries[0].type)
        assertEquals(LocalDate.of(2025, 5, 13), entries[1].date)
        assertEquals(TransactionType.DEBIT, entries[1].type)
    }

    @Test fun csvKeepsQuotedDescriptionsAndReferenceYear() {
        val input = "Data,Descrição,Valor\n12/05,\"Pix Maria, aluguel\",\"1.250,50\"\n13/05,Tarifa,\"-12,00\""
        val entries = CsvParser.parse(input.byteInputStream(), 2024)
        assertEquals(2, entries.size)
        assertEquals(LocalDate.of(2024, 5, 12), entries[0].date)
        assertEquals("Pix Maria, aluguel", entries[0].description)
        assertEquals(1250.50, entries[0].amount, 0.0)
        assertEquals(TransactionType.DEBIT, entries[1].type)
    }

    @Test fun unrelatedPdfTextDoesNotProduceTransactions() {
        assertTrue(PdfTransactionExtractor.extract("Saldo e informações cadastrais", 2026).isEmpty())
    }
}
