package com.example.skylight.ui.screens

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skylight.R
import com.example.skylight.model.RadioAircraft
import com.example.skylight.model.Runway
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
import com.example.skylight.util.RadarProjection
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class ProjectedTarget(
    val aircraft: RadioAircraft,
    val canvasOffset: Offset,
    val distanceNm: Double,
    val bearingDeg: Double
)

@Composable
fun LiveRadarScreen(
    uiState: SkylightControlUiState,
    onNavigateBack: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: (Double) -> Unit,
    onSelectAircraft: (String?) -> Unit,
    onTogglePolling: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val homeLat = uiState.config.centerLat
    val homeLon = uiState.config.centerLon
    val maxRangeNm = uiState.radarZoomMiles
    val mirrorX = uiState.config.mirrorX
    val mirrorY = uiState.config.mirrorY

    val aircraftList = uiState.radioSnapshot?.aircraft ?: emptyList()
    val selectedAircraft = remember(aircraftList, uiState.selectedAircraftHex) {
        aircraftList.firstOrNull { it.hex == uiState.selectedAircraftHex }
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Navigation, Title & Mirrored Control Panel Status
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
                            .testTag("radar_back_button")
                            .size(44.dp)
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
                            text = stringResource(R.string.title_live_radar),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = LuminousIce
                        )
                        val airportLabel = if (uiState.config.airport.icao.isNotBlank()) " · ${uiState.config.airport.icao}" else ""
                        Text(
                            text = "${uiState.config.locationName}$airportLabel · ${maxRangeNm.toInt()} NM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSlate
                        )
                    }
                }

                // Streaming toggle & In-range airframe counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onTogglePolling(null) },
                        modifier = Modifier
                            .testTag("radar_play_pause_button")
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElevatedCockpitSurface)
                            .border(1.dp, GridLineBlue, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (uiState.radarPolling) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Streaming",
                            tint = if (uiState.radarPolling) EmeraldStatus else MutedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DeepSpaceSurface,
                        modifier = Modifier.border(
                            1.dp,
                            if (aircraftList.any { it.hasPosition }) EmeraldStatus else GridLineBlue,
                            RoundedCornerShape(20.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.radarPolling) EmeraldStatus else AltitudeGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${aircraftList.count { it.hasPosition }} in range",
                                style = MaterialTheme.typography.labelSmall,
                                color = LuminousIce
                            )
                        }
                    }
                }
            }

            // Mirrored Settings Bar reflecting choices from Control Panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedCockpitSurface.copy(alpha = 0.85f),
                    modifier = Modifier.border(1.dp, if (mirrorX || mirrorY) TelemetryCyan.copy(alpha = 0.6f) else GridLineBlue, RoundedCornerShape(8.dp))
                ) {
                    val mirrorDesc = buildString {
                        append("MIRROR: ")
                        if (mirrorX) append("LOOK-UP (H)") else append("OFF")
                        if (mirrorY) append(" + V")
                    }
                    Text(
                        text = mirrorDesc,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = if (mirrorX || mirrorY) TelemetryCyan else MutedSlate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedCockpitSurface.copy(alpha = 0.85f),
                    modifier = Modifier.border(1.dp, GridLineBlue, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "FEED: ${uiState.status.source.label}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = LuminousIce,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (uiState.config.airport.runways.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElevatedCockpitSurface.copy(alpha = 0.85f),
                        modifier = Modifier.border(1.dp, GridLineBlue, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${uiState.config.airport.runways.size} RUNWAYS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = TelemetryCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Radar Canvas Container
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepSpaceSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, GridLineBlue, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Precompute projected targets for drawing & click detection
                    var currentProjected: List<ProjectedTarget> = remember { emptyList() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .pointerInput(aircraftList, maxRangeNm, homeLat, homeLon, mirrorX, mirrorY) {
                                detectTapGestures { tapOffset ->
                                    val threshold = 40f * density
                                    var closestTarget: ProjectedTarget? = null
                                    var minDistance = Float.MAX_VALUE

                                    for (target in currentProjected) {
                                        val dx = target.canvasOffset.x - tapOffset.x
                                        val dy = target.canvasOffset.y - tapOffset.y
                                        val dist = sqrt(dx * dx + dy * dy)
                                        if (dist < threshold && dist < minDistance) {
                                            minDistance = dist
                                            closestTarget = target
                                        }
                                    }

                                    onSelectAircraft(closestTarget?.aircraft?.hex)
                                }
                            }
                            .testTag("radar_canvas")
                    ) {
                        val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                        val radarRadius = min(size.width, size.height) / 2f - 16f

                        // 1. Draw Radar Background, Range Rings & Compass Cardinal Points
                        drawRadarGrid(
                            center = canvasCenter,
                            radius = radarRadius,
                            maxRangeNm = maxRangeNm,
                            mirrorX = mirrorX,
                            mirrorY = mirrorY
                        )

                        // 2. Draw Airport Runways if present on Control Panel
                        if (uiState.config.airport.runways.isNotEmpty()) {
                            drawAirportRunways(
                                runways = uiState.config.airport.runways,
                                homeLat = homeLat,
                                homeLon = homeLon,
                                maxRangeNm = maxRangeNm,
                                canvasCenter = canvasCenter,
                                radarRadius = radarRadius,
                                mirrorX = mirrorX,
                                mirrorY = mirrorY
                            )
                        }

                        // 3. Project all aircraft with valid positions
                        val projected = mutableListOf<ProjectedTarget>()
                        for (ac in aircraftList) {
                            if (!ac.hasPosition) continue
                            val lat = ac.lat ?: continue
                            val lon = ac.lon ?: continue

                            val distNm = RadarProjection.distanceNauticalMiles(homeLat, homeLon, lat, lon)
                            val bearing = RadarProjection.initialBearing(homeLat, homeLon, lat, lon)

                            val canvasOffset = RadarProjection.projectToCanvas(
                                bearingDeg = bearing,
                                distanceNm = distNm,
                                maxRangeNm = maxRangeNm,
                                center = canvasCenter,
                                radiusPx = radarRadius,
                                mirrorX = mirrorX,
                                mirrorY = mirrorY
                            )

                            projected.add(ProjectedTarget(ac, canvasOffset, distNm, bearing))
                        }
                        currentProjected = projected

                        // 4. Draw Aircraft Blips & Directional Triangles
                        for (target in projected) {
                            val ac = target.aircraft
                            val isSelected = ac.hex == uiState.selectedAircraftHex
                            val alt = ac.altBaro ?: ac.altGeom
                            val blipColor = RadarProjection.altitudeColor(alt, ac.isEmergency)

                            drawAircraftBlip(
                                target = target,
                                color = blipColor,
                                isSelected = isSelected,
                                mirrorX = mirrorX,
                                mirrorY = mirrorY
                            )
                        }

                        // 5. Center Home Location Marker
                        drawCircle(
                            color = TelemetryCyan.copy(alpha = 0.25f),
                            radius = 12f,
                            center = canvasCenter
                        )
                        drawCircle(
                            color = TelemetryCyan,
                            radius = 4f,
                            center = canvasCenter
                        )
                    }

                    // Floating Zoom Controls Overlay with generous padding between Zoom and Location buttons
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedCockpitSurface.copy(alpha = 0.9f),
                            modifier = Modifier.border(1.dp, GridLineBlue, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "${maxRangeNm.toInt()} NM",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TelemetryCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Zoom buttons grouped in a capsule
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(ElevatedCockpitSurface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
                                    .border(1.dp, GridLineBlue, RoundedCornerShape(24.dp))
                                    .padding(4.dp)
                            ) {
                                IconButton(
                                    onClick = onZoomIn,
                                    modifier = Modifier
                                        .testTag("radar_zoom_in_button")
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(DeepSpaceSurface)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.btn_zoom_in),
                                        tint = LuminousIce,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onZoomOut,
                                    modifier = Modifier
                                        .testTag("radar_zoom_out_button")
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(DeepSpaceSurface)
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LuminousIce
                                    )
                                }
                            }

                            // Dedicated Location Center Button with 16dp spacing from Zoom buttons
                            IconButton(
                                onClick = { onResetZoom(uiState.config.radiusMiles) },
                                modifier = Modifier
                                    .testTag("radar_center_home_button")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ElevatedCockpitSurface.copy(alpha = 0.95f))
                                    .border(1.dp, AltitudeGold.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = stringResource(R.string.btn_center_radar),
                                    tint = AltitudeGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Target Inspection HUD Card
            if (selectedAircraft != null) {
                val ac = selectedAircraft
                val distNm = if (ac.hasPosition) {
                    RadarProjection.distanceNauticalMiles(
                        homeLat,
                        homeLon,
                        ac.lat ?: 0.0,
                        ac.lon ?: 0.0
                    )
                } else null

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (ac.isEmergency) EmergencyCoral.copy(alpha = 0.15f)
                        else DeepSpaceSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (ac.isEmergency) EmergencyCoral else TelemetryCyan.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("radar_flight_card")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Callsign, Hex, Squawk & Dismiss button
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
                                    style = MaterialTheme.typography.titleMedium,
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
                                        color = if (ac.isEmergency) EmergencyCoral else AltitudeGold
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onSelectAircraft(null) },
                                modifier = Modifier
                                    .testTag("radar_deselect_button")
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.btn_close_inspection),
                                    tint = MutedSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Row 2: Metrics (Altitude, Speed, Distance, Track)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val alt = ac.altBaro ?: ac.altGeom
                            Text(
                                text = if (alt != null) "$alt ft" else "Alt: —",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AltitudeSkyBlue
                            )

                            ac.groundSpeedKt?.let { gs ->
                                Text(
                                    text = "${gs.toInt()} kt",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = AltitudeTeal
                                )
                            }

                            distNm?.let { d ->
                                Text(
                                    text = String.format(Locale.US, "%.1f NM out", d),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = AltitudeGold
                                )
                            }

                            ac.trackDeg?.let { trk ->
                                Text(
                                    text = "HDG ${trk.toInt()}°",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = LuminousIce
                                )
                            }
                        }
                    }
                }
            } else {
                // Tip row when nothing is selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MutedSlate,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.label_no_selection_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
            }
        }
    }
}

/**
 * Draws the concentric range rings, crosshairs, and cardinal compass letters.
 */
private fun DrawScope.drawRadarGrid(
    center: Offset,
    radius: Float,
    maxRangeNm: Double,
    mirrorX: Boolean,
    mirrorY: Boolean
) {
    // Outer perimeter ring
    drawCircle(
        color = GridLineBlue,
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Concentric range rings: 25%, 50%, 75%
    val ringSteps = listOf(0.25f, 0.50f, 0.75f)
    for (step in ringSteps) {
        val r = radius * step
        drawCircle(
            color = GridLineBlue.copy(alpha = 0.55f),
            radius = r,
            center = center,
            style = Stroke(width = 1f)
        )
    }

    // Crosshairs
    drawLine(
        color = GridLineBlue.copy(alpha = 0.6f),
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 1f
    )
    drawLine(
        color = GridLineBlue.copy(alpha = 0.6f),
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = 1f
    )

    // Range distance text on 50% and 100% rings
    val paint = Paint().apply {
        color = MutedSlate.toArgb()
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    val halfRange = (maxRangeNm * 0.5).toInt()
    val fullRange = maxRangeNm.toInt()

    drawContext.canvas.nativeCanvas.drawText(
        "$halfRange NM",
        center.x + 8f,
        center.y - (radius * 0.5f) - 6f,
        paint
    )
    drawContext.canvas.nativeCanvas.drawText(
        "$fullRange NM",
        center.x + 8f,
        center.y - radius + 22f,
        paint
    )

    // Cardinal compass points (N, S, E, W) respecting mirroring
    val northY = if (mirrorY) center.y + radius - 10f else center.y - radius + 22f
    val southY = if (mirrorY) center.y - radius + 22f else center.y + radius - 10f
    val eastX = if (mirrorX) center.x - radius + 12f else center.x + radius - 26f
    val westX = if (mirrorX) center.x + radius - 26f else center.x - radius + 12f

    val cardinalPaint = Paint().apply {
        color = TelemetryCyan.toArgb()
        textSize = 28f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    drawContext.canvas.nativeCanvas.drawText("N", center.x, northY, cardinalPaint)
    drawContext.canvas.nativeCanvas.drawText("S", center.x, southY, cardinalPaint)
    drawContext.canvas.nativeCanvas.drawText("E", eastX, center.y + 10f, cardinalPaint)
    drawContext.canvas.nativeCanvas.drawText("W", westX, center.y + 10f, cardinalPaint)
}

/**
 * Draws airport runways configured on the Control Panel onto the live radar projection.
 */
private fun DrawScope.drawAirportRunways(
    runways: List<Runway>,
    homeLat: Double,
    homeLon: Double,
    maxRangeNm: Double,
    canvasCenter: Offset,
    radarRadius: Float,
    mirrorX: Boolean,
    mirrorY: Boolean
) {
    val runwayLabelPaint = Paint().apply {
        color = TelemetryCyan.copy(alpha = 0.85f).toArgb()
        textSize = 20f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    for (rw in runways) {
        val distLe = RadarProjection.distanceNauticalMiles(homeLat, homeLon, rw.leLat, rw.leLon)
        val bearingLe = RadarProjection.initialBearing(homeLat, homeLon, rw.leLat, rw.leLon)
        val pLe = RadarProjection.projectToCanvas(
            bearingDeg = bearingLe,
            distanceNm = distLe,
            maxRangeNm = maxRangeNm,
            center = canvasCenter,
            radiusPx = radarRadius,
            mirrorX = mirrorX,
            mirrorY = mirrorY
        )

        val distHe = RadarProjection.distanceNauticalMiles(homeLat, homeLon, rw.heLat, rw.heLon)
        val bearingHe = RadarProjection.initialBearing(homeLat, homeLon, rw.heLat, rw.heLon)
        val pHe = RadarProjection.projectToCanvas(
            bearingDeg = bearingHe,
            distanceNm = distHe,
            maxRangeNm = maxRangeNm,
            center = canvasCenter,
            radiusPx = radarRadius,
            mirrorX = mirrorX,
            mirrorY = mirrorY
        )

        // Draw runway strip line
        drawLine(
            color = TelemetryCyan.copy(alpha = 0.9f),
            start = pLe,
            end = pHe,
            strokeWidth = 4.5f
        )

        // Threshold identifier labels if zoomed in reasonably close
        if (maxRangeNm <= 45.0) {
            drawContext.canvas.nativeCanvas.drawText(rw.leIdent, pLe.x, pLe.y - 6f, runwayLabelPaint)
            drawContext.canvas.nativeCanvas.drawText(rw.heIdent, pHe.x, pHe.y + 16f, runwayLabelPaint)
        }
    }
}

/**
 * Draws an individual aircraft blip: directional triangle, altitude color, label, and selection halo.
 */
private fun DrawScope.drawAircraftBlip(
    target: ProjectedTarget,
    color: Color,
    isSelected: Boolean,
    mirrorX: Boolean,
    mirrorY: Boolean
) {
    val center = target.canvasOffset
    val trkDeg = target.aircraft.trackDeg ?: target.bearingDeg

    // If selected, draw glowing halo ring
    if (isSelected) {
        drawCircle(
            color = TelemetryCyan.copy(alpha = 0.35f),
            radius = 18f,
            center = center
        )
        drawCircle(
            color = TelemetryCyan,
            radius = 14f,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    // Directional triangle pointing along track respecting mirror settings
    var angleDeg = trkDeg - 90.0
    if (mirrorX) angleDeg = 180.0 - angleDeg
    if (mirrorY) angleDeg = -angleDeg
    val angleRad = Math.toRadians(angleDeg)

    val headSize = 9f
    val wingSize = 6f

    val tip = Offset(
        center.x + (headSize * cos(angleRad)).toFloat(),
        center.y + (headSize * sin(angleRad)).toFloat()
    )
    val left = Offset(
        center.x + (wingSize * cos(angleRad + Math.toRadians(140.0))).toFloat(),
        center.y + (wingSize * sin(angleRad + Math.toRadians(140.0))).toFloat()
    )
    val right = Offset(
        center.x + (wingSize * cos(angleRad - Math.toRadians(140.0))).toFloat(),
        center.y + (wingSize * sin(angleRad - Math.toRadians(140.0))).toFloat()
    )

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(center.x, center.y)
        lineTo(right.x, right.y)
        close()
    }

    drawPath(path = path, color = color)

    // Aircraft label (callsign or hex) with positioning adapted to mirroring
    val labelText = target.aircraft.displayCallsign
    val labelPaint = Paint().apply {
        this.color = if (isSelected) Color.White.toArgb() else LuminousIce.copy(alpha = 0.85f).toArgb()
        textSize = 20f
        isAntiAlias = true
        typeface = android.graphics.Typeface.MONOSPACE
        if (isSelected) isFakeBoldText = true
        textAlign = if (mirrorX) Paint.Align.RIGHT else Paint.Align.LEFT
    }

    val labelX = if (mirrorX) center.x - 10f else center.x + 10f
    drawContext.canvas.nativeCanvas.drawText(
        labelText,
        labelX,
        center.y + 4f,
        labelPaint
    )
}
