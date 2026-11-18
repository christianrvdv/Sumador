// ui/settings/SettingsDialog.kt
package cu.christianrvdv.sumador.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cu.christianrvdv.sumador.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settingsState: SettingsState,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeOption) -> Unit,
    onCurrencyChange: (CurrencySymbol) -> Unit,
    onSortChange: (Boolean) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onConfirmClearChange: (Boolean) -> Unit,
    onLanguageChange: (LanguageOption) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onUseCoinsChange: (Boolean) -> Unit,
    onAboutClick: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onManageDenominations: () -> Unit,
    // onBackupRequest eliminado
    onExportRequest: () -> Unit,
    onImportRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Sección Tema
            SettingsSection(
                icon = Icons.Default.BrightnessMedium,
                title = stringResource(R.string.theme_label)
            ) {
                ThemeOption.values().forEach { option ->
                    SettingsRadioItem(
                        selected = settingsState.theme == option,
                        label = when (option) {
                            ThemeOption.LIGHT -> stringResource(R.string.theme_light)
                            ThemeOption.DARK -> stringResource(R.string.theme_dark)
                            ThemeOption.SYSTEM -> stringResource(R.string.theme_system)
                        },
                        icon = when (option) {
                            ThemeOption.LIGHT -> Icons.Default.LightMode
                            ThemeOption.DARK -> Icons.Default.DarkMode
                            ThemeOption.SYSTEM -> Icons.Default.DeviceUnknown
                        },
                        onClick = { onThemeChange(option) }
                    )
                }
            }

            // Sección Moneda
            SettingsSection(
                icon = Icons.Default.AttachMoney,
                title = stringResource(R.string.currency_label)
            ) {
                CurrencySymbol.values().forEach { currency ->
                    SettingsRadioItem(
                        selected = settingsState.currencySymbol == currency,
                        label = when (currency) {
                            CurrencySymbol.PESO -> stringResource(R.string.currency_peso)
                            CurrencySymbol.USD -> stringResource(R.string.currency_usd)
                            CurrencySymbol.EURO -> stringResource(R.string.currency_euro)
                        },
                        icon = when (currency) {
                            CurrencySymbol.PESO -> Icons.Default.MonetizationOn
                            CurrencySymbol.USD -> Icons.Default.AttachMoney
                            CurrencySymbol.EURO -> Icons.Default.Euro
                        },
                        onClick = { onCurrencyChange(currency) }
                    )
                }
            }

            // Sección Denominaciones
            SettingsSection(
                icon = Icons.Default.Money,
                title = stringResource(R.string.denominations_section_title)
            ) {
                SettingsButtonItem(
                    label = stringResource(R.string.manage_denominations_button),
                    icon = Icons.Default.Edit,
                    onClick = onManageDenominations
                )
            }

            // Sección Idioma
            SettingsSection(
                icon = Icons.Default.Language,
                title = stringResource(R.string.language_label)
            ) {
                LanguageOption.values().forEach { option ->
                    SettingsRadioItem(
                        selected = settingsState.language == option,
                        label = when (option) {
                            LanguageOption.ENGLISH -> stringResource(R.string.language_english)
                            LanguageOption.SPANISH -> stringResource(R.string.language_spanish)
                            LanguageOption.SYSTEM -> stringResource(R.string.language_system)
                        },
                        icon = when (option) {
                            LanguageOption.ENGLISH -> Icons.Default.Translate
                            LanguageOption.SPANISH -> Icons.Default.Translate
                            LanguageOption.SYSTEM -> Icons.Default.DeviceUnknown
                        },
                        onClick = { onLanguageChange(option) }
                    )
                }
            }

            // Sección Orden
            SettingsSection(
                icon = Icons.Default.Sort,
                title = stringResource(R.string.sort_label)
            ) {
                SettingsRadioItem(
                    selected = settingsState.sortAscending,
                    label = stringResource(R.string.sort_ascending),
                    icon = Icons.Default.SortByAlpha,
                    onClick = { onSortChange(true) }
                )
                SettingsRadioItem(
                    selected = !settingsState.sortAscending,
                    label = stringResource(R.string.sort_descending),
                    icon = Icons.Default.SortByAlpha,
                    onClick = { onSortChange(false) }
                )
            }

            // Sección Opciones
            SettingsSection(
                icon = Icons.Default.ToggleOn,
                title = stringResource(R.string.settings_options_section)
            ) {
                SettingsSwitchItem(
                    label = stringResource(R.string.auto_save_label),
                    checked = settingsState.autoSave,
                    onCheckedChange = onAutoSaveChange,
                    description = stringResource(R.string.auto_save_description)
                )
                SettingsSwitchItem(
                    label = stringResource(R.string.confirm_clear_label),
                    checked = settingsState.confirmClear,
                    onCheckedChange = onConfirmClearChange,
                    description = stringResource(R.string.confirm_clear_description)
                )
                SettingsSwitchItem(
                    label = stringResource(R.string.keep_screen_on_label),
                    checked = settingsState.keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange,
                    description = stringResource(R.string.keep_screen_on_description)
                )
                SettingsSwitchItem(
                    label = stringResource(R.string.use_coins_label),
                    checked = settingsState.useCoins,
                    onCheckedChange = onUseCoinsChange,
                    description = stringResource(R.string.use_coins_description)
                )
            }

            // === SECCIÓN: BACKUP AUTOMÁTICO (INFORMATIVO) ===
            SettingsSection(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.backup_section_title)
            ) {
                Text(
                    text = stringResource(R.string.backup_auto_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // === SECCIÓN: BACKUP MANUAL (EXPORT/IMPORT) ===
            SettingsSection(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.manual_backup_title)
            ) {
                // Botón Exportar
                Button(
                    onClick = onExportRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.export_backup_button))
                }

                Spacer(Modifier.height(4.dp))

                // Botón Importar
                Button(
                    onClick = onImportRequest,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.import_backup_button))
                }

                Text(
                    text = stringResource(R.string.manual_backup_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Sección Actualizaciones
            SettingsSection(
                icon = Icons.Default.SystemUpdate,
                title = stringResource(R.string.updates_section_title)
            ) {
                SettingsButtonItem(
                    label = stringResource(R.string.check_updates_button),
                    icon = Icons.Default.Search,
                    onClick = onCheckForUpdates
                )
            }

            // Botón About
            TextButton(
                onClick = {
                    onDismiss()
                    onAboutClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("settings_about_item"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.content_description_about)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_about_button))
            }

            // Botón Done
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.settings_done_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---- Componentes auxiliares (sin cambios) ----
@Composable
private fun SettingsSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun SettingsRadioItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsButtonItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}