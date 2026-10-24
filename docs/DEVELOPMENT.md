# Development guide

## Quick start (5 minutes)

```bash
git clone https://github.com/wized2/Fern.git
cd Fern
git checkout beta          # all new work starts here
./gradlew :app:assembleBetaDebug
./gradlew :app:testStableDebugUnitTest
```

Install the APK from:

`app/build/outputs/apk/beta/debug/`

Package id: **`com.endroid.fern.beta`** (can sit next to the stable app).

### Requirements

- JDK **17**
- Android SDK **35** (Android Studio Ladybug+ is fine)
- Optional: [Shizuku](https://shizuku.rikka.app/) for Advanced mode

## Branch & packages

| Git branch | Product flavor | Application id | Use |
|------------|----------------|----------------|-----|
| `beta` | `beta` | `com.endroid.fern.beta` | Default for PRs |
| `main` | `stable` | `com.endroid.fern` | Releases only |

```bash
./gradlew assembleBetaDebug
./gradlew assembleStableRelease
./gradlew testStableDebugUnitTest
```

## Where to change what

| Want to… | Look at |
|----------|---------|
| Dashboard UI | `app/.../ui/FernApp.kt` |
| Theme / seed colors | `app/.../ui/theme/` |
| Sampling loop / prefs | `FernViewModel.kt`, `data/Prefs.kt` |
| CPU / RAM / battery readouts | `monitor/SystemMetrics.kt` |
| **Pure parsers (easy tests)** | `monitor/MetricsParse.kt` |
| History CSV | `monitor/HistoryExport.kt` |
| Shizuku / root | `monitor/AdvancedAccess.kt`, `AdvancedMetrics.kt` |
| Overlay island | `overlay/OverlayService.kt` |
| Widgets | `widget/MetricWidgets.kt` |
| Strings | `res/values/strings.xml` (+ `values-xx/`) |

## Tests

Prefer **pure functions** (no `Context`) so CI stays fast:

```text
app/src/test/java/com/endroid/fern/SystemMetricsParseTest.kt
```

Add cases next to `MetricsParse` / `HistoryExport` behavior.

## PR flow

1. `git checkout beta && git pull`
2. `git checkout -b feat/short-name`
3. Code + tests
4. Open PR **→ `beta`** (not `main`)
5. Wait for CI green

Stable: maintainers merge `beta` → `main` when cutting a release.
