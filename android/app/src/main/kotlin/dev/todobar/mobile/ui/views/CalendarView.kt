package dev.todobar.mobile.ui.views

import android.content.Context
import android.graphics.Typeface
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.model.Task
import dev.todobar.mobile.model.TaskKind
import dev.todobar.mobile.store.ReminderClock
import dev.todobar.mobile.store.Store
import dev.todobar.mobile.store.TaskScope
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.CaptureRow
import dev.todobar.mobile.ui.Drawables
import dev.todobar.mobile.ui.Drawables.dp
import dev.todobar.mobile.ui.Icon
import dev.todobar.mobile.ui.TaskRowView
import dev.todobar.mobile.ui.Views
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarView(
    context: Context,
    private val store: Store,
    private var palette: Palette,
    private var settings: SidebarSettings,
) : SectionView(context) {

    private val scroll = ScrollView(context)
    private val body = Views.column(context)
    private val monthLabel = TextView(context)
    private val daysHeader = LinearLayout(context)
    private val daysGrid = Views.column(context)
    private val selectedHeader = TextView(context)
    private val captureMode = LinearLayout(context)
    private val captureSlot = LinearLayout(context).apply { orientation = VERTICAL }
    private val taskList = Views.column(context)
    private val emptyState = TextView(context)

    private var cursor = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    private var selectedKey: String = ReminderClock.dayKey(System.currentTimeMillis())
    private var entryMode: TaskKind = TaskKind.TASK

    init {
        orientation = VERTICAL
        scroll.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scroll.isFillViewport = true
        addView(scroll)
        scroll.addView(
            body,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        body.setPadding(dp(context, 14f), dp(context, 10f), dp(context, 14f), dp(context, 32f))

        // Month nav
        val navRow = Views.row(context, Gravity.CENTER_VERTICAL).apply {
            setPadding(dp(context, 4f), dp(context, 0f), dp(context, 4f), dp(context, 6f))
        }
        val prevBtn = Views.iconButton(context, Icon.CHEVRON_LEFT, palette.muted, sizeDp = 32) {
            cursor.add(Calendar.MONTH, -1)
            refresh()
        }
        navRow.addView(prevBtn)
        monthLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
        monthLabel.gravity = Gravity.CENTER
        val labelParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        monthLabel.layoutParams = labelParams
        navRow.addView(monthLabel)
        val nextBtn = Views.iconButton(context, Icon.CHEVRON_RIGHT, palette.muted, sizeDp = 32) {
            cursor.add(Calendar.MONTH, 1)
            refresh()
        }
        navRow.addView(nextBtn)
        body.addView(navRow)

        // Day-of-week header
        daysHeader.orientation = LinearLayout.HORIZONTAL
        body.addView(daysHeader)
        body.addView(daysGrid)
        body.addView(Views.spacer(context, 12))

        // Selected day header + mode tabs
        selectedHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        selectedHeader.typeface = Typeface.create(selectedHeader.typeface, Typeface.BOLD)
        selectedHeader.letterSpacing = -0.005f
        body.addView(selectedHeader)

        captureMode.orientation = LinearLayout.HORIZONTAL
        captureMode.setPadding(0, dp(context, 8f), 0, dp(context, 8f))
        body.addView(captureMode)

        body.addView(captureSlot)
        body.addView(Views.spacer(context, 8))

        body.addView(taskList)
        emptyState.text = "No events scheduled."
        emptyState.gravity = Gravity.CENTER
        emptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        emptyState.setPadding(dp(context, 18f))
        emptyState.setTextColor(palette.muted)
        body.addView(emptyState)
    }

    override fun applyTheme(palette: Palette, settings: SidebarSettings) {
        this.palette = palette
        this.settings = settings
        monthLabel.setTextColor(palette.ink)
        selectedHeader.setTextColor(palette.ink)
        emptyState.setTextColor(palette.muted)
        refresh()
    }

    override fun refresh() {
        renderMonth()
        renderDayHeader()
        renderModeTabs()
        renderCapture()
        renderDayTasks()
    }

    private fun renderMonth() {
        val fmt = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
        monthLabel.text = fmt.format(cursor.time)

        // Day-of-week header (Mon..Sun localized)
        daysHeader.removeAllViews()
        val first = Calendar.getInstance().firstDayOfWeek
        val dayFmt = SimpleDateFormat("EEEEEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        for (i in 0 until 7) {
            cal.set(Calendar.DAY_OF_WEEK, ((first - 1 + i) % 7) + 1)
            val tv = TextView(context).apply {
                text = dayFmt.format(cal.time)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
                setTextColor(palette.muted)
                gravity = Gravity.CENTER
            }
            tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            daysHeader.addView(tv)
        }

        // Day grid (6 rows of 7 days)
        daysGrid.removeAllViews()
        val gridStart = Calendar.getInstance().apply {
            time = cursor.time
            val offset = ((get(Calendar.DAY_OF_WEEK) - first) + 7) % 7
            add(Calendar.DAY_OF_MONTH, -offset)
        }
        val taskCountByDay = mutableMapOf<String, Int>()
        store.scheduled().forEach { t ->
            val key = ReminderClock.dayKey(t.reminderAt ?: return@forEach) ?: return@forEach
            taskCountByDay[key] = (taskCountByDay[key] ?: 0) + 1
        }
        val month = cursor.get(Calendar.MONTH)
        val todayKey = ReminderClock.dayKey(System.currentTimeMillis())
        val tmp = Calendar.getInstance().apply { time = gridStart.time }
        for (week in 0 until 6) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (d in 0 until 7) {
                val key = ReminderClock.dayKey(tmp.timeInMillis)
                val cellDate = tmp.get(Calendar.DAY_OF_MONTH)
                val cellMonth = tmp.get(Calendar.MONTH)
                val isCurrentMonth = cellMonth == month
                val isSelected = key == selectedKey
                val isToday = key == todayKey
                val taskCount = taskCountByDay[key] ?: 0
                val cell = buildDayCell(cellDate, isCurrentMonth, isSelected, isToday, taskCount, key)
                cell.layoutParams = LinearLayout.LayoutParams(0, dp(context, 44f), 1f)
                row.addView(cell)
                tmp.add(Calendar.DAY_OF_MONTH, 1)
            }
            daysGrid.addView(row)
        }
    }

    private fun buildDayCell(
        date: Int,
        inMonth: Boolean,
        selected: Boolean,
        today: Boolean,
        taskCount: Int,
        key: String,
    ): View {
        val container = Views.column(context).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(context, 4f), 0, dp(context, 4f))
        }
        val tv = TextView(context).apply {
            text = date.toString()
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            val ink = if (!inMonth) palette.muted else palette.ink
            setTextColor(if (selected) palette.accent else ink)
            if (today) typeface = android.graphics.Typeface.create(typeface, android.graphics.Typeface.BOLD)
            val size = dp(context, 28f)
            layoutParams = LinearLayout.LayoutParams(size, size)
            if (selected) {
                background = Drawables.roundedSurface(context, palette.accentSoft, 999f, palette.accentLine)
            } else if (today) {
                background = Drawables.roundedSurface(context, palette.controlBg, 999f, palette.taskBorder)
            }
        }
        container.addView(tv)
        if (taskCount > 0) {
            val dot = View(context)
            val dotSize = dp(context, 4f)
            dot.layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                topMargin = dp(context, 2f)
            }
            dot.background = Drawables.dotDrawable(context, palette.accent)
            container.addView(dot)
        }
        container.setOnClickListener {
            selectedKey = key
            refresh()
        }
        return container
    }

    private fun renderDayHeader() {
        val cal = Calendar.getInstance()
        val parsed = runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedKey)
        }.getOrNull()
        if (parsed != null) cal.time = parsed
        val fmt = DateFormat.getLongDateFormat(context)
        selectedHeader.text = fmt.format(cal.time)
    }

    private fun renderModeTabs() {
        captureMode.removeAllViews()
        listOf(TaskKind.TASK to "Task", TaskKind.EVENT to "Event").forEach { (kind, label) ->
            val chip = Views.chip(context, label, palette, active = entryMode == kind) {
                entryMode = kind
                renderModeTabs()
            }
            val params = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, dp(context, 6f), 0)
            chip.layoutParams = params
            captureMode.addView(chip)
        }
    }

    private fun renderCapture() {
        captureSlot.removeAllViews()
        val capture = CaptureRow(context, palette) { title, _ ->
            val cal = Calendar.getInstance()
            val parsed = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedKey)
            }.getOrNull()
            if (parsed != null) cal.time = parsed
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val reminderAt = ReminderClock.format(cal.timeInMillis)
            val meta = SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time) +
                " · " + if (entryMode == TaskKind.EVENT) "Event" else "Calendar"
            store.addScheduled(title, meta, reminderAt, entryMode)
        }
        capture.setHint(if (entryMode == TaskKind.EVENT) "Schedule an event…" else "Schedule a task…")
        captureSlot.addView(capture)
    }

    private fun renderDayTasks() {
        val all = store.scheduled()
        val forDay = all.filter { ReminderClock.dayKey(it.reminderAt ?: return@filter false) == selectedKey }
        val sorted = Store.sortTasks(forDay, settings.taskSortMode)
        val visible = if (settings.showCompleted) sorted else sorted.filterNot { it.done }

        taskList.removeAllViews()
        visible.forEach { task ->
            val row = TaskRowView(
                context, palette, settings.taskRowHeight, settings.taskTextSize,
                object : TaskRowView.Callbacks {
                    override fun onToggle(taskId: Long) = store.toggleTask(TaskScope.Scheduled, taskId)
                    override fun onCyclePriority(taskId: Long) = store.cyclePriority(TaskScope.Scheduled, taskId)
                    override fun onCycleReminder(taskId: Long) = store.cycleReminder(TaskScope.Scheduled, taskId)
                    override fun onClearReminder(taskId: Long) {}
                    override fun onSnooze(taskId: Long) = store.snoozeReminder(TaskScope.Scheduled, taskId, 10)
                    override fun onEditSave(taskId: Long, newTitle: String) = store.editTitle(TaskScope.Scheduled, taskId, newTitle)
                    override fun onDelete(taskId: Long) = store.removeTask(TaskScope.Scheduled, taskId)
                },
            )
            row.bind(task)
            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, dp(context, settings.taskGap.toFloat()))
            row.layoutParams = params
            taskList.addView(row)
        }
        emptyState.visibility = if (visible.isEmpty()) VISIBLE else GONE
    }
}
