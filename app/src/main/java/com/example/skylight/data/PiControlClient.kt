package com.example.skylight.data

import com.example.skylight.model.Airport
import com.example.skylight.model.AirportsCatalog
import com.example.skylight.model.AltitudeUnit
import com.example.skylight.model.ConstellationsCatalog
import com.example.skylight.model.DataSourceType
import com.example.skylight.model.DistanceUnit
import com.example.skylight.model.LabelDensity
import com.example.skylight.model.LocationDisplay
import com.example.skylight.model.LocationProfile
import com.example.skylight.model.NameDisplay
import com.example.skylight.model.Palette
import com.example.skylight.model.PiSourceStatus
import com.example.skylight.model.ProjectionMode
import com.example.skylight.model.Runway
import com.example.skylight.model.ShowFields
import com.example.skylight.model.SkylightConfig
import com.example.skylight.model.SpeedUnit
import com.example.skylight.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeocodeResult(
    val name: String,
    val lat: Double,
    val lon: Double
)

class PiControlClient {
    private val userAgent = "skylight-android-control/1.0"

    fun normalizeBaseUrl(rawUrl: String): String {
        val fallback = rawUrl.trim().ifBlank { DEFAULT_PI_URL }
        val withoutPath = fallback
            .removeSuffix("/")
            .removeSuffix("/control")
            .removeSuffix("/")
        return if (withoutPath.startsWith("http://") || withoutPath.startsWith("https://")) {
            withoutPath
        } else {
            "http://$withoutPath"
        }
    }

    suspend fun fetchConfig(piBaseUrl: String): SkylightConfig? = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(piBaseUrl)
        try {
            val json = getJsonObject("$base/api/config") ?: return@withContext null
            parseConfigJson(json, SkylightConfig())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun patchConfig(piBaseUrl: String, config: SkylightConfig): SkylightConfig? =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(piBaseUrl)
            try {
                val payload = configToJson(config)
                val response = postJsonObject("$base/api/config", payload) ?: return@withContext null
                parseConfigJson(response, config)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun resetConfigOnPi(piBaseUrl: String): SkylightConfig? = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(piBaseUrl)
        try {
            val response = postJsonObject("$base/api/config/reset", JSONObject()) ?: return@withContext null
            parseConfigJson(response, SkylightConfig())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchStatus(piBaseUrl: String): PiSourceStatus? = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(piBaseUrl)
        try {
            val json = getJsonObject("$base/api/status") ?: return@withContext null
            parseStatusJson(json)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun setSource(piBaseUrl: String, source: DataSourceType): PiSourceStatus? =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(piBaseUrl)
            try {
                val body = JSONObject().put("source", source.wireValue)
                val json = postJsonObject("$base/api/source", body) ?: return@withContext null
                parseStatusJson(json)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun fetchTrafficCentroid(piBaseUrl: String): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(piBaseUrl)
            try {
                val json = getJsonObject("$base/api/aircraft") ?: return@withContext null
                val arr = json.optJSONArray("aircraft") ?: return@withContext null
                var sumLat = 0.0
                var sumLon = 0.0
                var count = 0
                for (i in 0 until arr.length()) {
                    val ac = arr.optJSONObject(i) ?: continue
                    if (ac.has("lat") && ac.has("lon")) {
                        val lat = ac.optDouble("lat", Double.NaN)
                        val lon = ac.optDouble("lon", Double.NaN)
                        if (!lat.isNaN() && !lon.isNaN()) {
                            sumLat += lat
                            sumLon += lon
                            count++
                        }
                    }
                }
                if (count > 0) (sumLat / count) to (sumLon / count) else null
            } catch (_: Exception) {
                null
            }
        }

    suspend fun geocodeLocation(piBaseUrl: String, query: String): GeocodeResult? =
        withContext(Dispatchers.IO) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return@withContext null

            // 1. Direct lat,lon literal check
            val parts = trimmed.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lon = parts[1].trim().toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    return@withContext GeocodeResult("%.4f, %.4f".format(lat, lon), lat, lon)
                }
            }

            // 2. Try the Raspberry Pi server's /api/geocode endpoint first
            val base = normalizeBaseUrl(piBaseUrl)
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            try {
                val json = getJsonObject("$base/api/geocode?q=$encoded")
                if (json != null && json.has("lat") && json.has("lon")) {
                    return@withContext GeocodeResult(
                        name = json.optString("name", trimmed),
                        lat = json.getDouble("lat"),
                        lon = json.getDouble("lon")
                    )
                }
            } catch (_: Exception) {
                // Fall back to local airport catalog or Nominatim
            }

            // 3. Built-in airport catalog
            AirportsCatalog.findByCode(trimmed)?.let { ap ->
                return@withContext GeocodeResult("${ap.fullName} (${ap.name})", ap.lat, ap.lon)
            }

            // 4. Direct OpenStreetMap Nominatim lookup
            try {
                val url = URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=$encoded")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3500
                    readTimeout = 3500
                    setRequestProperty("User-Agent", userAgent)
                }
                if (conn.responseCode in 200..299) {
                    val raw = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONArray(raw)
                    if (arr.length() > 0) {
                        val hit = arr.getJSONObject(0)
                        val name = hit.optString("display_name", trimmed).split(",").take(2).joinToString(",")
                        return@withContext GeocodeResult(
                            name = name,
                            lat = hit.getString("lat").toDouble(),
                            lon = hit.getString("lon").toDouble()
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore
            }
            null
        }

    suspend fun lookupAirportRunways(piBaseUrl: String, code: String): Airport? =
        withContext(Dispatchers.IO) {
            val trimmed = code.trim().uppercase()
            if (trimmed.isEmpty()) return@withContext null

            val base = normalizeBaseUrl(piBaseUrl)
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            try {
                val json = getJsonObject("$base/api/airport?code=$encoded")
                if (json != null && json.has("icao")) {
                    return@withContext parseAirportJson(json)
                }
            } catch (_: Exception) {
                // Fall back to built-in catalog
            }

            AirportsCatalog.findByCode(trimmed)
        }

    private fun getJsonObject(urlString: String): JSONObject? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 2500
            readTimeout = 2500
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        if (conn.responseCode !in 200..299) return null
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    private fun postJsonObject(urlString: String, body: JSONObject): JSONObject? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 2500
            readTimeout = 2500
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        conn.outputStream.use { os ->
            os.write(body.toString().toByteArray(Charsets.UTF_8))
        }
        if (conn.responseCode !in 200..299) return null
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    companion object {
        const val DEFAULT_PI_URL = "http://skylight.local:3000"
        val PI_URL_PRESETS = listOf(
            "http://skylight.local:3000"
        )

        fun isExcludedUrl(url: String): Boolean {
            val clean = url.trim().lowercase().removeSuffix("/")
            return clean == "http://skylight.local" ||
                   clean == "https://skylight.local" ||
                   clean == "skylight.local" ||
                   clean == "upload" ||
                   clean == "http://upload" ||
                   clean.contains("raspberrypi.local")
        }

        fun parseStatusJson(json: JSONObject): PiSourceStatus {
            return PiSourceStatus(
                source = DataSourceType.fromWire(json.optString("source", "radio")),
                ok = json.optBoolean("ok", true),
                count = json.optInt("count", 0),
                positionedCount = json.optInt("positionedCount", json.optInt("count", 0)),
                message = json.optString("message", "").ifEmpty { null }
            )
        }

        fun parseAirportJson(json: JSONObject): Airport {
            val runwaysArr = json.optJSONArray("runways") ?: JSONArray()
            val runways = mutableListOf<Runway>()
            for (i in 0 until runwaysArr.length()) {
                val r = runwaysArr.optJSONObject(i) ?: continue
                val le = r.optJSONArray("le")
                val he = r.optJSONArray("he")
                if (le != null && he != null && le.length() >= 2 && he.length() >= 2) {
                    runways.add(
                        Runway(
                            leIdent = r.optString("leIdent", ""),
                            heIdent = r.optString("heIdent", ""),
                            leLat = le.optDouble(0),
                            leLon = le.optDouble(1),
                            heLat = he.optDouble(0),
                            heLon = he.optDouble(1),
                            widthFt = r.optInt("widthFt", 200)
                        )
                    )
                }
            }
            return Airport(
                icao = json.optString("icao", "KSFO"),
                name = json.optString("name", "SFO"),
                fullName = json.optString("fullName", json.optString("name", "SFO")),
                lat = json.optDouble("lat", 37.6213),
                lon = json.optDouble("lon", -122.3790),
                runways = runways.ifEmpty { AirportsCatalog.SFO.runways }
            )
        }

        fun airportToJson(airport: Airport): JSONObject {
            val rwArr = JSONArray()
            airport.runways.forEach { r ->
                rwArr.put(
                    JSONObject()
                        .put("leIdent", r.leIdent)
                        .put("heIdent", r.heIdent)
                        .put("le", JSONArray().put(r.leLat).put(r.leLon))
                        .put("he", JSONArray().put(r.heLat).put(r.heLon))
                        .put("widthFt", r.widthFt)
                )
            }
            return JSONObject()
                .put("icao", airport.icao)
                .put("name", airport.name)
                .put("fullName", airport.fullName)
                .put("lat", airport.lat)
                .put("lon", airport.lon)
                .put("runways", rwArr)
        }

        fun parseConfigJson(json: JSONObject, base: SkylightConfig): SkylightConfig {
            val paletteObj = json.optJSONObject("palette")
            val palette = if (paletteObj != null) {
                Palette(
                    bg = paletteObj.optString("bg", base.palette.bg),
                    glyph = paletteObj.optString("glyph", base.palette.glyph),
                    trail = paletteObj.optString("trail", base.palette.trail),
                    accent = paletteObj.optString("accent", base.palette.accent),
                    warn = paletteObj.optString("warn", base.palette.warn),
                    grid = paletteObj.optString("grid", base.palette.grid),
                    text = paletteObj.optString("text", base.palette.text)
                )
            } else {
                base.palette
            }

            val fieldsObj = json.optJSONObject("showFields")
            val showFields = if (fieldsObj != null) {
                ShowFields(
                    name = fieldsObj.optBoolean("name", base.showFields.name),
                    type = fieldsObj.optBoolean("type", base.showFields.type),
                    altitude = fieldsObj.optBoolean("altitude", base.showFields.altitude),
                    speed = fieldsObj.optBoolean("speed", base.showFields.speed),
                    verticalRate = fieldsObj.optBoolean("verticalRate", base.showFields.verticalRate),
                    destination = fieldsObj.optBoolean("destination", base.showFields.destination),
                    registration = fieldsObj.optBoolean("registration", base.showFields.registration)
                )
            } else {
                base.showFields
            }

            val profilesArr = json.optJSONArray("locationProfiles")
            val profiles = if (profilesArr != null) {
                buildList {
                    for (i in 0 until profilesArr.length()) {
                        val p = profilesArr.optJSONObject(i) ?: continue
                        val apObj = p.optJSONObject("airport")
                        add(
                            LocationProfile(
                                id = p.optString("id", "p_$i"),
                                name = p.optString("name", "Saved Place"),
                                lat = p.optDouble("lat", base.centerLat),
                                lon = p.optDouble("lon", base.centerLon),
                                radiusMiles = p.optDouble("radiusMiles", base.radiusMiles),
                                airport = apObj?.let { parseAirportJson(it) },
                                showAirport = p.optBoolean("showAirport", true)
                            )
                        )
                    }
                }
            } else {
                base.locationProfiles
            }

            val constObj = json.optJSONObject("constellations")
            val constellations = if (constObj != null) {
                val map = ConstellationsCatalog.defaultMap().toMutableMap()
                for (c in ConstellationsCatalog.ALL) {
                    if (constObj.has(c.id)) {
                        map[c.id] = constObj.optBoolean(c.id, true)
                    }
                }
                map
            } else {
                base.constellations
            }

            val airportObj = json.optJSONObject("airport")
            val airport = airportObj?.let { parseAirportJson(it) } ?: base.airport

            return base.copy(
                centerLat = json.optDouble("centerLat", base.centerLat),
                centerLon = json.optDouble("centerLon", base.centerLon),
                locationName = json.optString("locationName", base.locationName),
                radiusMiles = json.optDouble("radiusMiles", base.radiusMiles),
                locationProfiles = profiles,
                radioUrl = json.optString("radioUrl", base.radioUrl),
                apiUrl = json.optString("apiUrl", base.apiUrl),
                telemetry = json.optBoolean("telemetry", base.telemetry),
                rotationDeg = json.optDouble("rotationDeg", base.rotationDeg.toDouble()).toFloat(),
                mirrorX = json.optBoolean("mirrorX", base.mirrorX),
                mirrorY = json.optBoolean("mirrorY", base.mirrorY),
                labelRotationDeg = json.optDouble("labelRotationDeg", base.labelRotationDeg.toDouble()).toFloat(),
                projectionMode = ProjectionMode.fromWire(json.optString("projectionMode", base.projectionMode.wireValue)),
                minAltitudeFt = json.optInt("minAltitudeFt", base.minAltitudeFt),
                maxAltitudeFt = json.optInt("maxAltitudeFt", base.maxAltitudeFt),
                hideOnGround = json.optBoolean("hideOnGround", base.hideOnGround),
                interpolate = json.optBoolean("interpolate", base.interpolate),
                maxExtrapolationSec = json.optInt("maxExtrapolationSec", base.maxExtrapolationSec),
                staleSec = json.optInt("staleSec", base.staleSec),
                smoothing = json.optDouble("smoothing", base.smoothing.toDouble()).toFloat(),
                maxFps = json.optInt("maxFps", base.maxFps),
                theme = ThemeMode.fromWire(json.optString("theme", base.theme.wireValue)),
                palette = palette,
                glyphSizePx = json.optInt("glyphSizePx", base.glyphSizePx),
                altitudeColor = json.optBoolean("altitudeColor", base.altitudeColor),
                trailSeconds = json.optInt("trailSeconds", base.trailSeconds),
                brightness = json.optDouble("brightness", base.brightness.toDouble()).toFloat(),
                labelDensity = LabelDensity.fromWire(json.optString("labelDensity", base.labelDensity.wireValue)),
                nearestN = json.optInt("nearestN", base.nearestN),
                showFields = showFields,
                nameDisplay = NameDisplay.fromWire(json.optString("nameDisplay", base.nameDisplay.wireValue)),
                locationDisplay = LocationDisplay.fromWire(json.optString("locationDisplay", base.locationDisplay.wireValue)),
                speedUnit = SpeedUnit.fromWire(json.optString("speedUnit", base.speedUnit.wireValue)),
                altitudeUnit = AltitudeUnit.fromWire(json.optString("altitudeUnit", base.altitudeUnit.wireValue)),
                distanceUnit = DistanceUnit.fromWire(json.optString("distanceUnit", base.distanceUnit.wireValue)),
                rangeRings = json.optBoolean("rangeRings", base.rangeRings),
                compass = json.optBoolean("compass", base.compass),
                highlightEmergency = json.optBoolean("highlightEmergency", base.highlightEmergency),
                showAirport = json.optBoolean("showAirport", base.showAirport),
                airport = airport,
                showHud = json.optBoolean("showHud", base.showHud),
                planetariumMode = json.optBoolean("planetariumMode", base.planetariumMode),
                showStars = json.optBoolean("showStars", base.showStars),
                showSun = json.optBoolean("showSun", base.showSun),
                showMoon = json.optBoolean("showMoon", base.showMoon),
                showSatellites = json.optBoolean("showSatellites", base.showSatellites),
                satelliteLabels = json.optBoolean("satelliteLabels", base.satelliteLabels),
                showPlanets = json.optBoolean("showPlanets", base.showPlanets),
                showSkyEvents = json.optBoolean("showSkyEvents", base.showSkyEvents),
                constellations = constellations,
                starMagLimit = json.optDouble("starMagLimit", base.starMagLimit.toDouble()).toFloat(),
                starLabelMagLimit = json.optDouble("starLabelMagLimit", base.starLabelMagLimit.toDouble()).toFloat(),
                skyTimeOffsetMin = json.optInt("skyTimeOffsetMin", base.skyTimeOffsetMin),
                showDestArc = json.optBoolean("showDestArc", base.showDestArc),
                showRouteDetail = json.optBoolean("showRouteDetail", base.showRouteDetail)
            )
        }

        fun configToJson(cfg: SkylightConfig): JSONObject {
            val paletteObj = JSONObject()
                .put("bg", cfg.palette.bg)
                .put("glyph", cfg.palette.glyph)
                .put("trail", cfg.palette.trail)
                .put("accent", cfg.palette.accent)
                .put("warn", cfg.palette.warn)
                .put("grid", cfg.palette.grid)
                .put("text", cfg.palette.text)

            val fieldsObj = JSONObject()
                .put("name", cfg.showFields.name)
                .put("type", cfg.showFields.type)
                .put("altitude", cfg.showFields.altitude)
                .put("speed", cfg.showFields.speed)
                .put("verticalRate", cfg.showFields.verticalRate)
                .put("destination", cfg.showFields.destination)
                .put("registration", cfg.showFields.registration)

            val profilesArr = JSONArray()
            cfg.locationProfiles.forEach { p ->
                val pObj = JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("lat", p.lat)
                    .put("lon", p.lon)
                    .put("radiusMiles", p.radiusMiles)
                    .put("showAirport", p.showAirport)
                if (p.airport != null) {
                    pObj.put("airport", airportToJson(p.airport))
                }
                profilesArr.put(pObj)
            }

            val constObj = JSONObject()
            cfg.constellations.forEach { (k, v) -> constObj.put(k, v) }

            return JSONObject()
                .put("centerLat", cfg.centerLat)
                .put("centerLon", cfg.centerLon)
                .put("locationName", cfg.locationName)
                .put("radiusMiles", cfg.radiusMiles)
                .put("locationProfiles", profilesArr)
                .put("radioUrl", cfg.radioUrl)
                .put("apiUrl", cfg.apiUrl)
                .put("telemetry", cfg.telemetry)
                .put("rotationDeg", cfg.rotationDeg.toDouble())
                .put("mirrorX", cfg.mirrorX)
                .put("mirrorY", cfg.mirrorY)
                .put("labelRotationDeg", cfg.labelRotationDeg.toDouble())
                .put("projectionMode", cfg.projectionMode.wireValue)
                .put("minAltitudeFt", cfg.minAltitudeFt)
                .put("maxAltitudeFt", cfg.maxAltitudeFt)
                .put("hideOnGround", cfg.hideOnGround)
                .put("interpolate", cfg.interpolate)
                .put("maxExtrapolationSec", cfg.maxExtrapolationSec)
                .put("staleSec", cfg.staleSec)
                .put("smoothing", cfg.smoothing.toDouble())
                .put("maxFps", cfg.maxFps)
                .put("theme", cfg.theme.wireValue)
                .put("palette", paletteObj)
                .put("glyphSizePx", cfg.glyphSizePx)
                .put("altitudeColor", cfg.altitudeColor)
                .put("trailSeconds", cfg.trailSeconds)
                .put("brightness", cfg.brightness.toDouble())
                .put("labelDensity", cfg.labelDensity.wireValue)
                .put("nearestN", cfg.nearestN)
                .put("showFields", fieldsObj)
                .put("nameDisplay", cfg.nameDisplay.wireValue)
                .put("locationDisplay", cfg.locationDisplay.wireValue)
                .put("speedUnit", cfg.speedUnit.wireValue)
                .put("altitudeUnit", cfg.altitudeUnit.wireValue)
                .put("distanceUnit", cfg.distanceUnit.wireValue)
                .put("rangeRings", cfg.rangeRings)
                .put("compass", cfg.compass)
                .put("highlightEmergency", cfg.highlightEmergency)
                .put("showAirport", cfg.showAirport)
                .put("airport", airportToJson(cfg.airport))
                .put("showHud", cfg.showHud)
                .put("planetariumMode", cfg.planetariumMode)
                .put("showStars", cfg.showStars)
                .put("showSun", cfg.showSun)
                .put("showMoon", cfg.showMoon)
                .put("showSatellites", cfg.showSatellites)
                .put("satelliteLabels", cfg.satelliteLabels)
                .put("showPlanets", cfg.showPlanets)
                .put("showSkyEvents", cfg.showSkyEvents)
                .put("constellations", constObj)
                .put("starMagLimit", cfg.starMagLimit.toDouble())
                .put("starLabelMagLimit", cfg.starLabelMagLimit.toDouble())
                .put("skyTimeOffsetMin", cfg.skyTimeOffsetMin)
                .put("showDestArc", cfg.showDestArc)
                .put("showRouteDetail", cfg.showRouteDetail)
        }
    }
}
