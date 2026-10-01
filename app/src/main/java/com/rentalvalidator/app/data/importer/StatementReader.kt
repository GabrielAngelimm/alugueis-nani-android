package com.rentalvalidator.app.data.importer



import android.content.Context

import android.net.Uri

import com.rentalvalidator.app.domain.model.Transaction

import com.rentalvalidator.app.util.CsvParser

import com.rentalvalidator.app.util.PdfParser

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import javax.inject.Inject



/** Owns the SAF stream lifetime and keeps parsing off the main thread. */

class StatementReader @Inject constructor(@param:ApplicationContext private val context: Context) {

    suspend fun read(uri: Uri, referenceYear: Int): List<Transaction> = withContext(Dispatchers.IO) {

        val type = context.contentResolver.getType(uri).orEmpty()

        val isCsv = uri.path?.endsWith(".csv", ignoreCase = true) == true ||

            type.contains("csv") || type.contains("comma-separated")

        context.contentResolver.openInputStream(uri)?.use { stream ->

            if (isCsv) CsvParser.parse(stream, referenceYear)

            else PdfParser.parse(context, stream, referenceYear)

        } ?: error("Não foi possível abrir o arquivo")

    }

}

