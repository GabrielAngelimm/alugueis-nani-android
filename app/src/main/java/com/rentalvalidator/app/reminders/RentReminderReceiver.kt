package com.rentalvalidator.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class RentReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: RentReminderScheduler
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == RentReminderScheduler.ACTION_REMIND) {
                    val id = intent.getStringExtra(RentReminderScheduler.EXTRA_TENANT) ?: return@launch
                    scheduler.deliver(id, intent.getLongExtra("trigger", -1))
                } else scheduler.reconcile()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("RentReminder", "Não foi possível atualizar o agendamento", e)
            } finally { pending.finish() }
        }
    }
}
