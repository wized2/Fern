# Architecture (short)

Fern is a single-module Android app (`:app`), Kotlin + Jetpack Compose + Material 3.

```text
UI (Compose)  →  FernViewModel  →  SystemMetrics / Advanced*
                      ↓
                   Prefs (DataStore-style / SharedPreferences)
```

- **No network stack** for core monitoring — metrics stay on-device.
- **Elevated path** (optional): Shizuku or `su` for richer `/proc` and per-app stats.
- **Product flavors** `stable` / `beta` only change `applicationId` / version suffix, not source packages (`namespace` remains `com.endroid.fern`).

### Metrics path

1. `FernViewModel` ticks every `refreshMs`.
2. `SystemMetrics` reads battery intents, meminfo, loadavg, `/proc/stat` (via `MetricsParse`), storage, network.
3. If Advanced mode is on, `AdvancedAccess` may enrich CPU / per-app data.
4. History rings feed charts; `HistoryExport` can turn series into CSV.

### Background surfaces

- `OverlayService` — optional on-screen island (needs overlay permission).
- App widgets — periodic metric snapshots.
