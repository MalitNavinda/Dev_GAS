package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Distributor
import com.example.data.model.GasBrand
import com.example.data.model.QueueReport
import com.example.data.model.StockStatus
import com.example.ui.components.BrandBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
    isAdmin: Boolean,
    distributorsState: UiState<List<Distributor>>,
    flaggedReports: List<QueueReport>,
    onBackToExplore: () -> Unit,
    onVerifyDealer: (stationId: String, verified: Boolean, suspend: Boolean) -> Unit,
    onModerateReport: (reportId: String, dismiss: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToExplore() }

    val strings = LocalStrings.current
    val ext = LocalExtendedColors.current
    val allStations = (distributorsState as? UiState.Success)?.data.orEmpty()

    val availableCount = allStations.count { it.statusEnum == StockStatus.AVAILABLE && it.totalCylinders > 0L }
    val inTransitCount = allStations.count { it.statusEnum == StockStatus.IN_TRANSIT }
    val litroCylinders = allStations.filter { it.brandEnum == GasBrand.LITRO }.sumOf { it.totalCylinders }
    val laugfsCylinders = allStations.filter { it.brandEnum == GasBrand.LAUGFS }.sumOf { it.totalCylinders }
    val unverifiedStations = allStations.filter { !it.isVerified }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("admin_panel_screen")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = strings.adminConsoleTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = if (isAdmin) "Verified Administrator Session Active"
                        else "National Supply Metrics (Moderator actions restricted to verified admins)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isAdmin) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Island-Wide Supply Metrics Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricTile(
                    label = "Active Outlets",
                    value = "$availableCount / ${allStations.size}",
                    accentColor = ext.statusAvailable,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Trucks En Route",
                    value = inTransitCount.toString(),
                    accentColor = ext.statusTransit,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricTile(
                    label = "Litro Cylinders",
                    value = litroCylinders.toString(),
                    accentColor = ext.litroBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "LAUGFS Cylinders",
                    value = laugfsCylinders.toString(),
                    accentColor = ext.laugfsYellow,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Dealer Verification Queue
            Text(
                text = "Distributor Verification Queue (${unverifiedStations.size} Pending)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (allStations.isEmpty()) {
                Text(
                    text = "No distributors registered yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val sortedForReview = allStations.sortedBy { it.isVerified }
                sortedForReview.forEach { station ->
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
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
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                StockStatusBadge(status = station.statusEnum)
                            }

                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Owner: ${station.ownerName} · ${station.address} (${station.district}) · ${station.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (isAdmin) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (!station.isVerified) {
                                        Button(
                                            onClick = { onVerifyDealer(station.id, true, false) },
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("admin_verify_${station.id}")
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Approve Official Badge")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onVerifyDealer(station.id, false, false) },
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                        ) {
                                            Text("Revoke Badge")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onVerifyDealer(station.id, false, true) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.GppBad, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Suspend")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Flagged Crowd Reports Moderation
            if (isAdmin) {
                Text(
                    text = "Flagged Community Reports (${flaggedReports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (flaggedReports.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Zero flagged reports pending review — community trust engine healthy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    flaggedReports.forEach { rep ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = "${rep.stationName} · Reported by ${rep.reporterAlias}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Claimed: ${rep.reportedStatus} · Queue: ~${rep.queueLengthEstimate} · \"${rep.comment}\"",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onModerateReport(rep.id, true) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Dismiss False Report")
                                    }
                                    OutlinedButton(
                                        onClick = { onModerateReport(rep.id, false) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Keep Active")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
