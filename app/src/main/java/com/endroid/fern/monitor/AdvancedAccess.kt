package com.endroid.fern.monitor

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

enum class ElevatedBackend { None, Shizuku, Root }

data class ElevatedStatus(
    val backend: ElevatedBackend,
    val label: String,
    val detail: String
)

/**
 * Optional elevated shell for Advanced mode.
 * Prefer Shizuku (user-controlled); fall back to root `su` when present.
 * Never requested unless the user enables Advanced mode in Settings.
 */
class AdvancedAccess(private val context: Context) {

    private val lastBackend = AtomicReference(ElevatedBackend.None)

    fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: Exception) {
            try {
                context.packageManager.getPackageInfo("moe.shizuku.manager", 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun isShizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
        false
    }

    fun hasShizukuPermission(): Boolean = try {
        if (!isShizukuRunning()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    fun requestShizukuPermission(requestCode: Int = REQ_SHIZUKU) {
        try {
            if (isShizukuRunning() && !hasShizukuPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) {
        }
    }

    fun isRootAvailable(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su")
        if (paths.any { java.io.File(it).exists() }) {
            // Cheap presence check — full proof is a successful `su -c id`
            return true
        }
        return false
    }

    suspend fun probeRoot(): Boolean = withContext(Dispatchers.IO) {
        execRoot("id", 2500)?.contains("uid=0") == true
    }

    fun status(advancedEnabled: Boolean): ElevatedStatus {
        if (!advancedEnabled) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Off",
                "Enable Advanced mode in Settings to use Shizuku or root"
            )
        }
        if (hasShizukuPermission()) {
            lastBackend.set(ElevatedBackend.Shizuku)
            return ElevatedStatus(
                ElevatedBackend.Shizuku,
                "Shizuku",
                "Connected — thermal zones, per-app memory, force-stop"
            )
        }
        if (isShizukuRunning() && !hasShizukuPermission()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku running",
                "Tap “Grant Shizuku” to authorize Fern"
            )
        }
        if (isShizukuInstalled()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku installed",
                "Start Shizuku, then grant permission"
            )
        }
        // Root as fallback when Advanced is on
        if (isRootAvailable()) {
            lastBackend.set(ElevatedBackend.Root)
            return ElevatedStatus(
                ElevatedBackend.Root,
                "Root",
                "su detected — elevated shell available"
            )
        }
        lastBackend.set(ElevatedBackend.None)
        return ElevatedStatus(
            ElevatedBackend.None,
            "Unavailable",
            "Install Shizuku (recommended) or use a rooted device"
        )
    }

    fun isElevated(advancedEnabled: Boolean): Boolean {
        if (!advancedEnabled) return false
        return hasShizukuPermission() || isRootAvailable()
    }

    /** Run a shell command with the best available backend. */
    suspend fun exec(command: String, timeoutMs: Long = 5_000): String? =
        withContext(Dispatchers.IO) {
            if (hasShizukuPermission()) {
                execShizuku(command, timeoutMs)?.let { return@withContext it }
            }
            if (isRootAvailable()) {
                execRoot(command, timeoutMs)?.let { return@withContext it }
            }
            null
        }

    private fun execShizuku(command: String, timeoutMs: Long): String? {
        return try {
            // newProcess visibility varies by Shizuku API revision — call via reflection
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as? java.lang.Process ?: return null
            val out = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    out.append(line).append('\n')
                    if (out.length > 512_000) break
                }
            }
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            } catch (_: Exception) {
            }
            if (Build.VERSION.SDK_INT >= 26) {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                process.waitFor()
            }
            try {
                process.destroy()
            } catch (_: Exception) {
            }
            out.toString().trim().ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    private fun execRoot(command: String, timeoutMs: Long): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val out = StringBuilder()
            val readerThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            out.append(line).append('\n')
                            if (out.length > 512_000) break
                        }
                    }
                } catch (_: Exception) {
                }
            }
            readerThread.start()
            val finished = if (Build.VERSION.SDK_INT >= 26) {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                readerThread.join(timeoutMs)
                true
            }
            if (!finished) {
                process.destroy()
                return null
            }
            readerThread.join(500)
            out.toString().trim().ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val REQ_SHIZUKU = 7711
    }
}
