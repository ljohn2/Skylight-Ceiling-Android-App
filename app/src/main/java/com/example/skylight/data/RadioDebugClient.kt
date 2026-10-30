package com.example.skylight.data

import com.example.skylight.model.RadioAircraft
import com.example.skylight.model.RadioDebugSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed interface RadioFetchResult {
    data class Success(val snapshot: RadioDebugSnapshot) : RadioFetchResult
    data class Error(val message: String, val statusCode: Int? = null) : RadioFetchResult
}

class RadioDebugClient {
    private val userAgent = "skylight-android-control/1.2"

    companion object {
        const val DEFAULT_RADIO_URL = "http://skylight.local:8080/data/aircraft.json"

        fun deriveRadioUrl(piUrl: String): String {
            val trimmed = piUrl.trim()
            if (trimmed.isBlank() || trimmed.equals("upload", ignoreCase = true)) {
                return DEFAULT_RADIO_URL
            }
            return try {
                val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                    "http://$trimmed"
                } else {
                    trimmed
                }
                val uri = java.net.URI(withScheme)
                val scheme = uri.scheme ?: "http"
                val host = uri.host ?: trimmed.substringAfter("://").substringBefore(":").substringBefore("/")
                if (host.isNotBlank() && !host.equals("upload", ignoreCase = true)) {
                    "$scheme://$host:8080/data/aircraft.json"
                } else {
                    DEFAULT_RADIO_URL
                }
            } catch (_: Exception) {
                val host = trimmed.substringAfter("://").substringBefore(":").substringBefore("/")
                if (host.isNotBlank() && !host.equals("upload", ignoreCase = true)) {
                    "http://$host:8080/data/aircraft.json"
                } else {
                    DEFAULT_RADIO_URL
                }
            }
        }

        fun formatApiUrl(template: String, lat: Double, lon: Double, distMiles: Double): String {
            return template
                .replace("{lat}", "%.4f".format(java.util.Locale.US, lat))
                .replace("{lon}", "%.4f".format(java.util.Locale.US, lon))
                .replace("{r}", distMiles.toInt().coerceAtLeast(5).toString())
                .replace("{dist}", distMiles.toInt().coerceAtLeast(5).toString())
        }
    }

    suspend fun fetchAircraft(
        endpointUrl: String,
        fallbackUrls: List<String> = emptyList()
    ): RadioFetchResult = withContext(Dispatchers.IO) {
        val targets = buildList {
            val primary = endpointUrl.trim().ifBlank { DEFAULT_RADIO_URL }
            add(primary)
            fallbackUrls.forEach { fb ->
                val trimmed = fb.trim()
                if (trimmed.isNotBlank() && !contains(trimmed)) {
                    add(trimmed)
                }
            }
        }

        var lastError: String = "No valid endpoints"
        var lastCode: Int? = null

        for (target in targets) {
            val fullUrl = if (!target.startsWith("http://") && !target.startsWith("https://")) {
                "http://$target"
            } else {
                target
            }

            var conn: HttpURLConnection? = null
            try {
                val url = URL(fullUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("User-Agent", userAgent)
                    setRequestProperty("Accept", "application/json, text/plain, */*")
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                    val snapshot = parseSnapshot(body, fullUrl)
                    if (snapshot.aircraft.isNotEmpty() || targets.size == 1) {
                        return@withContext RadioFetchResult.Success(snapshot)
                    }
                } else {
                    lastCode = code
                    lastError = "HTTP $code: ${conn.responseMessage ?: "Error"}"
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: e.javaClass.simpleName
            } finally {
                conn?.disconnect()
            }
        }

        RadioFetchResult.Error(message = lastError, statusCode = lastCode)
    }

    private fun optSafeString(item: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (item.has(key) && !item.isNull(key)) {
                val str = item.optString(key, "").trim()
                if (str.isNotBlank() && !str.equals("null", ignoreCase = true)) {
                    return str
                }
            }
        }
        return null
    }

    fun parseSnapshot(rawJson: String, sourceUrl: String = DEFAULT_RADIO_URL): RadioDebugSnapshot {
        val obj = JSONObject(rawJson)
        val now = obj.optDouble("now", (System.currentTimeMillis() / 1000.0))
        val messages = obj.optLong("messages", 0L)
        val arr = obj.optJSONArray("aircraft") ?: obj.optJSONArray("ac")
        val aircraftList = mutableListOf<RadioAircraft>()

        if (arr != null) {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val hex = optSafeString(item, "hex", "icao") ?: continue
                val flight = optSafeString(item, "flight", "callsign", "r")
                val altBaro = if (item.has("alt_baro") && !item.isNull("alt_baro")) {
                    when (val v = item.get("alt_baro")) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull()
                        else -> null
                    }
                } else if (item.has("altitude") && !item.isNull("altitude")) {
                    when (val v = item.get("altitude")) {
                        is Number -> v.toInt()
                        is String -> v.toIntOrNull()
                        else -> null
                    }
                } else null

                val altGeom = if (item.has("alt_geom") && !item.isNull("alt_geom")) {
                    item.optInt("alt_geom")
                } else null

                val gs = if (item.has("gs") && !item.isNull("gs")) {
                    item.optDouble("gs")
                } else if (item.has("speed") && !item.isNull("speed")) {
                    item.optDouble("speed")
                } else null

                val track = if (item.has("track") && !item.isNull("track")) {
                    item.optDouble("track")
                } else if (item.has("heading") && !item.isNull("heading")) {
                    item.optDouble("heading")
                } else null

                val baroRate = if (item.has("baro_rate") && !item.isNull("baro_rate")) item.optInt("baro_rate") else null
                val squawk = optSafeString(item, "squawk")
                val emergency = optSafeString(item, "emergency")
                val category = optSafeString(item, "category")
                val lat = if (item.has("lat") && !item.isNull("lat")) item.optDouble("lat") else null
                val lon = if (item.has("lon") && !item.isNull("lon")) item.optDouble("lon") else null
                val nic = if (item.has("nic") && !item.isNull("nic")) item.optInt("nic") else null
                val rc = if (item.has("rc") && !item.isNull("rc")) item.optInt("rc") else null
                val seenPos = if (item.has("seen_pos") && !item.isNull("seen_pos")) item.optDouble("seen_pos") else null
                val version = if (item.has("version") && !item.isNull("version")) item.optInt("version") else null
                val nacP = if (item.has("nac_p") && !item.isNull("nac_p")) item.optInt("nac_p") else null
                val msgCount = if (item.has("messages") && !item.isNull("messages")) item.optLong("messages") else null
                val seen = if (item.has("seen") && !item.isNull("seen")) item.optDouble("seen") else null
                val rssi = if (item.has("rssi") && !item.isNull("rssi")) item.optDouble("rssi") else null

                aircraftList.add(
                    RadioAircraft(
                        hex = hex,
                        flight = flight,
                        altBaro = altBaro,
                        altGeom = altGeom,
                        groundSpeedKt = gs,
                        trackDeg = track,
                        baroRateFpm = baroRate,
                        squawk = squawk,
                        emergency = emergency,
                        category = category,
                        lat = lat,
                        lon = lon,
                        nic = nic,
                        rcMeters = rc,
                        seenPosSec = seenPos,
                        version = version,
                        nacP = nacP,
                        messages = msgCount,
                        seenSec = seen,
                        rssiDb = rssi
                    )
                )
            }
        }

        return RadioDebugSnapshot(
            now = now,
            totalMessages = messages,
            aircraft = aircraftList,
            rawJson = rawJson,
            fetchTimestampMillis = System.currentTimeMillis(),
            sourceUrl = sourceUrl
        )
    }

    /**
     * Generates active simulated radar aircraft positioned realistically around the given center location.
     */
    fun generateSimulatedSnapshot(
        centerLat: Double,
        centerLon: Double,
        radiusMiles: Double
    ): RadioDebugSnapshot {
        val nowSec = System.currentTimeMillis() / 1000.0
        val t = nowSec % 360.0

        val acConfigs = listOf(
            Triple("A4B821", "UAL245", 285.0 to 18000),
            Triple("A10F4E", "SWA182", 110.0 to 8500),
            Triple("A99C23", "DAL914", 45.0 to 31000),
            Triple("A6D3B8", "AAL430", 210.0 to 4200),
            Triple("A5E712", "SKY77", 330.0 to 2400)
        )

        val nmToDegLat = 1.0 / 60.0
        val nmToDegLon = 1.0 / (60.0 * kotlin.math.cos(Math.toRadians(centerLat)).coerceAtLeast(0.1))

        val list = acConfigs.mapIndexed { index, (hex, callsign, headingAndAlt) ->
            val (baseHdg, alt) = headingAndAlt
            val orbitRadiusNm = (radiusMiles * (0.35 + (index * 0.14))).coerceIn(1.5, radiusMiles * 0.95)
            val orbitAngleRad = Math.toRadians((t * (1.2 + index * 0.3) + index * 72.0) % 360.0)

            val dLat = (orbitRadiusNm * kotlin.math.cos(orbitAngleRad)) * nmToDegLat
            val dLon = (orbitRadiusNm * kotlin.math.sin(orbitAngleRad)) * nmToDegLon
            val heading = (Math.toDegrees(orbitAngleRad) + 90.0) % 360.0

            RadioAircraft(
                hex = hex,
                flight = callsign,
                altBaro = alt,
                altGeom = alt + 80,
                groundSpeedKt = 220.0 + (index * 45),
                trackDeg = heading,
                baroRateFpm = if (index % 2 == 0) -450 else 600,
                squawk = "1200",
                emergency = if (index == 4) "none" else null,
                category = "A3",
                lat = centerLat + dLat,
                lon = centerLon + dLon,
                nic = 8,
                rcMeters = 186,
                seenPosSec = 0.5,
                version = 2,
                nacP = 9,
                messages = 1420L + index * 210,
                seenSec = 0.3,
                rssiDb = -18.5 + (index * 2.1)
            )
        }

        val json = JSONObject().apply {
            put("now", nowSec)
            put("messages", 8450)
            val arr = org.json.JSONArray()
            list.forEach { ac ->
                arr.put(JSONObject().apply {
                    put("hex", ac.hex)
                    put("flight", ac.flight)
                    put("lat", ac.lat)
                    put("lon", ac.lon)
                    put("alt_baro", ac.altBaro)
                    put("track", ac.trackDeg)
                    put("gs", ac.groundSpeedKt)
                })
            }
            put("aircraft", arr)
        }

        return RadioDebugSnapshot(
            now = nowSec,
            totalMessages = 8450L,
            aircraft = list,
            rawJson = json.toString(),
            fetchTimestampMillis = System.currentTimeMillis(),
            sourceUrl = "simulated://traffic"
        )
    }
}
