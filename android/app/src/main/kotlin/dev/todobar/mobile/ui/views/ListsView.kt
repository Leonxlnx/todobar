package dev.todobar.mobile.ui.views

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.EditText
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

class ListsView(
    context: Context,
    private val store: Store,
    private var palette: Palette,
    private var settings: SidebarSettings,
) : SectionView(context) {

    private val scroll = ScrollView(context)
    private val body = Views.column(context)
    private val headerRow = Views.row(context, Gravity.CENTER_VERTICAL)
    private val listsContainer = Views.column(context)
    private val emptyState = TextView(context)

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

        headerRow.setPadding(0, 0, 0, dp(context, 10f))
        val title = Views.text(context, "Lists", sizeSp = 14.5f, color = palette.ink, bold = true)
        title.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        headerRow.addView(title)
        val addBtn = Views.chip(context, "+ New list", palette, active = true) { promptNewList() }
        headerRow.addView(addBtn)
        body.addView(headerRow)

        body.addView(listsContainer)

        emptyState.text = "No lists yet — tap “New list” to create your first."
        emptyState.gravity = Gravity.CENTER
        emptyState.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        emptyState.setPadding(dp(context, 16f))
        body.addView(emptyState)
    }

    override fun applyTheme(palette: Palette, settings: SidebarSettings) {
        this.palette = palette
        this.settings = settings
        refresh()
    }

    override fun refresh() {
        listsContainer.removeAllViews()
        val lists = store.customLists()
        emptyState.visibility = if (lists.isEmpty()) VISIBLE else GONE
        lists.forEach { list -> listsContainer.addView(buildList(list)) }
    }

    private fun buildList(list: CustomList): LinearLayout {
        val container = Views.column(context).apply {
            background = Drawables.roundedSurface(context, palette.sectionBg, 14f, palette.line)
            setPadding(dp(context, 12f))
        }
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, dp(context, 12f))
        container.layoutParams = params

        // Title row
        val titleRow = Views.row(context, Gravity.CENTER_VERTICAL)
        val titleView = Views.text(context, list.title, sizeSp = 13.5f, color = palette.ink, bold = true)
        titleView.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        titleView.setOnClickListener { promptRename(list) }
        titleRow.addView(titleView)

        val pinBtn = Views.chip(
            context,
            if (list.showOnToday) "Pinned" else "Pin to Today",
            palette,
            active = list.showOnToday,
        ) { store.toggleListPinOnToday(list.id) }
        titleRow.addView(pinBtn)

        val collapseBtn = Views.iconButton(
            context, Icon.CHEVRON_DOWN, palette.muted, sizeDp = 28,
        ) { store.toggleListCollapsed(list.id) }
        titleRow.addView(collapseBtn)

        val deleteBtn = Views.iconButton(context, Icon.DELETE, palette.muted, sizeDp = 28) {
            confirmDelete(list)
        }
        titleRow.addView(deleteBtn)
        container.addView(titleRow)

        if (!list.collapsed) {
            container.addView(Views.spacer(context, 8))
            val capture = CaptureRow(context, palette) { title, reminderMs ->
                store.addToCustomList(list.id, title, reminderMs?.let { ReminderClock.format(it) })
            }
            capture.setHint("Add to ${list.title}")
            container.addView(capture)

            val sorted = Store.sortTasks(list.tasks, settings.taskSortMode)
            val visible = if (settings.showCompleted) sorted else sorted.filterNot { it.done }
            if (visible.isEmpty()) {
                val empty = Views.text(context, "No tasks yet.", sizeSp = 11.5f, color = palette.muted)
                val emptyParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                emptyParams.setMargins(0, dp(context, 10f), 0, 0)
                empty.layoutParams = emptyParams
                empty.gravity = Gravity.CENTER
                container.addView(empty)
            } else {
                container.addView(Views.spacer(context, 6))
                visible.forEach { task ->
                    val row = TaskRowView(
                        context, palette, settings.taskRowHeight, settings.taskTextSize,
                        object : TaskRowView.Callbacks {
                            override fun onToggle(taskId: Long) = store.toggleTask(TaskScope.CustomList(list.id), taskId)
                            override fun onCyclePriority(taskId: Long) = store.cyclePriority(TaskScope.CustomList(list.id), taskId)
                            override fun onCycleReminder(taskId: Long) = store.cycleReminder(TaskScope.CustomList(list.id), taskId)
                            override fun onClearReminder(taskId: Long) {}
                            override fun onSnooze(taskId: Long) = store.snoozeReminder(TaskScope.CustomList(list.id), taskId, 10)
                            override fun onEditSave(taskId: Long, newTitle: String) = store.editTitle(TaskScope.CustomList(list.id), taskId, newTitle)
                            override fun onDelete(taskId: Long) = store.removeTask(TaskScope.CustomList(list.id), taskId)
                        },
                    )
                    row.bind(task)
                    val rowParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    rowParams.setMargins(0, dp(context, settings.taskGap.toFloat()), 0, 0)
                    row.layoutParams = rowParams
                    container.addView(row)
                }
            }
        }

        return container
    }

    private fun promptNewList() {
        val input = EditText(context).apply {
            hint = "List name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val padding = dp(context, 24f)
        val wrap = LinearLayout(context).apply {
            setPadding(padding, padding, padding, 0)
            addView(input)
        }
        AlertDialog.Builder(context)
            .setTitle("New list")
            .setView(wrap)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text?.toString().orEmpty()
                if (name.isNotBlank()) store.createCustomList(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptRename(list: CustomList) {
        val input = EditText(context).apply {
            setText(list.title)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val padding = dp(context, 24f)
        val wrap = LinearLayout(context).apply {
            setPadding(padding, padding, padding, 0)
            addView(input)
        }
        AlertDialog.Builder(context)
            .setTitle("Rename list")
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text?.toString().orEmpty()
                if (name.isNotBlank()) store.renameCustomList(list.id, name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(list: CustomList) {
        AlertDialog.Builder(context)
            .setTitle("Delete list?")
            .setMessage("Delete “${list.title}” and its ${list.tasks.size} tasks? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> store.deleteCustomList(list.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
