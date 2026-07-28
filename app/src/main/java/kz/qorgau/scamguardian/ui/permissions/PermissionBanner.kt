package kz.qorgau.scamguardian.ui.permissions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kz.qorgau.scamguardian.R

@Composable
fun PermissionBanner(
    snapshot: PermissionSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (snapshot.allCriticalGranted) return

    val text = when {
        !snapshot.notificationListenerEnabled ->
            stringResource(R.string.banner_need_listener)
        !snapshot.postNotificationsGranted ->
            stringResource(R.string.banner_need_alerts)
        else -> stringResource(R.string.banner_need_permissions)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
