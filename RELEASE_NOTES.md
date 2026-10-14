## Fern 1.11.0

Live system pulse — Material 3 monitor with **smoother Advanced mode**.

### Highlights
- **Elevated mode no longer floods the device**: staggered shell work (Lite / Balanced / Full), cached shell probes, longer intervals (3–15s), mutex so collects never stack
- Per-app CPU & RAM still available via Shizuku/root — gathered across ticks instead of all at once
- **Active Apps** search + sort (Recent / CPU / RAM)
- ZRAM size when elevated; process count; force Refresh still does a full heavy pass
- Color seed presets, Material 3, widgets, Dynamic Island overlay
- Offline · no ads · MIT

### Shizuku
1. Start Shizuku → authorize `com.endroid.fern`
2. Fern → Settings → Advanced ON → Grant
3. Choose **Lite** if the phone felt laggy; raise interval if needed
4. Active Apps → Refresh for an immediate full sample

**Fern-release.apk** — R8 + resource-shrunk · Min SDK 26
