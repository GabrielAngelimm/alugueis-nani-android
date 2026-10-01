package com.rentalvalidator.app.data.backup

import android.content.Context
import android.net.Uri
import com.rentalvalidator.app.data.local.dao.BackupDao
import com.rentalvalidator.app.data.local.entity.PaymentEntity
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.data.local.entity.UnitEntity
import com.rentalvalidator.app.domain.model.CapacityKind
import com.rentalvalidator.app.domain.model.OperationalStatus
import com.rentalvalidator.app.domain.model.RentalUnit
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.domain.model.UnitType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Compatibility with JSON v1-v4. Local attachment paths are not portable. */
@Singleton
class LegacyBackupService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupDao: BackupDao
) {
    private val mutex = Mutex()
    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val snapshot = backupDao.snapshot()
            val tenants = snapshot.tenants.map { it.toDomain() }.sortedBy { it.name }
            val units = snapshot.units.map { it.toDomain() }.sortedBy { it.name }
            val paymentsByTenant = snapshot.payments.groupBy { it.tenantId }
            val root = org.json.JSONObject()
            root.put("version", 4)
            root.put("date", java.time.Instant.now().toString())
            root.put("count", tenants.size)

            // --- Units array ---
            val unitsArray = org.json.JSONArray()
            for (u in units) {
                val uObj = org.json.JSONObject()
                uObj.put("id", u.id)
                uObj.put("name", u.name)
                uObj.put("type", u.type.stableCode)
                uObj.put("iconKey", u.iconKey ?: org.json.JSONObject.NULL)
                uObj.put("photoPath", u.photoPath ?: org.json.JSONObject.NULL)
                uObj.put("location", u.location)
                uObj.put("capacity", u.capacity)
                uObj.put("capacityKind", u.capacityKind.stableCode)
                uObj.put("operationalStatus", u.operationalStatus.stableCode)
                uObj.put("condominiumFee", u.condominiumFee ?: org.json.JSONObject.NULL)
                uObj.put("notes", u.notes)
                uObj.put("createdAt", u.createdAt)
                uObj.put("updatedAt", u.updatedAt)
                unitsArray.put(uObj)
            }
            root.put("units", unitsArray)

            // --- Tenants array ---
            val tenantsArray = org.json.JSONArray()
            for (t in tenants) {
                val tObj = org.json.JSONObject()
                tObj.put("id", t.id)
                tObj.put("name", t.name)
                tObj.put("amount", t.amount)
                tObj.put("dueDay", t.dueDay)
                tObj.put("bank", t.bank)
                tObj.put("phone", t.phone)
                tObj.put("cpf", t.cpf)
                tObj.put("whatsappName", t.whatsappName)
                tObj.put("unit", t.unit)
                tObj.put("unitId", t.unitId ?: org.json.JSONObject.NULL)

                val aliasesArray = org.json.JSONArray()
                t.aliases.forEach { aliasesArray.put(it) }
                tObj.put("aliases", aliasesArray)

                tObj.put("contractPath", t.contractPath)
                tObj.put("contractExpirationDate", t.contractExpirationDate)
                tObj.put("inspectionPath", t.inspectionPath)
                tObj.put("dateCreated", t.dateCreated)

                // Retrieve payments for tenant
                val payments = paymentsByTenant[t.id].orEmpty()
                val paymentsObj = org.json.JSONObject()
                // Group payments by year
                val paymentsByYear = payments.groupBy { it.year }
                for ((year, yearPayments) in paymentsByYear) {
                    val yearObj = org.json.JSONObject()
                    for (p in yearPayments) {
                        val monthStr = String.format(java.util.Locale.ROOT, "%02d", p.month)
                        yearObj.put(monthStr, p.status)
                    }
                    paymentsObj.put(year.toString(), yearObj)
                }
                tObj.put("payments", paymentsObj)

                tenantsArray.put(tObj)
            }
            root.put("tenants", tenantsArray)

            val bytes = root.toString(2).toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_BYTES) { "Os dados são grandes demais para o backup antigo." }
            (context.contentResolver.openOutputStream(uri, "wt")
                ?: throw IllegalStateException("Não foi possível criar o arquivo de backup")).use { outputStream ->
                outputStream.write(bytes)
            }
        }
    }

    suspend fun restore(uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                readLimited(inputStream)
            } ?: throw Exception("Não foi possível ler o arquivo")

            val root = org.json.JSONObject(content)
            if (!root.has("tenants")) {
                throw Exception("Arquivo de backup inválido")
            }

            val version = root.optInt("version", 1)
            require(version in 1..4) { "Versão de backup antigo não reconhecida" }

            val importedUnits = mutableListOf<RentalUnit>()
            val importedTenants = mutableListOf<Tenant>()
            val importedPayments = mutableListOf<com.rentalvalidator.app.domain.model.Payment>()

            val tenantsArray = root.getJSONArray("tenants")

            if (version >= 4 && root.has("units")) {
                // --- V4+: read units directly from backup ---
                val unitsArray = root.getJSONArray("units")
                for (i in 0 until unitsArray.length()) {
                    val uObj = unitsArray.getJSONObject(i)
                    val unit = RentalUnit(
                        id = uObj.getString("id"),
                        name = uObj.getString("name"),
                        type = UnitType.fromCode(uObj.optString("type", UnitType.OTHER.stableCode)),
                        iconKey = if (uObj.isNull("iconKey")) null else uObj.getString("iconKey"),
                        photoPath = if (uObj.isNull("photoPath")) null else uObj.getString("photoPath"),
                        location = uObj.optString("location", ""),
                        capacity = uObj.optInt("capacity", 1),
                        capacityKind = CapacityKind.fromCode(uObj.optString("capacityKind", CapacityKind.TENANTS.stableCode)),
                        operationalStatus = OperationalStatus.fromCode(uObj.optString("operationalStatus", OperationalStatus.ACTIVE.stableCode)),
                        condominiumFee = if (uObj.isNull("condominiumFee")) null else uObj.optDouble("condominiumFee"),
                        notes = uObj.optString("notes", ""),
                        createdAt = uObj.optString("createdAt", ""),
                        updatedAt = uObj.optString("updatedAt", "")
                    )
                    importedUnits.add(unit)
                }

                // Read tenants with unitId from backup
                for (i in 0 until tenantsArray.length()) {
                    val tObj = tenantsArray.getJSONObject(i)
                    val id = tObj.optString("id", UUID.randomUUID().toString())
                    val tenant = parseTenant(tObj, id, unitId = if (tObj.isNull("unitId")) null else tObj.getString("unitId"))
                    importedTenants.add(tenant)
                    parsePayments(tObj, id, importedPayments)
                }
            } else {
                // --- V1/V2/V3: synthesize units from tenant "unit" text ---
                // First pass: collect unique unit names
                val unitNameSet = mutableSetOf<String>()
                for (i in 0 until tenantsArray.length()) {
                    val tObj = tenantsArray.getJSONObject(i)
                    val unitName = tObj.optString("unit", RentalUnit.GERAL_NAME).trim().ifBlank { RentalUnit.GERAL_NAME }
                    unitNameSet.add(unitName)
                }

                // Create deterministic RentalUnit for each unique name
                val unitNameToId = mutableMapOf<String, String>()
                val now = java.time.Instant.now().toString()
                for (name in unitNameSet) {
                    val deterministicId = UUID.nameUUIDFromBytes(name.toByteArray(Charsets.UTF_8)).toString()
                    unitNameToId[name] = deterministicId
                    importedUnits.add(
                        RentalUnit(
                            id = deterministicId,
                            name = name,
                            type = UnitType.OTHER,
                            capacity = 1,
                            capacityKind = CapacityKind.TENANTS,
                            operationalStatus = OperationalStatus.ACTIVE,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }

                // Second pass: read tenants and assign unitId
                for (i in 0 until tenantsArray.length()) {
                    val tObj = tenantsArray.getJSONObject(i)
                    val id = tObj.optString("id", UUID.randomUUID().toString())
                    val unitName = tObj.optString("unit", RentalUnit.GERAL_NAME).trim().ifBlank { RentalUnit.GERAL_NAME }
                    val unitId = unitNameToId[unitName]
                    val tenant = parseTenant(tObj, id, unitId = unitId)
                    importedTenants.add(tenant)
                    parsePayments(tObj, id, importedPayments)
                }
            }

            validate(importedUnits, importedTenants, importedPayments)
            backupDao.replaceAllData(
                units = importedUnits.map(UnitEntity::fromDomain),
                tenants = importedTenants.map(TenantEntity::fromDomain),
                payments = importedPayments.map(PaymentEntity::fromDomain)
            )

        }
    }

    /**
     * Parses a [Tenant] from a backup JSON object.
     */
    private fun parseTenant(tObj: org.json.JSONObject, id: String, unitId: String?): Tenant {
        val name = tObj.getString("name")
        val amount = tObj.getDouble("amount")
        val dueDay = tObj.getInt("dueDay")
        val bank = tObj.optString("bank", "")
        val phone = tObj.optString("phone", "")
        val cpf = tObj.optString("cpf", "")
        val whatsappName = tObj.optString("whatsappName", "")
        val unit = tObj.optString("unit", "Geral")

        val aliasesList = mutableListOf<String>()
        val aliasesArray = tObj.optJSONArray("aliases")
        if (aliasesArray != null) {
            for (j in 0 until aliasesArray.length()) {
            aliasesList.add(aliasesArray.getString(j))
            }
        }

        val contractPath = tObj.optString("contractPath", "")
        val contractExpirationDate = tObj.optString("contractExpirationDate", "")
        val inspectionPath = tObj.optString("inspectionPath", "")
        val dateCreated = tObj.optString("dateCreated", "")

        return Tenant(
            id = id,
            name = name,
            amount = amount,
            dueDay = dueDay,
            bank = bank,
            phone = phone,
            cpf = cpf,
            whatsappName = whatsappName,
            unit = unit,
            aliases = aliasesList,
            contractPath = contractPath,
            contractExpirationDate = contractExpirationDate,
            inspectionPath = inspectionPath,
            dateCreated = dateCreated,
            unitId = unitId
        )
    }

    /**
     * Parses payments from a tenant's backup JSON object and appends them to the list.
     */
    private fun parsePayments(
        tObj: org.json.JSONObject,
        tenantId: String,
        target: MutableList<com.rentalvalidator.app.domain.model.Payment>
    ) {
        val paymentsObj = tObj.optJSONObject("payments") ?: return
        val years = paymentsObj.keys()
        while (years.hasNext()) {
            val yearStr = years.next()
            val year = yearStr.toIntOrNull()
            ?: throw IllegalArgumentException("Ano inválido no backup antigo")
            require(year in 1..9999) { "Ano inválido no backup antigo" }
            val yearObj = paymentsObj.getJSONObject(yearStr)
            val months = yearObj.keys()
            while (months.hasNext()) {
            val monthStr = months.next()
            val month = monthStr.toIntOrNull()
                ?: throw IllegalArgumentException("Mês inválido no backup antigo")
            require(month in 1..12) { "Mês inválido no backup antigo" }
            val statusStr = yearObj.getString(monthStr)
            val status = try {
                com.rentalvalidator.app.domain.model.PaymentStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Status de pagamento inválido no backup antigo: $statusStr", e)
            }
            target.add(
                com.rentalvalidator.app.domain.model.Payment(
                    tenantId = tenantId,
                    year = year,
                    month = month,
                    status = status
                )
            )
            }
        }
    }

    private fun validate(units: List<RentalUnit>, tenants: List<Tenant>, payments: List<com.rentalvalidator.app.domain.model.Payment>) {
        val unitIds = units.map { it.id }.toSet()
        val tenantIds = tenants.map { it.id }.toSet()
        require(unitIds.size == units.size && tenantIds.size == tenants.size &&
            units.map { it.name }.toSet().size == units.size &&
            units.all { it.id.isNotBlank() && it.name.isNotBlank() && it.capacity >= 1 &&
                (it.condominiumFee == null || it.condominiumFee.isFinite() && it.condominiumFee >= 0) } &&
            tenants.all { it.id.isNotBlank() && it.name.isNotBlank() && it.amount.isFinite() && it.amount >= 0 &&
                it.dueDay in 1..31 && (it.unitId == null || it.unitId in unitIds) } &&
            payments.map { Triple(it.tenantId, it.year, it.month) }.toSet().size == payments.size) {
            "Os dados do backup antigo são inválidos ou inconsistentes."
        }
    }

    private fun readLimited(input: InputStream): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_BYTES) { "O backup antigo é grande demais." }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object { const val MAX_BYTES = 16 * 1024 * 1024 }
}
