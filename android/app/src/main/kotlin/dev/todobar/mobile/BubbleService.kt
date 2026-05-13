package dev.todobar.mobile

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import dev.todobar.mobile.ui.SidebarOverlayController
import kotlin.math.abs

/**
 * Foreground service that hosts the right-edge floating handle and an expanded
 * sidebar overlay. The handle visual matches the desktop todobar tab: a slim,
 * vertically-tall pill nudged off the screen edge.
 *
 * Tap → expand the sidebar overlay.
 * Drag → reposition the handle vertically along the right edge.
 */
class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var sidebar: SidebarOverlayController? = null

    private val handleHeightPx by lazy { dp(92f).toInt() }
    private val handleWidthPx by lazy { dp(14f).toInt() }
    private val edgeNudgePx by lazy { dp(2f).toInt() }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_OPEN -> openSidebar()
            ACTION_CLOSE -> closeSidebar()
            else -> ensureBubble()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Keep the bubble pinned to the right edge if rotation changes.
        bubbleParams?.let { params ->
            params.x = 0
            params.y = clampVertical(params.y)
            bubbleView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
        }
        sidebar?.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
        sidebar?.dismiss()
        sidebar = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val channelId = "todobar.bubble"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.bubble_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = getString(R.string.bubble_channel_desc)
                setShowBadge(false)
            }
            mgr.createNotificationChannel(channel)
        }

        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_handle_indicator)
            .setContentTitle(getString(R.string.bubble_notification_title))
            .setContentText(getString(R.string.bubble_notification_body))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    private fun ensureBubble() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "overlay permission missing, cannot show bubble")
            stopSelf()
            return
        }
        if (bubbleView != null) return

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, FrameLayout(this), false)
        val params = WindowManager.LayoutParams(
            handleWidthPx,
            handleHeightPx,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = -edgeNudgePx
            y = defaultBubbleY()
        }

        attachDragAndTap(view, params)
        runCatching { windowManager.addView(view, params) }
            .onFailure { Log.e(TAG, "bubble add failed", it) }
        bubbleView = view
        bubbleParams = params
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var initialY = 0
        var touchStartY = 0f
        var touchStartTime = 0L
        var dragging = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    touchStartY = event.rawY
                    touchStartTime = System.currentTimeMillis()
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - touchStartY
                    if (!dragging && abs(dy) > dp(6f)) dragging = true
                    if (dragging) {
                        params.y = clampVertical((initialY + dy).toInt())
                        runCatching { windowManager.updateViewLayout(v, params) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime
                    if (!dragging && duration < 280) {
                        openSidebar()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun openSidebar() {
        if (!Settings.canDrawOverlays(this)) return
        if (sidebar != null) return
        bubbleView?.let { it.animate().alpha(0f).setDuration(120).start() }
        sidebar = SidebarOverlayController(
            context = this,
            windowManager = windowManager,
            overlayType = overlayType(),
            onDismiss = ::onSidebarDismissed,
        ).also { it.show() }
    }

    private fun closeSidebar() {
        sidebar?.dismiss()
    }

    private fun onSidebarDismissed() {
        sidebar = null
        bubbleView?.animate()?.alpha(1f)?.setDuration(160)
            ?.setInterpolator(AccelerateDecelerateInterpolator())?.start()
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        bubbleParams = null
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun defaultBubbleY(): Int {
        val metrics = resources.displayMetrics
        return (metrics.heightPixels / 2) - (handleHeightPx / 2)
    }

    private fun clampVertical(value: Int): Int {
        val metrics: DisplayMetrics = resources.displayMetrics
        val maxY = metrics.heightPixels - handleHeightPx - dp(24f).toInt()
        val minY = dp(24f).toInt()
        return value.coerceIn(minY, maxY)
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    companion object {
        private const val TAG = "BubbleService"
        private const val NOTIFICATION_ID = 4711
        const val ACTION_OPEN = "dev.todobar.mobile.action.OPEN"
        const val ACTION_CLOSE = "dev.todobar.mobile.action.CLOSE"

        fun start(context: Context) {
            val intent = Intent(context, BubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BubbleService::class.java))
        }
    }
}
