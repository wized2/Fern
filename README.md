# Fern

**Live system pulse for Android** — Material 3 resource monitor. Offline. No ads. Optional Shizuku / root for true per-app CPU & RAM.

[![Build](https://github.com/wized2/Fern/actions/workflows/build.yml/badge.svg)](https://github.com/wized2/Fern/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-8%2B-blue.svg)](https://github.com/wized2/Fern/releases)

**Site:** https://get-fern.vercel.app · **Mirror:** https://wized2.github.io/Fern/ · **Releases:** https://github.com/wized2/Fern/releases

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
3. Fern → More → Settings → **Advanced mode ON** → **Grant**  
4. Pick **Lite / Balanced / Full** and shell interval (default **Balanced**, 5s) if the device feels busy  
5. Open **Active Apps** → **Refresh** for an immediate full sample  

Elevated work is **staggered** (not every dumpsys every tick) so the phone stays smooth. Root works as a fallback when available.

**Site:** https://get-fern.vercel.app

## Install

- [Latest APK](https://github.com/wized2/Fern/releases/latest)  
- Website: https://get-fern.vercel.app
- GitHub Pages: https://wized2.github.io/Fern/  

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

## Related projects

- **[Latch](https://github.com/wized2/Latch)** — privacy-first Tasker-style automation (local rules, optional Shizuku)

## Support

If Fern is useful, a GitHub star helps others find it. Issues and PRs welcome.
