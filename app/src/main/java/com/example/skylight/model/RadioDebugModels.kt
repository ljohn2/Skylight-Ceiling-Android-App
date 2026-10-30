package com.example.skylight.model

data class RadioAircraft(
    val hex: String,
    val flight: String? = null,
    val altBaro: Int? = null,
    val altGeom: Int? = null,
    val groundSpeedKt: Double? = null,
    val trackDeg: Double? = null,
    val baroRateFpm: Int? = null,
    val squawk: String? = null,
    val emergency: String? = null,
    val category: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val nic: Int? = null,
    val rcMeters: Int? = null,
    val seenPosSec: Double? = null,
    val version: Int? = null,
    val nacP: Int? = null,
    val messages: Long? = null,
    val seenSec: Double? = null,
    val rssiDb: Double? = null
) {
    val hasPosition: Boolean get() = lat != null && lon != null
    val displayCallsign: String get() = flight?.trim()?.ifBlank { null } ?: hex.uppercase()
    val isEmergency: Boolean get() = (emergency != null && emergency != "none") ||
            squawk in listOf("7500", "7600", "7700")
}

data class RadioDebugSnapshot(
    val now: Double = 0.0,
    val totalMessages: Long = 0L,
    val aircraft: List<RadioAircraft> = emptyList(),
    val rawJson: String = "",
    val fetchTimestampMillis: Long = System.currentTimeMillis(),
    val sourceUrl: String = ""
) {
    val positionedCount: Int get() = aircraft.count { it.hasPosition }
    val totalCount: Int get() = aircraft.size
    val averageRssi: Double? get() {
        val withRssi = aircraft.mapNotNull { it.rssiDb }
        return if (withRssi.isNotEmpty()) withRssi.average() else null
    }
}
