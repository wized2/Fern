package com.endroid.fern.monitor

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class AppState { FOREGROUND, RECENT }

data class ActiveApp(
    val packageName: String,
    val label: String,
    val lastUsedMs: Long,
    val foregroundTimeMs: Long,
    val state: AppState,
    val isSystem: Boolean
)

enum class ActiveWindow(val label: String, val durationMs: Long) {
    MIN_15("15 min", 15 * 60_000L),
    HOUR_1("1 hour", 60 * 60_000L),
    HOUR_24("24 hours", 24 * 60 * 60_000L)
}

class ActiveAppsRepository(private val context: Context) {

    private val labelCache = ConcurrentHashMap<String, String>()
    private val systemCache = ConcurrentHashMap<String, Boolean>()

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun load(
        window: ActiveWindow,
        showSystem: Boolean
    ): List<ActiveApp> = withContext(Dispatchers.Default) {
        if (!hasUsageAccess()) return@withContext emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext emptyList()
        val end = System.currentTimeMillis()
        val start = end - window.durationMs
        val self = context.packageName

        val fgTimes = HashMap<String, Long>()
        val lastUsed = HashMap<String, Long>()
        try {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            for (s in stats) {
                val pkg = s.packageName ?: continue
                if (pkg == self) continue
                fgTimes[pkg] = (fgTimes[pkg] ?: 0L) + s.totalTimeInForeground
                val lu = s.lastTimeUsed
                if (lu > (lastUsed[pkg] ?: 0L)) lastUsed[pkg] = lu
            }
        } catch (_: Exception) {
            // Permission revoked mid-session or OEM restriction
            return@withContext emptyList()
        }

        var foregroundPkg: String? = null
        try {
            val events = usm.queryEvents(start, end)
            val event = UsageEvents.Event()
            val lastEventType = HashMap<String, Int>()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (pkg == self) continue
                val type = event.eventType
                if (type == UsageEvents.Event.ACTIVITY_RESUMED ||
                    type == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    lastEventType[pkg] = type
                    lastUsed[pkg] = maxOf(lastUsed[pkg] ?: 0L, event.timeStamp)
                } else if (type == UsageEvents.Event.ACTIVITY_PAUSED ||
                    type == UsageEvents.Event.MOVE_TO_BACKGROUND
                ) {
                    lastEventType[pkg] = type
                    lastUsed[pkg] = maxOf(lastUsed[pkg] ?: 0L, event.timeStamp)
                }
            }
            // Current foreground = last resume with no later pause (approx: most recent resume)
            var bestTs = 0L
            for ((pkg, type) in lastEventType) {
                if (type == UsageEvents.Event.ACTIVITY_RESUMED ||
                    type == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    val ts = lastUsed[pkg] ?: 0L
                    if (ts >= bestTs) {
                        bestTs = ts
                        foregroundPkg = pkg
                    }
                }
            }
        } catch (_: Exception) {
            // ignore event walk failures
        }

        val pm = context.packageManager
        val out = ArrayList<ActiveApp>(lastUsed.size)
        for ((pkg, lu) in lastUsed) {
            if (lu < start) continue
            val isSys = isSystemApp(pm, pkg)
            if (!showSystem && isSys) continue
            val fg = fgTimes[pkg] ?: 0L
            val state = if (pkg == foregroundPkg) AppState.FOREGROUND else AppState.RECENT
            out.add(
                ActiveApp(
                    packageName = pkg,
                    label = resolveLabel(pm, pkg),
                    lastUsedMs = lu,
                    foregroundTimeMs = fg,
                    state = state,
                    isSystem = isSys
                )
            )
        }
        out.sortByDescending { it.lastUsedMs }
        out
    }

    private fun resolveLabel(pm: PackageManager, pkg: String): String {
        labelCache[pkg]?.let { return it }
        val label = try {
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg
        }
        labelCache[pkg] = label
        return label
    }

    private fun isSystemApp(pm: PackageManager, pkg: String): Boolean {
        systemCache[pkg]?.let { return it }
        val sys = try {
            val ai = pm.getApplicationInfo(pkg, 0)
            val flags = ai.flags
            (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        } catch (_: Exception) {
            false
        }
        systemCache[pkg] = sys
        return sys
    }
}
