# Fern

**Live system pulse for Android** — Material 3 resource monitor. Offline. No ads. Optional Shizuku / root for true per-app CPU & RAM.

[![Build](https://github.com/wized2/Fern/actions/workflows/build.yml/badge.svg)](https://github.com/wized2/Fern/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-8%2B-blue.svg)](https://github.com/wized2/Fern/releases)

**Site:** https://wized2.github.io/Fern/ · **Releases:** https://github.com/wized2/Fern/releases

## Features

- Live **CPU**, **RAM**, **battery**, **storage**, **network** with history charts  
- **Thermal** bar (standard APIs + Advanced thermal zones)  
- **Active Apps** (Usage Access) with sort by recent / CPU / RAM  
- **Dynamic Island**-style overlay (display over other apps)  
- Home-screen **widgets**  
- **Material You** seed color palette (full-circle presets + custom HSV)  
- **Advanced mode** (Shizuku or root):
  - Accurate system CPU  
  - **Per-app RAM (RSS)** and **per-app CPU**  
  - Thermal zones from sysfs  
  - Force-stop apps  
  - Process count  

## Advanced mode (Shizuku)

1. Install and **start** [Shizuku](https://shizuku.rikka.app/)  
2. In Shizuku, **authorize Fern** (`com.endroid.fern`)  
3. Fern → More → Settings → **Advanced mode ON** → **Grant** / **Refresh**  
4. Open **Active Apps** → **Refresh** — CPU% and RAM appear per app when the shell is ready  

Root works as a fallback when available.

## Install

- [Latest APK](https://github.com/wized2/Fern/releases/latest)  
- In-page download: https://wized2.github.io/Fern/  

Min SDK 26 · Target 35 · arm64-v8a / armeabi-v7a  

## Build

```bash
./gradlew :app:assembleRelease
```

CI publishes signed release APK on `main` when secrets are configured.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT © wized2
