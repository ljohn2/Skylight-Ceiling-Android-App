package com.example.skylight.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skylight.R
import com.example.skylight.data.RadioDebugClient
import com.example.skylight.model.RadioAircraft
import com.example.skylight.ui.SkylightControlUiState
import com.example.skylight.ui.theme.AltitudeGold
import com.example.skylight.ui.theme.AltitudeSkyBlue
import com.example.skylight.ui.theme.AltitudeTeal
import com.example.skylight.ui.theme.DeepSpaceSurface
import com.example.skylight.ui.theme.ElevatedCockpitSurface
import com.example.skylight.ui.theme.EmeraldStatus
import com.example.skylight.ui.theme.EmergencyCoral
import com.example.skylight.ui.theme.GridLineBlue
import com.example.skylight.ui.theme.LuminousIce
import com.example.skylight.ui.theme.MutedSlate
import com.example.skylight.ui.theme.ObsidianBlack
import com.example.skylight.ui.theme.TelemetryCyan
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RadioDebugScreen(
    uiState: SkylightControlUiState,
    onNavigateBack: () -> Unit,
    onSetRadioUrl: (String) -> Unit,
    onFetchRadio: () -> Unit,
    onToggleAutoRefresh: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyWithPosition by remember { mutableStateOf(false) }

    val snapshot = uiState.radioSnapshot

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("radio_back_button")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_control_panel),
                            tint = TelemetryCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.title_radio_debug),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LuminousIce
                        )
                        Text(
                            text = stringResource(R.string.subtitle_radio_debug),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }

                // Live status pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DeepSpaceSurface,
                    modifier = Modifier.border(
                        1.dp,
                        if (uiState.radioError != null) EmergencyCoral
                        else if (snapshot != null) EmeraldStatus
                        else GridLineBlue,
                        RoundedCornerShape(20.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.radioLoading) AltitudeGold
                                    else if (uiState.radioError != null) EmergencyCoral
                                    else if (snapshot != null) EmeraldStatus
                                    else MutedSlate
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.radioLoading) {
                                "Polling…"
                            } else if (snapshot != null) {
                                "${snapshot.aircraft.size} ac"
                            } else if (uiState.radioError != null) {
                                "Offline"
                            } else {
                                "Ready"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = LuminousIce
                        )
                    }
                }
            }

            // Radio Endpoint Configuration Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSpaceSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GridLineBlue, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_radio_endpoint).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.radioUrl,
                            onValueChange = onSetRadioUrl,
                            placeholder = { Text(RadioDebugClient.DEFAULT_RADIO_URL) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                autoCorrect = false
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("radio_url_input")
                        )

                        Button(
                            onClick = onFetchRadio,
                            enabled = !uiState.radioLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelemetryCyan,
                                contentColor = ObsidianBlack
                            ),
                            modifier = Modifier.testTag("radio_fetch_button")
                        ) {
                            if (uiState.radioLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianBlack
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_radio_fetch),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Auto-Refresh Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.btn_radio_auto_refresh),
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.radioAutoRefresh,
                            onCheckedChange = { onToggleAutoRefresh(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = TelemetryCyan
                            ),
                            modifier = Modifier.testTag("radio_auto_refresh_toggle")
                        )
                    }
                }
            }

            // Error display with emulator diagnostic hint
            if (uiState.radioError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmergencyCoral.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EmergencyCoral.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("radio_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = EmergencyCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Failed to load radio data: ${uiState.radioError}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmergencyCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.radioUrl.contains("localhost")) {
                                Text(
                                    text = "Tip: When running inside the Android Emulator, use 10.0.2.2:8080 to connect to your host machine.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSlate
                                )
                            }
                        }
                    }
                }
            }

            // Radio Stats Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioStatPill(
                    label = stringResource(R.string.stat_total_messages),
                    value = snapshot?.totalMessages?.toString() ?: "—",
                    accentColor = TelemetryCyan,
                    modifier = Modifier.weight(1f)
                )
                RadioStatPill(
                    label = stringResource(R.string.stat_aircraft_tracked),
                    value = snapshot?.totalCount?.toString() ?: "0",
                    accentColor = AltitudeSkyBlue,
                    modifier = Modifier.weight(1f)
                )
                RadioStatPill(
                    label = stringResource(R.string.stat_with_position),
                    value = snapshot?.positionedCount?.toString() ?: "0",
                    accentColor = AltitudeTeal,
                    modifier = Modifier.weight(1f)
                )
                RadioStatPill(
                    label = stringResource(R.string.stat_avg_rssi),
                    value = snapshot?.averageRssi?.let { String.format(Locale.US, "%.1f dB", it) } ?: "—",
                    accentColor = AltitudeGold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Selector: Aircraft Telemetry vs Raw JSON
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepSpaceSurface,
                contentColor = TelemetryCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TelemetryCyan,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, GridLineBlue, RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.tab_aircraft_list)} (${snapshot?.aircraft?.size ?: 0})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_aircraft_telemetry")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = stringResource(R.string.tab_raw_json),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_raw_json")
                )
            }

            // Tab content
            if (selectedTab == 0) {
                // Aircraft Telemetry List View
                val aircraft = snapshot?.aircraft ?: emptyList()
                val filtered = remember(aircraft, searchQuery, onlyWithPosition) {
                    aircraft.filter { ac ->
                        val matchesSearch = searchQuery.isBlank() ||
                                ac.hex.contains(searchQuery, ignoreCase = true) ||
                                (ac.flight?.contains(searchQuery, ignoreCase = true) == true) ||
                                (ac.squawk?.contains(searchQuery, ignoreCase = true) == true)
                        val matchesPos = !onlyWithPosition || ac.hasPosition
                        matchesSearch && matchesPos
                    }
                }

                // Filter & Search Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_aircraft_hint), fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MutedSlate,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("aircraft_search_input")
                    )

                    FilterChip(
                        selected = onlyWithPosition,
                        onClick = { onlyWithPosition = !onlyWithPosition },
                        label = { Text("GPS Only", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AltitudeTeal,
                            selectedLabelColor = ObsidianBlack,
                            containerColor = ElevatedCockpitSurface,
                            labelColor = LuminousIce
                        ),
                        modifier = Modifier.testTag("filter_gps_only_chip")
                    )
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepSpaceSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MutedSlate,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = stringResource(R.string.empty_aircraft),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate
                            )
                            if (snapshot == null) {
                                Text(
                                    text = "Tap 'Fetch' to connect to ${uiState.radioUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TelemetryCyan
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.hex }) { ac ->
                            RadioAircraftCard(ac = ac)
                        }
                    }
                }
            } else {
                // Raw JSON Inspector View
                val rawJson = snapshot?.rawJson ?: "{}"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timestampText = snapshot?.let {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.fetchTimestampMillis))
                        } ?: "—"
                        Text(
                            text = "Last received: $timestampText · ${rawJson.length} bytes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate
                        )

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(rawJson))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_json_copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElevatedCockpitSurface,
                                contentColor = TelemetryCyan
                            ),
                            modifier = Modifier.testTag("copy_raw_json_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_copy_json),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepSpaceSurface)
                            .border(1.dp, GridLineBlue, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        val verticalScroll = rememberScrollState()
                        val horizontalScroll = rememberScrollState()

                        SelectionContainer {
                            Text(
                                text = rawJson,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = LuminousIce,
                                lineHeight = 16.sp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(verticalScroll)
                                    .horizontalScroll(horizontalScroll)
                                    .testTag("raw_json_text_view")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioStatPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DeepSpaceSurface,
        modifier = modifier.border(1.dp, GridLineBlue, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MutedSlate
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
        }
    }
}

@Composable
private fun RadioAircraftCard(
    ac: RadioAircraft,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ac.isEmergency) EmergencyCoral.copy(alpha = 0.15f)
            else DeepSpaceSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (ac.isEmergency) EmergencyCoral else GridLineBlue,
                RoundedCornerShape(10.dp)
            )
            .testTag("aircraft_card_${ac.hex}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Callsign, Hex, Squawk & RSSI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ac.displayCallsign,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (ac.isEmergency) EmergencyCoral else LuminousIce
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ElevatedCockpitSurface,
                        modifier = Modifier.border(0.5.dp, GridLineBlue, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = ac.hex.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TelemetryCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (ac.squawk != null) {
                        Text(
                            text = "SQ ${ac.squawk}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (ac.isEmergency) EmergencyCoral else MutedSlate
                        )
                    }
                }

                // Signal Strength RSSI
                ac.rssiDb?.let { rssi ->
                    val signalColor = when {
                        rssi > -15.0 -> EmeraldStatus
                        rssi > -25.0 -> AltitudeGold
                        else -> EmergencyCoral
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Signal",
                            tint = signalColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f dB", rssi),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = signalColor
                        )
                    }
                }
            }

            // Metrics row: Altitude, Ground Speed, Track, Position
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Altitude
                val alt = ac.altBaro ?: ac.altGeom
                Text(
                    text = if (alt != null) "$alt ft" else "Alt: —",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = AltitudeSkyBlue,
                    fontWeight = FontWeight.SemiBold
                )

                // Ground Speed
                ac.groundSpeedKt?.let { gs ->
                    Text(
                        text = "${gs.toInt()} kt",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AltitudeTeal
                    )
                }

                // Track heading arrow
                ac.trackDeg?.let { trk ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AltitudeGold,
                            modifier = Modifier
                                .size(12.dp)
                                .rotate(trk.toFloat() - 90f)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${trk.toInt()}°",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AltitudeGold
                        )
                    }
                }

                // Messages and recency
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${ac.messages ?: 0} msgs · ${String.format(Locale.US, "%.1fs", ac.seenSec ?: 0.0)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MutedSlate
                )
            }

            // Coordinates row if available
            if (ac.hasPosition) {
                Text(
                    text = String.format(Locale.US, "GPS: %.4f, %.4f (NIC %d)", ac.lat, ac.lon, ac.nic ?: 0),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MutedSlate
                )
            }
        }
    }
}
