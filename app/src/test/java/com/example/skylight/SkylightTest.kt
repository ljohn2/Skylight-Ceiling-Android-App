package com.example.skylight

import com.example.skylight.data.PiControlClient
import com.example.skylight.model.AirportsCatalog
import com.example.skylight.model.ProjectionMode
import com.example.skylight.model.SkylightConfig
import com.example.skylight.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test
    fun `default presets do not include plain skylight local or raspberrypi local`() {
        org.junit.Assert.assertFalse(
            "http://skylight.local should be removed from presets",
            PiControlClient.PI_URL_PRESETS.contains("http://skylight.local")
        )
        org.junit.Assert.assertFalse(
            "raspberrypi.local should be removed from presets",
            PiControlClient.PI_URL_PRESETS.any { it.contains("raspberrypi.local") }
        )
        org.junit.Assert.assertTrue(
            PiControlClient.PI_URL_PRESETS.contains("http://skylight.local:3000")
        )
    }

    @Test
    fun `SkylightViewModel adds custom URL and excludes plain skylight local and raspberrypi local`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.skylight.ui.SkylightViewModel(app)

        org.junit.Assert.assertFalse(vm.uiState.value.savedUrls.contains("http://skylight.local"))
        org.junit.Assert.assertFalse(vm.uiState.value.savedUrls.any { it.contains("raspberrypi.local") })

        vm.addSavedUrl("http://raspberrypi.local:3000")
        org.junit.Assert.assertFalse(vm.uiState.value.savedUrls.any { it.contains("raspberrypi.local") })

        vm.addSavedUrl("raspberrypi.local")
        org.junit.Assert.assertFalse(vm.uiState.value.savedUrls.any { it.contains("raspberrypi.local") })

        vm.addSavedUrl("192.168.1.150:3000")
        org.junit.Assert.assertTrue(vm.uiState.value.savedUrls.contains("http://192.168.1.150:3000"))

        vm.removeSavedUrl("http://192.168.1.150:3000")
        org.junit.Assert.assertFalse(vm.uiState.value.savedUrls.contains("http://192.168.1.150:3000"))
    }

    @Test
    fun `RadioDebugClient parseSnapshot parses dump1090 readsb aircraft json correctly`() {
        val sampleJson = """
            {
              "now": 1718293849.2,
              "messages": 542190,
              "aircraft": [
                {
                  "hex": "a1b2c3",
                  "flight": "UAL123  ",
                  "alt_baro": 34000,
                  "alt_geom": 34150,
                  "gs": 450.5,
                  "track": 92.0,
                  "baro_rate": -64,
                  "squawk": "7700",
                  "emergency": "general",
                  "category": "A3",
                  "lat": 37.6188,
                  "lon": -122.3750,
                  "nic": 8,
                  "rc": 186,
                  "seen_pos": 1.2,
                  "version": 2,
                  "nac_p": 9,
                  "messages": 128,
                  "seen": 0.4,
                  "rssi": -14.2
                },
                {
                  "hex": "400a12",
                  "flight": null,
                  "alt_baro": "ground",
                  "messages": 12,
                  "seen": 8.5
                }
              ]
            }
        """.trimIndent()

        val client = com.example.skylight.data.RadioDebugClient()
        val snapshot = client.parseSnapshot(sampleJson, "http://localhost:8080/data/aircraft.json")

        org.junit.Assert.assertEquals(1718293849.2, snapshot.now, 1e-3)
        org.junit.Assert.assertEquals(542190L, snapshot.totalMessages)
        org.junit.Assert.assertEquals(2, snapshot.aircraft.size)
        org.junit.Assert.assertEquals(1, snapshot.positionedCount)

        val first = snapshot.aircraft[0]
        org.junit.Assert.assertEquals("a1b2c3", first.hex)
        org.junit.Assert.assertEquals("UAL123", first.displayCallsign)
        org.junit.Assert.assertEquals(34000, first.altBaro)
        org.junit.Assert.assertEquals(true, first.hasPosition)
        org.junit.Assert.assertEquals(true, first.isEmergency)
        org.junit.Assert.assertEquals(-14.2, first.rssiDb ?: 0.0, 1e-2)

        val second = snapshot.aircraft[1]
        org.junit.Assert.assertEquals("400a12", second.hex)
        org.junit.Assert.assertEquals("400A12", second.displayCallsign)
        org.junit.Assert.assertNull(second.altBaro)
        org.junit.Assert.assertEquals(false, second.hasPosition)
        org.junit.Assert.assertEquals(false, second.isEmergency)
    }

    @Test
    fun `SkylightViewModel manages AppScreen navigation and syncs radio url from pi url`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.skylight.ui.SkylightViewModel(app)

        org.junit.Assert.assertEquals(
            com.example.skylight.ui.AppScreen.CONTROL_PANEL,
            vm.uiState.value.currentScreen
        )
        org.junit.Assert.assertEquals(
            "http://skylight.local:8080/data/aircraft.json",
            com.example.skylight.data.RadioDebugClient.DEFAULT_RADIO_URL
        )
        org.junit.Assert.assertEquals(
            "http://skylight.local:8080/data/aircraft.json",
            vm.uiState.value.radioUrl
        )

        // Changing Pi URL syncs the host portion to radio debug URL
        vm.setPiUrl("http://192.168.1.50:3000")
        org.junit.Assert.assertEquals("http://192.168.1.50:3000", vm.uiState.value.piUrl)
        org.junit.Assert.assertEquals(
            "http://192.168.1.50:8080/data/aircraft.json",
            vm.uiState.value.radioUrl
        )

        // Upload autofill word is ignored and does not corrupt piUrl or radioUrl
        vm.setPiUrl("upload")
        org.junit.Assert.assertEquals("http://192.168.1.50:3000", vm.uiState.value.piUrl)

        vm.navigateTo(com.example.skylight.ui.AppScreen.RADIO_DEBUG)
        org.junit.Assert.assertEquals(
            com.example.skylight.ui.AppScreen.RADIO_DEBUG,
            vm.uiState.value.currentScreen
        )

        vm.navigateBack()
        org.junit.Assert.assertEquals(
            com.example.skylight.ui.AppScreen.CONTROL_PANEL,
            vm.uiState.value.currentScreen
        )
    }

    @Test
    fun `RadarProjection calculates distance and bearing accurately`() {
        // San Francisco (37.7749, -122.4194) to Oakland (37.8044, -122.2712) ~ 7.4 NM
        val distNm = com.example.skylight.util.RadarProjection.distanceNauticalMiles(
            37.7749, -122.4194,
            37.8044, -122.2712
        )
        org.junit.Assert.assertTrue("Distance should be ~7.4 NM, was $distNm", distNm in 6.0..9.0)

        val bearing = com.example.skylight.util.RadarProjection.initialBearing(
            37.7749, -122.4194,
            37.8044, -122.2712
        )
        org.junit.Assert.assertTrue("Bearing should be east-northeast (60..80 deg), was $bearing", bearing in 50.0..90.0)
    }

    @Test
    fun `RadarProjection projects canvas offset and respects range scale`() {
        val center = androidx.compose.ui.geometry.Offset(200f, 200f)
        val radiusPx = 180f
        // Target straight North (bearing 0 deg) at max range
        val northPt = com.example.skylight.util.RadarProjection.projectToCanvas(
            bearingDeg = 0.0,
            distanceNm = 20.0,
            maxRangeNm = 20.0,
            center = center,
            radiusPx = radiusPx
        )
        // Straight north means X is center.x, Y is center.y - radiusPx
        org.junit.Assert.assertEquals(200f, northPt.x, 1f)
        org.junit.Assert.assertEquals(20f, northPt.y, 1f)
    }

    @Test
    fun `SkylightViewModel controls radar zoom and target selection`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.skylight.ui.SkylightViewModel(app)

        vm.navigateTo(com.example.skylight.ui.AppScreen.LIVE_RADAR)
        org.junit.Assert.assertEquals(
            com.example.skylight.ui.AppScreen.LIVE_RADAR,
            vm.uiState.value.currentScreen
        )

        val initialZoom = vm.uiState.value.radarZoomMiles
        vm.zoomInRadar()
        org.junit.Assert.assertTrue(vm.uiState.value.radarZoomMiles < initialZoom)

        vm.zoomOutRadar()
        vm.zoomOutRadar()
        org.junit.Assert.assertTrue(vm.uiState.value.radarZoomMiles > initialZoom)

        vm.selectAircraftHex("a1b2c3")
        org.junit.Assert.assertEquals("a1b2c3", vm.uiState.value.selectedAircraftHex)

        vm.selectAircraftHex(null)
        org.junit.Assert.assertNull(vm.uiState.value.selectedAircraftHex)
    }

    @Test
    fun `MainActivity launches successfully without crashing`() {
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                org.junit.Assert.assertNotNull(activity)
            }
        }
    }
}
