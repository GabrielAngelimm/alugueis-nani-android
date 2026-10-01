package com.rentalvalidator.app.util

import android.content.Context
import com.rentalvalidator.app.domain.model.Transaction
import com.rentalvalidator.app.domain.model.TransactionType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.io.IOException
import java.time.LocalDate

object PdfParser {

    @Volatile
    private var initialized = false

    /**
     * Initializes PdfBox-Android lazily and extracts transactions for the selected year.
     */
    fun parse(
        context: Context,
        inputStream: InputStream,
        referenceYear: Int = LocalDate.now().year
    ): List<Transaction> {
        ensureInitialized(context)
        return try {
            PDDocument.load(inputStream).use { document ->
                val fullText = PDFTextStripper().getText(document)
                PdfTransactionExtractor.extract(fullText, referenceYear)
            }
        } catch (failure: IOException) {
            throw IllegalArgumentException("Não foi possível ler o PDF. Verifique se o arquivo está íntegro e não exige senha.", failure)
        }
    }

    private fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                PDFBoxResourceLoader.init(context.applicationContext)
                initialized = true
            }
        }
    }
}
