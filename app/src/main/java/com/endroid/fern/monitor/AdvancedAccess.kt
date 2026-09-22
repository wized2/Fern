package com.endroid.fern.monitor

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class ElevatedBackend { None, Shizuku, Root }

data class ElevatedStatus(
    val backend: ElevatedBackend,
    val label: String,
    val detail: String
)

/**
 * Optional elevated shell for Advanced mode.
 * Prefer Shizuku; fall back to root `su`.
 *
 * Permission is tracked both via Shizuku.checkSelfPermission() and a local cache
 * updated from OnRequestPermissionResultListener (authorizing in the Shizuku app
 * only takes effect after a binder-connected check / requestPermission sync).
 */
class AdvancedAccess(private val context: Context) {

    private val lastBackend = AtomicReference(ElevatedBackend.None)
    private val permissionCache = AtomicBoolean(false)
    private val binderReady = AtomicBoolean(false)

    fun onBinderReceived() {
        binderReady.set(true)
        // If already allowed in Shizuku manager, requestPermission returns granted immediately.
        try {
            if (!Shizuku.isPreV11() && Shizuku.pingBinder()) {
                val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                permissionCache.set(granted)
                if (!granted) {
                    Log.i(TAG, "binder up, permission not yet granted — syncing via requestPermission")
                    Shizuku.requestPermission(REQ_SHIZUKU)
                } else {
                    Log.i(TAG, "binder up, permission already granted")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "onBinderReceived: ${e.message}")
        }
    }

    fun onBinderDead() {
        binderReady.set(false)
        permissionCache.set(false)
    }

    fun onPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != REQ_SHIZUKU) return
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        permissionCache.set(granted)
        Log.i(TAG, "permission result granted=$granted")
    }

    fun isShizukuInstalled(): Boolean {
        val pm = context.packageManager
        val pkgs = listOf(
            "moe.shizuku.privileged.api",
            "moe.shizuku.manager",
            "moe.shizuku.privileged"
        )
        for (pkg in pkgs) {
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    fun isShizukuRunning(): Boolean = try {
        val ok = Shizuku.pingBinder()
        if (ok) binderReady.set(true)
        ok
    } catch (e: Exception) {
        Log.d(TAG, "pingBinder: ${e.message}")
        false
    }

    fun hasShizukuPermission(): Boolean {
        if (permissionCache.get() && isShizukuRunning()) return true
        return try {
            if (!isShizukuRunning()) return false
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
                return true
            }
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (granted) permissionCache.set(true)
            granted
        } catch (e: Exception) {
            Log.w(TAG, "hasShizukuPermission: ${e.message}")
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int = REQ_SHIZUKU) {
        try {
            if (!isShizukuRunning()) {
                Log.w(TAG, "requestPermission: binder not ready")
                return
            }
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
                return
            }
            // Always request — if already allowed in manager, result fires granted=true immediately
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "requestShizukuPermission", e)
        }
    }

    fun isRootAvailable(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su")
        return paths.any { java.io.File(it).exists() }
    }

    suspend fun probeRoot(): Boolean = withContext(Dispatchers.IO) {
        execRoot("id", 2500)?.contains("uid=0") == true
    }

    fun packageNameForShizuku(): String = context.packageName

    fun diagnostic(): String {
        val running = isShizukuRunning()
        val ver = try {
            if (running) Shizuku.getVersion().toString() else "—"
        } catch (_: Exception) {
            "—"
        }
        val perm = try {
            if (running && !Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) "yes" else "no"
            } else if (permissionCache.get()) "yes(cache)" else "no"
        } catch (e: Exception) {
            "err:${e.javaClass.simpleName}"
        }
        return "pkg=${context.packageName} · binder=${if (running) "up" else "down"} · v$ver · perm=$perm"
    }

    fun status(advancedEnabled: Boolean): ElevatedStatus {
        if (!advancedEnabled) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Off",
                "Enable Advanced mode to use Shizuku or root"
            )
        }

        val running = isShizukuRunning()
        val granted = hasShizukuPermission()
        Log.i(TAG, "status ${diagnostic()}")

        if (running && granted) {
            lastBackend.set(ElevatedBackend.Shizuku)
            return ElevatedStatus(
                ElevatedBackend.Shizuku,
                "Shizuku connected",
                "Elevated shell ready · ${diagnostic()}"
            )
        }
        if (running && !granted) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku running — grant needed",
                "Tap Grant Shizuku (or allow ${context.packageName} in Shizuku). ${diagnostic()}"
            )
        }
        if (isShizukuInstalled()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku installed — service not connected",
                "Open Shizuku → Start, then return and tap Refresh. ${diagnostic()}"
            )
        }
        if (isRootAvailable()) {
            lastBackend.set(ElevatedBackend.Root)
            return ElevatedStatus(
                ElevatedBackend.Root,
                "Root (su)",
                "su binary found — elevated shell available"
            )
        }
        lastBackend.set(ElevatedBackend.None)
        return ElevatedStatus(
            ElevatedBackend.None,
            "Unavailable",
            "Install Shizuku, start it, allow ${context.packageName}"
        )
    }

    fun isElevated(advancedEnabled: Boolean): Boolean {
        if (!advancedEnabled) return false
        return hasShizukuPermission() || isRootAvailable()
    }

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
        tryReflectNewProcess(command, timeoutMs)?.let { return it }
        tryServiceNewProcess(command, timeoutMs)?.let { return it }
        Log.w(TAG, "execShizuku: no working newProcess")
        return null
    }

    private fun tryReflectNewProcess(command: String, timeoutMs: Long): String? {
        return try {
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
            readProcess(process, timeoutMs)
        } catch (e: Exception) {
            Log.d(TAG, "reflect newProcess: ${e.message}")
            null
        }
    }

    private fun tryServiceNewProcess(command: String, timeoutMs: Long): String? {
        return try {
            val binder = Shizuku.getBinder() ?: return null
            val serviceClass = Class.forName("moe.shizuku.server.IShizukuService\$Stub")
            val asInterface = serviceClass.getMethod("asInterface", IBinder::class.java)
            val service = asInterface.invoke(null, ShizukuBinderWrapper(binder)) ?: return null
            val newProcess = service.javaClass.methods.firstOrNull { m ->
                m.name == "newProcess" && m.parameterTypes.size == 3
            } ?: return null
            val remote = newProcess.invoke(
                service,
                arrayOf("sh", "-c", command),
                null,
                null
            ) ?: return null
            val getIn = remote.javaClass.methods.firstOrNull {
                it.name == "getInputStream" && it.parameterTypes.isEmpty()
            } ?: return null
            val input = getIn.invoke(remote) as? java.io.InputStream ?: return null
            val out = StringBuilder()
            BufferedReader(InputStreamReader(input)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    out.append(line).append('\n')
                    if (out.length > 512_000) break
                }
            }
            try {
                remote.javaClass.methods.firstOrNull {
                    it.name == "waitFor" && it.parameterTypes.isEmpty()
                }?.invoke(remote)
            } catch (_: Exception) {
            }
            try {
                remote.javaClass.methods.firstOrNull {
                    it.name == "destroy" && it.parameterTypes.isEmpty()
                }?.invoke(remote)
            } catch (_: Exception) {
            }
            out.toString().trim().ifEmpty { null }
        } catch (e: Exception) {
            Log.d(TAG, "service newProcess: ${e.message}")
            null
        }
    }

    private fun readProcess(process: java.lang.Process, timeoutMs: Long): String? {
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
        return out.toString().trim().ifEmpty { null }
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
        private const val TAG = "FernAdvanced"
    }
}
