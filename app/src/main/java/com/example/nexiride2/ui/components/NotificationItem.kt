package com.example.nexiride2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nexiride2.domain.model.AppNotification
import com.example.nexiride2.domain.model.NotificationType
import com.example.nexiride2.ui.theme.*

@Composable
fun NotificationItem(notification: AppNotification, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (icon, iconBg) = when (notification.type) {
        NotificationType.BOOKING_CONFIRMATION -> Icons.Default.CheckCircle to AccentGreen
        NotificationType.TRIP_REMINDER -> Icons.Default.AccessAlarm to StatusInfo
        NotificationType.DELAY -> Icons.Default.Warning to StatusWarning
        NotificationType.CANCELLATION -> Icons.Default.Cancel to StatusError
        NotificationType.REFUND -> Icons.Default.AccountBalanceWallet to AccentGreen
        NotificationType.PROMO -> Icons.Default.LocalOffer to SecondaryOrange
        NotificationType.GENERAL -> Icons.Default.Notifications to PrimaryBlueLight
    }
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(if (!notification.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(iconBg.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = iconBg)
        }
        Column(Modifier.weight(1f)) {
            Text(notification.title, style = MaterialTheme.typography.titleSmall,
                fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium)
            Text(notification.message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Text(notification.timestamp, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        if (!notification.isRead) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.Top))
        }
    }
}
