# Skylight — Raspberry Pi Remote Control Panel (Android)

Native Android remote control application for a [**Skylight** Raspberry Pi](https://github.com/cpaczek/skylight) projector hub (`skylight-server`), built with **Kotlin** and **Jetpack Compose** (Material 3) with zero `WebView` components.

## Overview

This app replaces the `/control` web interface on the Raspberry Pi with a dedicated native Android control panel, live radar preview, and telemetry diagnostics suite that connects over your local network (default `http://skylight.local:3000`):

- **Raspberry Pi Connection**:
  - Live status badge (connected state, active ADS-B source, live aircraft count, server error reporting), configurable host URL, saved host presets, and one-tap refresh.
- **Location & Runways**:
  - Search and geocode cities or addresses (`/api/geocode`).
  - Manual latitude, longitude, and radius (`NM`) inputs.
  - One-tap **Precise Location** button using Android GPS (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) to populate coordinates directly from your device.
  - Center coordinates on the live traffic centroid from `/api/aircraft`.
  - Saved location profiles (save, switch, and delete).
  - Airport runway overlay import by IATA/ICAO code (`/api/airport`).
- **ADS-B Source**:
  - Switch between **RTL-SDR Radio** (`dump1090`) and **Network API** (`/api/source`).
  - Presets for `airplanes.live`, `adsb.lol`, `adsb.fi`, and `TheAirTraffic`, plus custom aggregator or `dump1090` URLs and poll interval slider.
- **Calibration & Projection**:
  - Projection mode (`Flat Tangent` vs. `Sky Bowl`), observer height, ceiling height, and horizon floor.
  - Rotation (`-180°` to `180°`), quick `90°` step buttons, scale, X/Y offsets, keystone X/Y, and X/Y flip toggles.
- **Visuals & Theme**:
  - Color themes (`Night Vision`, `Amber`, `Phosphor`, `Cyan`, `White`, `Warm`, `Custom`), brightness, contrast, HUD visibility, and night-sky background toggle.
- **Labels, Units & Routes**:
  - Units (`Imperial`, `Metric`, `Aviation`), route format, aircraft icon scale, label scale, and individual field toggles (`Callsign`, `Type`, `Altitude`, `Speed`, `Route`, `Heading`, `Distance`).
- **Filtering & Motion**:
  - Minimum/maximum altitude (`ft`), ground traffic toggle, trail duration, and dead-reckoning extrapolation toggle.
- **Overlays & Live Sky Layer**:
  - Compass rose, range rings, runway overlays, airport pin, home pin, stars & planets, satellites, sun/moon arc, ambient mode when sky is empty, and individual constellation toggles.
- **System Controls**:
  - Projector standby (blackout) toggle and reset to Raspberry Pi defaults (`/api/config/reset`).
- **Live Preview Tab**:
  - Interactive radar scope previewing the projector output in real time with aircraft icons, heading vectors, trails, range rings, and runway overlays.
  - Interactive controls to toggle feed source (`Local Radio` vs. `Public API`), pause/resume live polling, mirror X-axis, zoom in/out/reset, and inspect individual aircraft telemetry cards.
- **Radio & Web Feed Debug Tab**:
  - Separate diagnostic modes for **Radio Dump (`dump1090`)** and **Web Feed (Public API)**.
  - Radio Dump mode strictly inspects direct `dump1090` JSON (`aircraft.json`) with endpoint auto-discovery, signal/RSSI metrics, message counts, and raw payload inspection.
  - Web Feed mode automatically syncs with the active public ADS-B API preset and observer coordinates configured on the Control tab.

## Project Structure

- `app/src/main/java/com/example/skylight/MainActivity.kt` — App entry point hosting bottom navigation across `ControlPanelScreen`, `LiveRadarScreen`, and `RadioDebugScreen`, plus runtime LAN/permission handling.
- `app/src/main/java/com/example/skylight/model/SkylightModels.kt` — Complete `SkylightConfig` data model, wire-format enums matching `skylight-server`, and diagnostic telemetry models.
- `app/src/main/java/com/example/skylight/data/PiControlClient.kt` — HTTP client for `/api/config`, `/api/config/reset`, `/api/status`, `/api/source`, `/api/aircraft`, `/api/geocode`, and `/api/airport`.
- `app/src/main/java/com/example/skylight/data/RadioDebugClient.kt` — Dedicated HTTP client for direct `dump1090` radio dump polling and public ADS-B web feed diagnostics.
- `app/src/main/java/com/example/skylight/ui/SkylightViewModel.kt` — State management, periodic Pi status and aircraft polling, live config patching, and debug feed state.
- `app/src/main/java/com/example/skylight/ui/screens/ControlPanelScreen.kt` — Native Jetpack Compose Material 3 control interface.
- `app/src/main/java/com/example/skylight/ui/screens/LiveRadarScreen.kt` — Real-time interactive radar canvas preview and aircraft telemetry inspector.
- `app/src/main/java/com/example/skylight/ui/screens/RadioDebugScreen.kt` — Dual-mode `dump1090` radio dump and public web feed diagnostic screen.

## Contact

- Suggestions and comments go in issues
