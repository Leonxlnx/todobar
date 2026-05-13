package dev.todobar.mobile.ui.views

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.R
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
 * "Today" view — matches the desktop `.panel-section` markup: section heading
 * (icon + name + count + collapse), capture row, task list, pinned custom
 * lists rendered as compact goal-style sections.
 */
class TodayView(
    context: Context,
    private val store: Store,
    private var palette: Palette,
    private var settings: SidebarSettings,
) : SectionView(context) {

    private val scroll = ScrollView(context)
    private val body = Views.column(context)

    // section-heading
    private val sectionHeading = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val sectionIcon = ImageView(context)
    private val sectionTitle = TextView(context)
    private val sectionCount = TextView(context)
    private val collapseBtn = ImageView(context)

    // section-content
    private val captureSlot = LinearLayout(context).apply { orientation = VERTICAL }
    private val taskList = Views.column(context)
    private val customListsContainer = Views.column(context)
    private val emptyState = TextView(context)
    private val pinnedHeading = TextView(context)

    private var collapsed = false

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
        body.orientation = VERTICAL

        // ── section-heading ───────────────────────────────────────────────
        sectionIcon.setImageResource(R.drawable.ic_today)
        sectionIcon.scaleType = ImageView.ScaleType.FIT_CENTER
        val iconParams = LayoutParams(dp(context, 16f), dp(context, 16f))
        iconParams.setMargins(0, 0, dp(context, 6f), 0)
        sectionIcon.layoutParams = iconParams

        sectionTitle.text = "Today"
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        sectionTitle.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        sectionTitle.letterSpacing = -0.005f

        sectionCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
        sectionCount.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val countParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        countParams.setMargins(dp(context, 8f), 0, 0, 0)
        sectionCount.layoutParams = countParams

        collapseBtn.setImageResource(R.drawable.ic_chevron_down)
        collapseBtn.scaleType = ImageView.ScaleType.FIT_CENTER
        val collapseParams = LayoutParams(dp(context, 28f), dp(context, 28f))
        collapseBtn.layoutParams = collapseParams
        collapseBtn.setPadding(dp(context, 4f))
        collapseBtn.setOnClickListener {
            collapsed = !collapsed
            collapseBtn.rotation = if (collapsed) -90f else 0f
            updateCollapsedVisibility()
        }

        sectionHeading.addView(sectionIcon)
        sectionHeading.addView(sectionTitle)
        sectionHeading.addView(sectionCount)
        sectionHeading.addView(collapseBtn)
        sectionHeading.setPadding(0, dp(context, 4f), 0, dp(context, 4f))
        body.addView(sectionHeading)

        // ── capture row ───────────────────────────────────────────────────
        captureSlot.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        body.addView(captureSlot)
        body.addView(Views.spacer(context, 8))

        // ── task list ─────────────────────────────────────────────────────
        body.addView(taskList)

        emptyState.text = "No open tasks here."
        emptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
        emptyState.gravity = Gravity.CENTER
        emptyState.setPadding(dp(context, 16f))
        body.addView(emptyState)

        body.addView(Views.spacer(context, 14))

        // ── pinned lists heading ──────────────────────────────────────────
        pinnedHeading.text = "Pinned lists"
        pinnedHeading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        pinnedHeading.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        pinnedHeading.setPadding(0, 0, 0, dp(context, 6f))
        body.addView(pinnedHeading)
        body.addView(customListsContainer)

        rebuildCapture()
    }

    private fun updateCollapsedVisibility() {
        val gone = if (collapsed) GONE else VISIBLE
        captureSlot.visibility = gone
        taskList.visibility = gone
        emptyState.visibility = if (collapsed) GONE else (if (taskList.childCount == 0) VISIBLE else GONE)
    }

    private fun rebuildCapture() {
        captureSlot.removeAllViews()
        val capture = CaptureRow(context, palette) { title, reminderMs ->
            store.addTodayTask(title, reminderMs?.let { ReminderClock.format(it) })
        }
        captureSlot.addView(capture)
        capture.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    override fun applyTheme(palette: Palette, settings: SidebarSettings) {
        this.palette = palette
        this.settings = settings
        sectionIcon.imageTintList = android.content.res.ColorStateList.valueOf(palette.ink)
        sectionTitle.setTextColor(palette.ink)
        sectionCount.setTextColor(palette.muted)
        collapseBtn.imageTintList = android.content.res.ColorStateList.valueOf(palette.muted)
        collapseBtn.background = Drawables.roundedSurface(context, palette.controlBg, 10f, palette.taskBorder)
        emptyState.setTextColor(palette.muted)
        pinnedHeading.setTextColor(palette.muted)
        body.setPadding(dp(context, 2f), dp(context, 2f), dp(context, 2f), dp(context, 32f))
        rebuildCapture()
        refresh()
    }

    override fun refresh() {
        val tasks = Store.sortTasks(store.today(), settings.taskSortMode)
        val visible = if (settings.showCompleted) tasks else tasks.filterNot { it.done }
        val open = tasks.count { !it.done }
        val complete = tasks.count { it.done }
        val totalCount = tasks.size

        sectionCount.text = "$complete done · $totalCount total"

        taskList.removeAllViews()
        visible.forEach { task ->
            taskList.addView(makeRow(task, TaskScope.Today))
            val row = taskList.getChildAt(taskList.childCount - 1)
            val params = row.layoutParams as LayoutParams
            params.setMargins(0, 0, 0, dp(context, settings.taskGap.toFloat()))
            row.layoutParams = params
        }

        emptyState.visibility = if (visible.isEmpty() && !collapsed) VISIBLE else GONE

        customListsContainer.removeAllViews()
        val pinned = store.customLists().filter { it.showOnToday }
        pinnedHeading.visibility = if (pinned.isEmpty()) GONE else VISIBLE
        pinned.forEach { list -> customListsContainer.addView(buildPinnedList(list)) }
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

        val header = Views.row(context, Gravity.CENTER_VERTICAL)
        val titleTv = TextView(context).apply {
            text = list.title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(palette.ink)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleTv)

        val open = list.tasks.count { !it.done }
        val total = list.tasks.size
        val countTv = TextView(context).apply {
            text = "$open / $total"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTextColor(palette.muted)
        }
        header.addView(countTv)

        val collapseListBtn = Views.iconButton(context, Icon.CHEVRON_DOWN, palette.muted, sizeDp = 28) {
            store.toggleListCollapsed(list.id)
        }
        header.addView(collapseListBtn)

        container.addView(header)

        if (!list.collapsed) {
            container.addView(Views.spacer(context, 6))
            val sorted = Store.sortTasks(list.tasks, settings.taskSortMode)
            val visible = if (settings.showCompleted) sorted else sorted.filterNot { it.done }
            if (visible.isEmpty()) {
                val empty = TextView(context).apply {
                    text = "No tasks in this list yet."
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setTextColor(palette.muted)
                }
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
}
