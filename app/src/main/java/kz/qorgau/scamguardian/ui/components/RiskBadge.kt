package kz.qorgau.scamguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kz.qorgau.scamguardian.R
import kz.qorgau.scamguardian.domain.model.RiskLevel
import kz.qorgau.scamguardian.ui.theme.AlertCoral
import kz.qorgau.scamguardian.ui.theme.SafeGreen
import kz.qorgau.scamguardian.ui.theme.WarningAmber

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
) {
    val (label, bg, fg, icon) = when (riskLevel) {
        RiskLevel.HIGH -> BadgeStyle(
            label = stringResource(R.string.risk_high),
            background = AlertCoral,
            content = Color.White,
            icon = Icons.Outlined.PriorityHigh,
        )
        RiskLevel.SUSPICIOUS -> BadgeStyle(
            label = stringResource(R.string.risk_suspicious),
            background = WarningAmber,
            content = Color(0xFF1A1A1A),
            icon = Icons.Outlined.WarningAmber,
        )
        RiskLevel.SAFE -> BadgeStyle(
            label = stringResource(R.string.risk_safe),
            background = SafeGreen.copy(alpha = 0.18f),
            content = SafeGreen,
            icon = Icons.Outlined.CheckCircle,
        )
    }

    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private data class BadgeStyle(
    val label: String,
    val background: Color,
    val content: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
