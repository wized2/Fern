package com.endroid.fern.data

import android.content.Context

enum class ThemeMode { AUTO, LIGHT, DARK }

class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("fern", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(p.getString("theme", ThemeMode.AUTO.name)!!)
        }.getOrDefault(ThemeMode.AUTO)
        set(v) {
            p.edit().putString("theme", v.name).commit()
        }

    var refreshMs: Int
        get() = p.getInt("refresh_ms", 1500).coerceIn(500, 10_000)
        set(v) {
            p.edit().putInt("refresh_ms", v.coerceIn(500, 10_000)).commit()
        }

    var keepScreenOn: Boolean
        get() = p.getBoolean("keep_screen_on", false)
        set(v) {
            p.edit().putBoolean("keep_screen_on", v).commit()
        }

    var haptics: Boolean
        get() = p.getBoolean("haptics", true)
        set(v) {
            p.edit().putBoolean("haptics", v).commit()
        }

    var pauseInBackground: Boolean
        get() = p.getBoolean("pause_bg", true)
        set(v) {
            p.edit().putBoolean("pause_bg", v).commit()
        }

    var advancedMode: Boolean
        get() = p.getBoolean("advanced_mode", false)
        set(v) {
            p.edit().putBoolean("advanced_mode", v).commit()
        }

    // Floating island overlay
    var overlayEnabled: Boolean
        get() = p.getBoolean("overlay_enabled", false)
        set(v) {
            p.edit().putBoolean("overlay_enabled", v).commit()
        }

    var overlayWidthDp: Int
        get() = p.getInt("overlay_w_dp", 168).coerceIn(120, 320)
        set(v) {
            p.edit().putInt("overlay_w_dp", v.coerceIn(120, 320)).commit()
        }

    var overlayHeightDp: Int
        get() = p.getInt("overlay_h_dp", 34).coerceIn(28, 56)
        set(v) {
            p.edit().putInt("overlay_h_dp", v.coerceIn(28, 56)).commit()
        }

    var overlayOpacity: Float
        get() = p.getFloat("overlay_opacity", 0.92f).coerceIn(0.35f, 1f)
        set(v) {
            p.edit().putFloat("overlay_opacity", v.coerceIn(0.35f, 1f)).commit()
        }

    /** Horizontal position 0–100 (% of screen width; 50 = center). */
    var overlayXPercent: Int
        get() = p.getInt("overlay_x_pct", 50).coerceIn(0, 100)
        set(v) {
            p.edit().putInt("overlay_x_pct", v.coerceIn(0, 100)).commit()
        }

    /** Vertical offset from top of screen in dp (0–120). */
    var overlayYDp: Int
        get() = p.getInt("overlay_y_dp", 6).coerceIn(0, 120)
        set(v) {
            p.edit().putInt("overlay_y_dp", v.coerceIn(0, 120)).commit()
        }

    /** Extra space between temp and RAM (dp), 0–48. */
    var overlayGapDp: Int
        get() = p.getInt("overlay_gap_dp", 12).coerceIn(0, 48)
        set(v) {
            p.edit().putInt("overlay_gap_dp", v.coerceIn(0, 48)).commit()
        }

    /** When true: "Temp 37° · RAM 81%" instead of "37° · 81%". */
    var overlayShowLabels: Boolean
        get() = p.getBoolean("overlay_labels", false)
        set(v) {
            p.edit().putBoolean("overlay_labels", v).commit()
        }

    var overlayShowGpu: Boolean
        get() = p.getBoolean("overlay_show_gpu", true)
        set(v) {
            p.edit().putBoolean("overlay_show_gpu", v).commit()
        }

    var overlayShowRam: Boolean
        get() = p.getBoolean("overlay_show_ram", true)
        set(v) {
            p.edit().putBoolean("overlay_show_ram", v).commit()
        }

    var overlayShowCpu: Boolean
        get() = p.getBoolean("overlay_show_cpu", true)
        set(v) {
            p.edit().putBoolean("overlay_show_cpu", v).commit()
        }
}
