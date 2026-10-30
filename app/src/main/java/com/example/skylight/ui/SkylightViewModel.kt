package com.example.skylight.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skylight.data.PiControlClient
import com.example.skylight.model.AirportsCatalog
import com.example.skylight.model.DataSourceType
import com.example.skylight.model.LocationProfile
import com.example.skylight.model.PiSourceStatus
import com.example.skylight.model.SkylightConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SkylightControlUiState(
    val piUrl: String = "http://skylight.local:3000",
    val piConnected: Boolean = false,
    val isSyncing: Boolean = false,
    val status: PiSourceStatus = PiSourceStatus(),
    val config: SkylightConfig = SkylightConfig(),
    val feedbackMessage: String? = null
)

class SkylightViewModel(
    private val client: PiControlClient = PiControlClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkylightControlUiState())
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

    fun setPiUrl(url: String) {
        _uiState.update { it.copy(piUrl = url, feedbackMessage = null) }
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
        val nextCfg = transform(_uiState.value.config)
        _uiState.update { it.copy(config = nextCfg) }
        val url = _uiState.value.piUrl
        viewModelScope.launch {
            val synced = client.patchConfig(url, nextCfg)
            if (synced != null) {
                _uiState.update { it.copy(piConnected = true, config = synced) }
            }
        }
    }

    fun switchDataSource(source: DataSourceType) {
        _uiState.update { state ->
            state.copy(status = state.status.copy(source = source))
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
                    feedbackMessage = "Reset configuration to defaults"
                )
            }
        }
    }
}
