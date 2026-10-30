package com.example.skylight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skylight.R
import com.example.skylight.data.PiControlClient
import com.example.skylight.model.AggregatorPresets
import com.example.skylight.model.AirportsCatalog
import com.example.skylight.model.AltitudeUnit
import com.example.skylight.model.ConstellationsCatalog
import com.example.skylight.model.DataSourceType
import com.example.skylight.model.DistanceUnit
import com.example.skylight.model.LabelDensity
import com.example.skylight.model.LocationDisplay
import com.example.skylight.model.LocationProfile
import com.example.skylight.model.NameDisplay
import com.example.skylight.model.ProjectionMode
import com.example.skylight.model.SkylightConfig
import com.example.skylight.model.SpeedUnit
import com.example.skylight.model.ThemeMode
import com.example.skylight.ui.SkylightControlUiState
import com.example.skylight.ui.theme.AltitudeGold
import com.example.skylight.ui.theme.DeepSpaceSurface
import com.example.skylight.ui.theme.ElevatedCockpitSurface
import com.example.skylight.ui.theme.EmeraldStatus
import com.example.skylight.ui.theme.EmergencyCoral
import com.example.skylight.ui.theme.GridLineBlue
import com.example.skylight.ui.theme.LuminousIce
import com.example.skylight.ui.theme.MutedSlate
import com.example.skylight.ui.theme.ObsidianBlack
import com.example.skylight.ui.theme.TelemetryCyan
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlPanelScreen(
    uiState: SkylightControlUiState,
    onSetPiUrl: (String) -> Unit,
    onSyncWithPi: () -> Unit,
    onAddSavedUrl: (String) -> Unit = {},
    onRemoveSavedUrl: (String) -> Unit = {},
    onUpdateConfig: ((SkylightConfig) -> SkylightConfig) -> Unit,
    onSwitchSource: (DataSourceType) -> Unit,
    onSearchLocation: (String) -> Unit,
    onImportAirport: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onSwitchProfile: (LocationProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onCenterOnTraffic: () -> Unit,
    onToggleConstellation: (String, Boolean) -> Unit,
    onResetConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.config
    val scrollState = rememberScrollState()
    var locationInput by remember { mutableStateOf("") }
    var airportCodeInput by remember(cfg.airport.icao) { mutableStateOf(cfg.airport.icao) }
    var showAddUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Title + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.title_control),
                        style = MaterialTheme.typography.headlineMedium,
                        color = LuminousIce
                    )
                    Text(
                        text = stringResource(R.string.subtitle_control),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DeepSpaceSurface,
                    modifier = Modifier.border(
                        1.dp,
                        if (uiState.piConnected) EmeraldStatus else GridLineBlue,
                        RoundedCornerShape(20.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.piConnected) EmeraldStatus else AltitudeGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.piConnected) {
                                "${uiState.status.source.wireValue} · ${uiState.status.count} ac"
                            } else {
                                "LAN Standby"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = LuminousIce
                        )
                    }
                }
            }

            // 0. Raspberry Pi Server URL & Connection Card
            ControlSectionCard(title = "RASPBERRY PI URL CONFIG") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (uiState.piUrl.equals("upload", ignoreCase = true)) "" else uiState.piUrl,
                        onValueChange = { input ->
                            if (!input.equals("upload", ignoreCase = true)) {
                                onSetPiUrl(input)
                            }
                        },
                        label = { Text(stringResource(R.string.label_pi_host)) },
                        placeholder = { Text("http://skylight.local:3000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrect = false
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pi_url_input")
                    )
                    Button(
                        onClick = onSyncWithPi,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TelemetryCyan,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("sync_pi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.btn_connect_pi),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_saved_urls).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate
                        )

                        val currentTrimmed = uiState.piUrl.trim().removeSuffix("/")
                        if (currentTrimmed.isNotBlank() && currentTrimmed !in uiState.savedUrls && !PiControlClient.isExcludedUrl(currentTrimmed)) {
                            Text(
                                text = "+ Save \"$currentTrimmed\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = AltitudeGold,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { onAddSavedUrl(currentTrimmed) }
                                    .testTag("save_current_url_btn")
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.savedUrls.filterNot { PiControlClient.isExcludedUrl(it) }.forEach { savedUrl ->
                            FilterChip(
                                selected = uiState.piUrl.trim().removeSuffix("/") == savedUrl,
                                onClick = {
                                    onSetPiUrl(savedUrl)
                                    onSyncWithPi()
                                },
                                label = {
                                    Text(
                                        text = savedUrl,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onRemoveSavedUrl(savedUrl) },
                                        modifier = Modifier
                                            .size(20.dp)
                                            .testTag(
                                                "remove_url_${savedUrl.replace(Regex("[^a-zA-Z0-9]"), "_")}"
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $savedUrl",
                                            tint = MutedSlate,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                },
                                modifier = Modifier.testTag(
                                    "pi_url_preset_${savedUrl.replace(Regex("[^a-zA-Z0-9]"), "_")}"
                                )
                            )
                        }

                        AssistChip(
                            onClick = {
                                customUrlInput = if (uiState.piUrl.isNotBlank() && uiState.piUrl !in uiState.savedUrls && !PiControlClient.isExcludedUrl(uiState.piUrl)) {
                                    uiState.piUrl
                                } else {
                                    "http://"
                                }
                                showAddUrlDialog = true
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.btn_add_saved_url),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TelemetryCyan
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = ElevatedCockpitSurface,
                                labelColor = TelemetryCyan
                            ),
                            modifier = Modifier.testTag("add_custom_url_chip")
                        )
                    }
                }

                if (showAddUrlDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddUrlDialog = false },
                        title = {
                            Text(
                                text = stringResource(R.string.dialog_add_url_title),
                                color = LuminousIce,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Enter the host or IP address of your Raspberry Pi:",
                                    color = MutedSlate,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = customUrlInput,
                                    onValueChange = { customUrlInput = it },
                                    placeholder = { Text(stringResource(R.string.dialog_add_url_hint)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_url_input")
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val trimmed = customUrlInput.trim().removeSuffix("/")
                                    if (trimmed.isNotBlank() && !PiControlClient.isExcludedUrl(trimmed)) {
                                        val finalUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                                            "http://$trimmed"
                                        } else {
                                            trimmed
                                        }
                                        if (!PiControlClient.isExcludedUrl(finalUrl)) {
                                            onAddSavedUrl(finalUrl)
                                            onSetPiUrl(finalUrl)
                                            onSyncWithPi()
                                        }
                                    }
                                    showAddUrlDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TelemetryCyan,
                                    contentColor = ObsidianBlack
                                ),
                                modifier = Modifier.testTag("confirm_add_url_button")
                            ) {
                                Text(stringResource(R.string.btn_save))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showAddUrlDialog = false },
                                modifier = Modifier.testTag("cancel_add_url_button")
                            ) {
                                Text(stringResource(R.string.btn_cancel), color = MutedSlate)
                            }
                        },
                        containerColor = DeepSpaceSurface,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                if (uiState.status.message != null) {
                    Text(
                        text = "Pi Status: ${uiState.status.message}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (uiState.status.ok) EmeraldStatus else EmergencyCoral
                    )
                }

                if (uiState.feedbackMessage != null) {
                    Text(
                        text = uiState.feedbackMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TelemetryCyan
                    )
                }
            }

            // 1. Location & Runways Section
            ControlSectionCard(title = "LOCATION & RUNWAYS") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedCockpitSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = cfg.locationName,
                            style = MaterialTheme.typography.titleMedium,
                            color = LuminousIce
                        )
                        Text(
                            text = "%.4f°, %.4f° · Airport: ${cfg.airport.icao} (${cfg.airport.name})".format(
                                cfg.centerLat,
                                cfg.centerLon
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = TelemetryCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        placeholder = { Text("City, airport code, or lat,lon") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("location_search_input")
                    )
                    Button(
                        onClick = { onSearchLocation(locationInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TelemetryCyan,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("location_search_button")
                    ) {
                        Text("Set", fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveProfile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_save_profile))
                    }
                    OutlinedButton(
                        onClick = onCenterOnTraffic,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("center_traffic_button")
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_center_traffic))
                    }
                }

                if (cfg.locationProfiles.isNotEmpty()) {
                    Text(
                        text = "Saved Location Profiles",
                        style = MaterialTheme.typography.labelLarge,
                        color = AltitudeGold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cfg.locationProfiles.forEach { profile ->
                            val isActive = abs(profile.lat - cfg.centerLat) < 1e-4 &&
                                abs(profile.lon - cfg.centerLon) < 1e-4
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AssistChip(
                                    onClick = { onSwitchProfile(profile) },
                                    label = { Text(profile.name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isActive) {
                                            TelemetryCyan.copy(alpha = 0.22f)
                                        } else {
                                            ElevatedCockpitSurface
                                        },
                                        labelColor = if (isActive) TelemetryCyan else LuminousIce
                                    ),
                                    modifier = Modifier.testTag("profile_chip_${profile.id}")
                                )
                                if (cfg.locationProfiles.size > 1) {
                                    IconButton(
                                        onClick = { onDeleteProfile(profile.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_profile_${profile.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove ${profile.name}",
                                            tint = MutedSlate,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LabeledSlider(
                    label = "Radius",
                    valueText = "${"%.1f".format(cfg.radiusMiles)} ${cfg.distanceUnit.label}",
                    value = cfg.radiusMiles.toFloat(),
                    onValueChange = { r -> onUpdateConfig { it.copy(radiusMiles = (r * 10).roundToInt() / 10.0) } },
                    valueRange = 1f..50f,
                    tag = "radius_slider"
                )

                HorizontalDivider(color = GridLineBlue)

                ControlToggleRow(
                    label = stringResource(R.string.label_airport_runways),
                    checked = cfg.showAirport,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showAirport = v) } },
                    tag = "toggle_airport_runways"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = airportCodeInput,
                        onValueChange = { airportCodeInput = it },
                        label = { Text("Runways ICAO / IATA Code") },
                        placeholder = { Text("KSFO, EGLL, KJFK, RJTT…") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("airport_code_input")
                    )
                    OutlinedButton(
                        onClick = { onImportAirport(airportCodeInput) },
                        modifier = Modifier.testTag("import_airport_button")
                    ) {
                        Text(stringResource(R.string.btn_import_runways))
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AirportsCatalog.ALL.forEach { ap ->
                        AssistChip(
                            onClick = {
                                airportCodeInput = ap.icao
                                onImportAirport(ap.icao)
                            },
                            label = { Text("${ap.name} (${ap.icao})") },
                            modifier = Modifier.testTag("quick_airport_${ap.name.lowercase()}")
                        )
                    }
                }
            }

            // 2. Source Section (Radio vs Public API)
            ControlSectionCard(title = "SOURCE (ADS-B FEED)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DataSourceType.entries.forEach { src ->
                        FilterChip(
                            selected = uiState.status.source == src,
                            onClick = { onSwitchSource(src) },
                            label = { Text(src.label) },
                            modifier = Modifier.testTag("source_chip_${src.wireValue}")
                        )
                    }
                }

                OutlinedTextField(
                    value = cfg.radioUrl,
                    onValueChange = { u -> onUpdateConfig { it.copy(radioUrl = u) } },
                    label = { Text("Radio URL (dump1090 / readsb aircraft.json)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("radio_url_input")
                )

                OutlinedTextField(
                    value = cfg.apiUrl,
                    onValueChange = { u -> onUpdateConfig { it.copy(apiUrl = u) } },
                    label = { Text("Public API URL ({lat}, {lon}, {r})") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_url_input")
                )

                Text(
                    text = "Aggregator Presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AggregatorPresets.ALL.forEach { (name, presetUrl) ->
                        AssistChip(
                            onClick = { onUpdateConfig { it.copy(apiUrl = presetUrl) } },
                            label = { Text(name) },
                            modifier = Modifier.testTag("aggregator_preset_${name.replace(".", "_")}")
                        )
                    }
                }
            }

            // 3. Calibration & Projection Section
            ControlSectionCard(title = "CALIBRATION & PROJECTION") {
                Text(
                    text = "Projection Mode",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectionMode.entries.forEach { mode ->
                        FilterChip(
                            selected = cfg.projectionMode == mode,
                            onClick = { onUpdateConfig { it.copy(projectionMode = mode) } },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("projection_chip_${mode.wireValue}")
                        )
                    }
                }

                LabeledSlider(
                    label = "Field Rotation",
                    valueText = "${cfg.rotationDeg.roundToInt()}°",
                    value = cfg.rotationDeg,
                    onValueChange = { deg -> onUpdateConfig { it.copy(rotationDeg = deg.roundToInt().toFloat()) } },
                    valueRange = 0f..360f,
                    tag = "rotation_slider"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0f, 90f, 180f, 270f).forEach { presetDeg ->
                        AssistChip(
                            onClick = { onUpdateConfig { it.copy(rotationDeg = presetDeg) } },
                            label = { Text("${presetDeg.roundToInt()}°") },
                            modifier = Modifier.testTag("rotation_preset_${presetDeg.roundToInt()}")
                        )
                    }
                }

                LabeledSlider(
                    label = "Label Rotation",
                    valueText = "${cfg.labelRotationDeg.roundToInt()}°",
                    value = cfg.labelRotationDeg,
                    onValueChange = { deg -> onUpdateConfig { it.copy(labelRotationDeg = deg.roundToInt().toFloat()) } },
                    valueRange = 0f..360f,
                    tag = "label_rotation_slider"
                )

                ControlToggleRow(
                    label = stringResource(R.string.label_mirror_x),
                    checked = cfg.mirrorX,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(mirrorX = v) } },
                    tag = "toggle_mirror_x"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_mirror_y),
                    checked = cfg.mirrorY,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(mirrorY = v) } },
                    tag = "toggle_mirror_y"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_show_hud),
                    checked = cfg.showHud,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showHud = v) } },
                    tag = "toggle_show_hud"
                )
            }

            // 4. Visuals & Theme Section
            ControlSectionCard(title = "VISUALS & THEME") {
                Text(
                    text = "Display Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { th ->
                        FilterChip(
                            selected = cfg.theme == th,
                            onClick = { onUpdateConfig { it.copy(theme = th) } },
                            label = { Text(th.label) },
                            modifier = Modifier.testTag("theme_chip_${th.wireValue}")
                        )
                    }
                }

                LabeledSlider(
                    label = "Projector Brightness",
                    valueText = "${(cfg.brightness * 100).roundToInt()}%",
                    value = cfg.brightness,
                    onValueChange = { b -> onUpdateConfig { it.copy(brightness = b) } },
                    valueRange = 0.1f..1.0f,
                    tag = "brightness_slider"
                )

                LabeledSlider(
                    label = "Aircraft Glyph Size",
                    valueText = "${cfg.glyphSizePx} px",
                    value = cfg.glyphSizePx.toFloat(),
                    onValueChange = { px -> onUpdateConfig { it.copy(glyphSizePx = px.roundToInt()) } },
                    valueRange = 12f..40f,
                    tag = "glyph_size_slider"
                )

                LabeledSlider(
                    label = "Comet Trail Duration",
                    valueText = "${cfg.trailSeconds} s",
                    value = cfg.trailSeconds.toFloat(),
                    onValueChange = { s -> onUpdateConfig { it.copy(trailSeconds = s.roundToInt()) } },
                    valueRange = 0f..120f,
                    tag = "trail_seconds_slider"
                )

                ControlToggleRow(
                    label = stringResource(R.string.label_altitude_color),
                    checked = cfg.altitudeColor,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(altitudeColor = v) } },
                    tag = "toggle_altitude_color"
                )
            }

            // 5. Labels, Units & Window to Elsewhere Section
            ControlSectionCard(title = "LABELS, UNITS & ROUTES") {
                Text(
                    text = "Label Density",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelDensity.entries.forEach { d ->
                        FilterChip(
                            selected = cfg.labelDensity == d,
                            onClick = { onUpdateConfig { it.copy(labelDensity = d) } },
                            label = { Text(d.label) },
                            modifier = Modifier.testTag("label_density_${d.wireValue}")
                        )
                    }
                }

                if (cfg.labelDensity == LabelDensity.NEAREST_N) {
                    LabeledSlider(
                        label = "Nearest N Aircraft",
                        valueText = "${cfg.nearestN}",
                        value = cfg.nearestN.toFloat(),
                        onValueChange = { n -> onUpdateConfig { it.copy(nearestN = n.roundToInt()) } },
                        valueRange = 1f..15f,
                        tag = "nearest_n_slider"
                    )
                }

                Text(
                    text = "Primary Identifier",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NameDisplay.entries.forEach { nd ->
                        FilterChip(
                            selected = cfg.nameDisplay == nd,
                            onClick = { onUpdateConfig { it.copy(nameDisplay = nd) } },
                            label = { Text(nd.label) },
                            modifier = Modifier.testTag("name_display_${nd.wireValue}")
                        )
                    }
                }

                Text(
                    text = "Destination Format",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocationDisplay.entries.forEach { ld ->
                        FilterChip(
                            selected = cfg.locationDisplay == ld,
                            onClick = { onUpdateConfig { it.copy(locationDisplay = ld) } },
                            label = { Text(ld.label) },
                            modifier = Modifier.testTag("location_display_${ld.wireValue}")
                        )
                    }
                }

                Text(
                    text = "Visible Label Fields",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = cfg.showFields.name,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(name = !it.showFields.name)) } },
                        label = { Text("Name") },
                        modifier = Modifier.testTag("field_chip_name")
                    )
                    FilterChip(
                        selected = cfg.showFields.type,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(type = !it.showFields.type)) } },
                        label = { Text("Type") },
                        modifier = Modifier.testTag("field_chip_type")
                    )
                    FilterChip(
                        selected = cfg.showFields.altitude,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(altitude = !it.showFields.altitude)) } },
                        label = { Text("Altitude") },
                        modifier = Modifier.testTag("field_chip_altitude")
                    )
                    FilterChip(
                        selected = cfg.showFields.speed,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(speed = !it.showFields.speed)) } },
                        label = { Text("Speed") },
                        modifier = Modifier.testTag("field_chip_speed")
                    )
                    FilterChip(
                        selected = cfg.showFields.verticalRate,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(verticalRate = !it.showFields.verticalRate)) } },
                        label = { Text("Vert. Rate") },
                        modifier = Modifier.testTag("field_chip_vertical_rate")
                    )
                    FilterChip(
                        selected = cfg.showFields.destination,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(destination = !it.showFields.destination)) } },
                        label = { Text("Departing / Arriving") },
                        modifier = Modifier.testTag("field_chip_destination")
                    )
                    FilterChip(
                        selected = cfg.showFields.registration,
                        onClick = { onUpdateConfig { it.copy(showFields = it.showFields.copy(registration = !it.showFields.registration)) } },
                        label = { Text("Registration") },
                        modifier = Modifier.testTag("field_chip_registration")
                    )
                }

                ControlToggleRow(
                    label = stringResource(R.string.label_dest_arcs),
                    checked = cfg.showDestArc,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showDestArc = v) } },
                    tag = "toggle_dest_arcs"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_route_detail),
                    checked = cfg.showRouteDetail,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showRouteDetail = v) } },
                    tag = "toggle_route_detail"
                )

                Text(
                    text = "Display Units",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeedUnit.entries.forEach { u ->
                        FilterChip(
                            selected = cfg.speedUnit == u,
                            onClick = { onUpdateConfig { it.copy(speedUnit = u) } },
                            label = { Text(u.label) },
                            modifier = Modifier.testTag("speed_unit_${u.wireValue}")
                        )
                    }
                    AltitudeUnit.entries.forEach { u ->
                        FilterChip(
                            selected = cfg.altitudeUnit == u,
                            onClick = { onUpdateConfig { it.copy(altitudeUnit = u) } },
                            label = { Text(u.label) },
                            modifier = Modifier.testTag("alt_unit_${u.wireValue}")
                        )
                    }
                    DistanceUnit.entries.forEach { u ->
                        FilterChip(
                            selected = cfg.distanceUnit == u,
                            onClick = { onUpdateConfig { it.copy(distanceUnit = u) } },
                            label = { Text(u.label) },
                            modifier = Modifier.testTag("dist_unit_${u.wireValue}")
                        )
                    }
                }
            }

            // 6. Filtering & Motion Section
            ControlSectionCard(title = "FILTERING & MOTION") {
                LabeledSlider(
                    label = "Minimum Altitude",
                    valueText = "${cfg.minAltitudeFt} ft",
                    value = cfg.minAltitudeFt.toFloat(),
                    onValueChange = { a -> onUpdateConfig { it.copy(minAltitudeFt = (a / 100).roundToInt() * 100) } },
                    valueRange = 0f..15000f,
                    tag = "min_altitude_slider"
                )

                LabeledSlider(
                    label = "Maximum Altitude",
                    valueText = "${cfg.maxAltitudeFt} ft",
                    value = cfg.maxAltitudeFt.toFloat(),
                    onValueChange = { a -> onUpdateConfig { it.copy(maxAltitudeFt = (a / 1000).roundToInt() * 1000) } },
                    valueRange = 5000f..60000f,
                    tag = "max_altitude_slider"
                )

                ControlToggleRow(
                    label = stringResource(R.string.label_hide_on_ground),
                    checked = cfg.hideOnGround,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(hideOnGround = v) } },
                    tag = "toggle_hide_on_ground"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_interpolate),
                    checked = cfg.interpolate,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(interpolate = v) } },
                    tag = "toggle_interpolate"
                )

                LabeledSlider(
                    label = "Position Smoothing",
                    valueText = "%.2f".format(cfg.smoothing),
                    value = cfg.smoothing,
                    onValueChange = { s -> onUpdateConfig { it.copy(smoothing = s) } },
                    valueRange = 0f..0.85f,
                    tag = "smoothing_slider"
                )

                LabeledSlider(
                    label = "Stale Target Timeout",
                    valueText = "${cfg.staleSec} s",
                    value = cfg.staleSec.toFloat(),
                    onValueChange = { s -> onUpdateConfig { it.copy(staleSec = s.roundToInt()) } },
                    valueRange = 5f..60f,
                    tag = "stale_sec_slider"
                )

                Text(
                    text = "Projector Frame Rate Cap (maxFps)",
                    style = MaterialTheme.typography.labelLarge,
                    color = AltitudeGold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Uncapped", 30 to "30 FPS", 60 to "60 FPS").forEach { (fps, lbl) ->
                        FilterChip(
                            selected = cfg.maxFps == fps,
                            onClick = { onUpdateConfig { it.copy(maxFps = fps) } },
                            label = { Text(lbl) },
                            modifier = Modifier.testTag("max_fps_chip_$fps")
                        )
                    }
                }
            }

            // 7. Overlays & Live Sky Layer Section
            ControlSectionCard(title = "OVERLAYS & LIVE SKY LAYER") {
                ControlToggleRow(
                    label = stringResource(R.string.label_range_rings),
                    checked = cfg.rangeRings,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(rangeRings = v) } },
                    tag = "toggle_range_rings"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_compass),
                    checked = cfg.compass,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(compass = v) } },
                    tag = "toggle_compass"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_highlight_emergency),
                    checked = cfg.highlightEmergency,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(highlightEmergency = v) } },
                    tag = "toggle_highlight_emergency"
                )

                HorizontalDivider(color = GridLineBlue)

                ControlToggleRow(
                    label = stringResource(R.string.label_planetarium_mode),
                    checked = cfg.planetariumMode,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(planetariumMode = v) } },
                    tag = "control_toggle_planetarium"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_stars),
                    checked = cfg.showStars,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showStars = v) } },
                    tag = "control_toggle_stars"
                )

                if (cfg.showStars) {
                    LabeledSlider(
                        label = "Star Magnitude Limit",
                        valueText = "Mag ${"%.1f".format(cfg.starMagLimit)}",
                        value = cfg.starMagLimit,
                        onValueChange = { m -> onUpdateConfig { it.copy(starMagLimit = m) } },
                        valueRange = 0.5f..4.5f,
                        tag = "star_mag_limit_slider"
                    )
                    LabeledSlider(
                        label = "Star Label Magnitude Limit",
                        valueText = "Mag ${"%.1f".format(cfg.starLabelMagLimit)}",
                        value = cfg.starLabelMagLimit,
                        onValueChange = { m -> onUpdateConfig { it.copy(starLabelMagLimit = m) } },
                        valueRange = -1.0f..2.5f,
                        tag = "star_label_mag_slider"
                    )

                    Text(
                        text = "Constellation Lines",
                        style = MaterialTheme.typography.labelLarge,
                        color = AltitudeGold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ConstellationsCatalog.ALL.forEach { c ->
                            val enabled = cfg.constellations[c.id] != false
                            FilterChip(
                                selected = enabled,
                                onClick = { onToggleConstellation(c.id, !enabled) },
                                label = { Text(c.name) },
                                modifier = Modifier.testTag("constellation_chip_${c.id}")
                            )
                        }
                    }
                }

                ControlToggleRow(
                    label = stringResource(R.string.label_sun),
                    checked = cfg.showSun,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showSun = v) } },
                    tag = "control_toggle_sun"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_moon),
                    checked = cfg.showMoon,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showMoon = v) } },
                    tag = "control_toggle_moon"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_planets),
                    checked = cfg.showPlanets,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showPlanets = v) } },
                    tag = "control_toggle_planets"
                )
                ControlToggleRow(
                    label = stringResource(R.string.label_satellites),
                    checked = cfg.showSatellites,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showSatellites = v) } },
                    tag = "control_toggle_satellites"
                )
                if (cfg.showSatellites) {
                    ControlToggleRow(
                        label = stringResource(R.string.label_satellite_labels),
                        checked = cfg.satelliteLabels,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(satelliteLabels = v) } },
                        tag = "control_toggle_satellite_labels"
                    )
                }
                ControlToggleRow(
                    label = stringResource(R.string.label_sky_events),
                    checked = cfg.showSkyEvents,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(showSkyEvents = v) } },
                    tag = "control_toggle_sky_events"
                )

                // Sky Clock Offset Scrubber
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (cfg.skyTimeOffsetMin == 0) {
                            "Sky Clock: Live (0m)"
                        } else {
                            val sign = if (cfg.skyTimeOffsetMin > 0) "+" else ""
                            "Sky Clock Offset: $sign${cfg.skyTimeOffsetMin / 60}h ${abs(cfg.skyTimeOffsetMin % 60)}m"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = LuminousIce
                    )
                    if (cfg.skyTimeOffsetMin != 0) {
                        Text(
                            text = "Reset Live",
                            style = MaterialTheme.typography.labelLarge,
                            color = TelemetryCyan,
                            modifier = Modifier
                                .clickable { onUpdateConfig { it.copy(skyTimeOffsetMin = 0) } }
                                .padding(4.dp)
                                .testTag("reset_sky_clock_button")
                        )
                    }
                }
                Slider(
                    value = cfg.skyTimeOffsetMin.toFloat().coerceIn(-720f, 720f),
                    onValueChange = { v -> onUpdateConfig { it.copy(skyTimeOffsetMin = v.roundToInt()) } },
                    valueRange = -720f..720f,
                    modifier = Modifier.testTag("sky_clock_slider")
                )
            }

            // 8. System Section
            ControlSectionCard(title = "SYSTEM") {
                ControlToggleRow(
                    label = stringResource(R.string.label_telemetry_ping),
                    checked = cfg.telemetry,
                    onCheckedChange = { v -> onUpdateConfig { it.copy(telemetry = v) } },
                    tag = "toggle_telemetry_ping"
                )

                OutlinedButton(
                    onClick = onResetConfig,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_config_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_reset_config))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ControlSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DeepSpaceSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridLineBlue, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TelemetryCyan
            )
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    tag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LuminousIce
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = TelemetryCyan,
                fontFamily = FontFamily.Monospace
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun ControlToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = LuminousIce,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
