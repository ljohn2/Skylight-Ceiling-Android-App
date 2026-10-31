package com.example.skylight.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.skylight.data.PiControlClient
import com.example.skylight.data.RadioDebugClient
import com.example.skylight.data.RadioFetchResult
import com.example.skylight.model.AirportsCatalog
import com.example.skylight.model.DataSourceType
import com.example.skylight.model.LocationProfile
import com.example.skylight.model.PiSourceStatus
import com.example.skylight.model.RadioDebugSnapshot
import com.example.skylight.model.SkylightConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.math.abs

enum class AppScreen {
    CONTROL_PANEL,
    LIVE_RADAR,
    RADIO_DEBUG
}

enum class DebugSourceMode {
    RADIO_DUMP,
    WEB_FEED
}

const val DEFAULT_SKYLIGHT_URL: String = PiControlClient.DEFAULT_PI_URL

val DEFAULT_SAVED_URLS: List<String> = PiControlClient.PI_URL_PRESETS

data class SkylightControlUiState(
    val currentScreen: AppScreen = AppScreen.CONTROL_PANEL,
    val piUrl: String = DEFAULT_SKYLIGHT_URL,
    val savedUrls: List<String> = DEFAULT_SAVED_URLS,
    val piConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val status: PiSourceStatus = PiSourceStatus(),
    val config: SkylightConfig = SkylightConfig(),
    val feedbackMessage: String? = null,
    val debugMode: DebugSourceMode = DebugSourceMode.RADIO_DUMP,
    val radioUrl: String = RadioDebugClient.DEFAULT_RADIO_URL,
    val radioLoading: Boolean = false,
    val radioAutoRefresh: Boolean = false,
    val radioSnapshot: RadioDebugSnapshot? = null,
    val radioError: String? = null,
    val webFeedUrl: String = "https://opendata.adsb.fi/api/v3/lat/37.6213/lon/-122.3790/dist/25",
    val webFeedLoading: Boolean = false,
    val webFeedAutoRefresh: Boolean = false,
    val webFeedSnapshot: RadioDebugSnapshot? = null,
    val webFeedError: String? = null,
    val radarSnapshot: RadioDebugSnapshot? = null,
    val radarLoading: Boolean = false,
    val radarError: String? = null,
    val radarZoomMiles: Double = 25.0,
    val selectedAircraftHex: String? = null,
    val radarPolling: Boolean = true
)

class SkylightViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val client: PiControlClient = PiControlClient()
    private val radioClient: RadioDebugClient = RadioDebugClient()
    private var radioJob: Job? = null
    private var webFeedJob: Job? = null

    private val prefs = application.getSharedPreferences("skylight_prefs", Context.MODE_PRIVATE)

    private val initialPiUrl: String = run {
        val saved = prefs.getString("pi_url", null)
        if (!saved.isNullOrBlank() && !PiControlClient.isExcludedUrl(saved)) {
            saved
        } else {
            DEFAULT_SKYLIGHT_URL
        }
    }

    private val initialConfig = SkylightConfig()

    private fun deriveWebFeedUrl(cfg: SkylightConfig, distMiles: Double = 25.0): String {
        return RadioDebugClient.formatApiUrl(
            template = cfg.apiUrl,
            lat = cfg.centerLat,
            lon = cfg.centerLon,
            distMiles = distMiles
        )
    }

    private val _uiState = MutableStateFlow(
        SkylightControlUiState(
            piUrl = initialPiUrl,
            savedUrls = loadSavedUrls(),
            config = initialConfig,
            radioUrl = RadioDebugClient.deriveRadioUrl(initialPiUrl),
            webFeedUrl = deriveWebFeedUrl(initialConfig, 25.0)
        )
    )
    val uiState: StateFlow<SkylightControlUiState> = _uiState.asStateFlow()

    init {
        syncWithPi()
        startStatusPolling()
    }

    private fun startStatusPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(4000L)
                val url = _uiState.value.piUrl
                val remoteStatus = client.fetchStatus(url)
                if (remoteStatus != null) {
                    _uiState.update {
                        it.copy(piConnected = true, status = remoteStatus)
                    }
                } else {
                    _uiState.update {
                        it.copy(piConnected = false)
                    }
                }
            }
        }
    }

    private fun loadSavedUrls(): List<String> {
        return try {
            val raw = prefs.getString("saved_urls", null)
            if (!raw.isNullOrBlank()) {
                val arr = JSONArray(raw)
                val list = mutableListOf<String>()
                var modified = false
                for (i in 0 until arr.length()) {
                    val u = arr.optString(i).trim().removeSuffix("/")
                    if (u.isNotBlank() && !PiControlClient.isExcludedUrl(u) && !list.contains(u)) {
                        list.add(u)
                    } else if (u.isNotBlank() && PiControlClient.isExcludedUrl(u)) {
                        modified = true
                    }
                }
                val result = if (list.isEmpty()) DEFAULT_SAVED_URLS else list
                if (modified) {
                    persistSavedUrls(result)
                }
                result
            } else {
                DEFAULT_SAVED_URLS
            }
        } catch (_: Exception) {
            DEFAULT_SAVED_URLS
        }
    }

    private fun persistSavedUrls(urls: List<String>) {
        try {
            val arr = JSONArray()
            urls.filterNot { PiControlClient.isExcludedUrl(it) }.forEach { arr.put(it) }
            prefs.edit().putString("saved_urls", arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun addSavedUrl(url: String) {
        var formatted = url.trim().removeSuffix("/")
        if (formatted.isBlank() || PiControlClient.isExcludedUrl(formatted)) return
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://$formatted"
        }
        if (PiControlClient.isExcludedUrl(formatted)) return
        val current = _uiState.value.savedUrls
        if (!current.contains(formatted)) {
            val updated = current + formatted
            _uiState.update { it.copy(savedUrls = updated) }
            persistSavedUrls(updated)
        }
    }

    fun removeSavedUrl(url: String) {
        val target = url.trim().removeSuffix("/")
        val current = _uiState.value.savedUrls
        val updated = current.filterNot { it.equals(target, ignoreCase = true) }
        _uiState.update { it.copy(savedUrls = updated) }
        persistSavedUrls(updated)
    }

    fun setPiUrl(url: String) {
        val clean = url.trim()
        if (PiControlClient.isExcludedUrl(clean)) {
            return
        }
        val derivedRadio = RadioDebugClient.deriveRadioUrl(clean)
        _uiState.update {
            it.copy(
                piUrl = clean,
                radioUrl = derivedRadio,
                feedbackMessage = null
            )
        }
        try {
            prefs.edit().putString("pi_url", clean).putString("radio_url", derivedRadio).apply()
        } catch (_: Exception) {
        }
    }

    fun syncWithPi() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, feedbackMessage = null) }
            val url = _uiState.value.piUrl
            val remoteCfg = client.fetchConfig(url)
            val remoteStatus = client.fetchStatus(url)
            if (remoteCfg != null) {
                _uiState.update {
                    it.copy(
                        piConnected = true,
                        isSyncing = false,
                        config = remoteCfg,
                        webFeedUrl = deriveWebFeedUrl(remoteCfg, it.radarZoomMiles),
                        status = remoteStatus ?: it.status,
                        feedbackMessage = "Connected to Raspberry Pi at $url"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        piConnected = false,
                        isSyncing = false,
                        feedbackMessage = "Pi unreachable at $url — editing config locally (will sync when reachable)"
                    )
                }
            }
        }
    }

    fun updateConfig(transform: (SkylightConfig) -> SkylightConfig) {
        val oldCfg = _uiState.value.config
        val nextCfg = transform(oldCfg)
        _uiState.update { state ->
            val updatedZoom = if (state.radarZoomMiles == oldCfg.radiusMiles || state.radarZoomMiles == 25.0) {
                nextCfg.radiusMiles
            } else {
                state.radarZoomMiles
            }
            val syncedWebFeedUrl = deriveWebFeedUrl(nextCfg, updatedZoom)
            state.copy(
                config = nextCfg,
                radarZoomMiles = updatedZoom,
                webFeedUrl = syncedWebFeedUrl,
                webFeedError = null
            )
        }
        val url = _uiState.value.piUrl
        viewModelScope.launch {
            val synced = client.patchConfig(url, nextCfg)
            if (synced != null) {
                _uiState.update {
                    it.copy(
                        piConnected = true,
                        config = synced,
                        webFeedUrl = deriveWebFeedUrl(synced, it.radarZoomMiles)
                    )
                }
            }
        }
    }

    fun switchDataSource(source: DataSourceType) {
        _uiState.update { state ->
            state.copy(
                status = state.status.copy(source = source)
            )
        }
        if (_uiState.value.currentScreen == AppScreen.LIVE_RADAR) {
            fetchLiveRadarFeed()
        }
        val url = _uiState.value.piUrl
        viewModelScope.launch {
            val remoteStatus = client.setSource(url, source)
            if (remoteStatus != null) {
                _uiState.update {
                    it.copy(
                        piConnected = true,
                        status = remoteStatus,
                        feedbackMessage = "Switched Pi data source to ${source.label}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "Selected ${source.label}")
                }
            }
        }
    }

    fun toggleFeedSource() {
        val nextSource = if (_uiState.value.status.source == DataSourceType.RADIO) {
            DataSourceType.API
        } else {
            DataSourceType.RADIO
        }
        switchDataSource(nextSource)
    }

    fun toggleMirrorX() {
        updateConfig { cfg ->
            cfg.copy(mirrorX = !cfg.mirrorX)
        }
    }

    fun searchLocation(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            val url = _uiState.value.piUrl
            val hit = client.geocodeLocation(url, query)
            if (hit != null) {
                val matchingAirport = AirportsCatalog.findByCode(query)
                updateConfig { cfg ->
                    cfg.copy(
                        centerLat = hit.lat,
                        centerLon = hit.lon,
                        locationName = hit.name,
                        airport = matchingAirport ?: cfg.airport
                    )
                }
                _uiState.update { it.copy(feedbackMessage = "Location set to ${hit.name}") }
            } else {
                _uiState.update { it.copy(feedbackMessage = "No match for \"$query\"") }
            }
        }
    }

    fun setPreciseLocation(lat: Double, lon: Double, isPreciseGps: Boolean = true) {
        val clampedLat = lat.coerceIn(-90.0, 90.0)
        val clampedLon = lon.coerceIn(-180.0, 180.0)
        val formattedCoords = String.format(java.util.Locale.US, "%.4f, %.4f", clampedLat, clampedLon)
        val label = if (isPreciseGps) "Precise GPS ($formattedCoords)" else "Custom ($formattedCoords)"
        updateConfig { cfg ->
            cfg.copy(
                centerLat = clampedLat,
                centerLon = clampedLon,
                locationName = label
            )
        }
        _uiState.update {
            it.copy(feedbackMessage = "Location updated to $formattedCoords")
        }
    }

    fun setFeedbackMessage(message: String) {
        _uiState.update { it.copy(feedbackMessage = message) }
    }

    fun importAirportRunways(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            val url = _uiState.value.piUrl
            val airport = client.lookupAirportRunways(url, code)
            if (airport != null) {
                updateConfig { cfg ->
                    cfg.copy(
                        airport = airport,
                        showAirport = true
                    )
                }
                _uiState.update {
                    it.copy(
                        feedbackMessage = "Imported ${airport.runways.size} runways for ${airport.icao} (${airport.name})"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "Airport \"$code\" not found (try KSFO, KLAX, KJFK, EGLL, RJTT)")
                }
            }
        }
    }

    fun saveCurrentProfile() {
        updateConfig { cfg ->
            val id = "loc_${System.currentTimeMillis()}"
            val profile = LocationProfile(
                id = id,
                name = cfg.locationName.ifBlank { "%.4f, %.4f".format(cfg.centerLat, cfg.centerLon) },
                lat = cfg.centerLat,
                lon = cfg.centerLon,
                radiusMiles = cfg.radiusMiles,
                airport = cfg.airport,
                showAirport = cfg.showAirport
            )
            val rest = cfg.locationProfiles.filterNot {
                abs(it.lat - cfg.centerLat) < 1e-4 && abs(it.lon - cfg.centerLon) < 1e-4
            }
            cfg.copy(locationProfiles = rest + profile)
        }
        _uiState.update { it.copy(feedbackMessage = "Saved current location profile") }
    }

    fun switchToProfile(profile: LocationProfile) {
        updateConfig { cfg ->
            cfg.copy(
                centerLat = profile.lat,
                centerLon = profile.lon,
                locationName = profile.name,
                radiusMiles = profile.radiusMiles,
                airport = profile.airport ?: cfg.airport,
                showAirport = profile.showAirport
            )
        }
        _uiState.update { it.copy(feedbackMessage = "Switched to ${profile.name}") }
    }

    fun removeProfile(id: String) {
        updateConfig { cfg ->
            cfg.copy(locationProfiles = cfg.locationProfiles.filterNot { it.id == id })
        }
    }

    fun centerOnTraffic() {
        viewModelScope.launch {
            val url = _uiState.value.piUrl
            val centroid = client.fetchTrafficCentroid(url)
            if (centroid != null) {
                updateConfig { cfg ->
                    cfg.copy(
                        centerLat = centroid.first,
                        centerLon = centroid.second,
                        locationName = "Traffic center"
                    )
                }
                _uiState.update { it.copy(feedbackMessage = "Centered on active traffic") }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "No live aircraft positions available from Pi to center on")
                }
            }
        }
    }

    fun toggleConstellation(id: String, enabled: Boolean) {
        updateConfig { cfg ->
            val nextMap = cfg.constellations.toMutableMap()
            nextMap[id] = enabled
            cfg.copy(constellations = nextMap)
        }
    }

    fun resetConfig() {
        viewModelScope.launch {
            val url = _uiState.value.piUrl
            val remoteReset = client.resetConfigOnPi(url)
            val fresh = remoteReset ?: SkylightConfig()
            _uiState.update {
                it.copy(
                    config = fresh,
                    webFeedUrl = deriveWebFeedUrl(fresh, it.radarZoomMiles),
                    feedbackMessage = "Reset configuration to defaults"
                )
            }
        }
    }

    private var liveRadarJob: Job? = null

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
        when (screen) {
            AppScreen.LIVE_RADAR -> {
                fetchLiveRadarFeed()
                if (_uiState.value.radarPolling) {
                    startRadarLoop()
                }
            }
            AppScreen.RADIO_DEBUG -> {
                liveRadarJob?.cancel()
                if (_uiState.value.debugMode == DebugSourceMode.RADIO_DUMP) {
                    if (_uiState.value.radioSnapshot == null) {
                        fetchRadioDump()
                    }
                } else {
                    if (_uiState.value.webFeedSnapshot == null) {
                        fetchWebFeed()
                    }
                }
            }
            AppScreen.CONTROL_PANEL -> {
                liveRadarJob?.cancel()
            }
        }
    }

    fun navigateBack() {
        liveRadarJob?.cancel()
        radioJob?.cancel()
        webFeedJob?.cancel()
        _uiState.update { it.copy(currentScreen = AppScreen.CONTROL_PANEL) }
    }

    fun setRadarZoom(miles: Double) {
        val clamped = miles.coerceIn(3.0, 150.0)
        _uiState.update {
            it.copy(
                radarZoomMiles = clamped,
                webFeedUrl = deriveWebFeedUrl(it.config, clamped)
            )
        }
    }

    fun zoomInRadar() {
        setRadarZoom(_uiState.value.radarZoomMiles * 0.75)
    }

    fun zoomOutRadar() {
        setRadarZoom(_uiState.value.radarZoomMiles * 1.35)
    }

    fun selectAircraftHex(hex: String?) {
        _uiState.update { it.copy(selectedAircraftHex = hex) }
    }

    fun toggleRadarPolling(enabled: Boolean? = null) {
        val next = enabled ?: !_uiState.value.radarPolling
        _uiState.update { it.copy(radarPolling = next) }
        liveRadarJob?.cancel()
        if (next && _uiState.value.currentScreen == AppScreen.LIVE_RADAR) {
            startRadarLoop()
        }
    }

    private fun startRadarLoop() {
        liveRadarJob?.cancel()
        liveRadarJob = viewModelScope.launch {
            while (isActive) {
                fetchLiveRadarFeed()
                delay(1500L)
            }
        }
    }

    fun setDebugMode(mode: DebugSourceMode) {
        _uiState.update { it.copy(debugMode = mode) }
        when (mode) {
            DebugSourceMode.RADIO_DUMP -> {
                if (_uiState.value.radioSnapshot == null) {
                    fetchRadioDump()
                }
            }
            DebugSourceMode.WEB_FEED -> {
                if (_uiState.value.webFeedSnapshot == null) {
                    fetchWebFeed()
                }
            }
        }
    }

    fun setRadioUrl(url: String) {
        val trimmed = url.trim()
        _uiState.update { it.copy(radioUrl = trimmed, radioError = null) }
        try {
            prefs.edit().putString("radio_url", trimmed).apply()
        } catch (_: Exception) {
        }
    }

    fun setWebFeedUrl(url: String) {
        val trimmed = url.trim()
        _uiState.update { state ->
            val formatted = if (trimmed.contains("{lat}") || trimmed.contains("{lon}") || trimmed.contains("{r}")) {
                RadioDebugClient.formatApiUrl(
                    template = trimmed,
                    lat = state.config.centerLat,
                    lon = state.config.centerLon,
                    distMiles = state.radarZoomMiles
                )
            } else {
                trimmed
            }
            state.copy(webFeedUrl = formatted, webFeedError = null)
        }
    }

    /**
     * Strictly pulls telemetry from the local ADS-B radio receiver dump only (no web feed, no mock fallback).
     * Mirroring cpaczek/skylight: queries aircraft.json directly without /api/aircraft or web proxying.
     */
    fun fetchRadioDump() {
        viewModelScope.launch {
            _uiState.update { it.copy(radioLoading = true, radioError = null) }
            val state = _uiState.value
            val primary = state.radioUrl.ifBlank { state.config.radioUrl }.ifBlank { RadioDebugClient.DEFAULT_RADIO_URL }

            when (val res = radioClient.fetchAircraft(primary, emptyList())) {
                is RadioFetchResult.Success -> {
                    _uiState.update {
                        it.copy(
                            radioLoading = false,
                            radioSnapshot = res.snapshot,
                            radioError = null
                        )
                    }
                }
                is RadioFetchResult.Error -> {
                    _uiState.update {
                        it.copy(
                            radioLoading = false,
                            radioSnapshot = null,
                            radioError = res.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Fetches public web feed telemetry from the configured aggregator API.
     */
    fun fetchWebFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(webFeedLoading = true, webFeedError = null) }
            val state = _uiState.value
            val target = state.webFeedUrl.ifBlank {
                deriveWebFeedUrl(state.config, state.radarZoomMiles)
            }
            when (val res = radioClient.fetchAircraft(target, emptyList())) {
                is RadioFetchResult.Success -> {
                    _uiState.update {
                        it.copy(
                            webFeedLoading = false,
                            webFeedSnapshot = res.snapshot,
                            webFeedError = null
                        )
                    }
                }
                is RadioFetchResult.Error -> {
                    _uiState.update {
                        it.copy(
                            webFeedLoading = false,
                            webFeedSnapshot = null,
                            webFeedError = res.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Feed fetcher for the Live Radar projector preview. Respects control panel data source (radio vs API).
     * Updates radarSnapshot independently so it never pollutes or overwrites the Radio Debug page.
     */
    fun fetchLiveRadarFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(radarLoading = true, radarError = null) }
            val state = _uiState.value
            val cfg = state.config
            val isApiSource = state.status.source == DataSourceType.API

            val targetUrl = if (isApiSource) {
                deriveWebFeedUrl(cfg, state.radarZoomMiles)
            } else {
                state.radioUrl.ifBlank { cfg.radioUrl }.ifBlank { RadioDebugClient.DEFAULT_RADIO_URL }
            }

            when (val res = radioClient.fetchAircraft(targetUrl, emptyList())) {
                is RadioFetchResult.Success -> {
                    _uiState.update {
                        it.copy(
                            radarLoading = false,
                            radarSnapshot = res.snapshot,
                            radarError = null
                        )
                    }
                }
                is RadioFetchResult.Error -> {
                    // Fallback to active simulated radar traffic around current center coordinates
                    val fallbackSnapshot = radioClient.generateSimulatedSnapshot(
                        centerLat = cfg.centerLat,
                        centerLon = cfg.centerLon,
                        radiusMiles = state.radarZoomMiles
                    )
                    _uiState.update {
                        it.copy(
                            radarLoading = false,
                            radarSnapshot = fallbackSnapshot,
                            radarError = null
                        )
                    }
                }
            }
        }
    }

    fun toggleRadioAutoRefresh(enabled: Boolean? = null) {
        val nextState = enabled ?: !_uiState.value.radioAutoRefresh
        _uiState.update { it.copy(radioAutoRefresh = nextState) }
        radioJob?.cancel()
        if (nextState) {
            radioJob = viewModelScope.launch {
                while (isActive) {
                    fetchRadioDump()
                    delay(2500L)
                }
            }
        }
    }

    fun toggleWebFeedAutoRefresh(enabled: Boolean? = null) {
        val nextState = enabled ?: !_uiState.value.webFeedAutoRefresh
        _uiState.update { it.copy(webFeedAutoRefresh = nextState) }
        webFeedJob?.cancel()
        if (nextState) {
            webFeedJob = viewModelScope.launch {
                while (isActive) {
                    fetchWebFeed()
                    delay(3000L)
                }
            }
        }
    }
}
