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
}
