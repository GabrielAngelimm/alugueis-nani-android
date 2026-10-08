package com.rentalvalidator.app.presentation.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.data.backup.BackupSummary
import com.rentalvalidator.app.data.backup.CompleteBackupService
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.rememberAppSnackbar
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.AppSpace
import com.rentalvalidator.app.presentation.theme.NaniTheme
import com.rentalvalidator.app.presentation.theme.NaniType
import com.rentalvalidator.app.presentation.viewmodel.SettingsViewModel
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class RestoreSelection(val uri: Uri, val legacy: Boolean, val summary: BackupSummary? = null)
private enum class BackupOperation { EXPORT, INSPECT, RESTORE }

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val message = rememberAppSnackbar()
    LaunchedEffect(viewModel) { viewModel.errors.collect { message(it) } }
    val penalty by viewModel.penaltyFee.collectAsStateWithLifecycle()
    val theme by viewModel.themeModeFlow.collectAsStateWithLifecycle()
    var penaltyInput by remember(penalty) { mutableStateOf(penalty.toString()) }
    var operation by remember { mutableStateOf<BackupOperation?>(null) }
    var restoreSelection by remember { mutableStateOf<RestoreSelection?>(null) }
    val busy = operation != null
    BackHandler(enabled = busy) { }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CompleteBackupService.MIME_TYPE)) { uri ->
        if (uri != null) {
            operation = BackupOperation.EXPORT
            viewModel.exportCompleteBackup(context, uri,
                onSuccess = { operation = null; message("Backup completo salvo") },
                onError = { error -> operation = null; message(error.message ?: "Não foi possível criar o backup") })
        }
    }
    // Providers report different MIME types for .nani; the importer validates its content.
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            operation = BackupOperation.INSPECT
            viewModel.inspectCompleteBackup(uri,
                onSuccess = { summary -> operation = null; restoreSelection = RestoreSelection(uri, legacy = false, summary) },
                onError = { error -> operation = null; message(error.message ?: "Não foi possível verificar o backup") })
        }
    }
    val importLegacy = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreSelection = RestoreSelection(uri, legacy = true)
    }

    restoreSelection?.let { selected ->
        NaniConfirmDialog(
            title = if (selected.legacy) "Restaurar backup antigo?" else "Restaurar backup completo?",
            onDismiss = { restoreSelection = null },
            confirmLabel = "Restaurar",
            onConfirm = {
                restoreSelection = null
                operation = BackupOperation.RESTORE
                val onSuccess = { operation = null; message("Backup restaurado") }
                val onError: (Exception) -> Unit = { error ->
                    operation = null
                    message(error.message ?: "Não foi possível restaurar o backup")
                }
                if (selected.legacy) viewModel.importBackup(context, selected.uri, onSuccess, onError)
                else viewModel.importCompleteBackup(context, selected.uri, onSuccess, onError)
            },
            text = {
                if (selected.legacy) {
                    DialogText("Os dados atuais serão substituídos pelos do arquivo JSON. Esse formato não incluía documentos, fotos nem lembretes.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        selected.summary?.let { summary ->
                            val date = remember(summary.createdAt) {
                                DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                                    .withZone(ZoneId.systemDefault()).format(Instant.parse(summary.createdAt))
                            }
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Cópia de $date", style = MaterialTheme.typography.titleSmall)
                                    Text("${summary.units} unidades e ${summary.tenants} inquilinos", style = MaterialTheme.typography.bodySmall)
                                    Text("${summary.payments} pagamentos e ${summary.documents} anexos", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        DialogText("Os dados atuais deste aparelho serão substituídos pelos da cópia selecionada.")
                    }
                }
            }
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(Modifier.widthIn(max = AppSize.editorMaxWidth).fillMaxWidth()) {
                NaniHeader("Configurações", onBack = { if (!busy) onNavigateBack() })

                NaniSection("Cópia de segurança", topPadding = 12.dp) {
                    BackupCard(operation,
                        onExport = { export.launch("alugueis-nani-${LocalDate.now()}.nani") },
                        onRestore = { import.launch(arrayOf("*/*")) },
                        onRestoreLegacy = { importLegacy.launch(arrayOf("application/json", "text/plain")) })
                }

                NaniSection("Aparência") {
                    LedgerSheet(contentPadding = PaddingValues(18.dp)) {
                        Text("Tema do aplicativo", style = MaterialTheme.typography.titleSmall)
                        Text("Sistema acompanha a configuração do celular.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        val options = listOf(AppTheme.SYSTEM to "Sistema", AppTheme.LIGHT to "Claro", AppTheme.DARK to "Escuro")
                        NaniTabs(options.map { it.second }, options.indexOfFirst { it.first == theme }.coerceAtLeast(0),
                            { viewModel.setTheme(options[it].first) }, role = Role.RadioButton)
                    }
                }

                NaniSection("Multa por atraso") {
                    LedgerSheet(contentPadding = PaddingValues(18.dp)) {
                        Text("Percentual sobre o aluguel, usado na conferência de extratos quando o pagamento chega depois do vencimento.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        NaniTextField(penaltyInput, { value ->
                            penaltyInput = value
                            value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
                                ?.let(viewModel::updatePenaltyFee)
                        }, "Percentual da multa", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = { Text("%", style = MaterialTheme.typography.titleSmall) })
                        val percentage = penaltyInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Aluguel de R$ 1.000,00 com multa", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.format(1000 + 1000 * percentage / 100), style = NaniType.moneyRow)
                        }
                        TextButton(onClick = { viewModel.resetPenalty(); penaltyInput = "20.0" }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Text("Restaurar 20% padrão", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                NaniSection("Sobre") {
                    LedgerSheet(contentPadding = PaddingValues(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            UnitPlaque("Aluguéis Nani", size = 44.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Aluguéis Nani", style = MaterialTheme.typography.titleLarge)
                                Text("Versão 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Funciona sem internet. Cadastros, pagamentos e documentos ficam somente neste aparelho e nos backups que você criar.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupCard(operation: BackupOperation?, onExport: () -> Unit, onRestore: () -> Unit, onRestoreLegacy: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val busy = operation != null
    LedgerSheet {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconTile(Icons.Rounded.FolderZip, tint = colors.onPrimaryContainer, container = colors.primaryContainer)
                Column(Modifier.weight(1f)) {
                    Text("Backup completo", style = MaterialTheme.typography.titleLarge)
                    Text("Arquivo .nani para guardar ou levar a outro celular", style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(18.dp))
            BackupIncluded(Icons.Rounded.MeetingRoom, "Unidades, inquilinos e pagamentos")
            BackupIncluded(Icons.Rounded.Description, "Contratos, vistorias e fotos anexados")
            BackupIncluded(Icons.Rounded.NotificationsNone, "Lembretes e preferências")
            Spacer(Modifier.height(18.dp))
            PrimaryButton("Criar backup completo", onExport, enabled = !busy, icon = Icons.Rounded.Download)
            if (operation != null) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)), color = colors.primary,
                    trackColor = NaniTheme.colors.track)
                Spacer(Modifier.height(6.dp))
                Text(when (operation) {
                    BackupOperation.EXPORT -> "Criando backup…"
                    BackupOperation.INSPECT -> "Verificando backup…"
                    BackupOperation.RESTORE -> "Restaurando backup…"
                }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp).padding(top = 1.dp), tint = colors.onSurfaceVariant)
                Text("O arquivo contém dados pessoais e não é criptografado. Guarde-o em um local seguro.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        LedgerRule()
        SettingsAction(Icons.Rounded.SettingsBackupRestore, "Restaurar backup", "Selecionar um arquivo .nani", !busy, onRestore)
        LedgerRule(Modifier.padding(start = 70.dp))
        SettingsAction(Icons.Rounded.DataObject, "Importar backup antigo", "Arquivo JSON de versões anteriores", !busy, onRestoreLegacy)
    }
}

@Composable
private fun BackupIncluded(icon: ImageVector, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = NaniTheme.colors.paid.ink)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsAction(icon: ImageVector, title: String, detail: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else .45f
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick).heightIn(min = 68.dp)
        .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(icon, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
