package com.example.skylight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.skylight.ui.SkylightViewModel
import com.example.skylight.ui.screens.ControlPanelScreen
import com.example.skylight.ui.theme.ObsidianBlack
import com.example.skylight.ui.theme.SkylightTheme

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
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        ControlPanelScreen(
            uiState = uiState,
            onSetPiUrl = viewModel::setPiUrl,
            onSyncWithPi = viewModel::syncWithPi,
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
}
