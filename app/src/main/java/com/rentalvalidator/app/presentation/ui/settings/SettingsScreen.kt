package com.rentalvalidator.app.presentation.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentalvalidator.app.data.local.datastore.AppTheme
import com.rentalvalidator.app.data.backup.BackupSummary
import com.rentalvalidator.app.data.backup.CompleteBackupService
import com.rentalvalidator.app.presentation.components.ModernTextField
import com.rentalvalidator.app.presentation.components.PrimaryButton
import com.rentalvalidator.app.presentation.components.rememberAppSnackbar
import com.rentalvalidator.app.presentation.design.NaniHeader
import com.rentalvalidator.app.presentation.design.NaniSection
import com.rentalvalidator.app.presentation.viewmodel.SettingsViewModel
import com.rentalvalidator.app.util.CurrencyUtils
import java.time.LocalDate
import java.time.Instant
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
        AlertDialog(
            onDismissRequest = { restoreSelection = null },
            title = { Text(if (selected.legacy) "Restaurar backup antigo?" else "Restaurar backup completo?") },
            text = {
                if (selected.legacy) {
                    Text("Os dados atuais serão substituídos pelos do arquivo JSON. Esse formato não incluía documentos, fotos nem lembretes.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        selected.summary?.let { summary ->
                            val date = remember(summary.createdAt) {
                                DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                                    .withZone(ZoneId.systemDefault()).format(Instant.parse(summary.createdAt))
                            }
                            Text("Cópia de $date", style = MaterialTheme.typography.titleSmall)
                            Text("${summary.units} unidades · ${summary.tenants} inquilinos\n${summary.payments} pagamentos · ${summary.documents} anexos",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Os dados atuais deste aparelho serão substituídos pelos da cópia selecionada.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    restoreSelection = null
                    operation = BackupOperation.RESTORE
                    val onSuccess = { operation = null; message("Backup restaurado") }
                    val onError: (Exception) -> Unit = { error ->
                        operation = null
                        message(error.message ?: "Não foi possível restaurar o backup")
                    }
                    if (selected.legacy) viewModel.importBackup(context, selected.uri, onSuccess, onError)
                    else viewModel.importCompleteBackup(context, selected.uri, onSuccess, onError)
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { restoreSelection = null }) { Text("Cancelar") } }
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                NaniHeader("Configurações", onBack = { if (!busy) onNavigateBack() })

                NaniSection("Cópia de segurança", topPadding = 8.dp) {
                    BackupCard(operation,
                        onExport = { export.launch("alugueis-nani-${LocalDate.now()}.nani") },
                        onRestore = { import.launch(arrayOf("*/*")) },
                        onRestoreLegacy = { importLegacy.launch(arrayOf("application/json", "text/plain")) })
                }

                NaniSection("Aparência") {
                    SettingsSurface {
                        Text("Tema do aplicativo", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(14.dp))
                        ThemeSelector(theme, viewModel::setTheme)
                    }
                }

                NaniSection("Multa por atraso") {
                    SettingsSurface {
                        Text("Percentual aplicado ao aluguel em atraso", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))
                        ModernTextField(penaltyInput, { value ->
                            penaltyInput = value
                            value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
                                ?.let(viewModel::updatePenaltyFee)
                        }, "Percentual da multa", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        val percentage = penaltyInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Exemplo com R$ 1.000,00", Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyUtils.format(1000 + 1000 * percentage / 100),
                                style = MaterialTheme.typography.titleSmall)
                        }
                        TextButton(onClick = { viewModel.resetPenalty(); penaltyInput = "20.0" }) {
                            Text("Restaurar 20% padrão")
                        }
                    }
                }

                NaniSection("Sobre") {
                    SettingsSurface {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Aluguéis Nani", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Text("Versão 1.0", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceContainerLow)))
            .padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(28.dp))
                Text("Backup completo", style = MaterialTheme.typography.titleLarge,
                    color = colors.onPrimaryContainer, modifier = Modifier.semantics { heading() })
            }
            Spacer(Modifier.height(10.dp))
            Text("Uma cópia para levar seus dados a outro celular ou guardar em segurança.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text("O que é salvo", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            BackupIncluded(Icons.Rounded.Home, "Imóveis, inquilinos e recebimentos")
            BackupIncluded(Icons.Rounded.Description, "Contratos, vistorias e fotos")
            BackupIncluded(Icons.Rounded.Notifications, "Lembretes e preferências")
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Criar backup completo", onExport, enabled = !busy)
            if (operation != null) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(when (operation) {
                    BackupOperation.EXPORT -> "Criando backup…"
                    BackupOperation.INSPECT -> "Verificando backup…"
                    BackupOperation.RESTORE -> "Restaurando backup…"
                },
                    modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
            Text("Backup manual. O arquivo contém dados pessoais: guarde-o em local seguro.", Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
        SettingsSurface(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            SettingsAction(Icons.Rounded.Restore, "Restaurar backup", "Selecionar arquivo .nani", !busy, onRestore)
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = .55f))
            SettingsAction(Icons.Rounded.History, "Importar backup antigo", "Arquivo JSON de versões anteriores", !busy, onRestoreLegacy)
        }
    }
}

@Composable
private fun BackupIncluded(icon: ImageVector, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .82f))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsSurface(contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

@Composable
private fun SettingsAction(icon: ImageVector, title: String, detail: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 10.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 14.dp), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemeSelector(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    val options = listOf(AppTheme.SYSTEM to "Sistema", AppTheme.LIGHT to "Claro", AppTheme.DARK to "Escuro")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(4.dp).selectableGroup()) {
        options.forEach { (value, label) ->
            val active = selected == value
            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(value) })
                .heightIn(min = 44.dp).padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
        }
    }
}
