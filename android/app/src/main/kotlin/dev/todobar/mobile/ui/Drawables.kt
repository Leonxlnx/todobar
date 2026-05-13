package dev.todobar.mobile.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import dev.todobar.mobile.theme.Palette

/**
 * Drawable factories the views share. Everything is rendered at runtime so
 * we can swap palettes (12 desktop presets) without bundling separate XMLs.
 */
object Drawables {

    fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics,
    ).toInt()

    fun dp(view: View, value: Float): Int = dp(view.context, value)

    fun sp(context: Context, value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, context.resources.displayMetrics,
    )

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

    /** Panel surface for the right-anchored sidebar drawer. */
    fun panelSurface(context: Context, palette: Palette, radius: Int): GradientDrawable =
        roundedSurface(
            context = context,
            color = palette.sidebarBg,
            radius = radius.toFloat(),
            strokeColor = palette.sidebarBorder,
        )

    /** Workspace backdrop gradient drawn behind the panel. */
    fun workspaceBackdrop(palette: Palette): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(palette.workspaceTop, palette.workspaceBottom),
    )

    /** Capture row background — control surface look. */
    fun captureRow(context: Context, palette: Palette): GradientDrawable =
        roundedSurface(
            context = context,
            color = palette.controlBg,
            radius = 12f,
            strokeColor = palette.taskBorder,
        )

    /** Standard task row background. */
    fun taskRow(context: Context, palette: Palette, completed: Boolean = false): GradientDrawable =
        roundedSurface(
            context = context,
            color = if (completed) palette.surfaceHover else palette.taskBg,
            radius = 10f,
            strokeColor = palette.taskBorder,
        )

    /** Pill-shaped button surface used by chips and rail icons. */
    fun pill(context: Context, color: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, 999f).toFloat()
        setColor(color)
        if (stroke != null) setStroke(dp(context, 1f), stroke)
    }

    /** Filled square with rounded corners — used by icon buttons. */
    fun iconChip(context: Context, color: Int, stroke: Int? = null, radius: Float = 10f) =
        roundedSurface(context, color, radius, stroke)

    fun primaryButton(context: Context, palette: Palette): GradientDrawable =
        roundedSurface(context, palette.accent, radius = 12f)

    fun secondaryButton(context: Context, palette: Palette): GradientDrawable =
        roundedSurface(context, palette.controlBg, radius = 12f, strokeColor = palette.taskBorder)

    /** Wrap a regular drawable in a ripple effect using the accent soft tint. */
    fun ripple(content: GradientDrawable, palette: Palette): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(palette.accentSoft.or(0xFF000000.toInt())), content, null)

    fun progressTrack(context: Context, palette: Palette): LayerDrawable {
        val track = roundedSurface(context, palette.controlBg, radius = 999f)
        val fill = roundedSurface(context, palette.accent, radius = 999f)
        return LayerDrawable(arrayOf(track, fill))
    }

    fun dotDrawable(context: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(dp(context, 8f), dp(context, 8f))
    }
}
