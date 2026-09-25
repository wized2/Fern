package com.endroid.fern.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.endroid.fern.MainActivity
import com.endroid.fern.R
import com.endroid.fern.data.Prefs
import com.endroid.fern.monitor.SystemMetrics
import kotlin.math.roundToInt

/**
 * Notch-style Dynamic Island pill:  temp° · RAM%
 * Size and position driven by Prefs (Additional screen sliders).
 */
class OverlayService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var windowManager: WindowManager
    private var island: FrameLayout? = null
    private var label: TextView? = null
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
                // Re-read prefs and re-apply size + position
                applyLayoutFromPrefs()
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
            NotificationChannel(channelId, "Fern island", NotificationManager.IMPORTANCE_LOW)
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
            .setContentText("Temp · RAM notch island")
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

    private fun dp(v: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).roundToInt()

    private fun screenWidth(): Int = resources.displayMetrics.widthPixels
    private fun screenHeight(): Int = resources.displayMetrics.heightPixels

    private fun attachIsland() {
        if (island != null) return

        val wPx = dp(prefs.overlayWidthDp.toFloat())
        val hPx = dp(prefs.overlayHeightDp.toFloat())
        val alpha = (prefs.overlayOpacity * 255).toInt().coerceIn(160, 255)

        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (hPx / 2f)
            setColor(Color.argb(alpha, 0x1B, 0x5E, 0x20))
            setStroke(dp(1f), Color.argb(alpha, 0x66, 0xBB, 0x6A))
        }

        val root = FrameLayout(this).apply {
            background = pill
            elevation = dp(6f).toFloat()
        }
        val tv = TextView(this).apply {
            setTextColor(Color.parseColor("#E8F5E9"))
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 11.5f
            letterSpacing = 0.02f
            gravity = android.view.Gravity.CENTER
            text = "—  ·  —"
            contentDescription = "Temperature and RAM"
        }
        root.addView(
            tv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        label = tv

        val lp = WindowManager.LayoutParams(
            wPx,
            hPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val (x, y) = computeXy(wPx, hPx)
            this.x = x
            this.y = y
        }
        params = lp

        try {
            windowManager.addView(root, lp)
            island = root
            refreshStats()
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun computeXy(wPx: Int, hPx: Int): Pair<Int, Int> {
        val sw = screenWidth()
        val x = ((prefs.overlayXPercent / 100f) * (sw - wPx)).roundToInt().coerceAtLeast(0)
        val y = dp(prefs.overlayYDp.toFloat()).coerceAtLeast(0)
        return x to y
    }

    /** Apply latest width / height / opacity / position from prefs without full service restart. */
    private fun applyLayoutFromPrefs() {
        val root = island
        val lp = params
        if (root == null || lp == null) {
            detachIsland()
            attachIsland()
            return
        }
        val wPx = dp(prefs.overlayWidthDp.toFloat())
        val hPx = dp(prefs.overlayHeightDp.toFloat())
        val alpha = (prefs.overlayOpacity * 255).toInt().coerceIn(160, 255)

        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = (hPx / 2f)
            setColor(Color.argb(alpha, 0x1B, 0x5E, 0x20))
            setStroke(dp(1f), Color.argb(alpha, 0x66, 0xBB, 0x6A))
        }
        root.background = pill

        lp.width = wPx
        lp.height = hPx
        val (x, y) = computeXy(wPx, hPx)
        lp.x = x
        lp.y = y
        lp.gravity = Gravity.TOP or Gravity.START
        try {
            windowManager.updateViewLayout(root, lp)
        } catch (_: Exception) {
            detachIsland()
            attachIsland()
        }
    }

    private fun detachIsland() {
        island?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) { }
        }
        island = null
        label = null
        params = null
    }

    private fun refreshStats() {
        val snap = try {
            SystemMetrics.capture(this)
        } catch (_: Exception) {
            null
        }
        val temp = snap?.batteryTempC
        val ram = snap?.ramPercent
        val tempStr = if (temp != null) "${temp.roundToInt()}°" else "—"
        val ramStr = if (ram != null) "${ram.roundToInt()}%" else "—"
        label?.text = "$tempStr  ·  $ramStr"
        label?.contentDescription = "Temperature $tempStr, RAM $ramStr"
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
