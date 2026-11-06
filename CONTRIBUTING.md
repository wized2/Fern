# Contributing to Fern

Thanks for helping. Fern stays **offline, FOSS, and tracker-free** — please keep it that way.

## Fast path (first PR)

1. Browse **[good first issues](https://github.com/wized2/Fern/labels/good%20first%20issue)** or **[contributor-hunt](https://github.com/wized2/Fern/labels/contributor-hunt)**
2. Comment *“I’d like to take this”* on the issue
3. Fork → branch from **`beta`** (preferred) or `main` for tiny docs
4. Open a PR early (draft is fine)

```bash
git clone https://github.com/<you>/Fern.git
cd Fern
./gradlew assembleDebug   # or open in Android Studio
./gradlew test            # unit tests, no device required
```

## What we merge quickly

| Kind | Examples | Label |
| --- | --- | --- |
| XS docs | README fix, captions, translation | `documentation` `size/XS` |
| XS code | contentDescription, unit test | `good first issue` `size/XS` |
| S UI | empty states, small Compose polish | `size/S` |

## Rules

- **No** analytics, ads, or `INTERNET` unless discussed
- Match existing Material 3 / Compose style
- Add or extend unit tests for pure logic (`monitor/`, parsers, export)
- PR description: what / why / how tested

## Project layout

- UI: `app/src/main/java/com/endroid/fern/ui/`
- Metrics: `app/src/main/java/com/endroid/fern/monitor/`
- Tests: `app/src/test/`

## Mentoring

Stuck for >15 minutes? Comment on the issue with the error. We prioritize `good first issue` reviews.
