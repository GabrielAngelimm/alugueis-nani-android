package com.rentalvalidator.app.data.files

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Owns attachment IO. Opaque names keep imported tenant IDs out of filesystem paths. */
@Singleton
class DocumentStorage @Inject constructor(@param:ApplicationContext private val context: Context) {
    suspend fun copy(uri: Uri, field: String): String = withContext(Dispatchers.IO) {
        val folder = folderFor(field)
        check(folder.isDirectory || folder.mkdirs()) { "Não foi possível preparar a pasta de documentos." }
        val destination = File(folder, "${UUID.randomUUID()}.pdf")
        val temporary = File(folder, ".${destination.name}.pending")
        try {
            val source = context.contentResolver.openInputStream(uri)
                ?: error("Não foi possível abrir o documento selecionado.")
            source.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "O documento excede o limite de 256 MB." }
                        output.write(buffer, 0, count)
                    }
                    require(total > 0) { "O documento selecionado está vazio." }
                    output.fd.sync()
                }
            }
            check(temporary.renameTo(destination)) { "Não foi possível concluir a cópia do documento." }
            destination.absolutePath
        } finally { temporary.delete() }
    }

    suspend fun remove(path: String, field: String) = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext
        val file = File(path).canonicalFile
        require(file.toPath().startsWith(folderFor(field).canonicalFile.toPath())) {
            "O documento não pertence à pasta de anexos do aplicativo."
        }
        check(!file.exists() || file.isFile && file.delete()) { "Não foi possível remover o documento." }
    }

    private fun folderFor(field: String): File {
        val directory = when (field) {
            "contractPath" -> "contracts"
            "inspectionPath" -> "inspections"
            else -> error("Tipo de documento inválido.")
        }
        val root = context.getExternalFilesDir(null) ?: error("O armazenamento de documentos não está disponível.")
        return File(root, directory)
    }

    private companion object { const val MAX_BYTES = 256L * 1024 * 1024 }
}
