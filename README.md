# Fern

**Live system pulse for Android** — memory, storage, battery, CPU, network.

Material 3 · green theme · no accounts · no ads · offline.

## Features

| Metric | Source |
|--------|--------|
| RAM | `ActivityManager.MemoryInfo` |
| Storage | `StatFs` on internal data |
| Battery | level, charging, temperature |
| CPU | best-effort `/proc/stat` |
| Network | Wi‑Fi / mobile / offline |
| Device | model, Android version, uptime |

Live gauges, progress bars, and sparkline trends refresh every ~1.5s.

## Build

```bash
./gradlew assembleDebug assembleRelease
```

Min SDK 26 · Target 35 · Kotlin + Jetpack Compose.

## Privacy

No internet permission for data collection. Only `ACCESS_NETWORK_STATE` for connectivity label.
