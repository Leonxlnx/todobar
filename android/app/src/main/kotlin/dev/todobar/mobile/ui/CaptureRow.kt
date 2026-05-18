package dev.todobar.mobile.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.R
import dev.todobar.mobile.store.ReminderClock
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.Drawables.dp
import java.util.Calendar

/**
 * Top "Quick add" row — title input, optional reminder picker, and a +
 * button. Mirrors the desktop `.quick-add` widget.
 */
class CaptureRow(
    context: Context,
    private val palette: Palette,
    private val onSubmit: (title: String, reminderAtMs: Long?) -> Unit,
) : LinearLayout(context) {

    private val input: EditText = Views.input(context, "Capture a task…", palette, textSizeSp = 13.5f)
    private val reminderChip = TextView(context)
    private val addBtn = ImageView(context)
    private var reminderDraftMs: Long? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = Drawables.captureRow(context, palette)
        val pad = dp(context, 10f)
        setPadding(pad, dp(context, 6f), dp(context, 6f), dp(context, 6f))

        input.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        addView(input)

        reminderChip.text = "Remind"
        reminderChip.background = Drawables.pill(context, palette.controlBg, palette.taskBorder)
        reminderChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        reminderChip.setTextColor(palette.muted)
        reminderChip.setPadding(dp(context, 10f), dp(context, 6f), dp(context, 10f), dp(context, 6f))
        val chipParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        chipParams.setMargins(dp(context, 6f), 0, dp(context, 6f), 0)
        reminderChip.layoutParams = chipParams
        reminderChip.setOnClickListener { pickReminder() }
        addView(reminderChip)

        addBtn.setImageResource(R.drawable.ic_plus)
        addBtn.imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
        addBtn.background = Drawables.primaryButton(context, palette)
        // Tiny lift so the accent +button reads as the primary action.
        addBtn.elevation = dp(context, 1.5f).toFloat()
        val btnSize = dp(context, 34f)
        val btnParams = LayoutParams(btnSize, btnSize)
        addBtn.layoutParams = btnParams
        addBtn.setPadding(dp(context, 6f))
        addBtn.setOnClickListener { submit() }
        addView(addBtn)

        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }
    }

    fun setHint(hint: String) {
        input.hint = hint
    }

    fun clearReminderDraft() {
        reminderDraftMs = null
        renderReminderChip()
    }

    private fun renderReminderChip() {
        val ms = reminderDraftMs
        if (ms == null) {
            reminderChip.text = "Remind"
            reminderChip.setTextColor(palette.muted)
            reminderChip.background = Drawables.pill(context, palette.controlBg, palette.taskBorder)
        } else {
            reminderChip.text = ReminderClock.displayLabel(ReminderClock.format(ms))
            reminderChip.setTextColor(palette.amber)
            reminderChip.background = Drawables.pill(context, palette.amberSoft, palette.amber)
        }
    }

    private fun pickReminder() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        reminderDraftMs = cal.timeInMillis
                        renderReminderChip()
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true,
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun submit() {
        val text = input.text?.toString().orEmpty().trim()
        if (text.isEmpty()) return
        val r = reminderDraftMs
        onSubmit(text, r)
        input.setText("")
        reminderDraftMs = null
        renderReminderChip()
    }
}
