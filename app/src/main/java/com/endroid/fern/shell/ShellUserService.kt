package com.endroid.fern.shell

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Runs in a Shizuku-privileged process. Runtime.exec here has shell/root rights.
 */
class ShellUserService : IShellService.Stub {

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(context: Context)

    override fun exec(command: String?): String {
        if (command.isNullOrBlank()) return ""
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
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
            if (Build.VERSION.SDK_INT >= 26) {
                process.waitFor(8_000, TimeUnit.MILLISECONDS)
            } else {
                tOut.join(8_000)
            }
            try {
                process.destroy()
            } catch (_: Exception) {
            }
            tOut.join(400)
            tErr.join(200)
            val text = out.toString().trim()
            if (text.isEmpty() && err.isNotEmpty()) {
                Log.w(TAG, "stderr: ${err.take(120)}")
                return err.toString().trim()
            }
            text
        } catch (e: Exception) {
            Log.e(TAG, "exec failed", e)
            "ERR:${e.message}"
        }
    }

    companion object {
        private const val TAG = "FernShellUser"
    }
}
