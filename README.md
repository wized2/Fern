# Fern

**Live system pulse for Android** — CPU, RAM, storage, battery, thermal, network.

Material 3 green theme · bottom tabs · gauges & sparklines · offline · no ads.

![Min API 26](https://img.shields.io/badge/minSdk-26-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)
![Compose](https://img.shields.io/badge/Material%203-green)
![Version](https://img.shields.io/badge/version-1.4.0-blue)

## Features

| Metric | Source |
|--------|--------|
| **CPU** | Dual-sample `/proc/stat` (loadavg fallback when restricted) |
| **RAM** | `ActivityManager.MemoryInfo` |
| **Storage** | `StatFs` on internal data |
| **Battery** | Level, charging, temperature, health + history sparkline |
| **Thermal** | `PowerManager` status (API 29+) |
| **Network** | Wi-Fi / mobile / Ethernet / offline |
| **Device** | Model, Android version, uptime, app heap |

- **Home** — live gauges, progress bars, CPU / RAM / Battery sparklines  
- **Details** — cores, clock, load averages, thermal, memory & power  
- **Settings** — theme (Auto / Light / Dark), refresh 0.5–5 s, keep-screen-on, About
- **Manual refresh** + last-updated age on Home; sampling pauses in background  

Fully offline after install. R8 + resource shrink for a small release APK.

## Build

```bash
./gradlew assembleDebug assembleRelease
```

Min SDK 26 · Target 35 · Kotlin + Jetpack Compose + Material 3.

CI builds both APKs and attaches them to the GitHub Release on every `main` push / tag.

## Privacy

Only `ACCESS_NETWORK_STATE` (connectivity label). No internet, no accounts, no tracking. All metrics stay on device.

## License

MIT

## Release signing (CI)

Release APKs are signed in GitHub Actions when repository secrets are configured.
If secrets are absent, CI falls back to debug signing so builds still pass.
Key material is never stored in this repository.
