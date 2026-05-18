package dev.todobar.mobile.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import dev.todobar.mobile.R
import dev.todobar.mobile.model.DockEdge
import dev.todobar.mobile.model.SidebarSettings
import kotlin.math.min

/**
 * Native Android version of the desktop edge handle. Renders a small docked
 * surface, flat against the dock edge and rounded on the exposed side. The
 * drawing path mirrors the desktop CSS so the visual feel matches across the
 * whole `--handle-bg` gradient set.
 *
 * Skeuomorphism layers (in stacking order):
 *  1. Hardware soft shadow via `elevation` + `ViewOutlineProvider`
 *  2. Faint outer drop shadow drawn by the canvas paint shadow layer
 *  3. Body gradient (theme-driven `--handle-bg` 2-stop)
 *  4. 1dp inner border using `--handle-stroke`
 *  5. ~1dp inset highlight drawn as a top-edge line (the desktop's
 *     `inset 0 1px 0 rgba(255,255,255,0.72)` shadow)
 *  6. Centered "panel-right" icon (chevron pulling out of a rail)
 */
class EdgeHandleView(context: Context) : View(context) {

    private var edge: DockEdge = DockEdge.RIGHT
    private val path = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.1f)
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        strokeCap = Paint.Cap.ROUND
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
        // Hardware soft shadow under the handle — matches the desktop
        // `box-shadow: -8px 16px 28px rgba(15, 23, 42, 0.08)` glow.
        elevation = dp(8f)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val w = view.width
                val h = view.height
                if (w <= 0 || h <= 0) return
                outline.setRoundRect(0, 0, w, h, dp(14f))
            }
        }
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

        // ── Soft drop shadow (in addition to elevation) ────────────────
        val shadowColor = if (isDark()) 0x66000000 else 0x1F0F172A
        fillPaint.style = Paint.Style.FILL
        fillPaint.shader = null
        fillPaint.color = Color.TRANSPARENT
        fillPaint.setShadowLayer(dp(14f), shadowDx(), dp(4f), shadowColor)
        canvas.drawPath(path, fillPaint)
        fillPaint.clearShadowLayer()

        // ── Body gradient ──────────────────────────────────────────────
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

        // ── Inset top highlight (1dp white line just inside the curve) ─
        highlightPaint.color = if (isDark()) 0x1FFFFFFF else 0xB8FFFFFF.toInt()
        highlightPaint.strokeWidth = dp(1f)
        canvas.save()
        canvas.clipPath(path)
        when (edge) {
            DockEdge.RIGHT, DockEdge.LEFT -> canvas.drawLine(
                if (edge == DockEdge.LEFT) dp(2f) else dp(2f),
                dp(1f),
                if (edge == DockEdge.LEFT) w - dp(2f) else w - dp(2f),
                dp(1f),
                highlightPaint,
            )
            DockEdge.TOP -> canvas.drawLine(dp(2f), dp(1f), w - dp(2f), dp(1f), highlightPaint)
        }
        canvas.restore()

        // ── 1dp border ─────────────────────────────────────────────────
        strokePaint.color = getColorCompat(R.color.handle_stroke)
        strokePaint.alpha = if (isDark()) 200 else 235
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

    /**
     * Renders the lucide `PanelRightOpen` glyph: a 13×16 outlined panel with
     * a vertical rail on the left and a chevron pointing to the right. Matches
     * the icon used on the desktop edge handle.
     */
    private fun drawHandleIcon(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val boxW = dp(11f)
        val boxH = dp(15f)
        val rect = RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f)

        iconPaint.color = if (isDark()) 0xFFB6C3D6.toInt() else 0xFF566275.toInt()
        iconPaint.alpha = 230

        canvas.save()
        // Open chevron points toward where the panel will slide in from.
        when (edge) {
            DockEdge.LEFT -> canvas.rotate(180f, cx, cy)
            DockEdge.TOP -> canvas.rotate(90f, cx, cy)
            DockEdge.RIGHT -> Unit
        }
        // Panel outline
        canvas.drawRoundRect(rect, dp(2.4f), dp(2.4f), iconPaint)
        // Rail
        val railX = rect.left + dp(3.4f)
        canvas.drawLine(railX, rect.top + dp(2.5f), railX, rect.bottom - dp(2.5f), iconPaint)
        // Chevron (>)
        val arrowX = cx + dp(0.6f)
        val arrowReach = dp(3.8f)
        val arrowSpread = dp(3.6f)
        canvas.drawLine(arrowX, cy - arrowSpread, arrowX + arrowReach, cy, iconPaint)
        canvas.drawLine(arrowX, cy + arrowSpread, arrowX + arrowReach, cy, iconPaint)
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
