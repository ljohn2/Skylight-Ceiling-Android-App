package com.example.skylight.model

data class Runway(
    val leIdent: String,
    val heIdent: String,
    val leLat: Double,
    val leLon: Double,
    val heLat: Double,
    val heLon: Double,
    val widthFt: Int = 200
)

data class Airport(
    val icao: String,
    val name: String,
    val fullName: String,
    val lat: Double,
    val lon: Double,
    val runways: List<Runway>
)

data class LocationProfile(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusMiles: Double,
    val airport: Airport? = null,
    val showAirport: Boolean = true
)

data class Palette(
    val bg: String = "#000000",
    val glyph: String = "#E8ECFF",
    val trail: String = "#6B7280",
    val accent: String = "#9B7ECF",
    val warn: String = "#FF5A47",
    val grid: String = "#3A4256",
    val text: String = "#AEB6C6"
)

data class ShowFields(
    val name: Boolean = true,
    val type: Boolean = true,
    val altitude: Boolean = true,
    val speed: Boolean = true,
    val verticalRate: Boolean = false,
    val destination: Boolean = true,
    val registration: Boolean = false
)

enum class ThemeMode(val wireValue: String, val label: String) {
    AMBIENT("ambient", "Ambient"),
    TELEMETRY("telemetry", "Telemetry"),
    FOCUS("focus", "Focus");

    companion object {
        fun fromWire(value: String?): ThemeMode =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: AMBIENT
    }
}

enum class ProjectionMode(val wireValue: String, val label: String) {
    MAP("map", "Map (Flat Ground)"),
    SKY("sky", "Sky (Look-Up Dome)");

    companion object {
        fun fromWire(value: String?): ProjectionMode =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: MAP
    }
}

enum class LabelDensity(val wireValue: String, val label: String) {
    ALL("all", "All"),
    NEAREST_N("nearestN", "Nearest N"),
    NEAREST_ONLY("nearestOnly", "Nearest Only");

    companion object {
        fun fromWire(value: String?): LabelDensity =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: ALL
    }
}

enum class NameDisplay(val wireValue: String, val label: String) {
    FLIGHT("flight", "Flight Callsign"),
    AIRLINE("airline", "Airline Name");

    companion object {
        fun fromWire(value: String?): NameDisplay =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: FLIGHT
    }
}

enum class LocationDisplay(val wireValue: String, val label: String) {
    NAME("name", "City Name"),
    IATA("iata", "IATA Code");

    companion object {
        fun fromWire(value: String?): LocationDisplay =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: NAME
    }
}

enum class DataSourceType(val wireValue: String, val label: String) {
    RADIO("radio", "Local Radio (dump1090)"),
    API("api", "Public API");

    companion object {
        fun fromWire(value: String?): DataSourceType =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: RADIO
    }
}

enum class SpeedUnit(val wireValue: String, val label: String) {
    KT("kt", "kt"),
    MPH("mph", "mph"),
    KMH("kmh", "km/h");

    companion object {
        fun fromWire(value: String?): SpeedUnit =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: KT
    }
}

enum class AltitudeUnit(val wireValue: String, val label: String) {
    FT("ft", "ft"),
    M("m", "m");

    companion object {
        fun fromWire(value: String?): AltitudeUnit =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: FT
    }
}

enum class DistanceUnit(val wireValue: String, val label: String) {
    MI("mi", "mi"),
    KM("km", "km");

    companion object {
        fun fromWire(value: String?): DistanceUnit =
            entries.firstOrNull { it.wireValue.equals(value, ignoreCase = true) } ?: MI
    }
}

data class PiSourceStatus(
    val source: DataSourceType = DataSourceType.RADIO,
    val ok: Boolean = true,
    val count: Int = 0,
    val positionedCount: Int = 0,
    val message: String? = null
)

data class ConstellationInfo(
    val id: String,
    val name: String
)

object ConstellationsCatalog {
    val ALL = listOf(
        ConstellationInfo("orion", "Orion"),
        ConstellationInfo("ursa_major", "Big Dipper"),
        ConstellationInfo("cassiopeia", "Cassiopeia"),
        ConstellationInfo("summer_triangle", "Summer Triangle"),
        ConstellationInfo("winter_triangle", "Winter Triangle")
    )

    fun defaultMap(): Map<String, Boolean> = ALL.associate { it.id to true }
}

data class SkylightConfig(
    // Location & Scope
    val centerLat: Double = 37.6213,
    val centerLon: Double = -122.3790,
    val locationName: String = "San Francisco International",
    val radiusMiles: Double = 3.0,
    val locationProfiles: List<LocationProfile> = defaultProfiles(),

    // Data Source URLs
    val radioUrl: String = "http://localhost:8080/data/aircraft.json",
    val apiUrl: String = "https://opendata.adsb.fi/api/v3/lat/{lat}/lon/{lon}/dist/{r}",

    // Usage Ping
    val telemetry: Boolean = true,

    // Calibration & Projection
    val rotationDeg: Float = 0f,
    val mirrorX: Boolean = true,
    val mirrorY: Boolean = false,
    val labelRotationDeg: Float = 0f,
    val projectionMode: ProjectionMode = ProjectionMode.MAP,

    // Filtering
    val minAltitudeFt: Int = 100,
    val maxAltitudeFt: Int = 60000,
    val hideOnGround: Boolean = true,

    // Motion
    val interpolate: Boolean = true,
    val maxExtrapolationSec: Int = 5,
    val staleSec: Int = 20,
    val smoothing: Float = 0.18f,
    val maxFps: Int = 0,

    // Visuals
    val theme: ThemeMode = ThemeMode.AMBIENT,
    val palette: Palette = Palette(),
    val glyphSizePx: Int = 22,
    val altitudeColor: Boolean = true,
    val trailSeconds: Int = 45,
    val brightness: Float = 1.0f,

    // Labels & Units
    val labelDensity: LabelDensity = LabelDensity.ALL,
    val nearestN: Int = 5,
    val showFields: ShowFields = ShowFields(),
    val nameDisplay: NameDisplay = NameDisplay.FLIGHT,
    val locationDisplay: LocationDisplay = LocationDisplay.NAME,
    val speedUnit: SpeedUnit = SpeedUnit.KT,
    val altitudeUnit: AltitudeUnit = AltitudeUnit.FT,
    val distanceUnit: DistanceUnit = DistanceUnit.MI,

    // Overlays
    val rangeRings: Boolean = true,
    val compass: Boolean = true,
    val highlightEmergency: Boolean = true,
    val showAirport: Boolean = true,
    val airport: Airport = AirportsCatalog.SFO,
    val showHud: Boolean = false,

    // Sky Layer & Planetarium
    val planetariumMode: Boolean = false,
    val showStars: Boolean = true,
    val showSun: Boolean = true,
    val showMoon: Boolean = true,
    val showSatellites: Boolean = true,
    val satelliteLabels: Boolean = false,
    val showPlanets: Boolean = true,
    val showSkyEvents: Boolean = false,
    val constellations: Map<String, Boolean> = ConstellationsCatalog.defaultMap(),
    val starMagLimit: Float = 2.6f,
    val starLabelMagLimit: Float = 0.3f,
    val skyTimeOffsetMin: Int = 0,

    // Window to Elsewhere
    val showDestArc: Boolean = true,
    val showRouteDetail: Boolean = true
)

object AggregatorPresets {
    val ALL = listOf(
        "adsb.fi" to "https://opendata.adsb.fi/api/v3/lat/{lat}/lon/{lon}/dist/{r}",
        "adsb.lol" to "https://api.adsb.lol/v2/point/{lat}/{lon}/{r}",
        "airplanes.live" to "https://api.airplanes.live/v2/point/{lat}/{lon}/{r}"
    )
}

object AirportsCatalog {
    val SFO = Airport(
        icao = "KSFO",
        name = "SFO",
        fullName = "San Francisco International Airport",
        lat = 37.6213,
        lon = -122.3790,
        runways = listOf(
            Runway("10L", "28R", 37.628742, -122.393410, 37.613538, -122.357160, 200),
            Runway("10R", "28L", 37.626298, -122.393124, 37.611720, -122.358367, 200),
            Runway("1L", "19R", 37.607898, -122.382950, 37.626476, -122.370630, 200),
            Runway("1R", "19L", 37.606333, -122.381061, 37.627346, -122.367124, 200)
        )
    )

    val LAX = Airport(
        icao = "KLAX",
        name = "LAX",
        fullName = "Los Angeles International Airport",
        lat = 33.9425,
        lon = -118.4081,
        runways = listOf(
            Runway("06L", "24R", 33.9491, -118.4312, 33.9521, -118.3985, 150),
            Runway("06R", "24L", 33.9468, -118.4354, 33.9502, -118.3971, 150),
            Runway("07L", "25R", 33.9358, -118.4191, 33.9399, -118.3752, 150),
            Runway("07R", "25L", 33.9337, -118.4184, 33.9375, -118.3782, 200)
        )
    )

    val JFK = Airport(
        icao = "KJFK",
        name = "JFK",
        fullName = "John F. Kennedy International Airport",
        lat = 40.6413,
        lon = -73.7781,
        runways = listOf(
            Runway("04L", "22R", 40.6221, -73.7856, 40.6512, -73.7623, 200),
            Runway("04R", "22L", 40.6254, -73.7702, 40.6458, -73.7540, 200),
            Runway("13L", "31R", 40.6575, -73.7905, 40.6380, -73.7585, 200),
            Runway("13R", "31L", 40.6485, -73.8168, 40.6215, -73.7712, 200)
        )
    )

    val LHR = Airport(
        icao = "EGLL",
        name = "LHR",
        fullName = "London Heathrow Airport",
        lat = 51.4700,
        lon = -0.4543,
        runways = listOf(
            Runway("09L", "27R", 51.4775, -0.4850, 51.4775, -0.4287, 164),
            Runway("09R", "27L", 51.4648, -0.4823, 51.4648, -0.4295, 164)
        )
    )

    val HND = Airport(
        icao = "RJTT",
        name = "HND",
        fullName = "Tokyo Haneda International Airport",
        lat = 35.5494,
        lon = 139.7798,
        runways = listOf(
            Runway("16R", "34L", 35.5625, 139.7681, 35.5378, 139.7824, 197),
            Runway("16L", "34R", 35.5681, 139.7845, 35.5408, 139.8012, 197),
            Runway("04", "22", 35.5390, 139.7665, 35.5562, 139.7875, 197)
        )
    )

    val ALL = listOf(SFO, LAX, JFK, LHR, HND)

    fun findByCode(code: String): Airport? {
        val clean = code.trim().uppercase()
        return ALL.firstOrNull { it.icao == clean || it.name == clean }
    }
}

fun defaultProfiles(): List<LocationProfile> = listOf(
    LocationProfile("sfo", "San Francisco (SFO)", 37.6213, -122.3790, 3.0, AirportsCatalog.SFO, true),
    LocationProfile("lax", "Los Angeles (LAX)", 33.9425, -118.4081, 5.0, AirportsCatalog.LAX, true),
    LocationProfile("jfk", "New York (JFK)", 40.6413, -73.7781, 5.0, AirportsCatalog.JFK, true),
    LocationProfile("lhr", "London Heathrow (LHR)", 51.4700, -0.4543, 5.0, AirportsCatalog.LHR, true),
    LocationProfile("hnd", "Tokyo Haneda (HND)", 35.5494, 139.7798, 5.0, AirportsCatalog.HND, true)
)
