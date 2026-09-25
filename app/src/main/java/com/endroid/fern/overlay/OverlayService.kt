package com.endroid.fern.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.endroid.fern.MainActivity
import com.endroid.fern.R
import com.endroid.fern.data.Prefs
import com.endroid.fern.monitor.GpuMetrics
import com.endroid.fern.monitor.SystemMetrics
import kotlin.math.roundToInt

/**
 * Floating “island” showing live CPU / RAM / GPU.
 * Requires SYSTEM_ALERT_WINDOW (display over other apps).
 */
class OverlayService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var windowManager: WindowManager
    private var island: LinearLayout? = null
    private var cpuTv: TextView? = null
    private var ramTv: TextView? = null
    private var gpuTv: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var params: WindowManager.LayoutParams? = null

    private val tick = object : Runnable {
        override fun run() {
            refreshStats()
            val ms = prefs.refreshMs.toLong().coerceIn(500L, 10_000L)
            handler.postDelayed(this, ms)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        attachIsland()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> {
                detachIsland()
                attachIsland()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        detachIsland()
        prefs.overlayEnabled = false
        super.onDestroy()
    }

    private fun startAsForeground() {
        val channelId = "fern_overlay"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Fern floating island", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Fern island")
            .setContentText("Showing live CPU · RAM · GPU")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).roundToInt()

    private fun attachIsland() {
        if (island != null) return
        val w = dp(prefs.overlayWidthDp)
        val h = dp(prefs.overlayHeightDp)
        val alpha = prefs.overlayOpacity

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(android.graphics.Color.argb((alpha * 230).toInt().coerceIn(80, 255), 18, 28, 22))
            elevation = dp(8).toFloat()
        }
        fun label(): TextView = TextView(this).apply {
            setTextColor(android.graphics.Color.parseColor("#E8F5E9"))
            typeface = Typeface.DEFAULT_BOLD
            textSize = 12f
            setPadding(0, dp(2), 0, dp(2))
        }
        cpuTv = label().also { root.addView(it) }
        ramTv = label().also { root.addView(it) }
        gpuTv = label().also { root.addView(it) }

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val lp = WindowManager.LayoutParams(
            w, h, type, flags, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.overlayX
            y = prefs.overlayY
        }
        params = lp

        var dragX = 0
        var dragY = 0
        root.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragX = e.rawX.toInt()
                    dragY = e.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX.toInt() - dragX
                    val dy = e.rawY.toInt() - dragY
                    dragX = e.rawX.toInt()
                    dragY = e.rawY.toInt()
                    lp.x += dx
                    lp.y += dy
                    try {
                        windowManager.updateViewLayout(root, lp)
                    } catch (_: Exception) { }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.overlayX = lp.x
                    prefs.overlayY = lp.y
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(root, lp)
            island = root
            refreshStats()
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun detachIsland() {
        island?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) { }
        }
        island = null
        cpuTv = null
        ramTv = null
        gpuTv = null
    }

    private fun refreshStats() {
        val snap = try {
            SystemMetrics.capture(this)
        } catch (_: Exception) {
            null
        }
        val cpu = snap?.cpuPercent
        val ram = snap?.ramPercent
        val gpu = GpuMetrics.readGpuPercent()

        cpuTv?.visibility = if (prefs.overlayShowCpu) View.VISIBLE else View.GONE
        ramTv?.visibility = if (prefs.overlayShowRam) View.VISIBLE else View.GONE
        gpuTv?.visibility = if (prefs.overlayShowGpu) View.VISIBLE else View.GONE

        cpuTv?.text = if (cpu != null) "CPU  ${"%.0f".format(cpu)}%" else "CPU  —"
        ramTv?.text = if (ram != null) "RAM  ${"%.0f".format(ram)}%" else "RAM  —"
        gpuTv?.text = if (gpu != null) "GPU  ${"%.0f".format(gpu)}%" else "GPU  n/a"

        // Apply size changes from prefs without full recreate
        params?.let { lp ->
            val nw = dp(prefs.overlayWidthDp)
            val nh = dp(prefs.overlayHeightDp)
            if (lp.width != nw || lp.height != nh) {
                lp.width = nw
                lp.height = nh
                island?.let { v ->
                    try {
                        windowManager.updateViewLayout(v, lp)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    companion object {
        const val NOTIF_ID = 7701
        const val ACTION_STOP = "com.endroid.fern.overlay.STOP"
        const val ACTION_RELOAD = "com.endroid.fern.overlay.RELOAD"

        fun start(ctx: Context) {
            val i = Intent(ctx, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, OverlayService::class.java).setAction(ACTION_STOP))
        }

        fun reload(ctx: Context) {
            ctx.startService(Intent(ctx, OverlayService::class.java).setAction(ACTION_RELOAD))
        }
    }
}
