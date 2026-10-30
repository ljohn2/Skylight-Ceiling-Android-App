package com.example.skylight.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.skylight.ui.theme.AltitudeGold
import com.example.skylight.ui.theme.AltitudePeriwinkle
import com.example.skylight.ui.theme.AltitudeSkyBlue
import com.example.skylight.ui.theme.AltitudeTeal
import com.example.skylight.ui.theme.EmergencyCoral
import com.example.skylight.ui.theme.MutedSlate
import com.example.skylight.ui.theme.NebulaViolet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RadarProjection {
    private const val EARTH_RADIUS_NM = 3440.065 // Earth radius in nautical miles

    /**
     * Computes great-circle distance between two coordinates in nautical miles using Haversine formula.
     */
    fun distanceNauticalMiles(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_NM * c
    }

    /**
     * Computes initial bearing from (lat1, lon1) towards (lat2, lon2) in degrees (0..360, clockwise from North).
     */
    fun initialBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)

        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val degrees = Math.toDegrees(atan2(y, x))
        return (degrees + 360.0) % 360.0
    }

    /**
     * Converts a target's bearing and distance from home into 2D canvas coordinates.
     */
    fun projectToCanvas(
        bearingDeg: Double,
        distanceNm: Double,
        maxRangeNm: Double,
        center: Offset,
        radiusPx: Float,
        mirrorX: Boolean = false,
        mirrorY: Boolean = false
    ): Offset {
        val clampedDist = distanceNm.coerceAtLeast(0.0)
        val normDist = (clampedDist / maxRangeNm.coerceAtLeast(0.1)).toFloat().coerceAtMost(1.05f)

        // Standard screen convention: North (0 deg) is straight up (-Y), East (90 deg) is right (+X)
        // angle from positive X axis counter-clockwise: bearing - 90 deg clockwise -> radians
        val angleRad = Math.toRadians(bearingDeg - 90.0)
        var dx = (normDist * radiusPx * cos(angleRad)).toFloat()
        var dy = (normDist * radiusPx * sin(angleRad)).toFloat()

        if (mirrorX) dx = -dx
        if (mirrorY) dy = -dy

        return Offset(center.x + dx, center.y + dy)
    }

    /**
     * Colorizes aircraft according to barometric/geometric altitude or emergency status.
     */
    fun altitudeColor(altitudeFeet: Int?, isEmergency: Boolean = false): Color {
        if (isEmergency) return EmergencyCoral
        if (altitudeFeet == null) return MutedSlate
        return when {
            altitudeFeet <= 0 -> MutedSlate
            altitudeFeet < 5_000 -> AltitudeTeal
            altitudeFeet < 15_000 -> AltitudeSkyBlue
            altitudeFeet < 25_000 -> AltitudePeriwinkle
            altitudeFeet < 35_000 -> NebulaViolet
            else -> AltitudeGold
        }
    }
}
