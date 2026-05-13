package dev.todobar.mobile.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.Drawables.dp

/** Tiny helpers for building the UI programmatically. */
object Views {

    fun row(context: Context, gravity: Int = Gravity.CENTER_VERTICAL, block: LinearLayout.() -> Unit = {}): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            this.gravity = gravity
            block()
        }

    fun column(context: Context, block: LinearLayout.() -> Unit = {}): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            block()
        }

    fun text(
        context: Context,
        text: String,
        sizeSp: Float = 13f,
        color: Int = Color.WHITE,
        bold: Boolean = false,
    ): TextView = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        if (bold) typeface = Typeface.create(typeface, Typeface.BOLD)
    }

    fun iconButton(
        context: Context,
        icon: Icon,
        tint: Int,
        sizeDp: Int = 32,
        onClick: () -> Unit,
    ): ImageButton {
        val btn = ImageButton(context)
        btn.setImageDrawable(icon.load(context))
        btn.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
        btn.background = null
        btn.scaleType = ImageView.ScaleType.FIT_CENTER
        val size = dp(context, sizeDp.toFloat())
        btn.layoutParams = ViewGroup.LayoutParams(size, size)
        val pad = dp(context, 6f)
        btn.setPadding(pad)
        btn.setOnClickListener { onClick() }
        return btn
    }

    fun chip(
        context: Context,
        label: String,
        palette: Palette,
        active: Boolean = false,
        onClick: () -> Unit = {},
    ): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        tv.setTextColor(if (active) palette.accent else palette.muted)
        tv.background = Drawables.pill(
            context,
            if (active) palette.accentSoft else palette.controlBg,
            if (active) palette.accentLine else palette.taskBorder,
        )
        tv.setPadding(dp(context, 12f), dp(context, 6f), dp(context, 12f), dp(context, 6f))
        tv.setOnClickListener { onClick() }
        return tv
    }

    fun input(context: Context, hint: String, palette: Palette, textSizeSp: Float = 13.5f): EditText {
        val edit = EditText(context)
        edit.hint = hint
        edit.setTextColor(palette.ink)
        edit.setHintTextColor(palette.muted)
        edit.background = null
        edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        edit.maxLines = 1
        edit.isSingleLine = true
        edit.ellipsize = TextUtils.TruncateAt.END
        return edit
    }

    fun divider(context: Context, color: Int): View {
        val v = View(context)
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1f))
        v.layoutParams = params
        v.setBackgroundColor(color)
        return v
    }

    fun spacer(context: Context, heightDp: Int = 8, widthDp: Int = 0): View {
        val v = View(context)
        v.layoutParams = LinearLayout.LayoutParams(
            if (widthDp == 0) LinearLayout.LayoutParams.MATCH_PARENT else dp(context, widthDp.toFloat()),
            if (heightDp == 0) 0 else dp(context, heightDp.toFloat()),
        )
        return v
    }

    fun slider(
        context: Context,
        min: Int,
        max: Int,
        value: Int,
        palette: Palette,
        onChange: (Int) -> Unit,
    ): SeekBar = SeekBar(context).apply {
        this.max = max - min
        this.progress = value - min
        progressTintList = android.content.res.ColorStateList.valueOf(palette.accent)
        thumbTintList = android.content.res.ColorStateList.valueOf(palette.accent)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onChange(progress + min)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun toggle(
        context: Context,
        label: String,
        checked: Boolean,
        palette: Palette,
        onChange: (Boolean) -> Unit,
    ): LinearLayout {
        val container = row(context, Gravity.CENTER_VERTICAL).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 10f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f))
        }
        val labelView = text(context, label, sizeSp = 12.5f, color = palette.ink)
        labelView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val pill = FrameLayout(context)
        val pillWidth = dp(context, 38f)
        val pillHeight = dp(context, 22f)
        pill.layoutParams = LinearLayout.LayoutParams(pillWidth, pillHeight)
        pill.background = Drawables.pill(
            context,
            if (checked) palette.accent else palette.controlBg,
            palette.taskBorder,
        )
        val thumb = View(context)
        val thumbSize = dp(context, 16f)
        val thumbParams = FrameLayout.LayoutParams(thumbSize, thumbSize)
        thumbParams.gravity = if (checked) (Gravity.CENTER_VERTICAL or Gravity.END) else (Gravity.CENTER_VERTICAL or Gravity.START)
        val pad = dp(context, 3f)
        thumbParams.setMargins(pad, 0, pad, 0)
        thumb.layoutParams = thumbParams
        thumb.background = Drawables.roundedSurface(context, Color.WHITE, 999f)
        pill.addView(thumb)

        container.addView(labelView)
        container.addView(pill)

        var state = checked
        container.setOnClickListener {
            state = !state
            pill.background = Drawables.pill(
                context,
                if (state) palette.accent else palette.controlBg,
                palette.taskBorder,
            )
            val newParams = thumb.layoutParams as FrameLayout.LayoutParams
            newParams.gravity = if (state) (Gravity.CENTER_VERTICAL or Gravity.END) else (Gravity.CENTER_VERTICAL or Gravity.START)
            thumb.layoutParams = newParams
            onChange(state)
        }

        return container
    }
}
