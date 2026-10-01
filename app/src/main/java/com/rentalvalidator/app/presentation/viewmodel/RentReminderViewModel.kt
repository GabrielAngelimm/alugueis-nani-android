package com.rentalvalidator.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.reminders.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderUiState(val loading: Boolean = true, val busy: Boolean = false, val reminder: RentReminder? = null, val error: String? = null)

@HiltViewModel
class RentReminderViewModel @Inject constructor(private val scheduler: RentReminderScheduler) : ViewModel() {
    private val mutable = MutableStateFlow(ReminderUiState())
    val state = mutable.asStateFlow()
    fun load(id: String) { viewModelScope.launch {
        mutable.value = ReminderUiState()
        runCatching { scheduler.get(id) }
            .onSuccess { mutable.value = ReminderUiState(loading = false, reminder = it) }
            .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; mutable.value = ReminderUiState(loading = false, error = "Não foi possível ler o lembrete.") }
    } }
    fun allowed() = scheduler.notificationsEnabled()
    fun save(id: String, hours: Int, done: () -> Unit) = change(done) { scheduler.save(id, hours) }
    fun remove(id: String, done: () -> Unit) = change(done) { scheduler.remove(id); null }
    private fun change(done: () -> Unit, operation: suspend () -> RentReminder?) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = null)
        viewModelScope.launch {
            runCatching { operation() }
                .onSuccess { mutable.value = ReminderUiState(loading = false, reminder = it); done() }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; mutable.value = mutable.value.copy(busy = false, error = it.message ?: "Não foi possível salvar. Tente novamente.") }
        }
    }
}
