package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CylinderSizeFilter
import com.example.data.model.DistributorWithDistance
import com.example.data.model.GasBrand
import com.example.data.model.QueueReport
import com.example.data.model.SriLankaDistricts
import com.example.data.model.StockLog
import com.example.data.model.WatchlistItem
import com.example.ui.components.DistributorStationCard
import com.example.ui.components.SriLankaSpatialMapCanvas
import com.example.ui.i18n.LocalStrings
import com.example.ui.viewmodel.ExploreViewMode
import com.example.ui.viewmodel.FilterState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerExploreScreen(
    distributorsState: UiState<List<DistributorWithDistance>>,
    filterState: FilterState,
    watchlist: List<WatchlistItem>,
    selectedStation: DistributorWithDistance?,
    selectedStationReports: List<QueueReport>,
    selectedStationLogs: List<StockLog>,
    isSubmittingAction: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleBrand: (GasBrand?) -> Unit,
    onSelectCylinderSize: (CylinderSizeFilter) -> Unit,
    onSelectDistrict: (String?) -> Unit,
    onToggleAvailableOnly: () -> Unit,
    onChangeViewMode: (ExploreViewMode) -> Unit,
    onRequestLiveGps: () -> Unit,
    onSelectStation: (String?) -> Unit,
    onToggleWatchSelected: () -> Unit,
    onSubmitCrowdReport: (String, Int, String) -> Unit,
    onFlagReport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var districtMenuExpanded by remember { mutableStateOf(false) }
    val watchedIds = remember(watchlist) { watchlist.map { it.stationId }.toSet() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 760.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left / Main Explorer Pane
            Column(
                modifier = Modifier
                    .weight(if (isWideLayout && selectedStation != null) 0.55f else 1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar + Live GPS Trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = filterState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = stringResource(id = R.string.cd_clear_search)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("station_search_input")
                    )

                    FilledTonalIconButton(
                        onClick = onRequestLiveGps,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("gps_locate_button")
                    ) {
                        Icon(
                            imageVector = if (filterState.isLiveGpsActive) Icons.Filled.MyLocation
                            else Icons.Filled.LocationSearching,
                            contentDescription = stringResource(id = R.string.cd_gps_locate),
                            tint = if (filterState.isLiveGpsActive) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Filter Strip (District, Brand, Cylinder Size, In-Stock Only)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { districtMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("district_filter_button")
                        ) {
                            val selectedLabel = filterState.selectedDistrict?.let { name ->
                                strings.districtLabel(SriLankaDistricts.findByName(name))
                            } ?: strings.allDistricts
                            Text(selectedLabel)
                        }
                        DropdownMenu(
                            expanded = districtMenuExpanded,
                            onDismissRequest = { districtMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.allDistricts) },
                                onClick = {
                                    onSelectDistrict(null)
                                    districtMenuExpanded = false
                                }
                            )
                            SriLankaDistricts.all.forEach { dist ->
                                DropdownMenuItem(
                                    text = { Text("${strings.districtLabel(dist)} (${dist.name})") },
                                    onClick = {
                                        onSelectDistrict(dist.name)
                                        districtMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    FilterChip(
                        selected = filterState.availableOnly,
                        onClick = onToggleAvailableOnly,
                        leadingIcon = if (filterState.availableOnly) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        label = { Text(strings.availableOnly) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_available_only")
                    )

                    FilterChip(
                        selected = filterState.selectedBrand == GasBrand.LITRO,
                        onClick = { onToggleBrand(GasBrand.LITRO) },
                        label = { Text("Litro (Blue)") },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_brand_litro")
                    )

                    FilterChip(
                        selected = filterState.selectedBrand == GasBrand.LAUGFS,
                        onClick = { onToggleBrand(GasBrand.LAUGFS) },
                        label = { Text("LAUGFS (Yellow)") },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("filter_brand_laugfs")
                    )

                    CylinderSizeFilter.entries.forEach { sizeFilter ->
                        if (sizeFilter != CylinderSizeFilter.ALL) {
                            FilterChip(
                                selected = filterState.selectedSize == sizeFilter,
                                onClick = {
                                    onSelectCylinderSize(
                                        if (filterState.selectedSize == sizeFilter) CylinderSizeFilter.ALL
                                        else sizeFilter
                                    )
                                },
                                label = { Text(sizeFilter.label) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("filter_size_${sizeFilter.id.lowercase()}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Map vs List Segmented Switcher
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("view_mode_toggle")
                ) {
                    SegmentedButton(
                        selected = filterState.viewMode == ExploreViewMode.MAP,
                        onClick = { onChangeViewMode(ExploreViewMode.MAP) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(Icons.Filled.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("mode_map_button")
                    ) {
                        Text(strings.mapMode)
                    }
                    SegmentedButton(
                        selected = filterState.viewMode == ExploreViewMode.LIST,
                        onClick = { onChangeViewMode(ExploreViewMode.LIST) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("mode_list_button")
                    ) {
                        Text(strings.listMode)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Body
                when (distributorsState) {
                    is UiState.Loading -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = distributorsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    is UiState.Success -> {
                        val stations = distributorsState.data
                        if (stations.isEmpty()) {
                            EmptyStationsCard()
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("stations_lazy_list")
                            ) {
                                if (filterState.viewMode == ExploreViewMode.MAP) {
                                    item(key = "spatial_map_header") {
                                        SriLankaSpatialMapCanvas(
                                            stations = stations,
                                            referenceLat = filterState.referenceLat,
                                            referenceLon = filterState.referenceLon,
                                            selectedStationId = selectedStation?.distributor?.id,
                                            onSelectStation = { onSelectStation(it) },
                                            modifier = Modifier.height(275.dp)
                                        )
                                    }
                                }

                                item(key = "results_count_header") {
                                    Text(
                                        text = "${stations.size} LPG Outlets Matched · Sorted by Proximity",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                items(
                                    items = stations,
                                    key = { it.distributor.id }
                                ) { stationItem ->
                                    DistributorStationCard(
                                        item = stationItem,
                                        isWatched = stationItem.distributor.id in watchedIds,
                                        isSelected = selectedStation?.distributor?.id == stationItem.distributor.id,
                                        onClick = { onSelectStation(stationItem.distributor.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Canonical Side-by-Side Detail Pane on Wide Tablets / Foldables
            if (isWideLayout && selectedStation != null) {
                Spacer(modifier = Modifier.width(12.dp))
                StationDetailPane(
                    stationItem = selectedStation,
                    isWatched = selectedStation.distributor.id in watchedIds,
                    reports = selectedStationReports,
                    stockLogs = selectedStationLogs,
                    isSubmitting = isSubmittingAction,
                    enableBackHandler = true,
                    onClose = { onSelectStation(null) },
                    onToggleWatch = onToggleWatchSelected,
                    onSubmitReport = onSubmitCrowdReport,
                    onFlagReport = onFlagReport,
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .padding(end = 16.dp, top = 8.dp, bottom = 12.dp)
                )
            }
        }

        // Modal Bottom Sheet Detail View on Compact Handhelds
        if (!isWideLayout && selectedStation != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onSelectStation(null) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                StationDetailPane(
                    stationItem = selectedStation,
                    isWatched = selectedStation.distributor.id in watchedIds,
                    reports = selectedStationReports,
                    stockLogs = selectedStationLogs,
                    isSubmitting = isSubmittingAction,
                    enableBackHandler = false,
                    onClose = { onSelectStation(null) },
                    onToggleWatch = onToggleWatchSelected,
                    onSubmitReport = onSubmitCrowdReport,
                    onFlagReport = onFlagReport,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EmptyStationsCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No matching LPG outlets found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try clearing the cylinder size, district, or 'In Stock Only' filter to view incoming scheduled dispatches.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
