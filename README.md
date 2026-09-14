# Fern

**Live system pulse for Android** — CPU, RAM, storage, battery, network.

Material 3 green theme · bottom tabs · gauges & sparklines · offline · no ads.

![Min API 26](https://img.shields.io/badge/minSdk-26-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)
![Compose](https://img.shields.io/badge/Material%203-green)

## Features

| Metric | Source |
|--------|--------|
| **CPU** | Dual-sample `/proc/stat` (loadavg fallback when restricted) |
| **RAM** | `ActivityManager.MemoryInfo` |
| **Storage** | `StatFs` on internal data |
| **Battery** | Level, charging, temperature, health |
| **Network** | Wi-Fi / mobile / Ethernet / offline |
| **Device** | Model, Android version, uptime, app heap |

- **Home** — live gauges, progress bars, CPU/RAM sparklines
- **Details** — cores, clock, load averages, memory & power breakdown
- **Settings** — theme (Auto / Light / Dark), refresh interval, About

Refresh 0.5–5 s (default 1.5 s). Fully offline after install.

## Build

```bash
./gradlew assembleDebug assembleRelease
```

Min SDK 26 · Target 35 · Kotlin + Jetpack Compose + Material 3.

## Privacy

Only `ACCESS_NETWORK_STATE` (connectivity label). No internet, no accounts, no tracking. All metrics stay on device.

## License

MIT
