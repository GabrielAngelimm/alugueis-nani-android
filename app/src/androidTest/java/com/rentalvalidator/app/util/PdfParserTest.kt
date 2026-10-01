package com.rentalvalidator.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfParserTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun aesEncryptedPdfWithEmptyUserPasswordStillParsesAfterCryptoUpdate() {
        PDFBoxResourceLoader.init(context)
        val bytes = ByteArrayOutputStream()
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { text ->
                text.beginText()
                text.setFont(PDType1Font.HELVETICA, 12f)
                text.newLineAtOffset(20f, 700f)
                text.showText("05/09/2026 Pix recebido Morador Exemplo 1.000,00")
                text.endText()
            }
            val protection = StandardProtectionPolicy("owner-fixture", "", AccessPermission())
            protection.encryptionKeyLength = 256
            protection.setPreferAES(true)
            document.protect(protection)
            document.save(bytes)
        }
        val parsed = PdfParser.parse(context, ByteArrayInputStream(bytes.toByteArray()), 2026).single()
        assertEquals(1000.0, parsed.amount, 0.001)
        assertEquals("Pix recebido Morador Exemplo", parsed.description)
    }

    @Test fun corruptPdfReportsAReadableError() {
        val failure = runCatching { PdfParser.parse(context, ByteArrayInputStream("broken pdf".toByteArray()), 2026) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure?.message.orEmpty().contains("Não foi possível ler o PDF"))
    }
}
