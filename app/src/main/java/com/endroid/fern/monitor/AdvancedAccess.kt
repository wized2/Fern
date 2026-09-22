package com.endroid.fern.monitor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class ElevatedBackend { None, Shizuku, Root }

data class ElevatedStatus(
    val backend: ElevatedBackend,
    val label: String,
    val detail: String
)

class AdvancedAccess(private val context: Context) {

    private val lastBackend = AtomicReference(ElevatedBackend.None)
    private val permissionCache = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val newProcessMethod: Method? by lazy {
        try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            m
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku.newProcess not found: ${e.message}")
            null
        }
    }

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
                mainHandler.post {
                    try {
                        Shizuku.requestPermission(REQ_SHIZUKU)
                    } catch (e: Exception) {
                        Log.w(TAG, "auto requestPermission: ${e.message}")
                    }
                }
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
        for (pkg in listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")) {
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

    fun requestShizukuPermission(): String {
        return try {
            if (!isShizukuRunning()) {
                openShizukuManager()
                return "Shizuku not running — opened Shizuku. Start the service, then tap Grant again."
            }
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
                return "Connected (legacy Shizuku)"
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                permissionCache.set(true)
                return "Already granted — status should show Connected"
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                openShizukuManager()
                return "Denied earlier. In Shizuku, allow ${context.packageName}, then Refresh."
            }
            // Must run on main thread
            mainHandler.post {
                try {
                    Shizuku.requestPermission(REQ_SHIZUKU)
                } catch (e: Exception) {
                    Log.e(TAG, "requestPermission", e)
                }
            }
            "Permission dialog requested — accept it for ${context.packageName}"
        } catch (e: Exception) {
            Log.e(TAG, "requestShizukuPermission", e)
            openShizukuManager()
            "Error: ${e.message}. Opened Shizuku."
        }
    }

    fun openShizukuManager() {
        for (pkg in listOf("moe.shizuku.privileged.api", "moe.shizuku.manager")) {
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
            return ElevatedStatus(ElevatedBackend.None, "Off", "Enable Advanced mode")
        }
        if (isShizukuRunning() && hasShizukuPermission()) {
            lastBackend.set(ElevatedBackend.Shizuku)
            return ElevatedStatus(ElevatedBackend.Shizuku, "Shizuku connected", diagnostic())
        }
        if (isShizukuRunning()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Shizuku running — tap Grant",
                "Allow ${context.packageName}. ${diagnostic()}"
            )
        }
        if (isShizukuInstalled()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(
                ElevatedBackend.None,
                "Start Shizuku first",
                "Open Shizuku → Start. ${diagnostic()}"
            )
        }
        if (isRootAvailable()) {
            lastBackend.set(ElevatedBackend.Root)
            return ElevatedStatus(ElevatedBackend.Root, "Root (su)", "su found")
        }
        lastBackend.set(ElevatedBackend.None)
        return ElevatedStatus(
            ElevatedBackend.None,
            "Unavailable",
            "Install Shizuku and allow ${context.packageName}"
        )
    }

    fun isElevatedLive(advancedEnabled: Boolean): Boolean {
        if (!advancedEnabled) return false
        return hasShizukuPermission() || isRootAvailable()
    }

    suspend fun exec(command: String, timeoutMs: Long = 4_000): String? =
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
        // Preferred: private Shizuku.newProcess (exists in API 13.1.5)
        try {
            val m = newProcessMethod
            if (m != null) {
                val process = m.invoke(
                    null,
                    arrayOf("sh", "-c", command),
                    null,
                    null
                ) as? java.lang.Process
                if (process != null) {
                    return readProcess(process, timeoutMs)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "newProcess reflect: ${e.message}")
        }
        return null
    }

    private fun readProcess(process: java.lang.Process, timeoutMs: Long): String? {
        val out = StringBuilder()
        val reader = Thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        out.append(line).append('\n')
                        if (out.length > 400_000) break
                    }
                }
            } catch (_: Exception) {
            }
        }
        reader.start()
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                reader.join(timeoutMs)
            }
        } catch (_: Exception) {
        }
        try {
            process.destroy()
        } catch (_: Exception) {
        }
        try {
            reader.join(300)
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

    fun forceStopPackage(packageName: String): Boolean {
        if (packageName.isBlank() || packageName.contains(' ')) return false
        if (!hasShizukuPermission()) return false
        return try {
            val raw = SystemServiceHelper.getSystemService("activity") ?: return false
            val binder = ShizukuBinderWrapper(raw)
            val stub = Class.forName("android.app.IActivityManager\$Stub")
            val am = stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
                ?: return false
            for (m in am.javaClass.methods.filter { it.name == "forceStopPackage" }) {
                try {
                    when (m.parameterTypes.size) {
                        1 -> m.invoke(am, packageName)
                        2 -> m.invoke(am, packageName, 0)
                        else -> continue
                    }
                    Log.i(TAG, "forceStop ok: $packageName")
                    return true
                } catch (e: Exception) {
                    Log.d(TAG, "forceStop arity ${m.parameterTypes.size}: ${e.message}")
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "forceStop: ${e.message}")
            false
        }
    }

    suspend fun readCpuPercent(): Pair<Float, Boolean>? = withContext(Dispatchers.IO) {
        val a = parseProcStat(exec("cat /proc/stat", 1_500)) ?: return@withContext null
        try {
            Thread.sleep(200)
        } catch (_: InterruptedException) {
        }
        val b = parseProcStat(exec("cat /proc/stat", 1_500)) ?: return@withContext null
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
        val idle = nums[3] + nums.getOrElse(4) { 0L }
        val total = nums.sum()
        return total to idle
    }

    companion object {
        const val REQ_SHIZUKU = 7711
        private const val TAG = "FernAdvanced"
    }
}
