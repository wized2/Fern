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
 */
class AdvancedAccess(private val context: Context) {

    private val lastBackend = AtomicReference(ElevatedBackend.None)

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
        Shizuku.pingBinder()
    } catch (e: Exception) {
        Log.d(TAG, "pingBinder failed: ${e.message}")
        false
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            // Pre-v11: binder presence implies access
            if (Shizuku.isPreV11()) return true
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "checkSelfPermission granted=$granted uid=${Shizuku.getUid()}")
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
            if (Shizuku.isPreV11()) return
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            }
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
        Log.i(TAG, "status advanced=true running=$running granted=$granted installed=${isShizukuInstalled()}")

        if (running && granted) {
            lastBackend.set(ElevatedBackend.Shizuku)
            return ElevatedStatus(
                ElevatedBackend.Shizuku,
                "Shizuku connected",
                "Thermal zones, per-app memory, and force-stop are available"
            )
        }
        if (running && !granted) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku running — permission needed",
                "Tap “Grant Shizuku”, or open the Shizuku app and allow Fern"
            )
        }
        if (isShizukuInstalled()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku installed — not connected",
                "Open Shizuku and start the service, then return here and tap Refresh"
            )
        }
        if (isRootAvailable()) {
            lastBackend.set(ElevatedBackend.Root)
            return ElevatedStatus(
                ElevatedBackend.Root,
                "Root (su)",
                "su found — elevated shell available"
            )
        }
        lastBackend.set(ElevatedBackend.None)
        return ElevatedStatus(
            ElevatedBackend.None,
            "Unavailable",
            "Install Shizuku from GitHub/Play, start it, then grant Fern"
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
        // 1) Reflection on Shizuku.newProcess (present on some API builds)
        tryReflectNewProcess(command, timeoutMs)?.let { return it }
        // 2) IShizukuService.newProcess via binder
        tryServiceNewProcess(command, timeoutMs)?.let { return it }
        Log.w(TAG, "execShizuku: no working newProcess path")
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
            val wrapped = ShizukuBinderWrapper(binder)
            val service = asInterface.invoke(null, wrapped) ?: return null
            val newProcess = service.javaClass.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val remote = newProcess.invoke(
                service,
                arrayOf("sh", "-c", command),
                null,
                null
            ) ?: return null
            // RemoteProcess: getInputStream()
            val getIn = remote.javaClass.methods.firstOrNull {
                it.name == "getInputStream" && it.parameterTypes.isEmpty()
            }
            val getErr = remote.javaClass.methods.firstOrNull {
                it.name == "getErrorStream" && it.parameterTypes.isEmpty()
            }
            val waitFor = remote.javaClass.methods.firstOrNull {
                it.name == "waitFor" && it.parameterTypes.isEmpty()
            }
            val destroy = remote.javaClass.methods.firstOrNull {
                it.name == "destroy" && it.parameterTypes.isEmpty()
            }
            val input = getIn?.invoke(remote) as? java.io.InputStream
                ?: return null
            val out = StringBuilder()
            BufferedReader(InputStreamReader(input)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    out.append(line).append('\n')
                    if (out.length > 512_000) break
                }
            }
            try {
                (getErr?.invoke(remote) as? java.io.InputStream)?.use {
                    BufferedReader(InputStreamReader(it)).readText()
                }
            } catch (_: Exception) {
            }
            try {
                waitFor?.invoke(remote)
            } catch (_: Exception) {
            }
            try {
                destroy?.invoke(remote)
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
