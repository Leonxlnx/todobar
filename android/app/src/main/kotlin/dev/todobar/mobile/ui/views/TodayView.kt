package dev.todobar.mobile.ui.views

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.model.CustomList
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.model.Task
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

/**
 * "Today" view: capture row, focus strip, primary task list, pinned custom
 * lists rendered as goal-style collapsible sections.
 */
class TodayView(
    context: Context,
    private val store: Store,
    private var palette: Palette,
    private var settings: SidebarSettings,
) : SectionView(context) {

    private val scroll = ScrollView(context)
    private val body = Views.column(context)
    private val captureContainer = LinearLayout(context).apply { orientation = VERTICAL }
    private val focusStripLabel = TextView(context)
    private val focusStripTrack = View(context)
    private val taskList = Views.column(context)
    private val customLists = Views.column(context)
    private val emptyState = TextView(context)

    init {
        orientation = VERTICAL
        scroll.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scroll.isFillViewport = true
        addView(scroll)
        scroll.addView(body)
        body.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        body.setPadding(dp(context, 14f), dp(context, 10f), dp(context, 14f), dp(context, 32f))

        body.addView(captureContainer)
        body.addView(Views.spacer(context, 12))

        val focusContainer = Views.column(context).apply {
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 12f))
        }
        focusStripLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        focusContainer.addView(focusStripLabel)
        val trackParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 6f))
        trackParams.setMargins(0, dp(context, 6f), 0, 0)
        focusStripTrack.layoutParams = trackParams
        focusContainer.addView(focusStripTrack)
        body.addView(focusContainer)
        body.addView(Views.spacer(context, 10))

        body.addView(taskList)
        body.addView(Views.spacer(context, 12))
        body.addView(customLists)
        emptyState.text = "Nothing scheduled. Capture a task to begin."
        emptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        emptyState.gravity = Gravity.CENTER
        emptyState.setPadding(dp(context, 16f))
        body.addView(emptyState)

        rebuildCapture()
    }

    private fun rebuildCapture() {
        captureContainer.removeAllViews()
        val capture = CaptureRow(context, palette) { title, reminderMs ->
            store.addTodayTask(title, reminderMs?.let { ReminderClock.format(it) })
        }
        captureContainer.addView(capture)
        capture.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun applyTheme(palette: Palette, settings: SidebarSettings) {
        this.palette = palette
        this.settings = settings
        rebuildCapture()
        focusStripLabel.setTextColor(palette.muted)
        focusStripTrack.background = Drawables.roundedSurface(context, palette.controlBg, 999f)
        emptyState.setTextColor(palette.muted)
        refresh()
    }

    override fun refresh() {
        val tasks = Store.sortTasks(store.today(), settings.taskSortMode)
        val visible = if (settings.showCompleted) tasks else tasks.filterNot { it.done }
        val open = tasks.count { !it.done }
        val complete = tasks.count { it.done }
        val totalCount = tasks.size

        focusStripLabel.text = "Today · $open open · $complete done"
        val progress = if (totalCount == 0) 0f else complete / totalCount.toFloat()
        focusStripTrack.background = makeProgressBar(progress)

        taskList.removeAllViews()
        visible.forEach { task ->
            taskList.addView(makeRow(task, TaskScope.Today))
            val params = (taskList.getChildAt(taskList.childCount - 1).layoutParams as LayoutParams)
            params.setMargins(0, 0, 0, dp(context, settings.taskGap.toFloat()))
            taskList.getChildAt(taskList.childCount - 1).layoutParams = params
        }

        emptyState.visibility = if (visible.isEmpty()) VISIBLE else GONE

        // Pinned custom lists (showOnToday)
        customLists.removeAllViews()
        val pinned = store.customLists().filter { it.showOnToday }
        pinned.forEach { list -> customLists.addView(buildPinnedList(list)) }
    }

    private fun makeRow(task: Task, scope: TaskScope): TaskRowView {
        return TaskRowView(
            context, palette, settings.taskRowHeight, settings.taskTextSize,
            object : TaskRowView.Callbacks {
                override fun onToggle(taskId: Long) = store.toggleTask(scope, taskId)
                override fun onCyclePriority(taskId: Long) = store.cyclePriority(scope, taskId)
                override fun onCycleReminder(taskId: Long) = store.cycleReminder(scope, taskId)
                override fun onClearReminder(taskId: Long) {}
                override fun onSnooze(taskId: Long) = store.snoozeReminder(scope, taskId, 10)
                override fun onEditSave(taskId: Long, newTitle: String) = store.editTitle(scope, taskId, newTitle)
                override fun onDelete(taskId: Long) = store.removeTask(scope, taskId)
            },
        ).apply { bind(task) }
    }

    private fun buildPinnedList(list: CustomList): LinearLayout {
        val container = Views.column(context).apply {
            background = Drawables.roundedSurface(context, palette.sectionBg, 12f, palette.line)
            setPadding(dp(context, 12f))
        }
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, dp(context, 10f))
        container.layoutParams = params

        // Header row
        val header = Views.row(context, Gravity.CENTER_VERTICAL)
        val titleView = Views.text(context, list.title, sizeSp = 12.5f, color = palette.ink, bold = true)
        val titleParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        titleView.layoutParams = titleParams
        header.addView(titleView)

        val openCount = list.tasks.count { !it.done }
        val totalCount = list.tasks.size
        val countView = Views.text(context, "$openCount / $totalCount", sizeSp = 10.5f, color = palette.muted)
        header.addView(countView)

        val collapseBtn = Views.iconButton(
            context, Icon.CHEVRON_DOWN, palette.muted, sizeDp = 28,
        ) { store.toggleListCollapsed(list.id) }
        header.addView(collapseBtn)

        container.addView(header)

        if (!list.collapsed) {
            container.addView(Views.spacer(context, 6))
            val sorted = Store.sortTasks(list.tasks, settings.taskSortMode)
            val visible = if (settings.showCompleted) sorted else sorted.filterNot { it.done }
            if (visible.isEmpty()) {
                val empty = Views.text(context, "No tasks in this list yet.", sizeSp = 11.5f, color = palette.muted)
                empty.setPadding(0, dp(context, 6f), 0, dp(context, 6f))
                container.addView(empty)
            } else {
                visible.forEach { task ->
                    val row = makeRow(task, TaskScope.CustomList(list.id))
                    val rowParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    rowParams.setMargins(0, 0, 0, dp(context, settings.taskGap.toFloat()))
                    row.layoutParams = rowParams
                    container.addView(row)
                }
            }
        }

        return container
    }

    private fun makeProgressBar(progress: Float): android.graphics.drawable.Drawable {
        val track = Drawables.roundedSurface(context, palette.controlBg, 999f)
        if (progress <= 0f) return track
        val fill = Drawables.roundedSurface(context, palette.accent, 999f)
        val layer = android.graphics.drawable.LayerDrawable(arrayOf(track, fill))
        val width = (1f - progress.coerceIn(0f, 1f)) * 1000f
        layer.setLayerInset(1, 0, 0, width.toInt(), 0)
        return layer
    }
}
