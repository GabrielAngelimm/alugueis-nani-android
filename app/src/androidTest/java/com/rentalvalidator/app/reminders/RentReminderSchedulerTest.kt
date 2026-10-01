package com.rentalvalidator.app.reminders

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.rentalvalidator.app.data.local.AppDatabase
import com.rentalvalidator.app.data.local.entity.TenantEntity
import com.rentalvalidator.app.domain.model.Tenant
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.UUID

/** Uses isolated preferences and an in-memory DB; never touches user schedules or tenants. */
class RentReminderSchedulerTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val testName = "reminder-test-${UUID.randomUUID()}"
    private val context = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(testName, mode)
    }
    private lateinit var db: AppDatabase
    private lateinit var scheduler: RentReminderScheduler
    private val id = "reminder-fixture-${UUID.randomUUID()}"
    private lateinit var tenant: Tenant

    @Before fun setup() = runBlocking {
        InstrumentationRegistry.getInstrumentation().uiAutomation.adoptShellPermissionIdentity("android.permission.POST_NOTIFICATIONS")
        db = Room.inMemoryDatabaseBuilder(base,AppDatabase::class.java).build()
        tenant = Tenant(id,"Teste de lembrete",1250.0,10)
        db.tenantDao().insertTenant(TenantEntity.fromDomain(tenant))
        scheduler = RentReminderScheduler(context,db.tenantDao())
    }
    @After fun close() = runBlocking {
        scheduler.remove(id)
        context.getSharedPreferences("rent_reminders",Context.MODE_PRIVATE).edit().clear().commit()
        db.close()
        InstrumentationRegistry.getInstrumentation().uiAutomation.dropShellPermissionIdentity()
    }
    @Test fun savedScheduleSurvivesRecreationAndUpdatesWithDueDay() = runBlocking {
        val original = scheduler.save(id,48)
        val recreated = RentReminderScheduler(context,db.tenantDao())
        assertEquals(original,recreated.get(id))
        db.tenantDao().updateTenant(TenantEntity.fromDomain(tenant.copy(dueDay=20)))
        recreated.reconcile()
        assertEquals(20,recreated.get(id)!!.dueDay)
        assertEquals(48,recreated.get(id)!!.leadHours)
        assertTrue(recreated.get(id)!!.triggerAt > System.currentTimeMillis())
        scheduler.remove(id)
        assertNull(recreated.get(id))
    }
    @Test fun deletedTenantRemovesSchedule() = runBlocking {
        scheduler.save(id,24)
        db.tenantDao().deleteTenant(TenantEntity.fromDomain(tenant))
        scheduler.reconcile()
        assertNull(scheduler.get(id))
    }
    @Test fun alarmPublishesOnceAndSchedulesFollowingMonth() = runBlocking {
        val now=Instant.now()
        val due=LocalDate.now().plusDays(1)
        tenant=tenant.copy(dueDay=due.dayOfMonth)
        db.tenantDao().updateTenant(TenantEntity.fromDomain(tenant))
        val at=now.minusSeconds(5).toEpochMilli()
        val raw=JSONObject().put("hours",24).put("day",tenant.dueDay).put("at",at)
            .put("due",due.toString()).put("zone",ZoneId.systemDefault().id)
        context.getSharedPreferences("rent_reminders",Context.MODE_PRIVATE).edit().putString(id,raw.toString()).commit()
        scheduler.deliver(id,at)
        val manager=base.getSystemService(NotificationManager::class.java)
        // NotificationManager queues work in the system process; await the observable result.
        val publishDeadline = android.os.SystemClock.uptimeMillis() + 3_000
        while (manager.activeNotifications.none { it.tag == id } && android.os.SystemClock.uptimeMillis() < publishDeadline) {
            kotlinx.coroutines.delay(25)
        }
        assertTrue(manager.activeNotifications.any { it.tag==id })
        val next=scheduler.get(id)!!
        assertTrue(next.triggerAt>at)
        manager.cancel(id,1)
        val cancelDeadline = android.os.SystemClock.uptimeMillis() + 3_000
        while (manager.activeNotifications.any { it.tag == id } && android.os.SystemClock.uptimeMillis() < cancelDeadline) {
            kotlinx.coroutines.delay(25)
        }
        scheduler.deliver(id,at)
        assertFalse(manager.activeNotifications.any { it.tag==id })
        assertEquals(next,scheduler.get(id))
    }
}
