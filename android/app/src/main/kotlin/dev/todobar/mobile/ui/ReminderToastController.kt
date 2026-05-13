package dev.todobar.mobile.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.R
import dev.todobar.mobile.model.Task
import dev.todobar.mobile.store.ReminderClock
import dev.todobar.mobile.store.Store
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.Drawables.dp

/**
 * In-app reminder toast that floats over the bubble, matching the desktop
 * `.reminder-toast` element. Two actions: "Snooze 10m" and "Dismiss".
 *
 * The desktop's "Open" link to source tasks isn't supported on Android yet, so
 * for Gmail-sourced tasks we hide the chip and fall back to title text.
 */
class ReminderToastController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val overlayType: Int,
    private val task: Task,
    private val onSnooze: () -> Unit,
    private val onDismiss: () -> Unit,
) {

    private var rootView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val palette = Palette.resolve(Store.get(context).settings())

    fun show() {
        val ctx = context
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = Drawables.roundedSurface(
                ctx, palette.sidebarBg, 14f, palette.accentLine,
            )
            elevation = dp(ctx, 8f).toFloat()
            setPadding(dp(ctx, 14f), dp(ctx, 12f), dp(ctx, 14f), dp(ctx, 12f))
        }

        val header = TextView(ctx).apply {
            text = "Reminder"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(palette.muted)
            isAllCaps = true
            letterSpacing = 0.08f
        }
        container.addView(header)

        val title = TextView(ctx).apply {
            text = task.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(palette.ink)
            setPadding(0, dp(ctx, 4f), 0, dp(ctx, 4f))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        container.addView(title)

        val meta = TextView(ctx).apply {
            text = task.meta.ifBlank { ReminderClock.displayLabel(task.reminderAt) }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(palette.muted)
        }
        container.addView(meta)

        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(ctx, 10f), 0, 0)
        }
        val snoozeBtn = Views.chip(ctx, "Snooze 10m", palette) {
            dismiss(); onSnooze()
        }
        actions.addView(snoozeBtn)
        val spacer = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 8f), 1)
        }
        actions.addView(spacer)
        val dismissBtn = Views.chip(ctx, "Dismiss", palette, active = true) { dismiss() }
        actions.addView(dismissBtn)
        container.addView(actions)

        val params = WindowManager.LayoutParams(
            dp(ctx, 280f),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(ctx, 16f).toInt()
            y = dp(ctx, 96f).toInt()
        }

        rootView = container
        container.alpha = 0f
        container.translationY = dp(ctx, 24f).toFloat()
        runCatching { windowManager.addView(container, params) }.onFailure {
            onDismiss(); return
        }
        container.animate().alpha(1f).translationY(0f).setDuration(220).start()

        // Auto-dismiss after 9 seconds (desktop default)
        handler.postDelayed({ dismiss() }, 9_000)
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        val view = rootView ?: return
        rootView = null
        view.animate()
            .alpha(0f)
            .translationY(dp(context, 24f).toFloat())
            .setDuration(160)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                runCatching { windowManager.removeView(view) }
                onDismiss()
            }
            .start()
    }
}
