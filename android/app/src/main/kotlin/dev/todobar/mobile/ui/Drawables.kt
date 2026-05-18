package dev.todobar.mobile.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import dev.todobar.mobile.model.DockEdge
import dev.todobar.mobile.model.ThemeMode
import dev.todobar.mobile.theme.Palette

/**
 * Drawable factories the views share. The shapes mirror the desktop's CSS
 * skeuomorphism: each surface gets a gradient body, an inner top highlight
 * (the white inset shadow), and a thin border. Where shadow softness matters
 * we rely on `View.elevation` because Android can't render true `box-shadow`
 * from drawables.
 */
object Drawables {

    fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics,
    ).toInt()

    fun dp(view: View, value: Float): Int = dp(view.context, value)

    fun sp(context: Context, value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics,
    )

    // ─── Generic rounded rectangle ─────────────────────────────────────────

    /** Solid rounded rectangle with an optional stroke. */
    fun roundedSurface(
        context: Context,
        color: Int,
        radius: Float = 14f,
        strokeColor: Int? = null,
        strokeWidth: Float = 1f,
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, radius).toFloat()
        setColor(color)
        if (strokeColor != null && Color.alpha(strokeColor) > 0) {
            setStroke(dp(context, strokeWidth), strokeColor)
        }
    }

    // ─── Panel surface (asymmetric corners, gradient + inner highlight) ────

    /**
     * Desktop sidebar surface — only the inner corners are rounded
     * (top-left + bottom-left when docked right, mirrored when docked left).
     * The fill is a vertical gradient over the palette's `sidebarBg`
     * with a faint top highlight to mimic the desktop's inset shadow.
     */
    fun panelSurface(
        context: Context,
        palette: Palette,
        themeMode: ThemeMode,
        dockEdge: DockEdge,
        radiusDp: Int,
    ): Drawable {
        val r = dp(context, radiusDp.toFloat()).toFloat()
        val radii = when (dockEdge) {
            DockEdge.RIGHT -> floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
            DockEdge.LEFT -> floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
            DockEdge.TOP -> floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
        }
        val isLight = themeMode == ThemeMode.LIGHT
        val body = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (isLight) {
                intArrayOf(
                    tint(palette.sidebarBg, 1.012f),
                    palette.sidebarBg,
                )
            } else {
                intArrayOf(
                    tint(palette.sidebarBg, 1.06f),
                    palette.sidebarBg,
                )
            },
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setStroke(dp(context, 1f), palette.sidebarBorder)
        }
        // Top highlight band — like `inset 0 1px 0 rgba(255,255,255,0.x)`.
        val highlight = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                if (isLight) 0x2FFFFFFF else 0x14FFFFFF,
                Color.TRANSPARENT,
            ),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
        }
        val layers = LayerDrawable(arrayOf(body, highlight))
        // Highlight band is only the top ~40dp
        layers.setLayerInset(1, 0, 0, 0, 0)
        return layers
    }

    /** Workspace backdrop gradient drawn behind the panel. */
    fun workspaceBackdrop(palette: Palette): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(palette.workspaceTop, palette.workspaceBottom),
    )

    // ─── Skeuomorphic chrome buttons (close, broom, edit, priority, …) ────

    /**
     * Pill / chip button surface — matches the desktop "icon-cluster button"
     * look: subtle gradient body, 1dp border, inset top highlight via a
     * LayerDrawable.
     */
    fun chromeButton(
        context: Context,
        palette: Palette,
        themeMode: ThemeMode,
        radiusDp: Float = 10f,
        pressed: Boolean = false,
        active: Boolean = false,
    ): Drawable {
        val isLight = themeMode == ThemeMode.LIGHT
        val (top, bot) = when {
            pressed && isLight -> tint(palette.controlBg, 0.96f) to palette.controlBg
            pressed -> tint(palette.controlBg, 1.04f) to palette.controlBg
            active && isLight -> palette.accentSoft.or(0xFF000000.toInt()) to palette.accentSoft
                .or(0xFF000000.toInt())
            active -> palette.accentSoft.or(0xFF000000.toInt()) to palette.accentSoft
                .or(0xFF000000.toInt())
            isLight -> 0xFFFFFFFF.toInt() to palette.controlBg
            else -> tint(palette.controlBg, 1.10f) to palette.controlBg
        }
        val body = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(top, bot),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
            setStroke(
                dp(context, 1f),
                if (active) palette.accentLine.or(0xFF000000.toInt()) else palette.taskBorder,
            )
        }
        val highlight = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                if (isLight) 0x52FFFFFF else 0x1FFFFFFF,
                Color.TRANSPARENT,
            ),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, radiusDp).toFloat()
        }
        return LayerDrawable(arrayOf(body, highlight))
    }

    /**
     * Rail icon button — same construction as the chrome button but slightly
     * stronger gradient and 11dp radius to match `.sidebar-rail button`.
     */
    fun railButton(
        context: Context,
        palette: Palette,
        themeMode: ThemeMode,
        active: Boolean,
    ): Drawable = chromeButton(
        context = context,
        palette = palette,
        themeMode = themeMode,
        radiusDp = 11f,
        active = active,
    )

    /** Capture row background — control surface look with top highlight. */
    fun captureRow(
        context: Context,
        palette: Palette,
        themeMode: ThemeMode = inferTheme(palette),
    ): Drawable = chromeButton(
        context = context,
        palette = palette,
        themeMode = themeMode,
        radiusDp = 12f,
    )

    /** Standard task row background. */
    fun taskRow(
        context: Context,
        palette: Palette,
        completed: Boolean = false,
        themeMode: ThemeMode = inferTheme(palette),
    ): Drawable {
        val isLight = themeMode == ThemeMode.LIGHT
        val baseColor = if (completed) palette.surfaceHover else palette.taskBg
        val top = if (isLight) tint(baseColor, 1.02f) else tint(baseColor, 1.06f)
        val body = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(top, baseColor),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 10f).toFloat()
            setStroke(dp(context, 1f), palette.taskBorder)
        }
        val highlight = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                if (isLight) 0x36FFFFFF else 0x14FFFFFF,
                Color.TRANSPARENT,
            ),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 10f).toFloat()
        }
        return LayerDrawable(arrayOf(body, highlight))
    }

    /** Pill-shaped button surface used by chips and tags. */
    fun pill(context: Context, color: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, 999f).toFloat()
        setColor(color)
        if (stroke != null) setStroke(dp(context, 1f), stroke)
    }

    /** Filled square with rounded corners — used by icon buttons. */
    fun iconChip(context: Context, color: Int, stroke: Int? = null, radius: Float = 10f) =
        roundedSurface(context, color, radius, stroke)

    fun primaryButton(context: Context, palette: Palette): Drawable {
        val body = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(tint(palette.accent, 1.10f), palette.accent),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 12f).toFloat()
        }
        val highlight = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x40FFFFFF, Color.TRANSPARENT),
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 12f).toFloat()
        }
        return LayerDrawable(arrayOf(body, highlight))
    }

    fun secondaryButton(
        context: Context,
        palette: Palette,
        themeMode: ThemeMode = inferTheme(palette),
    ): Drawable = chromeButton(context, palette, themeMode, radiusDp = 12f)

    /** Wrap a regular drawable in a ripple effect using the accent soft tint. */
    fun ripple(content: Drawable, palette: Palette): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(palette.accentSoft.or(0xFF000000.toInt())), content, null)

    fun progressTrack(context: Context, palette: Palette): LayerDrawable {
        val track = roundedSurface(context, palette.controlBg, radius = 999f, strokeColor = palette.taskBorder)
        val fill = roundedSurface(context, palette.accent, radius = 999f)
        return LayerDrawable(arrayOf(track, fill))
    }

    fun dotDrawable(context: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(dp(context, 8f), dp(context, 8f))
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /**
     * Guess whether the supplied palette is a light theme based on the
     * sidebar background luminance. Used so callers don't have to thread
     * a `ThemeMode` parameter everywhere.
     */
    fun inferTheme(palette: Palette): ThemeMode {
        val c = palette.sidebarBg or 0xFF000000.toInt()
        val r = Color.red(c)
        val g = Color.green(c)
        val b = Color.blue(c)
        val lum = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return if (lum > 170f) ThemeMode.LIGHT else ThemeMode.DARK
    }

    /**
     * Linearly tints an ARGB color toward white (factor > 1) or black (< 1).
     * Used to fabricate subtle 2-stop gradients from a single palette value.
     */
    private fun tint(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(
            a,
            (r * factor).toInt().coerceIn(0, 255),
            (g * factor).toInt().coerceIn(0, 255),
            (b * factor).toInt().coerceIn(0, 255),
        )
    }
}
