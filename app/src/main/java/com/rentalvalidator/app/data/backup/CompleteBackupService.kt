package com.rentalvalidator.app.data.backup

import android.content.Context
import android.net.Uri
import com.rentalvalidator.app.data.local.dao.BackupDao
import com.rentalvalidator.app.data.local.dao.BackupSnapshot
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.data.local.datastore.BackupPreferences
import com.rentalvalidator.app.data.local.datastore.PreferencesManager
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.PaymentStatus
import com.rentalvalidator.app.domain.model.UnitType
import com.rentalvalidator.app.reminders.RentReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupOptions(
    val includeDocuments: Boolean = true,
    val includeSettings: Boolean = true,
    val includeReminders: Boolean = true
)

data class BackupSummary(
    val createdAt: String,
    val units: Int,
    val tenants: Int,
    val payments: Int,
    val documents: Int,
    val reminders: Int,
    val includesSettings: Boolean,
    val includesReminders: Boolean
)

/** A portable archive. The legacy JSON backup remains a separate import path. */
@Singleton
class CompleteBackupService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupDao: BackupDao,
    private val preferences: PreferencesManager,
    private val reminders: RentReminderScheduler
) {
    private val mutex = Mutex()

    suspend fun export(uri: Uri, options: BackupOptions = BackupOptions()): BackupSummary = withContext(Dispatchers.IO) {
        mutex.withLock {
            val temporary = File.createTempFile("nani-export-", ".nani", context.cacheDir)
            try {
                val snapshot = backupDao.snapshot()
                val settings = if (options.includeSettings) preferences.backupSnapshot() else null
                val tenantIds = snapshot.tenants.mapTo(mutableSetOf()) { it.id }
                val reminderValues = if (options.includeReminders)
                    reminders.backupLeadHours().filterKeys { it in tenantIds } else null
                val createdAt = Instant.now().toString()
                val sources = mutableListOf<AssetSource>()
                val data = encodeData(snapshot, settings, reminderValues, options, sources)
                val dataBytes = data.toString().toByteArray(Charsets.UTF_8)
                require(dataBytes.size <= MAX_DATA_BYTES) { "Os dados são grandes demais para o backup." }
                val specs = mutableListOf(EntrySpec(DATA_ENTRY, dataBytes.size.toLong(), sha256(dataBytes)))
                sources.forEach { source ->
                    val length = source.file.length()
                    require(length in 1..MAX_ASSET_BYTES) { "Anexo vazio ou grande demais: ${source.file.name}" }
                    specs += EntrySpec(source.name, length, source.file.inputStream().use(::hashLimited))
                }
                require(specs.size + 1 <= MAX_ENTRIES) { "O backup contém anexos demais." }
                require(specs.sumOf { it.length } <= MAX_UNCOMPRESSED_BYTES) { "O backup é grande demais." }
                val manifest = JSONObject()
                    .put("format", FORMAT)
                    .put("version", VERSION)
                    .put("createdAt", createdAt)
                    .put("documents", options.includeDocuments)
                    .put("settings", options.includeSettings)
                    .put("reminders", options.includeReminders)
                    .put("entries", JSONArray().apply {
                        specs.forEach { put(JSONObject().put("name", it.name).put("length", it.length).put("sha256", it.hash)) }
                    })
                val manifestBytes = manifest.toString().toByteArray(Charsets.UTF_8)
                require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "Índice do backup grande demais." }
                ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                    writeEntry(zip, MANIFEST_ENTRY, manifestBytes)
                    writeEntry(zip, DATA_ENTRY, dataBytes)
                    sources.forEach { source ->
                        zip.putNextEntry(ZipEntry(source.name))
                        val actual = source.file.inputStream().use { transferAndHash(it, zip, MAX_ASSET_BYTES) }
                        zip.closeEntry()
                        val expected = specs.first { it.name == source.name }
                        check(actual.length == expected.length && actual.hash == expected.hash) {
                            "Um anexo mudou durante a criação do backup. Tente novamente."
                        }
                    }
                }
                require(temporary.length() <= MAX_ARCHIVE_BYTES) { "O backup é grande demais." }
                val checked = readValidatedArchive(temporary, null)
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    temporary.inputStream().use { it.copyTo(output) }
                    output.flush()
                } ?: error("Não foi possível gravar o arquivo escolhido.")
                val expectedCopy = temporary.inputStream().use { transferAndHash(it, null, MAX_ARCHIVE_BYTES) }
                val actualCopy = context.contentResolver.openInputStream(uri)?.use {
                    transferAndHash(it, null, MAX_ARCHIVE_BYTES)
                } ?: error("Não foi possível conferir o arquivo exportado.")
                check(expectedCopy == actualCopy) { "O arquivo exportado ficou incompleto. Tente salvar novamente." }
                checked.summary
            } finally {
                temporary.delete()
            }
        }
    }

    /** Reads and validates the whole archive; safe to call before showing a replacement confirmation. */
    suspend fun inspect(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        mutex.withLock {
            withArchiveFile(uri) { readValidatedArchive(it, null).summary }
        }
    }

    suspend fun restore(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        mutex.withLock {
            withArchiveFile(uri) { archive ->
                val stage = File(context.cacheDir, "nani-restore-${UUID.randomUUID()}")
                check(stage.mkdir()) { "Não foi possível preparar a restauração." }
                try {
                    val parsed = readValidatedArchive(archive, stage)
                    val oldData = backupDao.snapshot()
                    val oldSettings = preferences.backupSnapshot()
                    val oldReminders = reminders.backupLeadHours()
                    val installed = mutableListOf<File>()
                    var databaseReplaced = false
                    try {
                        val generation = UUID.randomUUID().toString()
                        val paths = parsed.assets.entries.associate { (name, staged) ->
                            val target = targetFile(name, generation)
                            installFile(staged, target)
                            installed += target
                            name to target.absolutePath
                        }
                        val units = parsed.units.map { it.copy(photoPath = it.photoPath?.let(paths::getValue)) }
                        val tenants = parsed.tenants.map { tenant ->
                            tenant.copy(
                                contractPath = tenant.contractPath.takeIf(String::isNotBlank)?.let(paths::getValue).orEmpty(),
                                inspectionPath = tenant.inspectionPath.takeIf(String::isNotBlank)?.let(paths::getValue).orEmpty()
                            )
                        }
                        withContext(NonCancellable) {
                            backupDao.replaceAllData(units, tenants, parsed.payments)
                            databaseReplaced = true
                        }
                        parsed.settings?.let { preferences.restoreBackup(it) }
                        if (parsed.reminders != null) reminders.restoreLeadHours(parsed.reminders)
                        else reminders.reconcile()
                        parsed.summary
                    } catch (failure: Exception) {
                        // Rollback must finish even when the caller's ViewModel has been cancelled.
                        withContext(NonCancellable) {
                            var dataRestored = !databaseReplaced
                            if (databaseReplaced) runCatching {
                                backupDao.replaceAllData(oldData.units, oldData.tenants, oldData.payments)
                            }.onSuccess { dataRestored = true }.onFailure(failure::addSuppressed)
                            runCatching { preferences.restoreBackup(oldSettings) }.onFailure(failure::addSuppressed)
                            if (dataRestored) runCatching { reminders.restoreLeadHours(oldReminders) }
                                .onFailure(failure::addSuppressed)
                            // Keep the new files if rollback failed and the database may refer to them.
                            if (dataRestored) installed.forEach(File::delete)
                        }
                        throw failure
                    }
                } finally {
                    stage.deleteRecursively()
                }
            }
        }
    }

    private suspend fun <T> withArchiveFile(uri: Uri, block: suspend (File) -> T): T {
        val temporary = File.createTempFile("nani-read-", ".nani", context.cacheDir)
        try {
            val source = context.contentResolver.openInputStream(uri) ?: error("Não foi possível ler o backup.")
            source.use { input -> temporary.outputStream().use { output -> transferAndHash(input, output, MAX_ARCHIVE_BYTES) } }
            return block(temporary)
        } finally {
            temporary.delete()
        }
    }

    private fun encodeData(
        snapshot: BackupSnapshot, settings: BackupPreferences?, reminderValues: Map<String, Int>?,
        options: BackupOptions, sources: MutableList<AssetSource>
    ): JSONObject {
        fun asset(path: String?, kind: String): String? {
            if (path.isNullOrBlank() || !options.includeDocuments) return null
            val file = File(path).canonicalFile
            val roots = listOfNotNull(context.filesDir, context.getExternalFilesDir(null)).map(File::getCanonicalFile)
            require(roots.any { file.toPath().startsWith(it.toPath()) } && file.isFile) {
                "Um anexo não está disponível no armazenamento do aplicativo: ${file.name}"
            }
            val name = "assets/$kind/${sources.size}.${if (kind == "units") "img" else "pdf"}"
            sources += AssetSource(name, file)
            return name
        }
        val units = JSONArray().apply { snapshot.units.forEach { u ->
            put(JSONObject().put("id", u.id).put("name", u.name).put("type", u.type)
                .put("iconKey", u.iconKey ?: JSONObject.NULL).put("photoAsset", asset(u.photoPath, "units") ?: JSONObject.NULL)
                .put("location", u.location).put("capacity", u.capacity).put("capacityKind", u.capacityKind)
                .put("operationalStatus", u.operationalStatus).put("condominiumFee", u.condominiumFee ?: JSONObject.NULL)
                .put("notes", u.notes).put("createdAt", u.createdAt).put("updatedAt", u.updatedAt))
        } }
        val tenants = JSONArray().apply { snapshot.tenants.forEach { t ->
            put(JSONObject().put("id", t.id).put("name", t.name).put("amount", t.amount).put("dueDay", t.dueDay)
                .put("bank", t.bank).put("phone", t.phone).put("cpf", t.cpf).put("whatsappName", t.whatsappName)
                .put("unit", t.unit).put("unitId", t.unitId ?: JSONObject.NULL)
                .put("aliases", JSONArray(t.aliases)).put("contractAsset", asset(t.contractPath, "contracts") ?: JSONObject.NULL)
                .put("contractExpirationDate", t.contractExpirationDate)
                .put("inspectionAsset", asset(t.inspectionPath, "inspections") ?: JSONObject.NULL)
                .put("dateCreated", t.dateCreated))
        } }
        val payments = JSONArray().apply { snapshot.payments.forEach { p ->
            put(JSONObject().put("id", p.id).put("tenantId", p.tenantId).put("year", p.year)
                .put("month", p.month).put("status", p.status))
        } }
        return JSONObject().put("units", units).put("tenants", tenants).put("payments", payments).apply {
            if (settings != null) put("settings", JSONObject().put("theme", settings.theme.name)
                .put("penaltyPct", settings.penaltyPct).put("dailyInterestPct", settings.dailyInterestPct))
            if (reminderValues != null) put("reminders", JSONArray().apply {
                reminderValues.toSortedMap().forEach { (id, hours) -> put(JSONObject().put("tenantId", id).put("leadHours", hours)) }
            })
        }
    }

    private fun readValidatedArchive(archive: File, stage: File?): Parsed = try {
        parseArchive(archive, stage)
    } catch (failure: ZipException) {
        throw IllegalArgumentException("Arquivo de backup inválido ou corrompido.", failure)
    } catch (failure: org.json.JSONException) {
        throw IllegalArgumentException("Arquivo de backup inválido ou corrompido.", failure)
    } catch (failure: DateTimeParseException) {
        throw IllegalArgumentException("Data inválida no arquivo de backup.", failure)
    }

    private fun parseArchive(archive: File, stage: File?): Parsed {
        ZipFile(archive).use { zip ->
            val entries = zip.entries().asSequence().toList()
            require(entries.size in 2..MAX_ENTRIES) { "Número de arquivos inválido no backup." }
            require(entries.none { it.isDirectory } && entries.map { it.name }.toSet().size == entries.size) {
                "O backup contém entradas duplicadas ou pastas inesperadas."
            }
            require(entries.all { safeName(it.name) && it.method in listOf(ZipEntry.DEFLATED, ZipEntry.STORED) }) {
                "O backup contém caminhos inválidos."
            }
            val manifestEntry = zip.getEntry(MANIFEST_ENTRY) ?: error("Índice do backup ausente.")
            val manifest = JSONObject(readLimited(zip.getInputStream(manifestEntry), MAX_MANIFEST_BYTES).toString(Charsets.UTF_8))
            require(manifest.getString("format") == FORMAT && manifest.getInt("version") == VERSION) {
                "Formato de backup incompatível."
            }
            val createdAt = manifest.getString("createdAt")
            Instant.parse(createdAt)
            val hasDocuments = manifest.getBoolean("documents")
            val hasSettings = manifest.getBoolean("settings")
            val hasReminders = manifest.getBoolean("reminders")
            val specifications = manifest.getJSONArray("entries")
            val specs = (0 until specifications.length()).map { index ->
                specifications.getJSONObject(index).let { EntrySpec(it.getString("name"), it.getLong("length"), it.getString("sha256")) }
            }
            require(specs.any { it.name == DATA_ENTRY } && specs.size + 1 == entries.size &&
                specs.map { it.name }.toSet().size == specs.size &&
                specs.map { it.name }.toSet() + MANIFEST_ENTRY == entries.map { it.name }.toSet() &&
                specs.all { safeName(it.name) && it.length in 0..entryLimit(it.name) && it.hash.matches(Regex("[0-9a-f]{64}")) } &&
                specs.sumOf { it.length } <= MAX_UNCOMPRESSED_BYTES &&
                (hasDocuments || specs.none { it.name.startsWith("assets/") })) {
                "Índice do backup inválido."
            }
            var dataBytes: ByteArray? = null
            val assets = mutableMapOf<String, File>()
            var total = 0L
            specs.forEach { spec ->
                val entry = zip.getEntry(spec.name) ?: error("Arquivo ausente: ${spec.name}")
                val destination = if (stage != null && spec.name.startsWith("assets/")) {
                    File(stage, spec.name.replace('/', '_')).also { check(it.createNewFile()) }
                } else null
                val actual = zip.getInputStream(entry).use { input ->
                    if (destination != null) FileOutputStream(destination).use { transferAndHash(input, it, spec.length) }
                    else transferAndHash(input, null, spec.length)
                }
                total += actual.length
                require(total <= MAX_UNCOMPRESSED_BYTES && actual.length == spec.length && actual.hash == spec.hash) {
                    "Arquivo corrompido no backup: ${spec.name}"
                }
                if (spec.name == DATA_ENTRY) dataBytes = zip.getInputStream(entry).use { readLimited(it, MAX_DATA_BYTES) }
                if (destination != null) assets[spec.name] = destination
            }
            val data = JSONObject(dataBytes?.toString(Charsets.UTF_8) ?: error("Dados ausentes no backup."))
            val parsed = decodeData(data, specs.map { it.name }.filter { it.startsWith("assets/") }.toSet(),
                hasDocuments, hasSettings, hasReminders)
            return parsed.copy(summary = BackupSummary(createdAt, parsed.units.size, parsed.tenants.size,
                parsed.payments.size, parsed.assetNames.size, parsed.reminders?.size ?: 0, hasSettings, hasReminders),
                assets = assets)
        }
    }

    private fun decodeData(
        data: JSONObject, assetNames: Set<String>, hasDocuments: Boolean,
        hasSettings: Boolean, hasReminders: Boolean
    ): Parsed {
        require(data.has("settings") == hasSettings && data.has("reminders") == hasReminders) {
            "Opções do backup inconsistentes com os dados."
        }
        val referenced = mutableSetOf<String>()
        fun reference(value: String?, kind: String): String? {
            if (value.isNullOrBlank()) return null
            require(hasDocuments && value in assetNames && value.startsWith("assets/$kind/") && referenced.add(value)) {
                "Referência de anexo inválida no backup."
            }
            return value
        }
        val unitsJson = data.getJSONArray("units")
        val units = (0 until unitsJson.length()).map { i -> unitsJson.getJSONObject(i).let { u ->
            UnitEntity(u.getString("id"), u.getString("name"), u.getString("type"), nullable(u, "iconKey"),
                reference(nullable(u, "photoAsset"), "units"), u.getString("location"), u.getInt("capacity"),
                u.getString("capacityKind"), u.getString("operationalStatus"),
                if (u.isNull("condominiumFee")) null else u.getDouble("condominiumFee"),
                u.getString("notes"), u.getString("createdAt"), u.getString("updatedAt"))
        } }
        val tenantsJson = data.getJSONArray("tenants")
        val tenants = (0 until tenantsJson.length()).map { i -> tenantsJson.getJSONObject(i).let { t ->
            val aliases = t.getJSONArray("aliases")
            TenantEntity(t.getString("id"), t.getString("name"), t.getDouble("amount"), t.getInt("dueDay"),
                t.getString("bank"), t.getString("phone"), t.getString("cpf"), t.getString("whatsappName"),
                t.getString("unit"), (0 until aliases.length()).map(aliases::getString),
                reference(nullable(t, "contractAsset"), "contracts").orEmpty(), t.getString("contractExpirationDate"),
                reference(nullable(t, "inspectionAsset"), "inspections").orEmpty(), t.getString("dateCreated"),
                nullable(t, "unitId"))
        } }
        val paymentsJson = data.getJSONArray("payments")
        val payments = (0 until paymentsJson.length()).map { i -> paymentsJson.getJSONObject(i).let { p ->
            PaymentEntity(p.getLong("id"), p.getString("tenantId"), p.getInt("year"), p.getInt("month"), p.getString("status"))
        } }
        require(referenced == assetNames) { "O backup contém anexos sem referência." }
        val settings = if (hasSettings) data.getJSONObject("settings").let {
            BackupPreferences(AppTheme.valueOf(it.getString("theme")), it.getDouble("penaltyPct"), it.getDouble("dailyInterestPct"))
        } else null
        val reminderValues = if (hasReminders) data.getJSONArray("reminders").let { array ->
            val records = (0 until array.length()).map { i -> array.getJSONObject(i).let { it.getString("tenantId") to it.getInt("leadHours") } }
            require(records.map { it.first }.toSet().size == records.size) { "Lembretes duplicados no backup." }
            records.toMap()
        } else null
        validateData(units, tenants, payments, settings, reminderValues)
        return Parsed(units, tenants, payments, settings, reminderValues, referenced,
            BackupSummary("", 0, 0, 0, 0, 0, hasSettings, hasReminders), emptyMap())
    }

    private fun validateData(
        units: List<UnitEntity>, tenants: List<TenantEntity>, payments: List<PaymentEntity>,
        settings: BackupPreferences?, reminderValues: Map<String, Int>?
    ) {
        val unitIds = units.map { it.id }.toSet()
        val tenantIds = tenants.map { it.id }.toSet()
        require(unitIds.size == units.size && tenantIds.size == tenants.size &&
            units.map { it.name }.toSet().size == units.size &&
            units.all { it.id.isNotBlank() && it.name.isNotBlank() && it.capacity >= 1 &&
                UnitType.entries.any { kind -> kind.stableCode == it.type } &&
                CapacityKind.entries.any { kind -> kind.stableCode == it.capacityKind } &&
                OperationalStatus.entries.any { kind -> kind.stableCode == it.operationalStatus } &&
                (it.condominiumFee == null || it.condominiumFee.isFinite() && it.condominiumFee >= 0) } &&
            tenants.all { it.id.isNotBlank() && it.name.isNotBlank() && it.amount.isFinite() && it.amount >= 0 &&
                it.dueDay in 1..31 && (it.unitId == null || it.unitId in unitIds) } &&
            payments.map { it.id }.toSet().size == payments.size &&
            payments.map { Triple(it.tenantId, it.year, it.month) }.toSet().size == payments.size &&
            payments.all { it.id > 0 && it.tenantId in tenantIds && it.year in 1..9999 && it.month in 1..12 &&
                PaymentStatus.entries.any { status -> status.name == it.status } } &&
            (settings == null || settings.penaltyPct.isFinite() && settings.penaltyPct >= 0 &&
                settings.dailyInterestPct.isFinite() && settings.dailyInterestPct >= 0) &&
            (reminderValues == null || reminderValues.all { (id, hours) -> id in tenantIds && hours in 1..672 })) {
            "Os dados do backup são inválidos ou inconsistentes."
        }
    }

    private fun targetFile(name: String, generation: String): File {
        val parts = name.split('/')
        val kind = parts[1]
        val externalRoot = context.getExternalFilesDir(null) ?: error("O armazenamento de anexos não está disponível.")
        val folder = when (kind) {
            "contracts", "inspections", "units" -> File(externalRoot, if (kind == "units") "unit_photos" else kind)
            else -> error("Tipo de anexo inválido.")
        }
        check(folder.isDirectory || folder.mkdirs()) { "Não foi possível preparar a pasta de anexos." }
        return File(folder, "backup_${generation}_${parts[2]}")
    }

    private fun installFile(source: File, target: File) {
        val pending = File(target.parentFile, ".${target.name}.pending")
        try {
            check(!target.exists() && !pending.exists()) { "Destino do anexo já existe." }
            source.inputStream().use { input -> FileOutputStream(pending).use { output ->
                input.copyTo(output)
                output.fd.sync()
            } }
            check(pending.renameTo(target)) { "Não foi possível concluir a cópia de um anexo." }
        } finally {
            pending.delete()
        }
    }

    private fun safeName(name: String): Boolean = name == MANIFEST_ENTRY || name == DATA_ENTRY ||
        name.matches(Regex("assets/(contracts|inspections)/[0-9]+\\.pdf|assets/units/[0-9]+\\.img"))

    private fun entryLimit(name: String): Long = when (name) {
        DATA_ENTRY -> MAX_DATA_BYTES.toLong()
        else -> MAX_ASSET_BYTES
    }

    private fun nullable(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.getString(key)

    private fun readLimited(input: InputStream, limit: Int): ByteArray = input.use {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val size = it.read(buffer)
            if (size < 0) break
            total += size
            require(total <= limit) { "Entrada grande demais no backup." }
            output.write(buffer, 0, size)
        }
        output.toByteArray()
    }

    private fun hashLimited(input: InputStream): String = transferAndHash(input, null, MAX_ASSET_BYTES).hash

    private fun transferAndHash(input: InputStream, output: OutputStream?, limit: Long): Transfer {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(32 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "Arquivo grande demais no backup." }
            digest.update(buffer, 0, count)
            output?.write(buffer, 0, count)
        }
        return Transfer(total, digest.digest().joinToString("") { "%02x".format(it) })
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private data class AssetSource(val name: String, val file: File)
    private data class EntrySpec(val name: String, val length: Long, val hash: String)
    private data class Transfer(val length: Long, val hash: String)
    private data class Parsed(
        val units: List<UnitEntity>, val tenants: List<TenantEntity>, val payments: List<PaymentEntity>,
        val settings: BackupPreferences?, val reminders: Map<String, Int>?, val assetNames: Set<String>,
        val summary: BackupSummary, val assets: Map<String, File>
    )

    companion object {
        const val MIME_TYPE = "application/vnd.alugueis-nani.backup"
        private const val FORMAT = "alugueis-nani-complete"
        private const val VERSION = 1
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val DATA_ENTRY = "data.json"
        private const val MAX_ENTRIES = 1024
        private const val MAX_MANIFEST_BYTES = 1024 * 1024
        private const val MAX_DATA_BYTES = 16 * 1024 * 1024
        private const val MAX_ASSET_BYTES = 256L * 1024 * 1024
        private const val MAX_UNCOMPRESSED_BYTES = 1024L * 1024 * 1024
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024 * 1024
    }
}
