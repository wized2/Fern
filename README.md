# Fern

**Live system pulse for Android** — CPU, RAM, storage, battery, thermal, network, and hardware tests.

Material 3 · offline · no ads · small release APK (R8 + resource shrink).

![Min API 26](https://img.shields.io/badge/minSdk-26-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)
![Compose](https://img.shields.io/badge/Material%203-green)
![Version](https://img.shields.io/badge/version-1.6.0-blue)

## Features

| Metric | Source |
|--------|--------|
| **CPU %** | Multi-path `/proc/stat` dual-sample + cross-tick; loadavg fallback |
| **CPU detail** | Cores, current/max MHz, governor, ABI, chip, load 1/5/15 |
| **RAM** | Used / free / total + app heap |
| **Storage** | Internal `StatFs`; external when distinct |
| **Battery** | Level, charge state, health, tech, °C, mV, mA + sparkline |
| **Thermal** | `PowerManager` status (API 29+) |
| **Network** | Type + live KB/s throughput |
| **Display** | Resolution, DPI, refresh Hz |
| **System** | Security patch, kernel, locale, timezone, sensor count |

### Tabs

- **Home** — live status, gauges, bars, trend sparklines, manual refresh  
- **Details** — processor, memory, power, display, device peaks; share snapshot  
- **Active Apps** — recently active apps (optional Usage access; data stays on device)  
- **More** — Tests, Settings, Sensors  

### Permissions

| Permission | Required | Purpose |
|------------|----------|---------|
| `ACCESS_NETWORK_STATE` | yes | Network type / throughput on Home |
| `VIBRATE` | yes | Haptics and Tests vibration |
| `PACKAGE_USAGE_STATS` | optional | Active Apps only — user grants Usage access in system settings; never leaves the device |

No internet permission. No accounts. No tracking.

### UX

- Centered top bar, fade tab transitions, nav labels only when selected  
- Soft surface containers, refined green Material 3 palette (light + OLED dark)  
- Sampling can pause in background; optional haptic feedback  

Fully offline after install.

## Build

```bash
./gradlew assembleDebug assembleRelease
```

Min SDK 26 · Target 35 · Kotlin + Jetpack Compose + Material 3.

CI builds debug + release APKs and attaches them to GitHub Releases on `main` / tags.  
Release signing uses GitHub secrets when present; otherwise falls back to the debug keystore.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

MIT · [wized2/Fern](https://github.com/wized2/Fern)
