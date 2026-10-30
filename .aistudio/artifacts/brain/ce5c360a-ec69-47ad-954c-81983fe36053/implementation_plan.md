# Implementation Plan - Live Ceiling Radar Preview

Create a real-time **Ceiling Radar Preview** screen on the Android app that connects directly to your Raspberry Pi's local APIs, rendering a mobile simulation of what is currently projected on the ceiling.

## Proposed User Experience & Architecture

### 1. Radar Canvas & Projection Engine (`LiveRadarScreen.kt`)
- **Real-Time Projection Canvas**: A custom Jetpack Compose `Canvas` drawing:
  - Concentric range rings scaled to the active location profile's radius (e.g., 5, 10, 25 miles).
  - Cardinal compass directions (N, E, S, W) respecting the projector's `mirrorX` and `mirrorY` flips.
  - Home center point and airport runway overlays (if enabled in location settings).
  - Aircraft blips positioned via spherical/polar projection relative to the home location profile.
  - Directional heading vectors and altitude-color-coded aircraft icons.
  - Interactive touch selection: tapping any aircraft blip locks onto it and displays an inspection flight card (callsign, altitude, ground speed, track, distance from home, and squawk).

### 2. Pi Local API Connection & Streaming
- Connects directly to the Raspberry Pi over LAN:
  - Queries the local ADS-B feed (`/data/aircraft.json` / `/api/traffic`) at configurable intervals (1s to 2s).
  - Computes aircraft relative bearing and range (nautical miles) from the home coordinates (`lat`, `lon`) using great-circle geometry.
  - Automatically respects the Pi's active `SkylightConfig` (altitude unit, distance unit, palette colors, label density, and mirror settings).

### 3. Navigation & App Integration
- **Top Navigation & Quick Access**:
  - Add "Live Radar" tab or button in the top app bar for seamless switching between **Control Panel**, **Live Radar**, and **Radio Debug**.
  - `BackHandler` integration ensuring Android hardware back navigation returns to the Control Panel.
- **Radar Controls Toolbar**:
  - Play / Pause real-time polling.
  - Zoom range adjustment (+ / - 5 miles).
  - "Center on Traffic" button.
  - Quick toggle to switch between Radar Blips and Detailed Overhead HUD.

## Implementation Steps

1. **Geometry & Projection Utility (`RadarProjection.kt`)**:
   - Haversine distance, bearing, and polar coordinate projection algorithms converting aircraft `(lat, lon)` into Canvas `(x, y)` relative to the home coordinates.
2. **ViewModel State Extension (`SkylightViewModel.kt`)**:
   - Add `AppScreen.LIVE_RADAR` to navigation.
   - Stream traffic updates and calculate relative positioning.
   - Maintain selected aircraft state for inspection.
3. **UI Screen (`LiveRadarScreen.kt`)**:
   - Render the radar canvas, target inspection cards, zoom controls, and status pill.
4. **Integration & Navigation (`MainActivity.kt`, `ControlPanelScreen.kt`)**:
   - Wire top navigation bar and route handlers.
5. **Unit & Robolectric Tests (`SkylightTest.kt`)**:
   - Verify projection calculations, distance conversions, and radar navigation.
