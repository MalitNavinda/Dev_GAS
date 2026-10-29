package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DistributorWithDistance
import com.example.data.model.QueueReport
import com.example.data.model.StockLog
import com.example.data.model.StockStatus
import com.example.ui.components.BrandBadge
import com.example.ui.components.CylinderMiniCounter
import com.example.ui.components.StockStatusBadge
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalExtendedColors
import com.example.util.GeoUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationDetailPane(
    stationItem: DistributorWithDistance,
    isWatched: Boolean,
    reports: List<QueueReport>,
    stockLogs: List<StockLog>,
    isSubmitting: Boolean,
    enableBackHandler: Boolean,
    onClose: () -> Unit,
    onToggleWatch: () -> Unit,
    onSubmitReport: (reportedStatus: String, queueCount: Int, comment: String) -> Unit,
    onFlagReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (enableBackHandler) {
        BackHandler { onClose() }
    }

    val context = LocalContext.current
    val station = stationItem.distributor
    val strings = LocalStrings.current
    val ext = LocalExtendedColors.current

    var showReportForm by remember(station.id) { mutableStateOf(false) }
    var reportedStatus by remember(station.id) {
        mutableStateOf(
            if (station.statusEnum == StockStatus.SOLD_OUT) "SOLD_OUT" else "AVAILABLE"
        )
    }
    var queueSlider by remember(station.id) {
        mutableFloatStateOf(station.estimatedQueueCount.toFloat().coerceIn(0f, 150f))
    }
    var reportComment by remember(station.id) { mutableStateOf("") }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        modifier = modifier.testTag("station_detail_pane")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Row
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
                    StockStatusBadge(status = station.statusEnum)
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_detail_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.cd_close_detail)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = station.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (station.isVerified) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.verifiedDealer,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    Text(
                        text = strings.unverifiedDealer,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "· ${GeoUtils.formatDistanceKm(stationItem.distanceKm)} away · ${station.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrival / Dispatch ETA Banner
            if (station.nextArrivalNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = station.nextArrivalNote,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Updated ${formatRelativeTimestamp(station.updatedAt ?: station.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (station.hasCrowdDiscrepancy) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = ext.statusSoldOutContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.crowdDiscrepancyWarning,
                        style = MaterialTheme.typography.labelLarge,
                        color = ext.onStatusSoldOutContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Cylinder Stock Grid
            Text(
                text = strings.liveCylinderStock,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Consumer Action Buttons (Watch Restock, Directions, Call)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onToggleWatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWatched) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = 150.dp, minHeight = 48.dp)
                        .testTag("watch_station_button")
                ) {
                    Icon(
                        imageVector = if (isWatched) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isWatched) strings.watchingRestock else strings.watchRestock)
                }

                OutlinedButton(
                    onClick = {
                        val geoUri = Uri.parse(
                            "geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}(${Uri.encode(station.name)})"
                        )
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("station_directions_button")
                ) {
                    Icon(Icons.Filled.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.directions)
                }

                OutlinedButton(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${station.phone}"))
                        try {
                            context.startActivity(dialIntent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("station_call_button")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.callDealer)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Queue Telemetry & Crowd Reporting Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.queueTelemetry,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "~${station.estimatedQueueCount} ${strings.peopleInQueue} · ~${station.estimatedWaitMinutes} ${strings.estWaitMins} · ${strings.trustScore} ${station.trustConfidencePercent}% (${station.confirmationsCount} pts)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showReportForm = !showReportForm },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("toggle_crowd_report_form_button")
            ) {
                Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.submitQueueReport)
            }

            if (showReportForm) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Observed Stock at Outlet (GPS <=1.5km earns 2x Verified Weight)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "AVAILABLE" to "In Stock",
                                "LOW_STOCK" to "Running Low",
                                "SOLD_OUT" to "Sold Out"
                            ).forEach { (code, label) ->
                                FilterChip(
                                    selected = reportedStatus == code,
                                    onClick = { reportedStatus = code },
                                    label = { Text(label) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                )
                            }
                        }

                        val estCount = queueSlider.roundToInt()
                        Text(
                            text = "Estimated Queue: ~$estCount people (~${(estCount * 1.4).roundToInt()} min wait)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = queueSlider,
                            onValueChange = { queueSlider = it },
                            valueRange = 0f..150f,
                            modifier = Modifier.testTag("crowd_queue_slider")
                        )

                        OutlinedTextField(
                            value = reportComment,
                            onValueChange = { reportComment = it.take(160) },
                            label = { Text("Helpful queue note (optional)") },
                            placeholder = { Text("e.g., Tokens issued for first 50 cylinders") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crowd_comment_input")
                        )

                        Button(
                            onClick = {
                                onSubmitReport(reportedStatus, estCount, reportComment)
                                reportComment = ""
                                showReportForm = false
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("submit_crowd_report_button")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.confirmStockAccurate)
                        }
                    }
                }
            }

            // Recent Community Reports Feed
            if (reports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recent Community Reports (${reports.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reports.take(5).forEach { rep ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = rep.reporterAlias,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        if (rep.isProximityVerified) {
                                            Surface(
                                                color = ext.statusAvailableContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "GPS Near Shop (2x)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ext.onStatusAvailableContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "· ${formatRelativeTimestamp(rep.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${rep.reportedStatus} · ~${rep.queueLengthEstimate} in queue",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (rep.comment.isNotBlank()) {
                                        Text(
                                            text = rep.comment,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onFlagReport(rep.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("flag_report_${rep.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Flag,
                                        contentDescription = "Flag inaccurate report",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Official Stock Transition Audit Logs
            if (stockLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Official Dealer Dispatch Log",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    stockLogs.take(4).forEach { log ->
                        Text(
                            text = "• ${formatRelativeTimestamp(log.createdAt)}: ${log.previousStatus} → ${log.newStatus} (12.5kg: ${log.stock12_5kg}, 5kg: ${log.stock5kg}, 2.3kg: ${log.stock2_3kg}) — ${log.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
