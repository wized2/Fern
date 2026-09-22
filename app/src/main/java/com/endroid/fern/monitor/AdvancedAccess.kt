package com.endroid.fern.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuRemoteProcess
import rikka.shizuku.SystemServiceHelper
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
 * Shizuku / root elevated access.
 * Shell uses IShizukuService.newProcess (transaction 14) → ShizukuRemoteProcess.
 * Force-stop uses IActivityManager via ShizukuBinderWrapper.
 */
class AdvancedAccess(private val context: Context) {

    private val lastBackend = AtomicReference(ElevatedBackend.None)
    private val permissionCache = AtomicBoolean(false)

    fun onBinderReceived() {
        try {
            if (!Shizuku.pingBinder()) return
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
                return
            }
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            permissionCache.set(granted)
            if (!granted) {
                // Sync with Shizuku manager allow-list (instant grant if already allowed)
                Shizuku.requestPermission(REQ_SHIZUKU)
            }
        } catch (e: Exception) {
            Log.w(TAG, "onBinderReceived: ${e.message}")
        }
    }

    fun onBinderDead() {
        permissionCache.set(false)
    }

    fun onPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != REQ_SHIZUKU) return
        permissionCache.set(grantResult == PackageManager.PERMISSION_GRANTED)
        Log.i(TAG, "permission result=${permissionCache.get()}")
    }

    fun isShizukuInstalled(): Boolean {
        val pkgs = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")
        for (pkg in pkgs) {
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(pkg, 0)
                }
                return true
            } catch (_: Exception) {
            }
        }
        return false
    }

    fun isShizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
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
            val ok = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (ok) permissionCache.set(true)
            ok
        } catch (e: Exception) {
            Log.w(TAG, "hasShizukuPermission: ${e.message}")
            false
        }
    }

    /**
     * @return human message for Toast
     */
    fun requestShizukuPermission(): String {
        return try {
            if (!isShizukuRunning()) {
                openShizukuManager()
                return "Shizuku is not running — opened Shizuku app. Start it, then tap Grant again."
            }
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
                return "Connected (legacy Shizuku)"
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                permissionCache.set(true)
                return "Already granted"
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                openShizukuManager()
                return "Permission denied earlier — allow ${context.packageName} in Shizuku, then Refresh"
            }
            Shizuku.requestPermission(REQ_SHIZUKU)
            "Permission request sent — accept the Shizuku prompt"
        } catch (e: Exception) {
            Log.e(TAG, "requestShizukuPermission", e)
            openShizukuManager()
            "Could not request permission (${e.message}). Opened Shizuku."
        }
    }

    fun openShizukuManager() {
        val pkgs = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")
        for (pkg in pkgs) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: continue
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launch)
                return
            } catch (_: Exception) {
            }
        }
    }

    fun isRootAvailable(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/vendor/bin/su")
        return paths.any { java.io.File(it).exists() }
    }

    fun diagnostic(): String {
        val running = isShizukuRunning()
        val ver = try {
            if (running) "v${Shizuku.getVersion()}" else "—"
        } catch (_: Exception) {
            "—"
        }
        val perm = when {
            hasShizukuPermission() -> "yes"
            running -> "no"
            else -> "n/a"
        }
        return "pkg=${context.packageName} · binder=${if (running) "up" else "down"} · $ver · perm=$perm"
    }

    fun status(advancedEnabled: Boolean): ElevatedStatus {
        if (!advancedEnabled) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(ElevatedBackend.None, "Off", "Enable Advanced mode for Shizuku or root")
        }
        if (isShizukuRunning() && hasShizukuPermission()) {
            lastBackend.set(ElevatedBackend.Shizuku)
            return ElevatedStatus(
                ElevatedBackend.Shizuku,
                "Shizuku connected",
                diagnostic()
            )
        }
        if (isShizukuRunning()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku running — tap Grant",
                "Allow ${context.packageName} when prompted. ${diagnostic()}"
            )
        }
        if (isShizukuInstalled()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Start Shizuku first",
                "Open Shizuku and start the service. ${diagnostic()}"
            )
        }
        if (isRootAvailable()) {
            lastBackend.set(ElevatedBackend.Root)
            return ElevatedStatus(ElevatedBackend.Root, "Root (su)", "su binary present")
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
        return hasShizukuPermission() || (isRootAvailable() && lastBackend.get() == ElevatedBackend.Root) ||
            (isRootAvailable() && !isShizukuInstalled())
    }

    /** Prefer Shizuku when permitted; else root. */
    fun isElevatedLive(advancedEnabled: Boolean): Boolean {
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

    /**
     * Official path: binder transact newProcess (code 14) → ShizukuRemoteProcess.
     */
    private fun execShizuku(command: String, timeoutMs: Long): String? {
        val binder = try {
            Shizuku.getBinder()
        } catch (_: Exception) {
            null
        } ?: return null

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(SHIZUKU_DESCRIPTOR)
            data.writeStringArray(arrayOf("sh", "-c", command))
            data.writeStringArray(null)
            data.writeString(null)
            // AIDL transaction id for newProcess = 14
            val ok = binder.transact(14, data, reply, 0)
            if (!ok) {
                Log.w(TAG, "newProcess transact returned false")
                return null
            }
            reply.readException()
            val process = if (reply.readInt() != 0) {
                ShizukuRemoteProcess.CREATOR.createFromParcel(reply)
            } else {
                null
            } ?: return null
            return readProcess(process, timeoutMs)
        } catch (e: Exception) {
            Log.w(TAG, "execShizuku transact: ${e.message}")
            // Fallback: try service stub reflection
            return tryServiceStub(command, timeoutMs)
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private fun tryServiceStub(command: String, timeoutMs: Long): String? {
        return try {
            val binder = Shizuku.getBinder() ?: return null
            val stub = Class.forName("moe.shizuku.server.IShizukuService\$Stub")
            val asInterface = stub.getMethod("asInterface", IBinder::class.java)
            val service = asInterface.invoke(null, binder) ?: return null
            val newProcess = service.javaClass.methods.firstOrNull {
                it.name == "newProcess" && it.parameterTypes.size == 3
            } ?: return null
            val remote = newProcess.invoke(
                service,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as? java.lang.Process ?: return null
            readProcess(remote, timeoutMs)
        } catch (e: Exception) {
            Log.d(TAG, "tryServiceStub: ${e.message}")
            null
        }
    }

    private fun readProcess(process: java.lang.Process, timeoutMs: Long): String? {
        val out = StringBuilder()
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    out.append(line).append('\n')
                    if (out.length > 512_000) break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "read stdout: ${e.message}")
        }
        try {
            process.errorStream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }
        } catch (_: Exception) {
        }
        try {
            if (process is ShizukuRemoteProcess) {
                process.waitForTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            } else if (Build.VERSION.SDK_INT >= 26) {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                process.waitFor()
            }
        } catch (_: Exception) {
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
            readProcess(process, timeoutMs)
        } catch (_: Exception) {
            null
        }
    }

    /** Force-stop via IActivityManager (no shell required). */
    fun forceStopPackage(packageName: String): Boolean {
        if (packageName.isBlank() || packageName.contains(' ')) return false
        if (hasShizukuPermission()) {
            if (forceStopViaAm(packageName)) return true
        }
        // Shell fallback
        return false // caller may use exec
    }

    private fun forceStopViaAm(packageName: String): Boolean {
        return try {
            val raw = SystemServiceHelper.getSystemService("activity") ?: return false
            val binder = ShizukuBinderWrapper(raw)
            val stub = Class.forName("android.app.IActivityManager\$Stub")
            val am = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
                ?: return false
            val methods = am.javaClass.methods.filter { it.name == "forceStopPackage" }
            for (m in methods) {
                try {
                    when (m.parameterTypes.size) {
                        1 -> m.invoke(am, packageName)
                        2 -> m.invoke(am, packageName, 0)
                        3 -> m.invoke(am, packageName, 0, 0)
                        else -> continue
                    }
                    Log.i(TAG, "forceStopPackage ok via AM: $packageName")
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "forceStop try ${m.parameterTypes.size}: ${e.message}")
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "forceStopViaAm: ${e.message}")
            false
        }
    }

    /** Accurate CPU% from /proc/stat via elevated shell (two samples). */
    suspend fun readCpuPercent(): Pair<Float, Boolean>? = withContext(Dispatchers.IO) {
        val a = parseProcStat(exec("cat /proc/stat", 2_000)) ?: return@withContext null
        try {
            Thread.sleep(280)
        } catch (_: InterruptedException) {
        }
        val b = parseProcStat(exec("cat /proc/stat", 2_000)) ?: return@withContext null
        val dTotal = b.first - a.first
        val dIdle = b.second - a.second
        if (dTotal <= 0L) return@withContext null
        val busy = ((dTotal - dIdle).toFloat() / dTotal * 100f).coerceIn(0f, 100f)
        busy to true
    }

    private fun parseProcStat(raw: String?): Pair<Long, Long>? {
        if (raw.isNullOrBlank()) return null
        val line = raw.lineSequence().firstOrNull { it.startsWith("cpu ") } ?: return null
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 5) return null
        val nums = parts.drop(1).mapNotNull { it.toLongOrNull() }
        if (nums.size < 4) return null
        val idle = nums[3] + nums.getOrElse(4) { 0L } // idle + iowait
        val total = nums.sum()
        return total to idle
    }

    companion object {
        const val REQ_SHIZUKU = 7711
        private const val TAG = "FernAdvanced"
        private const val SHIZUKU_DESCRIPTOR = "moe.shizuku.server.IShizukuService"
    }
}
