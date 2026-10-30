# Skylight — Raspberry Pi Remote Control Panel (Android)

Native Android remote control application for a **Skylight** Raspberry Pi projector hub (`skylight-server`), built with **Kotlin** and **Jetpack Compose** (Material 3) with zero `WebView` components.

## Overview

This app replaces the `/control` web interface on the Raspberry Pi with a dedicated native Android control panel that connects over your local network (default `http://skylight.local:3000`):

- **Raspberry Pi Connection**: Live status badge (connected state, active ADS-B source, live aircraft count, server error reporting), configurable host URL, and one-tap refresh.
- **Location & Runways**:
  - Search and geocode cities or addresses (`/api/geocode`).
  - Manual latitude, longitude, and radius (`NM`) inputs.
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

## Project Structure

- `app/src/main/java/com/example/skylight/MainActivity.kt` — App entry point hosting `ControlPanelScreen`.
- `app/src/main/java/com/example/skylight/model/SkylightModels.kt` — Complete `SkylightConfig` data model and wire-format enums matching `skylight-server`.
- `app/src/main/java/com/example/skylight/data/PiControlClient.kt` — HTTP client for `/api/config`, `/api/config/reset`, `/api/status`, `/api/source`, `/api/aircraft`, `/api/geocode`, and `/api/airport`.
- `app/src/main/java/com/example/skylight/ui/SkylightViewModel.kt` — State management, periodic Pi status polling, and live config patching.
- `app/src/main/java/com/example/skylight/ui/screens/ControlPanelScreen.kt` — Native Jetpack Compose Material 3 control interface.
