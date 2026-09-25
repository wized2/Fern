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
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.endroid.fern.MainActivity
import com.endroid.fern.R
import com.endroid.fern.data.Prefs
import com.endroid.fern.monitor.SystemMetrics
import kotlin.math.roundToInt

/**
 * Notch-style Dynamic Island: temp · RAM with adjustable gap and optional labels.
 */
class OverlayService : Service() {

    private lateinit var prefs: Prefs
    private lateinit var windowManager: WindowManager
    private var island: LinearLayout? = null
    private var tempTv: TextView? = null
    private var gapView: View? = null
    private var ramTv: TextView? = null
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
        startAsForegroundQuiet()
        attachIsland()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> applyLayoutFromPrefs()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        detachIsland()
        prefs.overlayEnabled = false
        super.onDestroy()
    }

    /**
     * Android requires a FGS notification — keep it as invisible as the platform allows
     * (MIN importance, silent, no badge, secret visibility).
     */
    private fun startAsForegroundQuiet() {
        val channelId = "fern_overlay_quiet"
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            channelId,
            "Background",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Required by Android for overlay"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        nm.createNotificationChannel(ch)

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(" ")
            .setContentText(" ")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
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

    private fun makeLabel(): TextView = TextView(this).apply {
        setTextColor(Color.parseColor("#E8F5E9"))
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = 11.5f
        letterSpacing = 0.01f
        gravity = Gravity.CENTER_VERTICAL
        maxLines = 1
        isSingleLine = true
    }

    private fun attachIsland() {
        if (island != null) return

        val wPx = dp(prefs.overlayWidthDp.toFloat())
        val hPx = dp(prefs.overlayHeightDp.toFloat())
        val alpha = (prefs.overlayOpacity * 255).toInt().coerceIn(160, 255)
        val gapPx = dp(prefs.overlayGapDp.toFloat())

        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = hPx / 2f
            setColor(Color.argb(alpha, 0x1B, 0x5E, 0x20))
            setStroke(dp(1f), Color.argb(alpha, 0x66, 0xBB, 0x6A))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = pill
            setPadding(dp(12f), 0, dp(12f), 0)
            elevation = dp(6f).toFloat()
        }

        val tTv = makeLabel()
        val gap = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(gapPx, 1)
        }
        val mid = TextView(this).apply {
            text = "·"
            setTextColor(Color.parseColor("#A5D6A7"))
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(2f), 0, dp(2f), 0)
        }
        val gap2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(gapPx, 1)
        }
        val rTv = makeLabel()

        root.addView(tTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        root.addView(gap)
        root.addView(mid, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT))
        root.addView(gap2)
        root.addView(rTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))

        tempTv = tTv
        gapView = gap
        ramTv = rTv
        // store second gap via tag for applyLayout
        root.tag = gap2

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
            val (x, y) = computeXy(wPx)
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

    private fun computeXy(wPx: Int): Pair<Int, Int> {
        val sw = screenWidth()
        val x = ((prefs.overlayXPercent / 100f) * (sw - wPx)).roundToInt().coerceAtLeast(0)
        val y = dp(prefs.overlayYDp.toFloat()).coerceAtLeast(0)
        return x to y
    }

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
        val gapPx = dp(prefs.overlayGapDp.toFloat())

        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = hPx / 2f
            setColor(Color.argb(alpha, 0x1B, 0x5E, 0x20))
            setStroke(dp(1f), Color.argb(alpha, 0x66, 0xBB, 0x6A))
        }
        root.background = pill

        gapView?.layoutParams = LinearLayout.LayoutParams(gapPx, 1)
        (root.tag as? View)?.layoutParams = LinearLayout.LayoutParams(gapPx, 1)

        lp.width = wPx
        lp.height = hPx
        val (x, y) = computeXy(wPx)
        lp.x = x
        lp.y = y
        try {
            windowManager.updateViewLayout(root, lp)
        } catch (_: Exception) {
            detachIsland()
            attachIsland()
            return
        }
        refreshStats()
    }

    private fun detachIsland() {
        island?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) { }
        }
        island = null
        tempTv = null
        gapView = null
        ramTv = null
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
        val labels = prefs.overlayShowLabels
        val tempStr = when {
            temp == null -> if (labels) "Temp —" else "—"
            labels -> "Temp ${temp.roundToInt()}°"
            else -> "${temp.roundToInt()}°"
        }
        val ramStr = when {
            ram == null -> if (labels) "RAM —" else "—"
            labels -> "RAM ${ram.roundToInt()}%"
            else -> "${ram.roundToInt()}%"
        }
        tempTv?.text = tempStr
        ramTv?.text = ramStr
        tempTv?.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        ramTv?.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        island?.contentDescription = "$tempStr, $ramStr"
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
