package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.Distributor
import com.example.data.model.DistributorWithDistance
import com.example.data.model.GasBrand
import com.example.data.model.InAppAlert
import com.example.data.model.StockStatus
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalExtendedColors
import com.example.util.GeoUtils
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LanguageSwitcherPill(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("language_switcher_pill"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            AppLanguage.entries.forEach { lang ->
                val selected = lang == currentLanguage
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable(role = Role.Tab) { onLanguageSelected(lang) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("lang_option_${lang.code.lowercase()}")
                ) {
                    Text(
                        text = lang.nativeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BrandBadge(
    brand: GasBrand,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val bgColor = if (brand == GasBrand.LITRO) ext.litroBlue else ext.laugfsYellow
    val textColor = if (brand == GasBrand.LITRO) ext.onLitroBlue else ext.onLaugfsYellow

    Surface(
        color = bgColor,
        contentColor = textColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (brand == GasBrand.LITRO) "LITRO" else "LAUGFS",
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

@Composable
fun StockStatusBadge(
    status: StockStatus,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val strings = LocalStrings.current

    val (container, content, icon) = when (status) {
        StockStatus.AVAILABLE -> Triple(
            ext.statusAvailableContainer,
            ext.onStatusAvailableContainer,
            Icons.Filled.CheckCircle
        )
        StockStatus.IN_TRANSIT -> Triple(
            ext.statusTransitContainer,
            ext.onStatusTransitContainer,
            Icons.Filled.LocalShipping
        )
        StockStatus.SCHEDULED -> Triple(
            ext.statusScheduledContainer,
            ext.onStatusScheduledContainer,
            Icons.Filled.Schedule
        )
        StockStatus.SOLD_OUT -> Triple(
            ext.statusSoldOutContainer,
            ext.onStatusSoldOutContainer,
            Icons.Filled.WarningAmber
        )
    }

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = content
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = strings.statusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

@Composable
fun CylinderMiniCounter(
    sizeLabel: String,
    count: Long,
    brand: GasBrand,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val hasStock = count > 0L
    val lowStock = count in 1L..9L

    val containerColor = when {
        !hasStock -> MaterialTheme.colorScheme.surfaceContainer
        lowStock -> ext.statusTransitContainer.copy(alpha = 0.75f)
        brand == GasBrand.LITRO -> ext.litroContainer.copy(alpha = 0.65f)
        else -> ext.laugfsContainer.copy(alpha = 0.7f)
    }
    val valueColor = when {
        !hasStock -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        lowStock -> ext.onStatusTransitContainer
        brand == GasBrand.LITRO -> ext.onLitroContainer
        else -> ext.onLaugfsContainer
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasStock) valueColor.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun DistributorStationCard(
    item: DistributorWithDistance,
    isWatched: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val station: Distributor = item.distributor
    val strings = LocalStrings.current
    val ext = LocalExtendedColors.current

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("station_card_${station.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BrandBadge(brand = station.brandEnum)
                    if (station.isVerified) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = strings.verifiedDealer,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isWatched) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = strings.watchingRestock,
                            tint = ext.statusTransit,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                StockStatusBadge(status = station.statusEnum)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${GeoUtils.formatDistanceKm(item.distanceKm)} · ${station.town}, ${station.district} · ${formatRelativeTimestamp(station.updatedAt ?: station.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (station.hasCrowdDiscrepancy) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ext.statusSoldOutContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.crowdDiscrepancyWarning,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.onStatusSoldOutContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CylinderMiniCounter(
                    sizeLabel = "12.5 kg",
                    count = station.stock12_5kg,
                    brand = station.brandEnum,
                    modifier = Modifier.weight(1f)
                )
                CylinderMiniCounter(
                    sizeLabel = "5.0 kg",
                    count = station.stock5kg,
                    brand = station.brandEnum,
                    modifier = Modifier.weight(1f)
                )
                CylinderMiniCounter(
                    sizeLabel = "2.3 kg",
                    count = station.stock2_3kg,
                    brand = station.brandEnum,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val queueSummary = if (station.estimatedQueueCount > 0L) {
                    "~${station.estimatedQueueCount} ${strings.peopleInQueue} (~${station.estimatedWaitMinutes}m)"
                } else {
                    strings.queueLabel(station.queueLevelEnum)
                }
                Text(
                    text = queueSummary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${strings.trustScore} ${station.trustConfidencePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { station.trustConfidencePercent / 100f },
                        modifier = Modifier
                            .width(44.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }
            }
        }
    }
}

@Composable
fun RestockAlertsDialog(
    alerts: List<InAppAlert>,
    watchlistCount: Int,
    onDismiss: () -> Unit,
    onSelectStation: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Restock Alerts ($watchlistCount Watched)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (alerts.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "No restock transitions triggered during this session yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Tap 'Alert on Restock' on any Litro or LAUGFS outlet to receive immediate system notifications when delivery trucks depart or cylinders arrive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    alerts.take(6).forEach { alert ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectStation(alert.stationId)
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    BrandBadge(brand = alert.brand)
                                    StockStatusBadge(status = alert.newStatus)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = alert.stationName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Close")
            }
        }
    )
}

fun formatRelativeTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val nowMs = System.currentTimeMillis()
    val thenMs = timestamp.toDate().time
    val diffMins = ((nowMs - thenMs) / 60000L).coerceAtLeast(0L)
    return when {
        diffMins < 1L -> "Just now"
        diffMins < 60L -> "${diffMins}m ago"
        diffMins < 1440L -> "${diffMins / 60L}h ago"
        else -> SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date(thenMs))
    }
}
