package com.rentalvalidator.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.rentalvalidator.app.reminders.RentReminderScheduler
import com.rentalvalidator.app.data.local.dao.TenantDao
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltAndroidApp
class RentalValidatorApplication : Application() {
    @Inject lateinit var reminders: RentReminderScheduler
    @Inject lateinit var tenants: TenantDao
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            tenants.getTenantsFlow().collect { values ->
                runCatching { reminders.reconcile(values.map { it.toDomain() }) }
                    .onFailure { if (it is CancellationException) throw it; android.util.Log.e("RentReminder", "Falha ao sincronizar lembretes", it) }
            }
        }
    }
}
