## 1.7.0
- Advanced mode (optional): Shizuku or root — off by default in Settings
- When elevated: thermal zones for Home Thermal bar, per-app memory on Active Apps, force-stop
- Honest status labels (Shizuku / Root / Unavailable)

## 1.6.2
- Home: Thermal bar under CPU/RAM (battery °C)
- < 40°C Normal · 40–50°C Overheating · 50°C+ Extreme
- Color-coded progress + system thermal label when available

## 1.6.1
- Icons: storage folder, battery full/charging (no more star/menu)
- Tabs: always show labels; Details uses Memory icon
- Details: section icons, peak chips, cleaner SpecRows
- Tests: removed duplicate sensors list (use More → Sensors)
- More: dedicated About screen; About removed from Settings

## 1.6.0
- Navigation: Home · Details · Active Apps · More
- Active Apps: optional Usage access — recent foreground apps, time windows, system filter
- More menu: Tests, Settings, Sensors (data-driven list)
- Sensors: full device list with details + optional live values
- Honest UI: no per-app CPU/RAM claims; open system App info on tap

## 1.5.35
- Overview: soft Live status chip, roomier Trends, taller sparklines
- Sleep-friendly density: slightly larger card padding, quieter progress tracks
- Gauge/bar motion remains 900ms ease-out for calm updates

## 1.5.34
- Details: clearer hardware section header and subtitle

## 1.5.33
- UI: calmer Overview hierarchy, zero-elevation cards, smoother gauge/bar motion
- UI: refined top bar + status bar contrast, roomier metric spacing
- Theme: OLED-friendlier dark surfaces, consistent corner radii

# Changelog

All notable changes to Fern are listed here.

## 1.5.22 — 2026-09-17

### Widgets
- Glass-style panel, thicker ring, mint accents
- Battery shows temp when available; RAM titled Memory

### UI / UX
- Overview section label, refined gauge captions

## 1.5.21 — 2026-09-17

### Metrics
- CPU layering: `/proc/stat` deltas → `dumpsys cpuinfo` → loadavg
- Clock: average `scaling_cur_freq` across cores; max from highest `cpuinfo_max_freq`

### UI
- Soft surface elevation, larger gauge percentage, refined top bar

## 1.5.20 — 2026-09-17

### Docs
- Release notes on GitHub Releases expanded for the 1.5.x line

## 1.5.19 — 2026-09-17

### UX
- Home trends: only CPU, RAM, and network (dropped slow battery/storage sparks)
- Tighter gauges, bars, and spark heights — less visual noise

## 1.5.18 — 2026-09-16

### UX
- Light haptic when switching tabs (if haptics enabled in Settings)
- Home status line: Live / Updated … without duplicating the app title under the top bar
- Softer sparkline strokes and fills; trend history capped at 36 samples

### Docs
- README aligned with current tabs, metrics, and UX
- This changelog added for release notes

## 1.5.17 — 2026-09-16

### UX
- Leaner Home header and clearer “Live / Updated” wording
- Thermal shown only when meaningful

## 1.5.16 — 2026-09-16

### UI
- Refined Material 3 green palette (light + near-black dark)
- Custom typography scale
- Center-aligned top app bar
- Fade transitions between tabs
- Navigation bar labels only when selected
- Calmer `surfaceContainer` cards; thinner progress tracks

## 1.5.15 — 2026-09-16

### Tests
- Live magnetometer readout (µT)

## 1.5.14 — 2026-09-16

### Tests
- Ambient light sensor (lx)
### Docs
- README feature table expanded

## 1.5.13 — 2026-09-16

### Features
- **Tests** tab: full-screen color panels, vibration, accel/gyro, touch counter, sensor list
- `VIBRATE` permission for the hardware vibration check
### Fix
- Restored Material Icons Extended so Battery / Science icons resolve in CI

## 1.5.12 — 2026-09-16

### Features
- Free RAM, sensor count, locale, timezone
- Optional external storage when distinct from internal

## 1.5.11 — 2026-09-16

### Features (DevInfo-style details)
- Battery voltage (mV), current (mA), technology
- Display resolution, DPI, refresh rate
- Security patch, kernel version
- Current CPU MHz and governor

## 1.5.10 — 2026-09-16

### Fix
- CPU no longer stuck at “0% est.”: multi-path `/proc/stat`, dual-sample + cross-tick, loadavg fallback
### About
- Launcher leaf artwork; link to GitHub repo

## Earlier

- Live gauges and sparklines for CPU, RAM, battery, storage, network
- Material 3 themes (Auto / Light / Dark)
- Widgets, share snapshot, pause sampling in background
- CI debug/release APKs on Releases
