package com.example.skylight

import com.example.skylight.data.PiControlClient
import com.example.skylight.model.AirportsCatalog
import com.example.skylight.model.ProjectionMode
import com.example.skylight.model.SkylightConfig
import com.example.skylight.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SkylightTest {

    @Test
    fun `configToJson and parseConfigJson round-trip preserves Raspberry Pi settings`() {
        val original = SkylightConfig(
            centerLat = 51.4700,
            centerLon = -0.4543,
            locationName = "London Heathrow",
            radiusMiles = 12.5,
            rotationDeg = 180f,
            mirrorX = false,
            projectionMode = ProjectionMode.SKY,
            theme = ThemeMode.TELEMETRY,
            airport = AirportsCatalog.LHR
        )
        val json = PiControlClient.configToJson(original)
        val parsed = PiControlClient.parseConfigJson(json, SkylightConfig())

        assertEquals(51.4700, parsed.centerLat, 1e-4)
        assertEquals(-0.4543, parsed.centerLon, 1e-4)
        assertEquals("London Heathrow", parsed.locationName)
        assertEquals(12.5, parsed.radiusMiles, 1e-4)
        assertEquals(180f, parsed.rotationDeg, 1e-2f)
        assertEquals(false, parsed.mirrorX)
        assertEquals(ProjectionMode.SKY, parsed.projectionMode)
        assertEquals(ThemeMode.TELEMETRY, parsed.theme)
        assertEquals("EGLL", parsed.airport.icao)
    }
}
