package com.endroid.fern.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
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
    private val shellWorks = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val newProcessMethod: Method? by lazy { resolveNewProcess() }

    private fun resolveNewProcess(): Method? {
        try {
            for (m in Shizuku::class.java.declaredMethods) {
                if (m.name != "newProcess") continue
                if (m.parameterTypes.size != 3) continue
                m.isAccessible = true
                Log.i(TAG, "newProcess found: ${m.returnType.name}")
                return m
            }
            Log.e(TAG, "newProcess missing; methods=" +
                Shizuku::class.java.declaredMethods.joinToString { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "resolveNewProcess", e)
        }
        return null
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
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "onBinderReceived: ${e.message}")
        }
    }

    fun onBinderDead() {
        permissionCache.set(false)
        shellWorks.set(false)
    }

    fun onPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode != REQ_SHIZUKU) return
        permissionCache.set(grantResult == PackageManager.PERMISSION_GRANTED)
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
                return "Connected (legacy)"
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                permissionCache.set(true)
                return "Already granted"
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

    fun shellHealthy(): Boolean = shellWorks.get()

    fun diagnostic(): String {
        val running = isShizukuRunning()
        val ver = try {
            if (running) "v${Shizuku.getVersion()}" else "—"
        } catch (_: Exception) {
            "—"
        }
        val perm = if (hasShizukuPermission()) "yes" else if (running) "no" else "n/a"
        val np = if (newProcessMethod != null) "np=yes" else "np=NO"
        val sh = if (shellWorks.get()) "shell=ok" else "shell=?"
        return "pkg=${context.packageName} · binder=${if (running) "up" else "down"} · $ver · perm=$perm · $np · $sh"
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

    suspend fun exec(command: String, timeoutMs: Long = 4_000): String? =
        withContext(Dispatchers.IO) {
            if (hasShizukuPermission()) {
                execShizuku(command, timeoutMs)?.let {
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

    /** Probe shell once (id). */
    suspend fun probeShell(): Boolean = withContext(Dispatchers.IO) {
        val out = exec("id", 2_000)
        val ok = out != null && (out.contains("uid=") || out.isNotBlank())
        shellWorks.set(ok)
        Log.i(TAG, "probeShell ok=$ok out=${out?.take(80)}")
        ok
    }

    private fun execShizuku(command: String, timeoutMs: Long): String? {
        val m = newProcessMethod ?: return null
        val cmds = listOf(
            arrayOf("sh", "-c", command),
            arrayOf("/system/bin/sh", "-c", command)
        )
        for (cmd in cmds) {
            try {
                val process = m.invoke(null, cmd, null, null) ?: continue
                if (process !is java.lang.Process) {
                    Log.w(TAG, "newProcess type=${process.javaClass.name}")
                    continue
                }
                val result = readProcess(process, timeoutMs)
                if (result != null) return result
            } catch (e: Exception) {
                Log.w(TAG, "execShizuku: ${e.message}")
            }
        }
        return null
    }

    private fun readProcess(process: java.lang.Process, timeoutMs: Long): String? {
        val out = StringBuilder()
        val err = StringBuilder()
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
        val tErr = Thread {
            try {
                BufferedReader(InputStreamReader(process.errorStream)).use { br ->
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        err.append(line).append('\n')
                        if (err.length > 8_000) break
                    }
                }
            } catch (_: Exception) {
            }
        }
        tOut.start()
        tErr.start()
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
            tErr.join(200)
        } catch (_: Exception) {
        }
        val text = out.toString().trim()
        if (text.isEmpty() && err.isNotEmpty()) {
            Log.w(TAG, "shell stderr: ${err.take(200)}")
        }
        return text.ifEmpty { null }
    }

    private fun execRoot(command: String, timeoutMs: Long): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            readProcess(process, timeoutMs)
        } catch (_: Exception) {
            null
        }
    }

    // ---------- Binder-based (no shell) ----------

    private fun activityManager(): Any? {
        return try {
            val raw = SystemServiceHelper.getSystemService("activity") ?: return null
            val binder = ShizukuBinderWrapper(raw)
            val stub = Class.forName("android.app.IActivityManager\$Stub")
            stub.getMethod("asInterface", IBinder::class.java).invoke(null, binder)
        } catch (e: Exception) {
            Log.w(TAG, "activityManager: ${e.message}")
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
                    2 -> {
                        if (m.parameterTypes[1] == Int::class.javaPrimitiveType) {
                            m.invoke(am, packageName, 0)
                        } else continue
                    }
                    3 -> m.invoke(am, packageName, 0, 0)
                    else -> continue
                }
                Log.i(TAG, "forceStop ok via ${m.name}: $packageName")
                return true
            } catch (e: Exception) {
                Log.d(TAG, "forceStop ${m.name}: ${e.message}")
            }
        }
        return false
    }

    /**
     * Per-app PSS (KB) via IActivityManager — works without shell on many devices.
     */
    fun runningAppPssKb(): Map<String, Long> {
        if (!hasShizukuPermission()) return emptyMap()
        val am = activityManager() ?: return emptyMap()
        return try {
            val getProcs = am.javaClass.methods.firstOrNull {
                it.name == "getRunningAppProcesses" && it.parameterTypes.isEmpty()
            } ?: return emptyMap()
            @Suppress("UNCHECKED_CAST")
            val list = getProcs.invoke(am) as? List<Any?> ?: return emptyMap()
            val pidToPkgs = HashMap<Int, List<String>>()
            val pids = ArrayList<Int>()
            for (item in list) {
                if (item == null) continue
                val cls = item.javaClass
                val pid = cls.getField("pid").getInt(item)
                @Suppress("UNCHECKED_CAST")
                val pkgs = (cls.getField("pkgList").get(item) as? Array<String>)?.toList()
                    ?: listOfNotNull(cls.getField("processName").get(item) as? String)
                if (pkgs.isEmpty()) continue
                pidToPkgs[pid] = pkgs
                pids.add(pid)
            }
            if (pids.isEmpty()) return emptyMap()
            // Prefer elevated getProcessMemoryInfo
            val getMem = am.javaClass.methods.firstOrNull {
                it.name == "getProcessMemoryInfo" && it.parameterTypes.size == 1
            }
            val map = HashMap<String, Long>()
            if (getMem != null) {
                val infos = getMem.invoke(am, pids.toIntArray()) as? Array<*>
                if (infos != null) {
                    for (i in infos.indices) {
                        val info = infos[i] ?: continue
                        val pss = try {
                            (info as Debug.MemoryInfo).totalPss.toLong()
                        } catch (_: Exception) {
                            try {
                                info.javaClass.getMethod("getTotalPss").invoke(info) as? Int
                            } catch (_: Exception) {
                                null
                            }?.toLong()
                        } ?: continue
                        val pkgs = pidToPkgs[pids[i]] ?: continue
                        for (pkg in pkgs) {
                            if (!pkg.contains('.')) continue
                            map[pkg] = maxOf(map[pkg] ?: 0L, pss)
                        }
                    }
                }
            }
            // Fallback: ActivityManager from context (limited)
            if (map.isEmpty()) {
                val ctxAm = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val mem = ctxAm.getProcessMemoryInfo(pids.toIntArray())
                for (i in mem.indices) {
                    val pss = mem[i].totalPss.toLong()
                    val pkgs = pidToPkgs[pids[i]] ?: continue
                    for (pkg in pkgs) {
                        if (!pkg.contains('.')) continue
                        map[pkg] = maxOf(map[pkg] ?: 0L, pss)
                    }
                }
            }
            Log.i(TAG, "runningAppPssKb size=${map.size}")
            map
        } catch (e: Exception) {
            Log.w(TAG, "runningAppPssKb: ${e.message}")
            emptyMap()
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
