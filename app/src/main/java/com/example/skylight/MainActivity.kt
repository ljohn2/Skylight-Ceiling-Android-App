package com.example.skylight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.skylight.ui.AppScreen
import com.example.skylight.ui.SkylightViewModel
import com.example.skylight.ui.screens.ControlPanelScreen
import com.example.skylight.ui.screens.LiveRadarScreen
import com.example.skylight.ui.screens.RadioDebugScreen
import com.example.skylight.ui.theme.DeepSpaceSurface
import com.example.skylight.ui.theme.GridLineBlue
import com.example.skylight.ui.theme.MutedSlate
import com.example.skylight.ui.theme.ObsidianBlack
import com.example.skylight.ui.theme.SkylightTheme
import com.example.skylight.ui.theme.TelemetryCyan

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkylightTheme {
                SkylightControlApp()
            }
        }
    }
}

@Composable
fun SkylightControlApp(
    viewModel: SkylightViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ObsidianBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = DeepSpaceSurface,
                contentColor = TelemetryCyan,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(
                        1.dp,
                        GridLineBlue,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .testTag("app_bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.CONTROL_PANEL,
                    onClick = { viewModel.navigateTo(AppScreen.CONTROL_PANEL) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Control Panel"
                        )
                    },
                    label = { Text("Control") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = TelemetryCyan,
                        indicatorColor = TelemetryCyan,
                        unselectedIconColor = MutedSlate,
                        unselectedTextColor = MutedSlate
                    ),
                    modifier = Modifier.testTag("nav_tab_control")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.LIVE_RADAR,
                    onClick = { viewModel.navigateTo(AppScreen.LIVE_RADAR) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Live Preview"
                        )
                    },
                    label = { Text("Live Preview") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = TelemetryCyan,
                        indicatorColor = TelemetryCyan,
                        unselectedIconColor = MutedSlate,
                        unselectedTextColor = MutedSlate
                    ),
                    modifier = Modifier.testTag("nav_tab_live_preview")
                )
                NavigationBarItem(
                    selected = uiState.currentScreen == AppScreen.RADIO_DEBUG,
                    onClick = { viewModel.navigateTo(AppScreen.RADIO_DEBUG) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Radio Debug"
                        )
                    },
                    label = { Text("Radio Debug") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ObsidianBlack,
                        selectedTextColor = TelemetryCyan,
                        indicatorColor = TelemetryCyan,
                        unselectedIconColor = MutedSlate,
                        unselectedTextColor = MutedSlate
                    ),
                    modifier = Modifier.testTag("nav_tab_radio_debug")
                )
            }
        }
    ) { innerPadding ->
        when (uiState.currentScreen) {
            AppScreen.CONTROL_PANEL -> {
                ControlPanelScreen(
                    uiState = uiState,
                    onSetPiUrl = viewModel::setPiUrl,
                    onSyncWithPi = viewModel::syncWithPi,
                    onAddSavedUrl = viewModel::addSavedUrl,
                    onRemoveSavedUrl = viewModel::removeSavedUrl,
                    onUpdateConfig = viewModel::updateConfig,
                    onSwitchSource = viewModel::switchDataSource,
                    onSearchLocation = viewModel::searchLocation,
                    onImportAirport = viewModel::importAirportRunways,
                    onSaveProfile = viewModel::saveCurrentProfile,
                    onSwitchProfile = viewModel::switchToProfile,
                    onDeleteProfile = viewModel::removeProfile,
                    onCenterOnTraffic = viewModel::centerOnTraffic,
                    onToggleConstellation = viewModel::toggleConstellation,
                    onResetConfig = viewModel::resetConfig,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            AppScreen.LIVE_RADAR -> {
                LiveRadarScreen(
                    uiState = uiState,
                    onNavigateBack = viewModel::navigateBack,
                    onZoomIn = viewModel::zoomInRadar,
                    onZoomOut = viewModel::zoomOutRadar,
                    onResetZoom = viewModel::setRadarZoom,
                    onSelectAircraft = viewModel::selectAircraftHex,
                    onTogglePolling = viewModel::toggleRadarPolling,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            AppScreen.RADIO_DEBUG -> {
                RadioDebugScreen(
                    uiState = uiState,
                    onNavigateBack = viewModel::navigateBack,
                    onSetRadioUrl = viewModel::setRadioUrl,
                    onFetchRadio = viewModel::fetchRadioDebug,
                    onToggleAutoRefresh = viewModel::toggleRadioAutoRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}
