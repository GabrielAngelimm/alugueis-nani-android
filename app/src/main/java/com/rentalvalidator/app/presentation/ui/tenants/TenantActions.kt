package com.rentalvalidator.app.presentation.ui.tenants

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.automirrored.outlined.Chat
import com.rentalvalidator.app.presentation.theme.AppSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rentalvalidator.app.domain.model.Tenant
import com.rentalvalidator.app.presentation.components.*
import com.rentalvalidator.app.presentation.design.*
import com.rentalvalidator.app.presentation.viewmodel.*
import com.rentalvalidator.app.reminders.*
import com.rentalvalidator.app.util.PhoneUtils
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun TenantContactSheet(tenant: Tenant, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val message = rememberAppSnackbar()
    val digits = PhoneUtils.getDigits(tenant.phone)
    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }
            .onSuccess { onDismiss() }
            .onFailure { message("Não foi possível abrir o aplicativo de contato.") }
    }
    NaniSheet("Contato", onDismiss) {
        NaniSheetTenantCard(tenant.name, PhoneUtils.formatPhone(tenant.phone).ifBlank { "Telefone não informado" })
        if (digits.isBlank()) {
            Text("Cadastre um telefone para conversar pelo WhatsApp ou ligar.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton("Adicionar telefone", { onDismiss(); onEdit() })
        } else {
            LedgerSheet(Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(AppSize.sheetRadius))) {
                NaniActionRow("WhatsApp", "Abrir uma conversa", Icons.AutoMirrored.Outlined.Chat, {
                    val phone = if (digits.length in 10..11) "55$digits" else digits
                    val intent = Intent(Intent.ACTION_VIEW, "https://wa.me/$phone".toUri()).setPackage("com.whatsapp")
                    runCatching { context.startActivity(intent) }.onSuccess { onDismiss() }
                        .onFailure { open(Intent(Intent.ACTION_VIEW, "https://wa.me/$phone".toUri())) }
                })
                LedgerRule(Modifier.padding(start = 70.dp))
                NaniActionRow("Ligar", "Abrir o discador com o número", Icons.Outlined.Phone, {
                    open(Intent(Intent.ACTION_DIAL, "tel:$digits".toUri()))
                })
            }
        }
    }
}
@Composable
internal fun TenantReminderSheet(tenant: Tenant, onDismiss: () -> Unit, vm: RentReminderViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val snackbar = rememberAppSnackbar()
    var allowed by remember { mutableStateOf(vm.allowed()) }
    var pendingHours by rememberSaveable { mutableIntStateOf(24) }
    var denied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = vm.allowed()
        if (granted && allowed) vm.save(tenant.id, pendingHours) { snackbar("Lembrete agendado"); onDismiss() }
        else denied = true
    }
    LaunchedEffect(tenant.id) { vm.load(tenant.id) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) allowed = vm.allowed() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    ReminderEditor(tenant, state, allowed, denied, onDismiss,
        onSettings = {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, RentReminderScheduler.CHANNEL)
            runCatching { context.startActivity(intent) }.onFailure { snackbar("Abra as configurações de notificações do celular.") }
        },
        onSave = { hours ->
            pendingHours = hours
            allowed = vm.allowed()
            if (allowed) vm.save(tenant.id, hours) { snackbar("Lembrete agendado"); onDismiss() }
            else if (Build.VERSION.SDK_INT >= 33 && !denied) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else denied = true
        },
        onRemove = { vm.remove(tenant.id) { snackbar("Lembrete removido"); onDismiss() } })
}

@Composable
internal fun ReminderEditor(tenant: Tenant, state: ReminderUiState, allowed: Boolean, denied: Boolean,
    onDismiss: () -> Unit, onSettings: () -> Unit, onSave: (Int) -> Unit, onRemove: () -> Unit) {
    var preset by rememberSaveable(tenant.id, state.reminder?.leadHours) {
        mutableIntStateOf(state.reminder?.leadHours?.takeIf { it in listOf(24, 48, 72) } ?: if (state.reminder == null) 24 else 0)
    }
    var amount by rememberSaveable(tenant.id, state.reminder?.leadHours) { mutableStateOf((state.reminder?.leadHours ?: 6).toString()) }
    var days by rememberSaveable(tenant.id) { mutableStateOf(false) }
    val number = amount.toIntOrNull()
    val hours = if (preset != 0) preset else number?.takeIf { it in 1..(if (days) 28 else 672) }?.let { if (days) it * 24 else it }
    val occurrence = hours?.let { ReminderTime.next(tenant.dueDay, it, Instant.now(), ZoneId.systemDefault()) }

    NaniSheet("Lembrete de aluguel", { if (!state.busy) onDismiss() }, actions = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PrimaryButton(if (state.busy) "Salvando…" else if (!allowed) "Permitir e agendar" else "Salvar lembrete",
                { hours?.let(onSave) }, enabled = !state.loading && !state.busy && hours != null)
            if (state.reminder != null) TextButton(onClick = onRemove, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("Remover lembrete", style = MaterialTheme.typography.labelLarge)
            }
        }
    }) {
        NaniSheetTenantCard(tenant.name, "Vence todo dia ${tenant.dueDay}")
        if (state.loading) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 3.dp)
        } else {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quando lembrar?", style = MaterialTheme.typography.titleMedium)
                listOf(24 to "1 dia antes", 48 to "2 dias antes", 72 to "3 dias antes", 0 to "Personalizar")
                    .forEach { (value, title) ->
                        NaniChoice(title, selected = preset == value, onClick = { if (!state.busy) preset = value })
                    }
            }
            if (preset == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NaniTabs(listOf("Horas", "Dias"), if (days) 1 else 0, {
                        if (!state.busy) { days = it == 1; amount = "1" }
                    })
                    NaniTextField(amount, { if (!state.busy) amount = it.filter(Char::isDigit).take(3) },
                        "Antecedência em " + if (days) "dias" else "horas",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = hours == null,
                        errorMessage = "Informe de 1 a " + if (days) "28 dias." else "672 horas.")
                }
            }
            occurrence?.let {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Outlined.NotificationsActive, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Próxima notificação", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .8f))
                            Text(it.trigger.atZone(ZoneId.systemDefault()).format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"))),
                                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            if (!allowed && denied) {
                Text("Ative as notificações do aplicativo para agendar o lembrete.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onSettings) { Text("Configurar notificações") }
            }
            state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}
