# Changelog

All notable changes to Fern are listed here.

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
