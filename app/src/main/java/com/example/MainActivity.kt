package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.AppLanguage
import com.example.data.repository.GasRepository
import com.example.ui.components.LanguageSwitcherPill
import com.example.ui.components.RestockAlertsDialog
import com.example.ui.i18n.LocalStrings
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.ConsumerExploreScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.LankaGasTheme
import com.example.ui.viewmodel.AppWorkspaceTab
import com.example.ui.viewmodel.GasTrackerViewModel
import com.example.util.LocationHelper
import com.example.util.RestockNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RestockNotificationHelper.ensureChannel(this)
        setContent {
            LankaGasAppRoot()
        }
    }
}

@Composable
fun LankaGasAppRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var unauthLanguage by rememberSaveable { mutableStateOf(AppLanguage.EN) }

    val user = currentUser
    if (user == null) {
        // Navigation-level Auth Gate: No Firestore ViewModels or queries instantiate while unauthenticated
        LankaGasTheme(language = unauthLanguage) {
            SignInScreen(
                currentLanguage = unauthLanguage,
                onLanguageSelected = { unauthLanguage = it },
                onAuthSuccess = { /* AuthStateListener updates currentUser reactively */ }
            )
        }
    } else {
        AuthenticatedWorkspace(user = user)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedWorkspace(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // Keyed by user.uid and initialized via viewModelFactory with named firestore_database_id
    val viewModel: GasTrackerViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GasTrackerViewModel(
                    repository = GasRepository(db),
                    currentUserId = user.uid,
                    currentUserName = user.displayName ?: user.email?.substringBefore("@") ?: "Citizen",
                    currentUserEmail = user.email,
                    isEmailVerified = user.isEmailVerified
                )
            }
        }
    )

    val language by viewModel.language.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val filteredDistributors by viewModel.filteredDistributors.collectAsStateWithLifecycle()
    val publicDistributors by viewModel.publicDistributorsState.collectAsStateWithLifecycle()
    val ownedDistributors by viewModel.ownedDistributorsState.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlistState.collectAsStateWithLifecycle()
    val selectedStation by viewModel.selectedStation.collectAsStateWithLifecycle()
    val selectedReports by viewModel.selectedStationReports.collectAsStateWithLifecycle()
    val selectedLogs by viewModel.selectedStationLogs.collectAsStateWithLifecycle()
    val flaggedReports by viewModel.flaggedReportsState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmittingAction.collectAsStateWithLifecycle()
    val inAppAlerts by viewModel.inAppAlerts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAlertsDialog by remember { mutableStateOf(false) }

    // Runtime Permission Launchers (Location + Android 13+ Notifications)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val coords = LocationHelper.fetchCurrentCoordinates(context)
                if (coords != null) {
                    viewModel.onDeviceGpsUpdated(coords.latitude, coords.longitude, coords.isInsideSriLanka)
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* User preference respected */ }

    val triggerGpsLookup: () -> Unit = {
        if (LocationHelper.hasLocationPermission(context)) {
            scope.launch {
                val coords = LocationHelper.fetchCurrentCoordinates(context)
                if (coords != null) {
                    viewModel.onDeviceGpsUpdated(coords.latitude, coords.longitude, coords.isInsideSriLanka)
                } else {
                    snackbarHostState.showSnackbar("GPS signal unavailable — using selected district center.")
                }
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.restockAlertEvents.collect { alert ->
            RestockNotificationHelper.postRestockNotification(
                context = context,
                stationId = alert.stationId,
                stationName = alert.stationName,
                brand = alert.brand,
                newStatus = alert.newStatus,
                summaryMessage = alert.message
            )
            snackbarHostState.showSnackbar("${alert.stationName}: ${alert.message}")
        }
    }

    LankaGasTheme(language = language) {
        val strings = LocalStrings.current

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 640.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        actions = {
                            LanguageSwitcherPill(
                                currentLanguage = language,
                                onLanguageSelected = { viewModel.setLanguage(it) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !RestockNotificationHelper.hasNotificationPermission(context)
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    showAlertsDialog = true
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_alerts_button")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (inAppAlerts.isNotEmpty() || watchlist.isNotEmpty()) {
                                            Badge {
                                                Text("${inAppAlerts.size.takeIf { it > 0 } ?: watchlist.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = stringResource(id = R.string.cd_notifications)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        onSignOutComplete = {}
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                                    contentDescription = stringResource(id = R.string.cd_sign_out)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                            NavigationBarItem(
                                selected = activeTab == AppWorkspaceTab.EXPLORE,
                                onClick = { viewModel.selectTab(AppWorkspaceTab.EXPLORE) },
                                icon = {
                                    Icon(
                                        if (activeTab == AppWorkspaceTab.EXPLORE) Icons.Filled.Explore
                                        else Icons.Outlined.Explore,
                                        contentDescription = strings.tabExplore
                                    )
                                },
                                label = { Text(strings.tabExplore) },
                                modifier = Modifier.testTag("nav_tab_explore")
                            )
                            NavigationBarItem(
                                selected = activeTab == AppWorkspaceTab.SELLER,
                                onClick = { viewModel.selectTab(AppWorkspaceTab.SELLER) },
                                icon = {
                                    Icon(
                                        if (activeTab == AppWorkspaceTab.SELLER) Icons.Filled.Storefront
                                        else Icons.Outlined.Storefront,
                                        contentDescription = strings.tabSeller
                                    )
                                },
                                label = { Text(strings.tabSeller) },
                                modifier = Modifier.testTag("nav_tab_seller")
                            )
                            NavigationBarItem(
                                selected = activeTab == AppWorkspaceTab.ADMIN,
                                onClick = { viewModel.selectTab(AppWorkspaceTab.ADMIN) },
                                icon = {
                                    Icon(
                                        if (activeTab == AppWorkspaceTab.ADMIN) Icons.Filled.AdminPanelSettings
                                        else Icons.Outlined.AdminPanelSettings,
                                        contentDescription = strings.tabAdmin
                                    )
                                },
                                label = { Text(strings.tabAdmin) },
                                modifier = Modifier.testTag("nav_tab_admin")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("side_navigation_rail")
                        ) {
                            NavigationRailItem(
                                selected = activeTab == AppWorkspaceTab.EXPLORE,
                                onClick = { viewModel.selectTab(AppWorkspaceTab.EXPLORE) },
                                icon = {
                                    Icon(
                                        if (activeTab == AppWorkspaceTab.EXPLORE) Icons.Filled.Explore
                                        else Icons.Outlined.Explore,
                                        contentDescription = strings.tabExplore
                                    )
                                },
                                label = { Text(strings.tabExplore) }
                            )
                            NavigationRailItem(
                                selected = activeTab == AppWorkspaceTab.SELLER,
                                onClick = { viewModel.selectTab(AppWorkspaceTab.SELLER) },
                                icon = {
                                    Icon(
                                        if (activeTab == AppWorkspaceTab.SELLER) Icons.Filled.Storefront
                                        else Icons.Outlined.Storefront,
                                        contentDescription = strings.tabSeller
                                    )
                                },
                                label = { Text(strings.tabSeller) }
                            )
                            NavigationRailItem(
                                selected = activeTab == AppWorkspaceTab.ADMIN,
                                onClick = { viewModel.selectTab(AppWorkspaceTab.ADMIN) },
                                icon = {
                                    Icon(
                                        if (activeTab == AppWorkspaceTab.ADMIN) Icons.Filled.AdminPanelSettings
                                        else Icons.Outlined.AdminPanelSettings,
                                        contentDescription = strings.tabAdmin
                                    )
                                },
                                label = { Text(strings.tabAdmin) }
                            )
                        }
                    }

                    when (activeTab) {
                        AppWorkspaceTab.EXPLORE -> {
                            ConsumerExploreScreen(
                                distributorsState = filteredDistributors,
                                filterState = filterState,
                                watchlist = watchlist,
                                selectedStation = selectedStation,
                                selectedStationReports = selectedReports,
                                selectedStationLogs = selectedLogs,
                                isSubmittingAction = isSubmitting,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onToggleBrand = viewModel::toggleBrandFilter,
                                onSelectCylinderSize = viewModel::setCylinderSizeFilter,
                                onSelectDistrict = viewModel::setDistrictFilter,
                                onToggleAvailableOnly = viewModel::toggleAvailableOnly,
                                onChangeViewMode = viewModel::setExploreViewMode,
                                onRequestLiveGps = triggerGpsLookup,
                                onSelectStation = viewModel::selectStation,
                                onToggleWatchSelected = {
                                    selectedStation?.distributor?.let { st ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !RestockNotificationHelper.hasNotificationPermission(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        viewModel.toggleWatchStation(st)
                                    }
                                },
                                onSubmitCrowdReport = { repStatus, qCount, comment ->
                                    selectedStation?.distributor?.let { st ->
                                        viewModel.submitCrowdReport(st, repStatus, qCount, comment)
                                    }
                                },
                                onFlagReport = viewModel::flagReport,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AppWorkspaceTab.SELLER -> {
                            SellerDashboardScreen(
                                ownedStationsState = ownedDistributors,
                                isSubmitting = isSubmitting,
                                liveLat = filterState.deviceRawLat,
                                liveLon = filterState.deviceRawLon,
                                onBackToExplore = { viewModel.selectTab(AppWorkspaceTab.EXPLORE) },
                                onRequestGps = triggerGpsLookup,
                                onRegisterOutlet = viewModel::registerSellerOutlet,
                                onPublishUpdate = viewModel::publishSellerUpdate,
                                onMarkSoldOut = viewModel::markSoldOutImmediately,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AppWorkspaceTab.ADMIN -> {
                            AdminPanelScreen(
                                isAdmin = isAdmin,
                                distributorsState = publicDistributors,
                                flaggedReports = flaggedReports,
                                onBackToExplore = { viewModel.selectTab(AppWorkspaceTab.EXPLORE) },
                                onVerifyDealer = viewModel::setStationVerification,
                                onModerateReport = viewModel::moderateCrowdReport,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (showAlertsDialog) {
            RestockAlertsDialog(
                alerts = inAppAlerts,
                watchlistCount = watchlist.size,
                onDismiss = { showAlertsDialog = false },
                onSelectStation = { stationId ->
                    viewModel.selectTab(AppWorkspaceTab.EXPLORE)
                    viewModel.selectStation(stationId)
                }
            )
        }
    }
}
