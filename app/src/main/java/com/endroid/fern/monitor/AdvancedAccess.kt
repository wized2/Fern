package com.endroid.fern.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.endroid.fern.shell.IShellService
import com.endroid.fern.shell.ShellUserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.util.concurrent.CountDownLatch
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
    private val shellWorks = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val shellService = AtomicReference<IShellService?>(null)
    private val bindLatch = AtomicReference<CountDownLatch?>(null)

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(false)
            .version(BuildConfigVersion)
    }

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                shellService.set(IShellService.Stub.asInterface(binder))
                Log.i(TAG, "UserService connected")
            }
            bindLatch.get()?.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService.set(null)
            shellWorks.set(false)
            Log.w(TAG, "UserService disconnected")
        }
    }

    private val newProcessMethod: Method? by lazy { resolveNewProcess() }

    private fun resolveNewProcess(): Method? {
        return try {
            for (m in Shizuku::class.java.declaredMethods) {
                if (m.name != "newProcess" || m.parameterTypes.size != 3) continue
                m.isAccessible = true
                return m
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun onBinderReceived() {
        try {
            if (!Shizuku.pingBinder()) return
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
            } else {
                val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                permissionCache.set(granted)
                if (!granted) {
                    mainHandler.post {
                        try {
                            Shizuku.requestPermission(REQ_SHIZUKU)
                        } catch (_: Exception) {
                        }
                    }
                } else {
                    ensureUserService()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "onBinderReceived: ${e.message}")
        }
    }

    fun onBinderDead() {
        permissionCache.set(false)
        shellWorks.set(false)
        shellService.set(null)
    }

    fun onPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != REQ_SHIZUKU) return
        val ok = grantResult == PackageManager.PERMISSION_GRANTED
        permissionCache.set(ok)
        if (ok) ensureUserService()
    }

    /** Bind privileged UserService (best shell path). */
    fun ensureUserService() {
        if (!hasShizukuPermission()) return
        if (shellService.get() != null) return
        try {
            val latch = CountDownLatch(1)
            bindLatch.set(latch)
            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            // Don't block UI; wait happens in exec
        } catch (e: Exception) {
            Log.e(TAG, "bindUserService", e)
        }
    }

    private fun awaitShellService(timeoutMs: Long = 4_000): IShellService? {
        shellService.get()?.let { return it }
        ensureUserService()
        try {
            bindLatch.get()?.await(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        }
        return shellService.get()
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
        } catch (_: Exception) {
            false
        }
    }

    fun requestShizukuPermission(): String {
        return try {
            if (!isShizukuRunning()) {
                openShizukuManager()
                return "Shizuku not running — opened Shizuku"
            }
            if (Shizuku.isPreV11()) {
                permissionCache.set(true)
                ensureUserService()
                return "Connected (legacy)"
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                permissionCache.set(true)
                ensureUserService()
                return "Already granted — binding elevated service"
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                openShizukuManager()
                return "Allow ${context.packageName} in Shizuku"
            }
            mainHandler.post {
                try {
                    Shizuku.requestPermission(REQ_SHIZUKU)
                } catch (_: Exception) {
                }
            }
            "Accept the Shizuku permission prompt"
        } catch (e: Exception) {
            openShizukuManager()
            "Error: ${e.message}"
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
        val perm = if (hasShizukuPermission()) "yes" else if (running) "no" else "n/a"
        val svc = if (shellService.get() != null) "svc=up" else "svc=down"
        val sh = when {
            shellWorks.get() -> "shell=ok"
            else -> "shell=?"
        }
        return "perm=$perm · $svc · $sh · $ver"
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
            return ElevatedStatus(ElevatedBackend.None, "Shizuku running — tap Grant", diagnostic())
        }
        if (isShizukuInstalled()) {
            lastBackend.set(ElevatedBackend.None)
            return ElevatedStatus(ElevatedBackend.None, "Start Shizuku first", diagnostic())
        }
        if (isRootAvailable()) {
            lastBackend.set(ElevatedBackend.Root)
            return ElevatedStatus(ElevatedBackend.Root, "Root (su)", "su found")
        }
        lastBackend.set(ElevatedBackend.None)
        return ElevatedStatus(ElevatedBackend.None, "Unavailable", "Install Shizuku")
    }

    fun isElevatedLive(advancedEnabled: Boolean): Boolean {
        if (!advancedEnabled) return false
        return hasShizukuPermission() || isRootAvailable()
    }

    suspend fun exec(command: String, timeoutMs: Long = 5_000): String? =
        withContext(Dispatchers.IO) {
            if (hasShizukuPermission()) {
                // 1) UserService (most reliable)
                try {
                    val svc = awaitShellService(timeoutMs.coerceAtMost(5_000))
                    if (svc != null) {
                        val result = svc.exec(command)
                        if (result != null && !result.startsWith("ERR:")) {
                            shellWorks.set(true)
                            return@withContext result.ifBlank { null }
                        }
                        Log.w(TAG, "UserService exec: ${result?.take(80)}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "UserService: ${e.message}")
                }
                // 2) private newProcess reflection
                execShizukuNewProcess(command, timeoutMs)?.let {
                    shellWorks.set(true)
                    return@withContext it
                }
            }
            if (isRootAvailable()) {
                execRoot(command, timeoutMs)?.let {
                    shellWorks.set(true)
                    return@withContext it
                }
            }
            null
        }

    suspend fun probeShell(): Boolean = withContext(Dispatchers.IO) {
        if (hasShizukuPermission()) ensureUserService()
        val out = exec("echo FERN_OK; id", 3_000)
        val ok = out != null && (out.contains("FERN_OK") || out.contains("uid="))
        shellWorks.set(ok)
        Log.i(TAG, "probeShell ok=$ok out=${out?.take(100)}")
        ok
    }

    private fun execShizukuNewProcess(command: String, timeoutMs: Long): String? {
        val m = newProcessMethod ?: return null
        return try {
            val process = m.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as? java.lang.Process ?: return null
            readProcess(process, timeoutMs)
        } catch (e: Exception) {
            Log.w(TAG, "newProcess: ${e.message}")
            null
        }
    }

    private fun readProcess(process: java.lang.Process, timeoutMs: Long): String? {
        val out = StringBuilder()
        val tOut = Thread {
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
        tOut.start()
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                tOut.join(timeoutMs)
            }
        } catch (_: Exception) {
        }
        try {
            process.destroy()
        } catch (_: Exception) {
        }
        try {
            tOut.join(400)
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

    private fun activityManager(): Any? {
        return try {
            val raw = SystemServiceHelper.getSystemService("activity") ?: return null
            val binder = ShizukuBinderWrapper(raw)
            val stub = Class.forName("android.app.IActivityManager\$Stub")
            stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        } catch (e: Exception) {
            Log.w(TAG, "AM: ${e.message}")
            null
        }
    }

    fun forceStopPackage(packageName: String): Boolean {
        if (packageName.isBlank() || packageName.contains(' ')) return false
        if (!hasShizukuPermission()) return false
        val am = activityManager() ?: return false
        for (m in am.javaClass.methods.filter { it.name.contains("forceStop", ignoreCase = true) }) {
            try {
                when (m.parameterTypes.size) {
                    1 -> m.invoke(am, packageName)
                    2 -> if (m.parameterTypes[1] == Int::class.javaPrimitiveType) {
                        m.invoke(am, packageName, 0)
                    } else continue
                    3 -> m.invoke(am, packageName, 0, 0)
                    else -> continue
                }
                Log.i(TAG, "forceStop ok: $packageName via ${m.name}")
                return true
            } catch (e: Exception) {
                Log.d(TAG, "forceStop ${m.name}: ${e.message}")
            }
        }
        return false
    }

    fun runningAppPssKb(): Map<String, Long> {
        // Prefer shell dumpsys when available — more complete than getRunningAppProcesses
        return emptyMap() // filled by AdvancedMetrics via shell
    }

    suspend fun readCpuPercent(): Pair<Float, Boolean>? = withContext(Dispatchers.IO) {
        val a = parseProcStat(exec("cat /proc/stat", 2_000)) ?: return@withContext null
        delay(220)
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
        val idle = nums[3] + nums.getOrElse(4) { 0L }
        val total = nums.sum()
        return total to idle
    }

    companion object {
        const val REQ_SHIZUKU = 7711
        private const val TAG = "FernAdvanced"
        // Increment when ShellUserService API changes
        private const val BuildConfigVersion = 2
    }
}
