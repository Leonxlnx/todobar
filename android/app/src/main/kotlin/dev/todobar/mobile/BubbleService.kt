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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.NotificationCompat
import dev.todobar.mobile.model.DockEdge
import dev.todobar.mobile.store.Store
import dev.todobar.mobile.store.TaskScope
import dev.todobar.mobile.ui.EdgeHandleView
import dev.todobar.mobile.ui.ReminderToastController
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
    private var reminderToast: ReminderToastController? = null
    private val store by lazy { Store.get(this) }

    private val storeListener: () -> Unit = {
        bubbleView?.post { applyHandleSettings() }
        bubbleView?.post { checkRemindersDue() }
    }

    private val edgeOverlapPx by lazy { dp(1f).toInt() }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startInForeground()
        store.addListener(storeListener)
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
            val settings = store.settings()
            params.gravity = handleGravity()
            params.x = handleX(settings)
            params.y = handleY(settings)
            bubbleView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
        }
        sidebar?.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        store.removeListener(storeListener)
        removeBubble()
        sidebar?.dismiss()
        sidebar = null
        reminderToast?.dismiss()
        reminderToast = null
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
        if (bubbleView != null) {
            applyHandleSettings()
            return
        }

        val settings = store.settings()
        val view = EdgeHandleView(this)
        val params = WindowManager.LayoutParams(
            handleWidthPx(settings),
            handleHeightPx(settings),
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = handleGravity()
            x = handleX(settings)
            y = handleY(settings)
        }

        attachDragAndTap(view, params)
        view.alpha = 1f
        view.visibility = View.VISIBLE
        view.applySettings(settings)
        val added = runCatching { windowManager.addView(view, params) }
        if (added.isFailure) {
            Log.e(TAG, "bubble add failed", added.exceptionOrNull())
            stopSelf()
            return
        }
        bubbleView = view
        bubbleParams = params
        applyHandleSettings()
        checkRemindersDue()
    }

    private fun handleGravity(): Int = Gravity.TOP or Gravity.START

    private fun handleWidthPx(settings: dev.todobar.mobile.model.SidebarSettings): Int =
        if (settings.dockEdge == DockEdge.TOP) {
            dp(settings.handleHeight.coerceIn(56, 176).toFloat()).toInt()
        } else {
            dp(settings.tabWidth.coerceIn(22, 112).toFloat()).toInt()
        }.coerceAtLeast(dp(18f).toInt())

    private fun handleHeightPx(settings: dev.todobar.mobile.model.SidebarSettings): Int =
        if (settings.dockEdge == DockEdge.TOP) {
            dp(settings.tabWidth.coerceIn(22, 112).toFloat()).toInt()
        } else {
            dp(settings.handleHeight.coerceIn(56, 176).toFloat()).toInt()
        }.coerceAtLeast(dp(18f).toInt())

    private fun positionForHandle(settings: dev.todobar.mobile.model.SidebarSettings): Int {
        val metrics = resources.displayMetrics
        val handleLength = if (settings.dockEdge == DockEdge.TOP) {
            handleWidthPx(settings)
        } else {
            handleHeightPx(settings)
        }
        val screenLength = if (settings.dockEdge == DockEdge.TOP) {
            metrics.widthPixels
        } else {
            metrics.heightPixels
        }
        val available = screenLength - handleLength - dp(48f).toInt()
        val ratio = settings.handleY.coerceIn(0, 100) / 100f
        return (available * ratio).toInt() + dp(24f).toInt()
    }

    private fun applyHandleSettings() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        val settings = store.settings()
        params.width = handleWidthPx(settings)
        params.height = handleHeightPx(settings)
        params.gravity = handleGravity()
        params.x = handleX(settings)
        params.y = handleY(settings)
        (view as? EdgeHandleView)?.applySettings(settings)
        if (sidebar == null) {
            view.animate().cancel()
            view.alpha = 1f
            view.visibility = View.VISIBLE
        }
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun handleX(settings: dev.todobar.mobile.model.SidebarSettings): Int {
        val metrics = resources.displayMetrics
        return when (settings.dockEdge) {
            DockEdge.LEFT -> -edgeOverlapPx
            DockEdge.TOP -> positionForHandle(settings)
            DockEdge.RIGHT -> metrics.widthPixels - handleWidthPx(settings) + edgeOverlapPx
        }
    }

    private fun handleY(settings: dev.todobar.mobile.model.SidebarSettings): Int =
        when (settings.dockEdge) {
            DockEdge.TOP -> -edgeOverlapPx
            else -> positionForHandle(settings)
        }

    private fun checkRemindersDue() {
        if (!store.settings().notificationsEnabled) return
        val now = System.currentTimeMillis()
        val notified = store.notifiedReminders().toMutableSet()
        val candidates = buildList {
            store.today().forEach { add(TaskScope.Today to it) }
            store.scheduled().forEach { add(TaskScope.Scheduled to it) }
            store.customLists().forEach { list ->
                list.tasks.forEach { add(TaskScope.CustomList(list.id) to it) }
            }
        }
        candidates.forEach { (scope, task) ->
            val r = task.reminderAt ?: return@forEach
            val ms = runCatching { dev.todobar.mobile.store.ReminderClock.parse(r) }.getOrNull() ?: return@forEach
            val key = task.id.toString() + "@" + r
            if (ms <= now && key !in notified && !task.done) {
                showReminderToast(task, scope)
                store.markReminderNotified(key)
                notified.add(key)
            }
        }
    }

    private fun showReminderToast(
        task: dev.todobar.mobile.model.Task,
        scope: TaskScope,
    ) {
        reminderToast?.dismiss()
        reminderToast = ReminderToastController(
            context = this,
            windowManager = windowManager,
            overlayType = overlayType(),
            task = task,
            onSnooze = {
                store.snoozeReminder(scope, task.id, 10)
                reminderToast = null
            },
            onDismiss = {
                reminderToast = null
            },
        ).also { it.show() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragAndTap(view: View, params: WindowManager.LayoutParams) {
        var initialY = 0
        var initialX = 0
        var touchStartY = 0f
        var touchStartX = 0f
        var touchStartTime = 0L
        var dragging = false

        view.setOnTouchListener { v, event ->
            val isTopDock = store.settings().dockEdge == DockEdge.TOP
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = params.y
                    initialX = params.x
                    touchStartY = event.rawY
                    touchStartX = event.rawX
                    touchStartTime = System.currentTimeMillis()
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val delta = if (isTopDock) {
                        event.rawX - touchStartX
                    } else {
                        event.rawY - touchStartY
                    }
                    if (!dragging && abs(delta) > dp(6f)) dragging = true
                    if (dragging) {
                        if (isTopDock) {
                            params.x = clampHorizontal((initialX + delta).toInt())
                        } else {
                            params.y = clampVertical((initialY + delta).toInt())
                        }
                        runCatching { windowManager.updateViewLayout(v, params) }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - touchStartTime
                    if (!dragging && duration < 280) {
                        v.performClick()
                        openSidebar()
                    }
                    if (dragging) {
                        if (isTopDock) {
                            persistHandlePosition(params.x, horizontal = true)
                        } else {
                            persistHandlePosition(params.y, horizontal = false)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        if (isTopDock) {
                            persistHandlePosition(params.x, horizontal = true)
                        } else {
                            persistHandlePosition(params.y, horizontal = false)
                        }
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
        val controller = SidebarOverlayController(
            serviceContext = this,
            windowManager = windowManager,
            overlayType = overlayType(),
            onDismiss = ::onSidebarDismissed,
            onRequestBackdropPicker = {
                val pickerIntent = Intent(this, BackdropPickerActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(pickerIntent)
            },
        )
        sidebar = controller
        if (!controller.show()) {
            sidebar = null
            bubbleView?.let {
                it.animate().cancel()
                it.alpha = 1f
                it.visibility = View.VISIBLE
            }
            return
        }
        bubbleView?.let {
            it.animate().cancel()
            it.animate().alpha(0f).setDuration(120).start()
        }
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

    private fun clampVertical(value: Int): Int {
        val metrics: DisplayMetrics = resources.displayMetrics
        val h = handleHeightPx(store.settings())
        val maxY = metrics.heightPixels - h - dp(24f).toInt()
        val minY = dp(24f).toInt()
        return value.coerceIn(minY, maxY)
    }

    private fun clampHorizontal(value: Int): Int {
        val metrics = resources.displayMetrics
        val w = handleWidthPx(store.settings())
        val maxX = metrics.widthPixels - w - dp(24f).toInt()
        val minX = dp(24f).toInt()
        return value.coerceIn(minX, maxX)
    }

    private fun persistHandlePosition(value: Int, horizontal: Boolean) {
        val metrics = resources.displayMetrics
        val handleLength = if (horizontal) {
            handleWidthPx(store.settings())
        } else {
            handleHeightPx(store.settings())
        }
        val screenLength = if (horizontal) metrics.widthPixels else metrics.heightPixels
        val available = screenLength - handleLength - dp(48f).toInt()
        if (available <= 0) return
        val ratio = ((value - dp(24f).toInt()).toFloat() / available).coerceIn(0f, 1f)
        val percent = (ratio * 100).toInt()
        if (percent != store.settings().handleY) {
            store.saveSettings(store.settings().copy(handleY = percent))
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    companion object {
        private const val TAG = "BubbleService"
        private const val NOTIFICATION_ID = 4711
        @Volatile
        var isRunning: Boolean = false
            private set
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
