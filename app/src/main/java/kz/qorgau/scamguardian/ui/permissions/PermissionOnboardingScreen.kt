package kz.qorgau.scamguardian.ui.permissions

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.ui.components.PrivacyBadge

/**
 * First-run / incomplete-permission gate.
 * Automatically requests runtime permissions; system special access is opened with one tap.
 */
@Composable
fun PermissionOnboardingScreen(
    onAllReady: () -> Unit,
    onContinueAnyway: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var snapshot by remember { mutableStateOf(PermissionStatus.snapshot(context)) }
    var autoRequestedPost by remember { mutableStateOf(false) }
    var autoOpenedListener by remember { mutableStateOf(false) }
    var autoRequestedBattery by remember { mutableStateOf(false) }

    fun refresh() {
        snapshot = PermissionStatus.snapshot(context)
    }

    val postPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh() }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refresh() }

    val listenerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-request chain: notifications → listener settings → battery.
    LaunchedEffect(snapshot) {
        if (snapshot.allCriticalGranted && snapshot.batteryOptimizationIgnored) {
            onAllReady()
            return@LaunchedEffect
        }

        if (!snapshot.postNotificationsGranted &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !autoRequestedPost
        ) {
            autoRequestedPost = true
            postPermissionLauncher.launch(PermissionStatus.postNotificationsPermission())
            return@LaunchedEffect
        }

        if (snapshot.postNotificationsGranted &&
            !snapshot.notificationListenerEnabled &&
            !autoOpenedListener
        ) {
            autoOpenedListener = true
            listenerLauncher.launch(PermissionStatus.notificationListenerIntent())
            return@LaunchedEffect
        }

        if (snapshot.notificationListenerEnabled &&
            !snapshot.batteryOptimizationIgnored &&
            !autoRequestedBattery
        ) {
            autoRequestedBattery = true
            runCatching {
                batteryLauncher.launch(PermissionStatus.batteryOptimizationIntent(context))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrivacyBadge()

        PermissionStepCard(
            title = stringResource(R.string.onboarding_step_alerts_title),
            body = stringResource(R.string.onboarding_step_alerts_body),
            icon = Icons.Outlined.NotificationsActive,
            granted = snapshot.postNotificationsGranted,
            actionLabel = stringResource(R.string.onboarding_action_allow),
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    postPermissionLauncher.launch(PermissionStatus.postNotificationsPermission())
                } else {
                    refresh()
                }
            },
        )

        PermissionStepCard(
            title = stringResource(R.string.onboarding_step_listener_title),
            body = stringResource(R.string.onboarding_step_listener_body),
            icon = Icons.Outlined.Notifications,
            granted = snapshot.notificationListenerEnabled,
            actionLabel = stringResource(R.string.onboarding_action_open_settings),
            onAction = {
                listenerLauncher.launch(PermissionStatus.notificationListenerIntent())
            },
            highlight = !snapshot.notificationListenerEnabled,
        )

        PermissionStepCard(
            title = stringResource(R.string.onboarding_step_battery_title),
            body = stringResource(R.string.onboarding_step_battery_body),
            icon = Icons.Outlined.BatterySaver,
            granted = snapshot.batteryOptimizationIgnored,
            actionLabel = stringResource(R.string.onboarding_action_allow),
            onAction = {
                runCatching {
                    batteryLauncher.launch(PermissionStatus.batteryOptimizationIntent(context))
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (snapshot.allCriticalGranted) {
                    onAllReady()
                } else if (!snapshot.notificationListenerEnabled) {
                    listenerLauncher.launch(PermissionStatus.notificationListenerIntent())
                } else if (!snapshot.postNotificationsGranted &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    postPermissionLauncher.launch(PermissionStatus.postNotificationsPermission())
                } else {
                    onAllReady()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = snapshot.allCriticalGranted || snapshot.notificationListenerEnabled,
        ) {
            Text(
                text = if (snapshot.allCriticalGranted) {
                    stringResource(R.string.onboarding_continue)
                } else {
                    stringResource(R.string.onboarding_enable_required)
                },
            )
        }

        if (snapshot.allCriticalGranted) {
            // optional battery still missing
        } else {
            OutlinedButton(
                onClick = onContinueAnyway,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.onboarding_later))
            }
        }

        Text(
            text = stringResource(R.string.onboarding_privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionStepCard(
    title: String,
    body: String,
    icon: ImageVector,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    highlight: Boolean = false,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight && !granted) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (granted) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = if (granted) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (!granted) {
                Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}
