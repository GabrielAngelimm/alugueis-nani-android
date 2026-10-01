package com.rentalvalidator.app.data.files

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class DocumentStorageTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val storage = DocumentStorage(context)

    @Test fun copiesAnAttachmentAndRejectsDeletionOutsideItsFolder() = runBlocking {
        val source = File(context.cacheDir, "document-test-${UUID.randomUUID()}.pdf")
        source.writeText("%PDF-1.4\nfixture")
        var copied: File? = null
        try {
            copied = File(storage.copy(Uri.fromFile(source), "contractPath"))
            assertEquals(source.readText(), copied.readText())
            assertTrue(copied.toPath().startsWith(File(context.getExternalFilesDir(null), "contracts").toPath()))
            assertTrue(runCatching { storage.remove(source.absolutePath, "contractPath") }.isFailure)
            assertTrue(source.exists())
            storage.remove(copied.absolutePath, "contractPath")
            assertFalse(copied.exists())
        } finally { source.delete(); copied?.delete() }
    }

    @Test fun missingOrEmptyDocumentsCannotBeReportedAsCopied() = runBlocking {
        val source = File(context.cacheDir, "missing-document-${UUID.randomUUID()}.pdf")
        assertTrue(runCatching { storage.copy(Uri.fromFile(source), "inspectionPath") }.isFailure)
        source.createNewFile()
        try { assertTrue(runCatching { storage.copy(Uri.fromFile(source), "inspectionPath") }.isFailure) }
        finally { source.delete() }
    }
}
