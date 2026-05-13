package dev.todobar.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.view.View
import dev.todobar.mobile.R
import dev.todobar.mobile.model.DockEdge
import dev.todobar.mobile.model.SidebarSettings
import kotlin.math.min

/**
 * Native Android version of the desktop edge handle.
 *
 * XML rounded rectangles look cheap here because the desktop handle is really
 * a small docked surface: flat on the screen edge, rounded on the exposed side,
 * with a controlled stroke and an icon that stays centered through resizing.
 */
class EdgeHandleView(context: Context) : View(context) {

    private var edge: DockEdge = DockEdge.RIGHT
    private val path = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.1f)
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.6f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        isClickable = true
        isFocusable = false
        contentDescription = context.getString(R.string.bubble_card_title)
    }

    fun applySettings(settings: SidebarSettings) {
        if (edge != settings.dockEdge) {
            edge = settings.dockEdge
            invalidate()
        } else {
            invalidate()
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        rebuildPath(w, h)

        val shadowColor = if (isDark()) 0x7A000000 else 0x2A0F172A
        fillPaint.style = Paint.Style.FILL
        fillPaint.shader = null
        fillPaint.color = Color.TRANSPARENT
        fillPaint.setShadowLayer(dp(14f), shadowDx(), dp(4f), shadowColor)
        canvas.drawPath(path, fillPaint)

        fillPaint.clearShadowLayer()
        fillPaint.shader = LinearGradient(
            0f,
            0f,
            if (edge == DockEdge.TOP) w else 0f,
            if (edge == DockEdge.TOP) 0f else h,
            getColorCompat(R.color.handle_grad_top),
            getColorCompat(R.color.handle_grad_bot),
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(path, fillPaint)

        strokePaint.color = getColorCompat(R.color.handle_stroke)
        strokePaint.alpha = if (isDark()) 190 else 230
        canvas.drawPath(path, strokePaint)

        drawHandleIcon(canvas, w, h)
    }

    private fun rebuildPath(w: Float, h: Float) {
        path.reset()
        val r = min(min(w * 0.5f, h * 0.24f), dp(18f))
        val edgeBleed = dp(2f)
        when (edge) {
            DockEdge.RIGHT -> {
                path.moveTo(w + edgeBleed, 0f)
                path.lineTo(r, 0f)
                path.cubicTo(r * 0.45f, 0f, 0f, r * 0.45f, 0f, r)
                path.lineTo(0f, h - r)
                path.cubicTo(0f, h - r * 0.45f, r * 0.45f, h, r, h)
                path.lineTo(w + edgeBleed, h)
                path.close()
            }
            DockEdge.LEFT -> {
                path.moveTo(-edgeBleed, 0f)
                path.lineTo(w - r, 0f)
                path.cubicTo(w - r * 0.45f, 0f, w, r * 0.45f, w, r)
                path.lineTo(w, h - r)
                path.cubicTo(w, h - r * 0.45f, w - r * 0.45f, h, w - r, h)
                path.lineTo(-edgeBleed, h)
                path.close()
            }
            DockEdge.TOP -> {
                path.moveTo(0f, -edgeBleed)
                path.lineTo(w, -edgeBleed)
                path.lineTo(w, h - r)
                path.cubicTo(w, h - r * 0.45f, w - r * 0.45f, h, w - r, h)
                path.lineTo(r, h)
                path.cubicTo(r * 0.45f, h, 0f, h - r * 0.45f, 0f, h - r)
                path.close()
            }
        }
    }

    private fun drawHandleIcon(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val boxW = dp(10f)
        val boxH = dp(15f)
        val rect = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)

        iconPaint.color = if (isDark()) 0xFF97A6B8.toInt() else 0xFF647184.toInt()
        iconPaint.alpha = 210

        canvas.save()
        when (edge) {
            DockEdge.LEFT -> canvas.rotate(180f, cx, cy)
            DockEdge.TOP -> canvas.rotate(90f, cx, cy)
            DockEdge.RIGHT -> Unit
        }
        canvas.drawRoundRect(rect, dp(2.2f), dp(2.2f), iconPaint)
        canvas.drawLine(rect.left + dp(3.1f), rect.top + dp(3.1f), rect.left + dp(3.1f), rect.bottom - dp(3.1f), iconPaint)
        canvas.drawLine(cx + dp(0.7f), cy - dp(4f), cx + dp(4f), cy, iconPaint)
        canvas.drawLine(cx + dp(0.7f), cy + dp(4f), cx + dp(4f), cy, iconPaint)
        canvas.restore()
    }

    private fun shadowDx(): Float = when (edge) {
        DockEdge.LEFT -> dp(5f)
        DockEdge.TOP -> 0f
        DockEdge.RIGHT -> -dp(5f)
    }

    private fun isDark(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun getColorCompat(resId: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resources.getColor(resId, context.theme)
        } else {
            @Suppress("DEPRECATION")
            resources.getColor(resId)
        }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density
}
