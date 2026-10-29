package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppLanguage
import com.example.data.model.CylinderSizeFilter
import com.example.data.model.Distributor
import com.example.data.model.DistributorWithDistance
import com.example.data.model.GasBrand
import com.example.data.model.InAppAlert
import com.example.data.model.QueueLevel
import com.example.data.model.QueueReport
import com.example.data.model.SriLankaDistricts
import com.example.data.model.StockLog
import com.example.data.model.StockStatus
import com.example.data.model.WatchlistItem
import com.example.data.remote.friendlyFirestoreErrorMessage
import com.example.data.repository.GasRepository
import com.example.util.GeoUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class ExploreViewMode {
    MAP,
    LIST
}

enum class AppWorkspaceTab {
    EXPLORE,
    SELLER,
    ADMIN
}

data class FilterState(
    val searchQuery: String = "",
    val selectedBrand: GasBrand? = null,
    val selectedSize: CylinderSizeFilter = CylinderSizeFilter.ALL,
    val selectedDistrict: String? = null,
    val availableOnly: Boolean = false,
    val viewMode: ExploreViewMode = ExploreViewMode.MAP,
    val referenceLat: Double = SriLankaDistricts.DEFAULT_COLOMBO_LAT,
    val referenceLon: Double = SriLankaDistricts.DEFAULT_COLOMBO_LON,
    val isLiveGpsActive: Boolean = false,
    val deviceRawLat: Double? = null,
    val deviceRawLon: Double? = null
)

class GasTrackerViewModel(
    private val repository: GasRepository,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String?,
    private val isEmailVerified: Boolean
) : ViewModel() {

    private val _activeTab = MutableStateFlow(AppWorkspaceTab.EXPLORE)
    val activeTab: StateFlow<AppWorkspaceTab> = _activeTab.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.EN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _selectedStationId = MutableStateFlow<String?>(null)
    val selectedStationId: StateFlow<String?> = _selectedStationId.asStateFlow()

    private val _isSubmittingAction = MutableStateFlow(false)
    val isSubmittingAction: StateFlow<Boolean> = _isSubmittingAction.asStateFlow()

    private val _inAppAlerts = MutableStateFlow<List<InAppAlert>>(emptyList())
    val inAppAlerts: StateFlow<List<InAppAlert>> = _inAppAlerts.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _restockAlertEvents = MutableSharedFlow<InAppAlert>(extraBufferCapacity = 4)
    val restockAlertEvents = _restockAlertEvents.asSharedFlow()

    // Anti-spam per-station crowd report cooldown tracker (15 minutes)
    private val lastReportTimestampByStation = mutableMapOf<String, Long>()

    // Two-tier Kotlin Flow error handling on public distributors stream
    val publicDistributorsState: StateFlow<UiState<List<Distributor>>> =
        repository.observePublicDistributors()
            .map<List<Distributor>, UiState<List<Distributor>>> { UiState.Success(it) }
            .catch { err ->
                Log.w(TAG, "Error observing public distributors", err)
                emit(UiState.Error(friendlyFirestoreErrorMessage(err, "Failed to load LPG stations")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    // Owned stations for Seller Dashboard
    val ownedDistributorsState: StateFlow<UiState<List<Distributor>>> =
        repository.observeOwnedDistributors(currentUserId)
            .map<List<Distributor>, UiState<List<Distributor>>> { list ->
                UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: 0L })
            }
            .catch { err ->
                Log.w(TAG, "Error observing owned distributors", err)
                emit(UiState.Error(friendlyFirestoreErrorMessage(err, "Failed to load your outlets")))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    // User's restock watchlist subscriptions
    val watchlistState: StateFlow<List<WatchlistItem>> =
        repository.observeWatchlist(currentUserId)
            .catch { err ->
                Log.w(TAG, "Error observing watchlist", err)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    // Filtered + Haversine distance-sorted distributors for Consumer Explore view
    val filteredDistributors: StateFlow<UiState<List<DistributorWithDistance>>> =
        combine(publicDistributorsState, _filterState) { pubState, filters ->
            when (pubState) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> UiState.Error(pubState.message)
                is UiState.Success -> {
                    val query = filters.searchQuery.trim().lowercase()
                    val matched = pubState.data.filter { station ->
                        val brandOk = filters.selectedBrand == null || station.brandEnum == filters.selectedBrand
                        val sizeOk = station.matchesCylinderFilter(filters.selectedSize)
                        val districtOk = filters.selectedDistrict == null ||
                            station.district.equals(filters.selectedDistrict, ignoreCase = true)
                        val availOk = !filters.availableOnly ||
                            (station.statusEnum == StockStatus.AVAILABLE && station.totalCylinders > 0L)
                        val searchOk = query.isEmpty() ||
                            station.name.lowercase().contains(query) ||
                            station.town.lowercase().contains(query) ||
                            station.district.lowercase().contains(query) ||
                            station.address.lowercase().contains(query)
                        brandOk && sizeOk && districtOk && availOk && searchOk
                    }.map { station ->
                        val distKm = GeoUtils.haversineDistanceKm(
                            filters.referenceLat,
                            filters.referenceLon,
                            station.latitude,
                            station.longitude
                        )
                        DistributorWithDistance(station, distKm)
                    }.sortedWith(
                        compareBy<DistributorWithDistance> {
                            // Prioritize Available & In Transit slightly when practically equal distance, otherwise pure distance
                            it.distanceKm
                        }
                    )
                    UiState.Success(matched)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val selectedStation: StateFlow<DistributorWithDistance?> =
        combine(publicDistributorsState, _selectedStationId, _filterState) { pubState, selectedId, filters ->
            if (selectedId == null || pubState !is UiState.Success) return@combine null
            val found = pubState.data.firstOrNull { it.id == selectedId } ?: return@combine null
            val distKm = GeoUtils.haversineDistanceKm(
                filters.referenceLat,
                filters.referenceLon,
                found.latitude,
                found.longitude
            )
            DistributorWithDistance(found, distKm)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedStationReports: StateFlow<List<QueueReport>> =
        _selectedStationId.flatMapLatest { stationId ->
            if (stationId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeStationReports(stationId)
                    .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(15) }
                    .catch { err ->
                        Log.w(TAG, "Error observing station reports", err)
                        emit(emptyList())
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedStationLogs: StateFlow<List<StockLog>> =
        _selectedStationId.flatMapLatest { stationId ->
            if (stationId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeStationStockLogs(stationId)
                    .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(8) }
                    .catch { err ->
                        Log.w(TAG, "Error observing station stock logs", err)
                        emit(emptyList())
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val flaggedReportsState: StateFlow<List<QueueReport>> =
        _isAdmin.flatMapLatest { admin ->
            if (!admin) {
                flowOf(emptyList())
            } else {
                repository.observeFlaggedReportsForAdmin()
                    .map { list -> list.sortedByDescending { it.updatedAt?.seconds ?: 0L } }
                    .catch { err ->
                        Log.w(TAG, "Error observing flagged reports", err)
                        emit(emptyList())
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        initializeUserAndSeedIfEmpty()
        observeWatchlistRestockTransitions()
    }

    private fun initializeUserAndSeedIfEmpty() {
        viewModelScope.launch {
            val adminVerified = repository.checkIsAdmin(
                userId = currentUserId,
                userEmail = currentUserEmail,
                isEmailVerified = isEmailVerified
            )
            _isAdmin.value = adminVerified

            val profileRes = repository.getUserProfile(currentUserId)
            val existingProfile = profileRes.getOrNull()
            if (existingProfile != null) {
                _language.value = AppLanguage.fromCode(existingProfile.preferredLanguage)
            } else {
                repository.upsertUserProfile(currentUserName, _language.value.code)
            }

            repository.seedSriLankaOutletsIfEmpty(isAdminUser = adminVerified)
        }
    }

    private fun observeWatchlistRestockTransitions() {
        viewModelScope.launch {
            combine(publicDistributorsState, watchlistState) { pubState, watchlist ->
                if (pubState !is UiState.Success || watchlist.isEmpty()) return@combine emptyList()
                val stationsById = pubState.data.associateBy { it.id }
                watchlist.mapNotNull { item ->
                    val liveStation = stationsById[item.stationId] ?: return@mapNotNull null
                    val prevStatus = StockStatus.fromId(item.lastKnownStatus)
                    val currStatus = liveStation.statusEnum
                    val matchesDesiredSize = when (item.desiredSize) {
                        "12_5KG" -> liveStation.stock12_5kg > 0L
                        "5KG" -> liveStation.stock5kg > 0L
                        "2_3KG" -> liveStation.stock2_3kg > 0L
                        else -> liveStation.totalCylinders > 0L || currStatus == StockStatus.IN_TRANSIT
                    }
                    if (currStatus != prevStatus &&
                        (currStatus == StockStatus.AVAILABLE || currStatus == StockStatus.IN_TRANSIT) &&
                        matchesDesiredSize
                    ) {
                        Pair(item, liveStation)
                    } else if (currStatus != prevStatus && currStatus == StockStatus.SOLD_OUT) {
                        // Silently sync lastKnownStatus to SOLD_OUT so future restock fires alert again
                        repository.updateWatchlistLastStatus(liveStation.id, currStatus)
                        null
                    } else {
                        null
                    }
                }
            }.collect { triggeredPairs ->
                for ((watchItem, station) in triggeredPairs) {
                    repository.updateWatchlistLastStatus(station.id, station.statusEnum)
                    val summary = if (station.statusEnum == StockStatus.AVAILABLE) {
                        "12.5kg: ${station.stock12_5kg} · 5kg: ${station.stock5kg} · 2.3kg: ${station.stock2_3kg} now in stock!"
                    } else {
                        "Delivery truck en route (${station.nextArrivalNote.ifBlank { "arriving soon" }})"
                    }
                    val alert = InAppAlert(
                        id = UUID.randomUUID().toString(),
                        stationId = station.id,
                        stationName = station.name,
                        brand = station.brandEnum,
                        newStatus = station.statusEnum,
                        message = summary,
                        timestampMillis = System.currentTimeMillis()
                    )
                    _inAppAlerts.update { current -> (listOf(alert) + current).take(20) }
                    _restockAlertEvents.tryEmit(alert)
                }
            }
        }
    }

    fun selectTab(tab: AppWorkspaceTab) {
        _activeTab.value = tab
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        viewModelScope.launch {
            repository.upsertUserProfile(currentUserName, lang.code)
        }
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun toggleBrandFilter(brand: GasBrand?) {
        _filterState.update {
            it.copy(selectedBrand = if (it.selectedBrand == brand) null else brand)
        }
    }

    fun setCylinderSizeFilter(size: CylinderSizeFilter) {
        _filterState.update { it.copy(selectedSize = size) }
    }

    fun setDistrictFilter(districtName: String?) {
        _filterState.update { current ->
            if (districtName == null) {
                current.copy(selectedDistrict = null)
            } else {
                val district = SriLankaDistricts.findByName(districtName)
                current.copy(
                    selectedDistrict = district.name,
                    referenceLat = if (current.isLiveGpsActive) current.referenceLat else district.centerLat,
                    referenceLon = if (current.isLiveGpsActive) current.referenceLon else district.centerLon
                )
            }
        }
    }

    fun toggleAvailableOnly() {
        _filterState.update { it.copy(availableOnly = !it.availableOnly) }
    }

    fun setExploreViewMode(mode: ExploreViewMode) {
        _filterState.update { it.copy(viewMode = mode) }
    }

    fun onDeviceGpsUpdated(lat: Double, lon: Double, insideSriLanka: Boolean) {
        _filterState.update { current ->
            if (insideSriLanka) {
                current.copy(
                    referenceLat = lat,
                    referenceLon = lon,
                    isLiveGpsActive = true,
                    deviceRawLat = lat,
                    deviceRawLon = lon
                )
            } else {
                // Keep Sri Lanka map centered on Colombo/selected district while storing raw GPS
                current.copy(
                    isLiveGpsActive = true,
                    deviceRawLat = lat,
                    deviceRawLon = lon
                )
            }
        }
        val msg = if (insideSriLanka) {
            "Live GPS locked inside Sri Lanka — sorting nearby outlets."
        } else {
            "GPS detected outside Sri Lanka — centering radar on Colombo reference."
        }
        _snackbarMessages.tryEmit(msg)
    }

    fun selectStation(stationId: String?) {
        _selectedStationId.value = stationId
    }

    fun toggleWatchStation(station: Distributor) {
        val currentlyWatched = watchlistState.value.any { it.stationId == station.id }
        val desiredSize = _filterState.value.selectedSize
        viewModelScope.launch {
            val res = repository.toggleWatchlist(station, desiredSize, currentlyWatched)
            res.fold(
                onSuccess = { nowWatched ->
                    _snackbarMessages.tryEmit(
                        if (nowWatched) "Restock alerts enabled for ${station.name}"
                        else "Removed ${station.name} from watchlist"
                    )
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Could not update watchlist"))
                }
            )
        }
    }

    fun submitCrowdReport(
        station: Distributor,
        reportedStatus: String,
        queueCount: Int,
        comment: String
    ) {
        val now = System.currentTimeMillis()
        val lastAt = lastReportTimestampByStation[station.id] ?: 0L
        val cooldownRemainingSec = ((COOLDOWN_MS - (now - lastAt)) / 1000L).toInt()
        if (cooldownRemainingSec > 0) {
            val mins = (cooldownRemainingSec / 60) + 1
            _snackbarMessages.tryEmit("Anti-spam cooldown: please wait ~$mins min before reporting again for this station.")
            return
        }

        _isSubmittingAction.value = true
        viewModelScope.launch {
            val filters = _filterState.value
            val res = repository.submitCrowdQueueReport(
                station = station,
                reporterAlias = currentUserName.substringBefore(" ").ifBlank { "Citizen" },
                reportedStatus = reportedStatus,
                queueLengthEstimate = queueCount,
                comment = comment,
                userLatitude = filters.deviceRawLat ?: filters.referenceLat,
                userLongitude = filters.deviceRawLon ?: filters.referenceLon
            )
            _isSubmittingAction.value = false
            res.fold(
                onSuccess = {
                    lastReportTimestampByStation[station.id] = System.currentTimeMillis()
                    _snackbarMessages.tryEmit("Thank you! Your live queue report has been published.")
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Failed to submit report"))
                }
            )
        }
    }

    fun flagReport(reportId: String) {
        viewModelScope.launch {
            repository.flagQueueReport(reportId).fold(
                onSuccess = { _snackbarMessages.tryEmit("Report flagged for moderator review.") },
                onFailure = { err -> _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Could not flag report")) }
            )
        }
    }

    // --- Seller Actions ---

    fun registerSellerOutlet(
        name: String,
        brand: GasBrand,
        district: String,
        town: String,
        address: String,
        phone: String,
        latitude: Double,
        longitude: Double,
        s12: Int,
        s5: Int,
        s2: Int,
        status: StockStatus,
        arrivalNote: String,
        onComplete: () -> Unit
    ) {
        if (name.trim().length < 2 || town.trim().length < 2 || address.trim().length < 3) {
            _snackbarMessages.tryEmit("Please enter valid outlet name, town, and street address.")
            return
        }
        _isSubmittingAction.value = true
        viewModelScope.launch {
            val res = repository.registerDistributor(
                name = name,
                ownerName = currentUserName,
                brand = brand,
                district = district,
                town = town,
                address = address,
                phone = phone,
                latitude = latitude,
                longitude = longitude,
                initialStock12_5kg = s12,
                initialStock5kg = s5,
                initialStock2_3kg = s2,
                status = status,
                nextArrivalNote = arrivalNote,
                isVerifiedSeed = _isAdmin.value
            )
            _isSubmittingAction.value = false
            res.fold(
                onSuccess = {
                    _snackbarMessages.tryEmit("Dealership registered! Live on LankaGas map.")
                    onComplete()
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Failed to register dealership"))
                }
            )
        }
    }

    fun publishSellerUpdate(
        station: Distributor,
        newStatus: StockStatus,
        s12: Int,
        s5: Int,
        s2: Int,
        arrivalNote: String,
        queueLevel: QueueLevel,
        estimatedQueueCount: Int
    ) {
        _isSubmittingAction.value = true
        viewModelScope.launch {
            val res = repository.updateSellerStock(
                station = station,
                newStatus = newStatus,
                stock12_5kg = s12,
                stock5kg = s5,
                stock2_3kg = s2,
                nextArrivalNote = arrivalNote,
                queueLevel = queueLevel,
                estimatedQueueCount = estimatedQueueCount
            )
            _isSubmittingAction.value = false
            res.fold(
                onSuccess = { _snackbarMessages.tryEmit("Live stock & queue broadcast updated for ${station.name}!") },
                onFailure = { err -> _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Failed to update stock")) }
            )
        }
    }

    fun markSoldOutImmediately(station: Distributor) {
        _isSubmittingAction.value = true
        viewModelScope.launch {
            val res = repository.markStationSoldOut(station)
            _isSubmittingAction.value = false
            res.fold(
                onSuccess = { _snackbarMessages.tryEmit("${station.name} marked SOLD OUT.") },
                onFailure = { err -> _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Could not mark sold out")) }
            )
        }
    }

    // --- Admin Actions ---

    fun setStationVerification(stationId: String, verified: Boolean, suspendListing: Boolean = false) {
        if (!_isAdmin.value) {
            _snackbarMessages.tryEmit("Administrator verification required.")
            return
        }
        viewModelScope.launch {
            val visibility = if (suspendListing) "SUSPENDED" else "PUBLIC"
            repository.adminSetDistributorVerified(stationId, verified, visibility).fold(
                onSuccess = {
                    val action = when {
                        suspendListing -> "Outlet suspended"
                        verified -> "Dealer verified"
                        else -> "Verification badge revoked"
                    }
                    _snackbarMessages.tryEmit(action)
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Admin update failed"))
                }
            )
        }
    }

    fun moderateCrowdReport(reportId: String, dismiss: Boolean) {
        if (!_isAdmin.value) {
            _snackbarMessages.tryEmit("Administrator privileges required.")
            return
        }
        viewModelScope.launch {
            val targetState = if (dismiss) "DISMISSED" else "ACTIVE"
            repository.adminModerateReport(reportId, targetState).fold(
                onSuccess = {
                    _snackbarMessages.tryEmit(if (dismiss) "Report dismissed" else "Report restored to active")
                },
                onFailure = { err ->
                    _snackbarMessages.tryEmit(friendlyFirestoreErrorMessage(err, "Moderation update failed"))
                }
            )
        }
    }

    private companion object {
        const val TAG = "GasTrackerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val COOLDOWN_MS = 15 * 60 * 1000L
    }
}
