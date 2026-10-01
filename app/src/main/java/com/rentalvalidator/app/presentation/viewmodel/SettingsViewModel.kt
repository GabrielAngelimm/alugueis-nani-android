package com.rentalvalidator.app.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentalvalidator.app.data.backup.CompleteBackupService
import com.rentalvalidator.app.data.backup.LegacyBackupService
import com.rentalvalidator.app.data.backup.BackupSummary
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val legacyBackupService: LegacyBackupService,
    private val completeBackupService: CompleteBackupService
) : ViewModel() {
    private val errorChannel = Channel<String>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()

    val penaltyFee: StateFlow<Double> = preferencesManager.penaltyConfigFlow
        .map { it.penaltyPct }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20.0)

    val themeModeFlow: StateFlow<AppTheme> = preferencesManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.LIGHT)

    fun updatePenaltyFee(fee: Double) {
        updateSettings { preferencesManager.updatePenaltyConfig(fee, 0.0) }
    }

    fun resetPenalty() {
        updateSettings { preferencesManager.updatePenaltyConfig(20.0, 0.0) }
    }

    fun setTheme(theme: AppTheme) {
        updateSettings { preferencesManager.setThemeMode(theme) }
    }

    fun exportCompleteBackup(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) =
        perform(onSuccess, onError) { completeBackupService.export(uri) }

    fun inspectCompleteBackup(uri: Uri, onSuccess: (BackupSummary) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try { onSuccess(completeBackupService.inspect(uri)) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { onError(failure) }
        }
    }

    fun importCompleteBackup(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) =
        perform(onSuccess, onError) { completeBackupService.restore(uri) }

    fun exportBackup(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) =
        perform(onSuccess, onError) { legacyBackupService.export(uri) }

    fun importBackup(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (Exception) -> Unit) =
        perform(onSuccess, onError) { legacyBackupService.restore(uri) }

    private fun perform(onSuccess: () -> Unit, onError: (Exception) -> Unit, operation: suspend () -> Any?) {
        viewModelScope.launch {
            try { operation(); onSuccess() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { onError(failure) }
        }
    }

    private fun updateSettings(operation: suspend () -> Unit) = viewModelScope.launch {
        try { operation() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { errorChannel.send(failure.message ?: "Não foi possível salvar as configurações.") }
    }
}
