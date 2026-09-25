package com.endroid.fern.monitor

import java.io.File

object GpuMetrics {
    /**
     * Best-effort GPU load 0..100. Many devices block sysfs without elevated access.
     * Returns null when unavailable.
     */
    fun readGpuPercent(): Float? {
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
            "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load",
            "/sys/devices/platform/kgsl-2d0.0/kgsl/kgsl-2d0/gpu_busy_percentage",
            "/sys/class/devfreq/gpufreq/load",
            "/sys/kernel/gpu/gpu_busy"
        )
        for (path in paths) {
            try {
                val f = File(path)
                if (!f.canRead()) continue
                val raw = f.readText().trim()
                val n = raw.toFloatOrNull() ?: continue
                return when {
                    n in 0f..100f -> n
                    n > 100f -> (n / 10f).coerceIn(0f, 100f)
                    else -> null
                }
            } catch (_: Exception) { }
        }
        // Mali: try utilization files
        try {
            File("/sys/devices").walkTopDown().maxDepth(4).forEach { f ->
                if (f.isFile && (f.name == "utilization" || f.name == "gpu_load") && f.canRead()) {
                    val n = f.readText().trim().toFloatOrNull()
                    if (n != null && n in 0f..100f) return n
                }
            }
        } catch (_: Exception) { }
        return null
    }

    fun readGpuLabel(): String {
        val candidates = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_model",
            "/sys/kernel/gpu/gpu_model"
        )
        for (p in candidates) {
            try {
                val f = File(p)
                if (f.canRead()) {
                    val s = f.readText().trim()
                    if (s.isNotEmpty()) return s
                }
            } catch (_: Exception) { }
        }
        return "GPU"
    }
}
