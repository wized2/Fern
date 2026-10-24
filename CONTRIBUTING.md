# Contributing to Fern

Thanks for helping improve Fern — a free, offline Material 3 system monitor for Android.

## Branch model

| Branch | Role | Package id | Who merges |
|--------|------|------------|------------|
| **`main`** | **Stable** releases only | `com.endroid.fern` | Maintainers, from `beta` when ready |
| **`beta`** | Day-to-day development / nightly | `com.endroid.fern.beta` | Default target for PRs |

```text
feature/*  →  PR → beta  →  (soak)  →  main (stable release)
```

- **Open PRs against `beta`**, not `main`, unless the change is a critical stable hotfix.
- `main` should not move on every merged PR; it moves when we cut a stable release.
- Beta and stable **install side-by-side** (different application ids).

### Local builds

```bash
./gradlew :app:assembleStableDebug    # stable channel, debuggable
./gradlew :app:assembleBetaDebug      # beta channel → com.endroid.fern.beta
./gradlew :app:assembleStableRelease
./gradlew :app:assembleBetaRelease
./gradlew :app:testStableDebugUnitTest
```

## Ways to contribute

- Bug reports with device model, Android version, and steps
- Features that stay **offline-first** (no accounts, ads, or trackers)
- Metrics accuracy, Shizuku/root paths, widgets, accessibility
- Unit tests under `app/src/test` (prefer pure functions in `monitor/MetricsParse.kt`)
- Translations (`values-xx/strings.xml`)
- Docs and honest reviews

## Development setup

1. Clone and open in Android Studio (JDK 17+).
2. Checkout **`beta`** for feature work:
   ```bash
   git clone https://github.com/wized2/Fern.git
   cd Fern && git checkout beta
   ```
3. Optional: [Shizuku](https://shizuku.rikka.app/) for Advanced mode testing.

## PR checklist

1. Branch from **`beta`** (`feat/…`, `fix/…`, `test/…`).
2. One concern per PR when possible.
3. **No** new network SDKs, ads, analytics, or trackers.
4. Do not commit secrets, keystores, or huge binaries.
5. Match existing Kotlin style (clear names, few comments).
6. UI: Material 3, accessible labels (not emoji-only).
7. Add or extend **unit tests** for parsing / pure logic.
8. Wait for CI green before requesting review.
9. User-visible changes: note them in the PR body (maintainers update release notes).

### Good first issues

Look for labels `good first issue` and `help wanted`. Comment if you start work so we avoid duplicate effort.

## Code of conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Be respectful; maintainers may close hostile or off-topic threads.

## License

Contributions are licensed under the MIT License (see [LICENSE](LICENSE)).
