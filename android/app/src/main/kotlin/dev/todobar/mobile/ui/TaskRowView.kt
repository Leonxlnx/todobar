package dev.todobar.mobile.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.R
import dev.todobar.mobile.model.Priority
import dev.todobar.mobile.model.Task
import dev.todobar.mobile.model.TaskKind
import dev.todobar.mobile.store.ReminderClock
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.Drawables.dp

/**
 * Single task row matching the desktop "task-row" markup: a circular check
 * button, a clickable priority dot, the title + meta, an inline edit field
 * (toggled on Edit tap), reminder chip, and the trailing reminder/edit/delete
 * actions.
 */
class TaskRowView(
    context: Context,
    private val palette: Palette,
    private val rowHeightDp: Int,
    private val titleSizeSp: Float,
    private val callbacks: Callbacks,
) : LinearLayout(context) {

    interface Callbacks {
        fun onToggle(taskId: Long)
        fun onCyclePriority(taskId: Long)
        fun onCycleReminder(taskId: Long)
        fun onClearReminder(taskId: Long)
        fun onSnooze(taskId: Long)
        fun onEditSave(taskId: Long, newTitle: String)
        fun onDelete(taskId: Long)
        fun onOpenSource(task: Task) {}
    }

    private val checkBtn = ImageView(context)
    private val priorityDot = View(context)
    private val titleBlock = LinearLayout(context).apply {
        orientation = VERTICAL
    }
    private val titleView = TextView(context)
    private val metaView = TextView(context)
    private val titleEdit: EditText = EditText(context).apply { visibility = GONE }
    private val reminderChip = TextView(context)
    private val sourceBtn = ImageView(context)
    private val editBtn = ImageView(context)
    private val snoozeBtn = ImageView(context)
    private val deleteBtn = ImageView(context)

    private var current: Task? = null
    private var editing = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(context, rowHeightDp.toFloat())
        setPadding(
            dp(context, 12f),
            dp(context, 8f),
            dp(context, 8f),
            dp(context, 8f),
        )
        clipChildren = false
        clipToPadding = false

        // Check button
        val checkSize = dp(context, 24f)
        checkBtn.layoutParams = LayoutParams(checkSize, checkSize)
        checkBtn.setOnClickListener { current?.let { callbacks.onToggle(it.id) } }
        addView(checkBtn)

        // Priority dot
        val dotWrap = FrameLayout(context)
        val dotSize = dp(context, 8f)
        priorityDot.layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
            gravity = Gravity.CENTER
        }
        dotWrap.addView(priorityDot)
        val dotWrapSize = dp(context, 24f)
        val dotWrapParams = LayoutParams(dotWrapSize, dotWrapSize)
        dotWrapParams.setMargins(dp(context, 8f), 0, dp(context, 8f), 0)
        dotWrap.layoutParams = dotWrapParams
        dotWrap.setOnClickListener { current?.let { callbacks.onCyclePriority(it.id) } }
        addView(dotWrap)

        // Title + meta
        val titleParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        titleBlock.layoutParams = titleParams
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSizeSp)
        titleView.typeface = Typeface.create(titleView.typeface, Typeface.BOLD)
        titleView.maxLines = 2
        metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSizeSp - 1.5f)
        metaView.maxLines = 1
        titleEdit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        titleEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSizeSp)
        titleEdit.background = Drawables.captureRow(context, palette)
        titleEdit.setPadding(dp(context, 8f), dp(context, 6f), dp(context, 8f), dp(context, 6f))
        titleEdit.setTextColor(palette.ink)
        titleBlock.addView(titleView)
        titleBlock.addView(metaView)
        titleBlock.addView(titleEdit)
        addView(titleBlock)

        // Reminder chip (visible when reminder set)
        reminderChip.background = Drawables.pill(context, palette.amberSoft, palette.amber)
        reminderChip.setPadding(dp(context, 10f), dp(context, 4f), dp(context, 10f), dp(context, 4f))
        reminderChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
        reminderChip.setTextColor(palette.amber)
        reminderChip.gravity = Gravity.CENTER
        val chipParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        chipParams.setMargins(0, 0, dp(context, 4f), 0)
        reminderChip.layoutParams = chipParams
        reminderChip.setOnClickListener { current?.let { callbacks.onCycleReminder(it.id) } }
        addView(reminderChip)

        // Action buttons
        configureActionIcon(sourceBtn, R.drawable.ic_source)
        sourceBtn.setOnClickListener { current?.let { callbacks.onOpenSource(it) } }
        configureActionIcon(snoozeBtn, R.drawable.ic_snooze)
        snoozeBtn.setOnClickListener { current?.let { callbacks.onSnooze(it.id) } }
        configureActionIcon(editBtn, R.drawable.ic_edit)
        editBtn.setOnClickListener { toggleEdit() }
        configureActionIcon(deleteBtn, R.drawable.ic_trash)
        deleteBtn.setOnClickListener { current?.let { callbacks.onDelete(it.id) } }
        addView(sourceBtn)
        addView(snoozeBtn)
        addView(editBtn)
        addView(deleteBtn)

        // Edit field — commit on focus loss
        titleEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && editing) commitEdit()
        }
        titleEdit.setOnEditorActionListener { _, _, _ ->
            commitEdit()
            true
        }
    }

    private fun configureActionIcon(view: ImageView, resId: Int) {
        view.setImageResource(resId)
        val size = dp(context, 30f)
        val params = LayoutParams(size, size)
        params.setMargins(dp(context, 2f), 0, 0, 0)
        view.layoutParams = params
        view.setPadding(dp(context, 5f))
        view.background = Drawables.roundedSurface(context, palette.controlBg, 8f, palette.taskBorder)
        view.imageTintList = android.content.res.ColorStateList.valueOf(palette.muted)
    }

    fun bind(task: Task) {
        current = task
        titleView.text = task.title
        titleView.setTextColor(if (task.done) palette.muted else palette.ink)
        if (task.done) {
            titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            titleView.paintFlags = titleView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        metaView.text = task.meta
        metaView.setTextColor(palette.muted)
        metaView.visibility = if (task.meta.isBlank()) GONE else VISIBLE

        // Priority dot color
        priorityDot.background = Drawables.dotDrawable(
            context,
            when (task.priority) {
                Priority.FOCUS -> palette.accent
                Priority.LATER -> palette.muted
                Priority.NORMAL -> palette.amber
            },
        )

        // Check button look
        if (task.done) {
            checkBtn.setImageResource(R.drawable.ic_check)
            checkBtn.background = Drawables.roundedSurface(context, palette.accent, 999f)
            checkBtn.imageTintList = android.content.res.ColorStateList.valueOf(palette.sidebarBg)
            checkBtn.setPadding(dp(context, 4f))
        } else {
            checkBtn.setImageDrawable(null)
            checkBtn.background = Drawables.roundedSurface(context, palette.controlBg, 999f, palette.accentLine)
        }

        // Reminder chip
        val label = ReminderClock.displayLabel(task.reminderAt)
        if (label.isNotBlank()) {
            reminderChip.text = label
            reminderChip.visibility = VISIBLE
            snoozeBtn.visibility = VISIBLE
        } else {
            reminderChip.text = "Remind"
            reminderChip.visibility = if (editing) VISIBLE else GONE
            snoozeBtn.visibility = GONE
        }

        // Event tasks pick up an amber title underline like the desktop
        if (task.kind == TaskKind.EVENT) {
            titleView.setTextColor(palette.amber)
        }

        // Source button only visible for Gmail-linked tasks
        sourceBtn.visibility = if (task.source != null) VISIBLE else GONE

        background = Drawables.taskRow(context, palette, task.done)
    }

    private fun toggleEdit() {
        val task = current ?: return
        editing = !editing
        if (editing) {
            titleEdit.setText(task.title)
            titleEdit.visibility = VISIBLE
            titleView.visibility = GONE
            metaView.visibility = GONE
            reminderChip.visibility = VISIBLE
            titleEdit.requestFocus()
            titleEdit.setSelection(titleEdit.text?.length ?: 0)
        } else {
            commitEdit()
        }
    }

    private fun commitEdit() {
        val task = current ?: return
        val text = titleEdit.text?.toString().orEmpty().trim()
        editing = false
        titleEdit.visibility = GONE
        titleView.visibility = VISIBLE
        metaView.visibility = if (task.meta.isBlank()) GONE else VISIBLE
        if (text.isNotEmpty() && text != task.title) {
            callbacks.onEditSave(task.id, text)
        }
        reminderChip.visibility = if (task.reminderAt.isNullOrBlank()) GONE else VISIBLE
    }
}
