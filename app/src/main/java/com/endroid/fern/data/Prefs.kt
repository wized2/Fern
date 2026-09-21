package com.endroid.fern.data

import android.content.Context

enum class ThemeMode { AUTO, LIGHT, DARK }

class Prefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("fern", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = runCatching {
            ThemeMode.valueOf(p.getString("theme", ThemeMode.AUTO.name)!!)
        }.getOrDefault(ThemeMode.AUTO)
        set(v) = p.edit().putString("theme", v.name).apply()

    var refreshMs: Int
        get() = p.getInt("refresh_ms", 1500).coerceIn(500, 10_000)
        set(v) = p.edit().putInt("refresh_ms", v.coerceIn(500, 10_000)).apply()

    var keepScreenOn: Boolean
        get() = p.getBoolean("keep_screen_on", false)
        set(v) = p.edit().putBoolean("keep_screen_on", v).apply()

    /** Haptic feedback on manual refresh. */
    var haptics: Boolean
        get() = p.getBoolean("haptics", true)
        set(v) = p.edit().putBoolean("haptics", v).apply()

    /** Pause live sampling while app is in background (saves battery). */
    var pauseInBackground: Boolean
        get() = p.getBoolean("pause_bg", true)
        set(v) = p.edit().putBoolean("pause_bg", v).apply()

    /**
     * Advanced mode: optional Shizuku or root for thermal zones,
     * per-app memory, and force-stop. Off by default.
     */
    var advancedMode: Boolean
        get() = p.getBoolean("advanced_mode", false)
        set(v) = p.edit().putBoolean("advanced_mode", v).apply()
}
