package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Distributor
import com.example.data.model.GasBrand
import com.example.data.model.QueueLevel
import com.example.data.model.SriLankaDistricts
import com.example.data.model.StockStatus
import com.example.ui.components.BrandBadge
import com.example.ui.components.StockStatusBadge
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalExtendedColors
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellerDashboardScreen(
    ownedStationsState: UiState<List<Distributor>>,
    isSubmitting: Boolean,
    liveLat: Double?,
    liveLon: Double?,
    onBackToExplore: () -> Unit,
    onRequestGps: () -> Unit,
    onRegisterOutlet: (
        name: String,
        brand: GasBrand,
        district: String,
        town: String,
        address: String,
        phone: String,
        lat: Double,
        lon: Double,
        s12: Int,
        s5: Int,
        s2: Int,
        status: StockStatus,
        arrivalNote: String,
        onComplete: () -> Unit
    ) -> Unit,
    onPublishUpdate: (
        station: Distributor,
        newStatus: StockStatus,
        s12: Int,
        s5: Int,
        s2: Int,
        arrivalNote: String,
        queueLevel: QueueLevel,
        estimatedQueueCount: Int
    ) -> Unit,
    onMarkSoldOut: (Distributor) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToExplore() }

    val strings = LocalStrings.current
    val ext = LocalExtendedColors.current
    var showRegisterDialog by remember { mutableStateOf(false) }
    var selectedOwnedIndex by remember { mutableIntStateOf(0) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.sellerDashboardTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Broadcast real-time cylinder counts, lorry ETAs & queue status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = { showRegisterDialog = true },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("open_register_outlet_button")
                ) {
                    Icon(Icons.Filled.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.registerNewOutlet)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (ownedStationsState) {
                is UiState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = ownedStationsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    val stations = ownedStationsState.data
                    if (stations.isEmpty()) {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Dealership Registered Yet",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Are you an authorized Litro or LAUGFS retailer? Register your outlet in 30 seconds to broadcast incoming cylinder trucks and stop unnecessary street queues.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showRegisterDialog = true },
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("empty_register_outlet_button")
                                ) {
                                    Icon(Icons.Filled.AddBusiness, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.registerNewOutlet)
                                }
                            }
                        }
                    } else {
                        val safeIdx = selectedOwnedIndex.coerceIn(0, stations.lastIndex)
                        val activeStation = stations[safeIdx]

                        // Outlet Switcher Chips if Seller manages multiple stations
                        if (stations.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                stations.forEachIndexed { idx, st ->
                                    FilterChip(
                                        selected = idx == safeIdx,
                                        onClick = { selectedOwnedIndex = idx },
                                        label = { Text(st.name) },
                                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        SellerStationEditorCard(
                            station = activeStation,
                            isSubmitting = isSubmitting,
                            soldOutColor = ext.statusSoldOut,
                            onPublishUpdate = onPublishUpdate,
                            onMarkSoldOut = onMarkSoldOut
                        )
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        RegisterOutletDialog(
            liveLat = liveLat,
            liveLon = liveLon,
            isSubmitting = isSubmitting,
            onRequestGps = onRequestGps,
            onDismiss = { showRegisterDialog = false },
            onSubmit = { name, brand, district, town, address, phone, lat, lon, s12, s5, s2, status, note ->
                onRegisterOutlet(
                    name, brand, district, town, address, phone, lat, lon, s12, s5, s2, status, note
                ) {
                    showRegisterDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SellerStationEditorCard(
    station: Distributor,
    isSubmitting: Boolean,
    soldOutColor: androidx.compose.ui.graphics.Color,
    onPublishUpdate: (Distributor, StockStatus, Int, Int, Int, String, QueueLevel, Int) -> Unit,
    onMarkSoldOut: (Distributor) -> Unit
) {
    val strings = LocalStrings.current

    var status by remember(station.id, station.updatedAt) { mutableStateOf(station.statusEnum) }
    var stock12 by remember(station.id, station.updatedAt) { mutableIntStateOf(station.stock12_5kg.toInt()) }
    var stock5 by remember(station.id, station.updatedAt) { mutableIntStateOf(station.stock5kg.toInt()) }
    var stock2 by remember(station.id, station.updatedAt) { mutableIntStateOf(station.stock2_3kg.toInt()) }
    var arrivalNote by remember(station.id, station.updatedAt) { mutableStateOf(station.nextArrivalNote) }
    var queueLevel by remember(station.id, station.updatedAt) { mutableStateOf(station.queueLevelEnum) }

    LaunchedEffect(station.status, station.stock12_5kg, station.stock5kg, station.stock2_3kg) {
        status = station.statusEnum
        stock12 = station.stock12_5kg.toInt()
        stock5 = station.stock5kg.toInt()
        stock2 = station.stock2_3kg.toInt()
    }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(18.dp)
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
                    Text(
                        text = if (station.isVerified) strings.verifiedDealer else strings.unverifiedDealer,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (station.isVerified) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StockStatusBadge(status = station.statusEnum)
            }

            Text(
                text = "${station.name} · ${station.town}, ${station.district}",
                style = MaterialTheme.typography.titleLarge
            )

            // 1. Dispatch Lifecycle Selector
            Text(
                text = "1. Stock & Lorry Lifecycle State",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StockStatus.entries.forEach { candidate ->
                    FilterChip(
                        selected = status == candidate,
                        onClick = { status = candidate },
                        label = { Text(strings.statusLabel(candidate)) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("seller_status_${candidate.id.lowercase()}")
                    )
                }
            }

            // 2. Rapid One-Handed Cylinder Steppers
            Text(
                text = "2. Cylinder Quantities (12.5kg · 5kg · 2.3kg)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            SellerCylinderStepperRow(
                label = "12.5 kg Domestic",
                count = stock12,
                tagPrefix = "s12",
                onAdjust = { delta ->
                    stock12 = (stock12 + delta).coerceIn(0, 10000)
                    if (stock12 > 0 && status == StockStatus.SOLD_OUT) status = StockStatus.AVAILABLE
                }
            )
            SellerCylinderStepperRow(
                label = "5.0 kg Medium",
                count = stock5,
                tagPrefix = "s5",
                onAdjust = { delta ->
                    stock5 = (stock5 + delta).coerceIn(0, 10000)
                    if (stock5 > 0 && status == StockStatus.SOLD_OUT) status = StockStatus.AVAILABLE
                }
            )
            SellerCylinderStepperRow(
                label = "2.3 kg Compact",
                count = stock2,
                tagPrefix = "s2",
                onAdjust = { delta ->
                    stock2 = (stock2 + delta).coerceIn(0, 10000)
                    if (stock2 > 0 && status == StockStatus.SOLD_OUT) status = StockStatus.AVAILABLE
                }
            )

            // 3. Expected Arrival ETA / Dispatch Note
            OutlinedTextField(
                value = arrivalNote,
                onValueChange = { arrivalNote = it.take(120) },
                label = { Text("Expected Lorry ETA / Customer Notice") },
                placeholder = { Text("e.g., Bowser arriving 2:30 PM · 1 cylinder per household") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seller_arrival_note_input")
            )

            // 4. Official Queue Status
            Text(
                text = "3. Current Queue Outside Shop",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QueueLevel.entries.forEach { qLevel ->
                    FilterChip(
                        selected = queueLevel == qLevel,
                        onClick = { queueLevel = qLevel },
                        label = { Text(strings.queueLabel(qLevel)) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }
            }

            // Primary Publish Button
            Button(
                onClick = {
                    val estCount = when (queueLevel) {
                        QueueLevel.NONE -> 0
                        QueueLevel.SHORT -> 10
                        QueueLevel.MODERATE -> 30
                        QueueLevel.LONG -> 75
                    }
                    onPublishUpdate(
                        station,
                        status,
                        stock12,
                        stock5,
                        stock2,
                        arrivalNote,
                        queueLevel,
                        estCount
                    )
                },
                enabled = !isSubmitting,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 54.dp)
                    .testTag("publish_stock_button")
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.publishStockUpdate, fontWeight = FontWeight.Bold)
            }

            // High-Contrast 1-Tap SOLD OUT Emergency Button
            Button(
                onClick = { onMarkSoldOut(station) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = soldOutColor,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("mark_sold_out_button")
            ) {
                Icon(Icons.Filled.Block, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.markSoldOutNow, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SellerCylinderStepperRow(
    label: String,
    count: Int,
    tagPrefix: String,
    onAdjust: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "$count cylinders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepperMiniButton("-5", "${tagPrefix}_minus_5") { onAdjust(-5) }
                StepperMiniButton("-1", "${tagPrefix}_minus_1") { onAdjust(-1) }
                StepperMiniButton("+10", "${tagPrefix}_plus_10") { onAdjust(10) }
                StepperMiniButton("+50", "${tagPrefix}_plus_50") { onAdjust(50) }
            }
        }
    }
}

@Composable
private fun StepperMiniButton(label: String, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .testTag(tag)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RegisterOutletDialog(
    liveLat: Double?,
    liveLon: Double?,
    isSubmitting: Boolean,
    onRequestGps: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        brand: GasBrand,
        district: String,
        town: String,
        address: String,
        phone: String,
        lat: Double,
        lon: Double,
        s12: Int,
        s5: Int,
        s2: Int,
        status: StockStatus,
        note: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf(GasBrand.LITRO) }
    var district by remember { mutableStateOf("Colombo") }
    var districtMenu by remember { mutableStateOf(false) }
    var town by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+94112") }
    var initial12 by remember { mutableStateOf("40") }
    var initial5 by remember { mutableStateOf("15") }
    var initial2 by remember { mutableStateOf("10") }
    var useShopGps by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register LPG Dealership") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = brand == GasBrand.LITRO,
                        onClick = { brand = GasBrand.LITRO },
                        label = { Text("Litro (Blue)") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                    FilterChip(
                        selected = brand == GasBrand.LAUGFS,
                        onClick = { brand = GasBrand.LAUGFS },
                        label = { Text("LAUGFS (Yellow)") },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shop / Agency Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_shop_name_input")
                )

                Box {
                    OutlinedButton(
                        onClick = { districtMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("District: $district")
                    }
                    DropdownMenu(
                        expanded = districtMenu,
                        onDismissRequest = { districtMenu = false }
                    ) {
                        SriLankaDistricts.all.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.name) },
                                onClick = {
                                    district = d.name
                                    districtMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = town,
                    onValueChange = { town = it },
                    label = { Text("Town / Suburb (e.g., Maharagama)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_town_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_address_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Shop Hotline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initial12,
                        onValueChange = { initial12 = it.filter(Char::isDigit) },
                        label = { Text("12.5kg") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = initial5,
                        onValueChange = { initial5 = it.filter(Char::isDigit) },
                        label = { Text("5.0kg") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = initial2,
                        onValueChange = { initial2 = it.filter(Char::isDigit) },
                        label = { Text("2.3kg") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = {
                        useShopGps = true
                        onRequestGps()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (useShopGps && liveLat != null) "GPS Pin Ready (${String.format("%.4f", liveLat)})"
                        else "Pin Exact Shop GPS (or use District Center)"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val distObj = SriLankaDistricts.findByName(district)
                    val chosenLat = if (useShopGps && liveLat != null && liveLat in 5.7..10.1) liveLat else distObj.centerLat + 0.012
                    val chosenLon = if (useShopGps && liveLon != null && liveLon in 79.4..82.1) liveLon else distObj.centerLon + 0.012
                    val s12 = initial12.toIntOrNull() ?: 0
                    val s5 = initial5.toIntOrNull() ?: 0
                    val s2 = initial2.toIntOrNull() ?: 0
                    val st = if (s12 + s5 + s2 > 0) StockStatus.AVAILABLE else StockStatus.SCHEDULED
                    onSubmit(
                        name, brand, district, town, address, phone,
                        chosenLat, chosenLon, s12, s5, s2, st, "Newly registered dealer"
                    )
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("confirm_register_outlet_button")
            ) {
                Text("Register")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
