package kz.qorgau.scamguardian.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.domain.model.AppLanguage
import kz.qorgau.scamguardian.domain.model.Sensitivity
import kz.qorgau.scamguardian.ui.components.PrivacyBadge
import kz.qorgau.scamguardian.ui.components.SectionHeader
import kz.qorgau.scamguardian.ui.util.appNotificationSettingsIntent
import kz.qorgau.scamguardian.ui.util.isNotificationListenerEnabled
import kz.qorgau.scamguardian.ui.util.notificationListenerSettingsIntent

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var listenerEnabled by remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }
    var showClearConfirm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* state refreshed via recomposition helpers below */ }

    var notificationsGranted by remember {
        mutableStateOf(arePostNotificationsGranted(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = isNotificationListenerEnabled(context)
                notificationsGranted = arePostNotificationsGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.HistoryCleared -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_clear_history_done),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displaySmall,
            )
            PrivacyBadge()
        }
        Spacer(Modifier.height(8.dp))

        SectionHeader(stringResource(R.string.settings_section_protection))

        SettingsCard {
            SettingsNavRow(
                title = stringResource(R.string.settings_notification_access),
                subtitle = if (listenerEnabled) {
                    stringResource(R.string.settings_notification_access_on)
                } else {
                    stringResource(R.string.settings_notification_access_off)
                },
                subtitleColor = if (listenerEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    null
                },
                onClick = {
                    context.startActivity(notificationListenerSettingsIntent())
                },
            )
            HorizontalDivider()
            SettingsNavRow(
                title = stringResource(R.string.settings_post_notifications),
                subtitle = stringResource(R.string.settings_post_notifications_hint),
                trailing = if (notificationsGranted) {
                    stringResource(R.string.settings_notification_access_on)
                } else {
                    stringResource(R.string.settings_open_system)
                },
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(appNotificationSettingsIntent(context))
                    }
                },
            )
            HorizontalDivider()
            SettingsToggleRow(
                title = stringResource(R.string.settings_monitor_sms),
                subtitle = stringResource(R.string.settings_monitor_sms_hint),
                checked = settings.monitorSms,
                onCheckedChange = viewModel::setMonitorSms,
            )
            HorizontalDivider()
            SettingsToggleRow(
                title = stringResource(R.string.settings_monitor_whatsapp),
                subtitle = stringResource(R.string.settings_monitor_whatsapp_hint),
                checked = settings.monitorWhatsapp,
                onCheckedChange = viewModel::setMonitorWhatsapp,
            )
            HorizontalDivider()
            SettingsToggleRow(
                title = stringResource(R.string.settings_monitor_telegram),
                subtitle = stringResource(R.string.settings_monitor_telegram_hint),
                checked = settings.monitorTelegram,
                onCheckedChange = viewModel::setMonitorTelegram,
            )
        }

        SectionHeader(stringResource(R.string.settings_section_analysis))

        SettingsCard {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
            LanguageOption(
                label = stringResource(R.string.settings_language_ru),
                selected = settings.language == AppLanguage.RUSSIAN,
                onSelect = { viewModel.setLanguage(AppLanguage.RUSSIAN) },
            )
            LanguageOption(
                label = stringResource(R.string.settings_language_kk),
                selected = settings.language == AppLanguage.KAZAKH,
                onSelect = { viewModel.setLanguage(AppLanguage.KAZAKH) },
            )
            LanguageOption(
                label = stringResource(R.string.settings_language_en),
                selected = settings.language == AppLanguage.ENGLISH,
                onSelect = { viewModel.setLanguage(AppLanguage.ENGLISH) },
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_sensitivity),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
            Text(
                text = stringResource(R.string.settings_sensitivity_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            SensitivityOption(
                label = stringResource(R.string.settings_sensitivity_low),
                selected = settings.sensitivity == Sensitivity.LOW,
                onSelect = { viewModel.setSensitivity(Sensitivity.LOW) },
            )
            SensitivityOption(
                label = stringResource(R.string.settings_sensitivity_medium),
                selected = settings.sensitivity == Sensitivity.MEDIUM,
                onSelect = { viewModel.setSensitivity(Sensitivity.MEDIUM) },
            )
            SensitivityOption(
                label = stringResource(R.string.settings_sensitivity_high),
                selected = settings.sensitivity == Sensitivity.HIGH,
                onSelect = { viewModel.setSensitivity(Sensitivity.HIGH) },
            )
        }

        SectionHeader(stringResource(R.string.settings_section_data))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_clear_history),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { showClearConfirm = true }) {
                    Text(stringResource(R.string.settings_clear_history))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_history)) },
            text = { Text(stringResource(R.string.settings_clear_history_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearHistory()
                    },
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = { content() },
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    trailing: String? = null,
    subtitleColor: Color? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
        trailing?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // row handles selection (avoids double-fire)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SensitivityOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    LanguageOption(label = label, selected = selected, onSelect = onSelect)
}

private fun arePostNotificationsGranted(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}
