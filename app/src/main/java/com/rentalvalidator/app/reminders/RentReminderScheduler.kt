package com.rentalvalidator.app.reminders

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rentalvalidator.app.MainActivity
import com.rentalvalidator.app.R
import com.rentalvalidator.app.data.local.dao.TenantDao
import com.rentalvalidator.app.domain.model.Tenant
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class RentReminder(val leadHours: Int, val dueDay: Int, val triggerAt: Long, val dueDate: String, val zone: String)

/** Device-local schedules are deliberately separate from the legacy JSON backup and Room schema. */
@Singleton
class RentReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tenantDao: TenantDao
) {
    private val storage = RentReminderStore(context.getSharedPreferences("rent_reminders", Context.MODE_PRIVATE))
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val mutex = Mutex()

    init {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Lembretes de aluguel", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisos agendados antes do vencimento de cada aluguel"
        })
    }

    fun notificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        notifications.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    suspend fun get(id: String): RentReminder? = withContext(Dispatchers.IO) { mutex.withLock { storage.read(id) } }

    /** Portable backup representation: schedules are recalculated in the receiving time zone. */
    suspend fun backupLeadHours(): Map<String, Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            storage.ids().mapNotNull { id -> storage.read(id)?.let { id to it.leadHours } }.toMap()
        }
    }

    /** Replaces readable configurations without depending on the device's notification permission. */
    suspend fun restoreLeadHours(values: Map<String, Int>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tenants = tenantDao.getAllTenants().associate { it.id to it.toDomain() }
            require(values.keys.all { it in tenants }) { "O backup contém lembretes sem inquilino." }
            require(values.values.all { it in 1..672 }) { "Antecedência de lembrete inválida." }
            val previous = storage.ids().mapNotNull { id -> storage.read(id)?.let { id to it } }.toMap()
            val next = values.mapValues { (id, hours) -> create(tenants.getValue(id), hours) }
            try {
                previous.keys.forEach(::cancel)
                next.forEach { (id, value) -> storage.write(id, value); schedule(id, value) }
            } catch (failure: Exception) {
                runCatching {
                    storage.ids().filter { storage.read(it) != null }.forEach(::cancel)
                    previous.forEach { (id, value) -> storage.write(id, value); schedule(id, value) }
                }.onFailure(failure::addSuppressed)
                throw failure
            }
        }
    }

    suspend fun save(id: String, hours: Int): RentReminder = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(notificationsEnabled()) { "Permita as notificações para ativar o lembrete." }
            val tenant = tenantDao.getTenantById(id)?.toDomain() ?: error("Este inquilino não está mais cadastrado.")
            create(tenant, hours).also { storage.write(id, it); schedule(id, it) }
        }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) { mutex.withLock { cancel(id) } }

    /** Called on database changes, process start, reboot, clock change and app update. */
    suspend fun reconcile(tenants: List<Tenant>? = null) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = (tenants ?: tenantDao.getAllTenants().map { it.toDomain() }).associateBy { it.id }
            storage.ids().forEach { id ->
                val config = storage.read(id) ?: return@forEach
                val tenant = current[id]
                if (tenant == null) cancel(id) else {
                    val now = Instant.now()
                    val expired = LocalDate.parse(config.dueDate).isBefore(LocalDate.now())
                    val updated = if (config.dueDay != tenant.dueDay || config.zone != ZoneId.systemDefault().id || expired)
                        create(tenant, config.leadHours, now) else config
                    if (updated != config) storage.write(id, updated)
                    schedule(id, updated)
                }
            }
        }
    }

    suspend fun deliver(id: String, expectedTrigger: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val config = storage.read(id) ?: return@withLock
            if (config.triggerAt != expectedTrigger) return@withLock // stale or duplicated alarm
            val tenant = tenantDao.getTenantById(id)?.toDomain()
            if (tenant == null) { cancel(id); return@withLock }
            val now = Instant.now()
            if (now.toEpochMilli() < config.triggerAt) { schedule(id, config); return@withLock }
            val valid = config.dueDay == tenant.dueDay && config.zone == ZoneId.systemDefault().id &&
                !LocalDate.parse(config.dueDate).isBefore(LocalDate.now())
            // Commit the next occurrence before publishing: duplicated broadcasts cannot notify twice.
            val next = create(tenant, config.leadHours, now)
            storage.write(id, next)
            schedule(id, next)
            if (valid && notificationsEnabled()) {
                val intent = Intent(context, MainActivity::class.java)
                    .setAction("com.rentalvalidator.app.OPEN_TENANT_PAYMENTS")
                    .setData("nani://payments/${Uri.encode(id)}".toUri())
                    .putExtra(EXTRA_TENANT, id)
                    .putExtra(EXTRA_PERIOD, config.dueDate.take(7))
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val click = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val date = LocalDate.parse(config.dueDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val notification = NotificationCompat.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_nani_notification)
                    .setContentTitle("Lembrete de aluguel")
                    .setContentText("${tenant.name} · vencimento em $date")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("${tenant.name}\n${tenant.unit}\nVencimento em $date. Toque para consultar os pagamentos."))
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setContentIntent(click).setAutoCancel(true).build()
                // Permission may be revoked between checking and publishing.
                try { notifications.notify(id, 1, notification) } catch (_: SecurityException) { }
            }
        }
    }

    private fun create(tenant: Tenant, hours: Int, now: Instant = Instant.now()): RentReminder {
        val zone = ZoneId.systemDefault()
        val occurrence = ReminderTime.next(tenant.dueDay, hours, now, zone)
        return RentReminder(hours, tenant.dueDay, occurrence.trigger.toEpochMilli(), occurrence.dueDate.toString(), zone.id)
    }

    private fun pending(id: String, at: Long) = PendingIntent.getBroadcast(context, 0,
        Intent(context, RentReminderReceiver::class.java).setAction(ACTION_REMIND)
            .setData("nani://reminder/${Uri.encode(id)}".toUri())
            .putExtra(EXTRA_TENANT, id).putExtra("trigger", at),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun schedule(id: String, value: RentReminder) {
        // Inexact by design: no special exact-alarm permission, works during Doze.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            maxOf(value.triggerAt, System.currentTimeMillis() + 1000), pending(id, value.triggerAt))
    }

    private fun cancel(id: String) {
        alarms.cancel(pending(id, 0))
        notifications.cancel(id, 1)
        storage.remove(id)
    }

    companion object {
        const val CHANNEL = "rent_due_reminders"
        const val ACTION_REMIND = "com.rentalvalidator.app.RENT_REMINDER"
        const val EXTRA_TENANT = "reminder_tenant_id"
        const val EXTRA_PERIOD = "reminder_period"
    }
}

