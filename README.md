# Fern

**Live system pulse for Android** — CPU, RAM, storage, battery, thermal, network, and hardware tests.

Material 3 · offline · no ads · small release APK (R8 + resource shrink).

![Min API 26](https://img.shields.io/badge/minSdk-26-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)
![Compose](https://img.shields.io/badge/Material%203-green)
![Version](https://img.shields.io/badge/version-1.9.1-blue)

## Features

| Metric | Source |
|--------|--------|
| **CPU %** | Multi-path `/proc/stat` dual-sample + cross-tick; loadavg fallback |
| **CPU detail** | Cores, current/max MHz, governor, ABI, chip, load 1/5/15 |
| **RAM** | Used / free / total + app heap + low-memory pressure |
| **Storage** | Internal `StatFs`; external when distinct |
| **Battery** | Level, charge, health, tech, °C, mV, mA, estimated watts |
| **Thermal** | `PowerManager` status (API 29+) + battery °C bar |
| **Network** | Type + live KB/s throughput |
| **Display** | Resolution, DPI, refresh Hz |
| **System** | Security patch, kernel, locale, timezone, sensor count |

### Tabs

- **Home** — live gauges, session averages, uptime, trends, thermal, refresh
- **Details** — processor, memory, power, display, peaks; share snapshot
- **Active Apps** — recent apps (optional Usage access); elevated per-app RAM/CPU
- **More** — Tests, Settings (alerts, theme, advanced), Sensors, Notch island, About

### Alerts

Optional CPU % and battery temperature thresholds in Settings. Alerts are on-device toasts only (no network).

### Permissions

| Permission | Required | Purpose |
|------------|----------|---------|
| `ACCESS_NETWORK_STATE` | yes | Network type / throughput |
| `VIBRATE` | yes | Haptics and Tests |
| `PACKAGE_USAGE_STATS` | optional | Active Apps |
| `SYSTEM_ALERT_WINDOW` | optional | Notch island overlay |

No internet permission. No accounts. No tracking.

### Advanced mode

Optional Shizuku or root for thermal zones, per-app memory/CPU, force-stop.

## Build

```bash
./gradlew assembleDebug assembleRelease
```

Min SDK 26 · Target 35 · Kotlin + Jetpack Compose + Material 3.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## Website

[index.html](index.html) (GitHub Pages). Not in the APK.

## License

MIT · [wized2/Fern](https://github.com/wized2/Fern)

## Contributing

1. Prefer offline-first, small APK  
2. Run `./gradlew assembleDebug` before PR  
3. No telemetry  

## Acknowledgments

Jetpack Compose, Material 3, optional Shizuku.
