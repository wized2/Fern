<p align="center">
  <img src="assets/home-dark.png" width="240" alt="Fern home (dark)"/>
</p>

<h1 align="center">Fern</h1>

<p align="center">
  <b>Live system pulse for Android</b><br/>
  Material You monitor — CPU, RAM, battery, thermals, network.<br/>
  Offline · No ads · No trackers · Optional Shizuku / root
</p>

<p align="center">
  <a href="https://github.com/wized2/Fern/actions/workflows/build.yml"><img src="https://github.com/wized2/Fern/actions/workflows/build.yml/badge.svg" alt="Build &amp; Release"/></a>
  <a href="https://github.com/wized2/Fern/releases/latest"><img src="https://img.shields.io/github/v/release/wized2/Fern?label=release" alt="Release"/></a>
  <a href="https://github.com/wized2/Fern/stargazers"><img src="https://img.shields.io/github/stars/wized2/Fern?style=social" alt="Stars"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="MIT"/></a>
  <img src="https://img.shields.io/badge/Android-8%2B-green.svg" alt="Android 8+"/>
  <img src="https://img.shields.io/badge/Material%20You-3-purple.svg" alt="Material You"/>
</p>

<p align="center">
  <a href="https://get-fern.vercel.app"><b>Website</b></a> ·
  <a href="https://github.com/wized2/Fern/releases/latest"><b>Download APK</b></a> ·
  <a href="#features"><b>Features</b></a>
</p>

## Screenshots

| Home (dark) | Home (light) | Details | Active apps | More |
|:---:|:---:|:---:|:---:|:---:|
| <img src="assets/home-dark.png" width="160" alt="Home dark"/> | <img src="assets/home-light.png" width="160" alt="Home light"/> | <img src="assets/details.png" width="160" alt="Details"/> | <img src="assets/apps.png" width="160" alt="Apps"/> | <img src="assets/more.png" width="160" alt="More"/> |

## Features

- **Live dashboard** — CPU, RAM, battery, storage, network with smooth history charts  
- **Material You** — seed colors, light / dark / system, AMOLED-friendly dark  
- **Details & peaks** — breakdowns and shareable metric snapshots  
- **Active apps** — process view; per-app CPU / RAM with elevated access  
- **Sensors, tests, about** — under More without cluttering Home  
- **Dynamic Island–style overlay** (optional) — compact temp · RAM  
- **Advanced mode** — Shizuku (preferred) or root for higher-fidelity sampling  
- **Fully offline** — no account, no trackers, no ads  

## Elevated access

Enable **Advanced mode** in Settings → grant Fern in [Shizuku](https://shizuku.rikka.app/) (or use root).  
When connected: richer CPU sampling and per-app memory / CPU on Active apps.

## Privacy

No analytics SDK. Metrics stay on your device. Elevated access only runs what you enable.

### Permissions

Every permission in `app/src/main/AndroidManifest.xml`, in plain words:

| Permission | What it is used for | Optional? |
|---|---|---|
| `ACCESS_NETWORK_STATE` | Shows the network type (Wi-Fi, mobile, Ethernet) on the dashboard | No |
| `VIBRATE` | Short buzz when you refresh by hand, and the vibration test | Yes, switch off Haptic feedback in Settings |
| `SYSTEM_ALERT_WINDOW` | Draws the floating overlay bubble over other apps | Yes, only if you turn on the overlay |
| `FOREGROUND_SERVICE` | Keeps the overlay running while it is on screen | Yes, only while the overlay runs |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Same as above, for the overlay service | Yes, only while the overlay runs |
| `POST_NOTIFICATIONS` | Shows the small "overlay is running" notice | Yes, only while the overlay runs |
| `PACKAGE_USAGE_STATS` | Lists your active apps with their CPU and RAM use | Yes, only if you turn on Usage access for Active apps |


## Stable vs beta

| Channel | Branch | Application id |
|---------|--------|----------------|
| Stable | `main` | `com.endroid.fern` |
| Beta / nightly | `beta` | `com.endroid.fern.beta` |

PRs should target **`beta`**. Stable releases are promoted from `beta` to `main`. Both builds can be installed at once.

## Install

```text
https://github.com/wized2/Fern/releases/latest
```

Or build:

```bash
./gradlew assembleRelease
```

## Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## Related

| Project | What |
|---------|------|
| **[Latch](https://github.com/wized2/Latch)** | Privacy-first Tasker-style automation (local rules, optional Shizuku) |


## Why Fern vs others?

| | Fern | CPU-Z | Simple System Monitor |
|--|------|-------|------------------------|
| Open source | MIT | No | Varies |
| Material You | Yes | No | Limited |
| No ads / trackers | Yes | Ads in free | Often |
| Shizuku per-app stats | Yes | No | Rare |
| Fully offline | Yes | Partial | Often |

## Install options

- [Latest APK](https://github.com/wized2/Fern/releases/latest)
- **Obtainium:** add `https://github.com/wized2/Fern`
- F-Droid-style metadata lives in `fastlane/metadata` (listing pending)

## Related

Also try **[Latch](https://github.com/wized2/Latch)** — privacy-first automation on the same stack.

## Why Fern?

Most monitors are either bloated with ads or need root for basics. Fern aims for a **clean Material You** experience that works offline, with deeper metrics only when you opt into Shizuku.



## Contributing

We want first-time contributors to succeed quickly.

1. Read **[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)** (5‑minute setup).
2. Branch from **`beta`** and open PRs against **`beta`** (not `main`).
3. Pick a [`good first issue`](https://github.com/wized2/Fern/labels/good%20first%20issue) and comment that you’re on it.

Stable releases are promoted from `beta` → `main`. Beta builds use package id `com.endroid.fern.beta` so both apps can install together.



See [CONTRIBUTING.md](CONTRIBUTING.md). Issues and PRs welcome — especially accessibility, OEM quirks, and chart polish.

## License

[MIT](LICENSE) · Project Endroid

---

<p align="center">If Fern is useful, a <b>star</b> helps others find it ⭐</p>


## First contribution in 15 minutes

1. Fork and clone this repo
2. Open in Android Studio (Giraffe+) or run `./gradlew assembleDebug`
3. Run unit tests: `./gradlew testStableDebugUnitTest`
4. Pick a [good first issue](https://github.com/wized2/Fern/labels/good%20first%20issue)
5. Open a PR against **`beta`** (tiny docs PRs may target `main`)

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Contribute / contributor hunt

We actively welcome first-time PRs:

| Start here | Link |
| --- | --- |
| Good first issues | https://github.com/wized2/Fern/labels/good%20first%20issue |
| Contributor hunt | https://github.com/wized2/Fern/labels/contributor-hunt |
| Help wanted | https://github.com/wized2/Fern/labels/help%20wanted |
| Guide | [CONTRIBUTING.md](CONTRIBUTING.md) |

Typical first PRs: accessibility labels, empty states, unit tests, README translations.

