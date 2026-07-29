package kz.qorgau.scamguardian.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.domain.model.AnalysisRecord
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.domain.model.SourceApp
import kz.qorgau.scamguardian.notification.NotificationListenerController
import kz.qorgau.scamguardian.ui.components.PrivacyBadge
import kz.qorgau.scamguardian.ui.components.RiskBadge
import kz.qorgau.scamguardian.ui.util.formatTimeAgo
import kz.qorgau.scamguardian.ui.util.isNotificationListenerEnabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.history.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<AnalysisRecord?>(null) }
    var whyExpanded by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var accessGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var listenerConnected by remember { mutableStateOf(NotificationListenerController.isConnected) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessGranted = isNotificationListenerEnabled(context)
                listenerConnected = NotificationListenerController.isConnected
                NotificationListenerController.ensureBound(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // NLS bind is async after rebind — poll briefly so empty state updates.
    LaunchedEffect(accessGranted) {
        if (!accessGranted) return@LaunchedEffect
        repeat(15) {
            listenerConnected = NotificationListenerController.isConnected
            if (listenerConnected) return@LaunchedEffect
            delay(1_000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.displaySmall,
            )
            PrivacyBadge()
        }
        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            HistoryEmptyState(
                accessGranted = accessGranted,
                listenerConnected = listenerConnected,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { record ->
                    HistoryListItem(
                        record = record,
                        onClick = {
                            selected = record
                            whyExpanded = true
                            viewModel.markRead(record.id)
                        },
                    )
                }
            }
        }
    }

    selected?.let { record ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
        ) {
            HistoryDetailSheet(
                record = record,
                whyExpanded = whyExpanded,
                onToggleWhy = { whyExpanded = !whyExpanded },
                onFalsePositive = {
                    viewModel.markFalsePositive(record.id)
                    selected = null
                },
                onMarkSafe = {
                    viewModel.markFalsePositive(record.id)
                    selected = null
                },
                onGotIt = {
                    viewModel.markConfirmed(record.id)
                    selected = null
                },
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(
    accessGranted: Boolean,
    listenerConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val title: String
    val hint: String
    when {
        !accessGranted -> {
            title = stringResource(R.string.history_empty_no_access)
            hint = stringResource(R.string.history_empty_no_access_hint)
        }
        !listenerConnected -> {
            title = stringResource(R.string.history_empty_disconnected)
            hint = stringResource(R.string.history_empty_disconnected_hint)
        }
        else -> {
            title = stringResource(R.string.history_empty)
            hint = stringResource(R.string.history_empty_hint)
        }
    }
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryListItem(
    record: AnalysisRecord,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = sourceIcon(record.sourceApp),
                contentDescription = sourceLabel(record.sourceApp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.sender
                        ?: stringResource(R.string.history_unknown_sender),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = record.messageText.replace('\n', ' '),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatTimeAgo(context, record.createdAtEpochMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RiskBadge(riskLevel = record.riskLevel)
        }
    }
}

@Composable
private fun HistoryDetailSheet(
    record: AnalysisRecord,
    whyExpanded: Boolean,
    onToggleWhy: () -> Unit,
    onFalsePositive: () -> Unit,
    onMarkSafe: () -> Unit,
    onGotIt: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.history_details_title),
            style = MaterialTheme.typography.titleLarge,
        )
        RiskBadge(riskLevel = record.riskLevel)
        PrivacyBadge()

        Text(
            text = stringResource(R.string.history_message_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = record.messageText,
            style = MaterialTheme.typography.bodyLarge,
        )

        TextButton(onClick = onToggleWhy) {
            Text(stringResource(R.string.why_scam))
        }
        AnimatedVisibility(visible = whyExpanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = record.explanation,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (record.matchedRules.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.history_matched_rules),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = record.matchedRules.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        HorizontalDivider()

        if (record.riskLevel != RiskLevel.SAFE) {
            TextButton(onClick = onMarkSafe) {
                Text(stringResource(R.string.action_mark_safe))
            }
            TextButton(onClick = onFalsePositive) {
                Text(stringResource(R.string.action_false_positive))
            }
        }
        TextButton(onClick = onGotIt) {
            Text(stringResource(R.string.action_got_it))
        }
    }
}

@Composable
private fun sourceLabel(source: SourceApp): String =
    when (source) {
        SourceApp.SMS -> stringResource(R.string.history_source_sms)
        SourceApp.WHATSAPP -> stringResource(R.string.history_source_whatsapp)
        SourceApp.TELEGRAM -> stringResource(R.string.history_source_telegram)
        SourceApp.INSTAGRAM -> stringResource(R.string.history_source_instagram)
        SourceApp.MESSENGER -> stringResource(R.string.history_source_messenger)
        SourceApp.VIBER -> stringResource(R.string.history_source_viber)
        SourceApp.VK -> stringResource(R.string.history_source_vk)
        SourceApp.OK -> stringResource(R.string.history_source_ok)
        SourceApp.OTHER -> stringResource(R.string.history_source_other)
        SourceApp.MANUAL -> stringResource(R.string.history_source_manual)
    }

private fun sourceIcon(source: SourceApp) =
    when (source) {
        SourceApp.SMS -> Icons.Outlined.Sms
        SourceApp.WHATSAPP -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.TELEGRAM -> Icons.AutoMirrored.Outlined.Send
        SourceApp.INSTAGRAM -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.MESSENGER -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.VIBER -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.VK -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.OK -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.OTHER -> Icons.AutoMirrored.Outlined.Chat
        SourceApp.MANUAL -> Icons.Outlined.EditNote
    }
