package com.rentalvalidator.app.presentation.ui.tenants
import androidx.compose.ui.res.stringResource
import com.rentalvalidator.app.R
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentalvalidator.app.presentation.components.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TenantAliasesSection(aliasInput: String, aliases: List<String>, onInput: (String) -> Unit,
    onAdd: () -> Unit, onRemove: (String) -> Unit) {
                    AppFormSection(
                        title = stringResource(R.string.statement_identity),
                        subtitle = "Adicione somente nomes que aparecem nas transações",
                        icon = Icons.Rounded.LocalOffer
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                            ModernTextField(aliasInput, onInput, stringResource(R.string.new_alias), Modifier.weight(1f))
                            val addShape = RoundedCornerShape(14.dp)
                            val canAddAlias = aliasInput.isNotBlank()
                            FilledIconButton(
                                onClick = onAdd,
                                enabled = canAddAlias,
                                modifier = Modifier
                                    .size(56.dp)
                                    .then(if (canAddAlias) Modifier.premiumShadow(addShape) else Modifier),
                                shape = addShape
                            ) { Icon(Icons.Rounded.Add, stringResource(R.string.add_alias)) }
                        }
                        if (aliases.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                aliases.forEach { alias ->
                                    InputChip(
                                        selected = false,
                                        onClick = { onRemove(alias) },
                                        label = { Text(alias) },
                                        shape = RoundedCornerShape(14.dp),
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { onRemove(alias) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Rounded.Close, "Remover", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
}
