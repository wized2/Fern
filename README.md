# Fern

<p align="center">
  <img src="assets/home-dark.png" width="220" alt="Fern home (dark)"/>
</p>

**Live system pulse for Android** — Material 3 resource monitor. Offline. No ads. Optional Shizuku / root for deeper metrics.

[![Build & Release](https://github.com/wized2/Fern/actions/workflows/build-release.yml/badge.svg)](https://github.com/wized2/Fern/actions/workflows/build-release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Site:** https://get-fern.vercel.app · **Releases:** https://github.com/wized2/Fern/releases

## Screenshots

| Home (dark) | Home (light) | Details | Active apps | More |
|:---:|:---:|:---:|:---:|:---:|
| ![Home dark](assets/home-dark.png) | ![Home light](assets/home-light.png) | ![Details](assets/details.png) | ![Apps](assets/apps.png) | ![More](assets/more.png) |

## Features

- **Live dashboard** — CPU, RAM, battery, storage, network with smooth history charts
- **Material You** — dynamic seed colors, light / dark / system, AMOLED-friendly dark
- **Details & peaks** — clearer breakdowns and shareable metric snapshots
- **Active apps** — process-oriented view; per-app CPU / RAM when elevated access is on
- **Sensors, tests, about** — extras under More without cluttering Home
- **Dynamic Island–style overlay** (optional) — compact temp · RAM near the camera area
- **Advanced mode** — Shizuku (preferred) or root for higher-fidelity sampling
- **Fully offline** — no account, no trackers, no ads

## Elevated access

Enable **Advanced mode** in Settings. Grant Fern in Shizuku (or use root). When connected you get richer CPU sampling and per-app memory / CPU detail on the Active apps tab.

## Privacy

No analytics SDK. Metrics stay on your device. Optional elevated access only runs what you enable.

## Install

- [Latest release APK](https://github.com/wized2/Fern/releases/latest)
- Or build: `./gradlew assembleRelease`

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Related

- **[Latch](https://github.com/wized2/Latch)** — privacy-first Tasker-style automation (local rules, optional Shizuku)

## License

MIT · Project Endroid

## Support

If Fern is useful, a GitHub star helps others find it. Issues and PRs welcome.
