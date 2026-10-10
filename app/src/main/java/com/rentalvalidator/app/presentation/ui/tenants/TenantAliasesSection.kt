package com.rentalvalidator.app.presentation.ui.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.R
import com.rentalvalidator.app.presentation.components.AppFormSection
import com.rentalvalidator.app.presentation.components.NaniTextField
import com.rentalvalidator.app.presentation.theme.AppSize
import com.rentalvalidator.app.presentation.theme.NaniTheme

/** Other names under which this tenant's transfers appear in bank statements. */
@Composable
internal fun TenantAliasesSection(aliasInput: String, aliases: List<String>, onInput: (String) -> Unit,
    onAdd: () -> Unit, onRemove: (String) -> Unit) {
    AppFormSection(
        title = stringResource(R.string.statement_identity),
        subtitle = "Nomes que aparecem nas transferências do extrato, como o de quem paga pela pessoa",
        icon = Icons.Rounded.Sell
    ) { TenantAliasesEditor(aliasInput, aliases, onInput, onAdd, onRemove) }
}

/** The entry field and the chips of a tenant's statement names, without a heading of its own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TenantAliasesEditor(aliasInput: String, aliases: List<String>, onInput: (String) -> Unit,
    onAdd: () -> Unit, onRemove: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            NaniTextField(aliasInput, onInput, stringResource(R.string.new_alias), Modifier.weight(1f))
            FilledIconButton(onClick = onAdd, enabled = aliasInput.isNotBlank(), modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(AppSize.controlRadius),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = NaniTheme.colors.action,
                    contentColor = NaniTheme.colors.onAction)) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_alias))
            }
        }
        if (aliases.isEmpty()) {
            Text("Nenhum apelido. O nome completo já é usado na conferência.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            aliases.forEach { alias ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(alias) },
                    label = { Text(alias, style = MaterialTheme.typography.labelLarge) },
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = { Icon(Icons.Rounded.Close, "Remover $alias", Modifier.size(16.dp)) },
                    colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = null,
                    modifier = Modifier.heightIn(min = 40.dp)
                )
            }
        }
    }
}
