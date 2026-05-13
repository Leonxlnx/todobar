package dev.todobar.mobile.store

import android.content.Context
import android.content.SharedPreferences
import dev.todobar.mobile.model.CustomList
import dev.todobar.mobile.model.Priority
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.model.Task
import dev.todobar.mobile.model.TaskKind
import dev.todobar.mobile.model.TaskSortMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Single SharedPreferences-backed store for everything the sidebar persists:
 * - today's tasks
 * - scheduled (month) tasks shown in the Calendar view
 * - custom user lists
 * - sidebar settings
 * - notified reminder ids (so we don't re-toast)
 *
 * Listeners are fired on whichever thread mutated state — call sites should
 * hop to the UI thread before touching views.
 */
class Store private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val listeners = mutableSetOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun fire() {
        listeners.toList().forEach { runCatching { it() } }
    }

    // ── Today tasks ────────────────────────────────────────────────────────
    fun today(): List<Task> = loadTaskList(KEY_TODAY)
    fun saveToday(tasks: List<Task>) = saveTaskList(KEY_TODAY, tasks)

    // ── Scheduled tasks (calendar view) ────────────────────────────────────
    fun scheduled(): List<Task> = loadTaskList(KEY_MONTH)
    fun saveScheduled(tasks: List<Task>) = saveTaskList(KEY_MONTH, tasks)

    // ── Custom lists ───────────────────────────────────────────────────────
    fun customLists(): List<CustomList> {
        val raw = prefs.getString(KEY_LISTS, null) ?: return defaultLists()
        return runCatching {
            CustomList.listFromJson(JSONArray(raw))
        }.getOrElse { defaultLists() }
    }

    fun saveCustomLists(lists: List<CustomList>) {
        prefs.edit().putString(KEY_LISTS, CustomList.listToJson(lists).toString()).apply()
        fire()
    }

    // ── Settings ───────────────────────────────────────────────────────────
    fun settings(): SidebarSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return SidebarSettings()
        return runCatching {
            SidebarSettings.fromJson(JSONObject(raw))
        }.getOrElse { SidebarSettings() }
    }

    fun saveSettings(settings: SidebarSettings) {
        prefs.edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply()
        fire()
    }

    // ── Notified reminders ─────────────────────────────────────────────────
    fun notifiedReminders(): Set<String> =
        prefs.getStringSet(KEY_NOTIFIED, emptySet()) ?: emptySet()

    fun markReminderNotified(reminderKey: String) {
        val next = notifiedReminders().toMutableSet().also { it.add(reminderKey) }
        prefs.edit().putStringSet(KEY_NOTIFIED, next).apply()
    }

    fun clearReminderNotified(reminderKey: String) {
        val next = notifiedReminders().toMutableSet().also { it.remove(reminderKey) }
        prefs.edit().putStringSet(KEY_NOTIFIED, next).apply()
    }

    private fun loadTaskList(key: String): List<Task> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { Task.listFromJson(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    private fun saveTaskList(key: String, tasks: List<Task>) {
        prefs.edit().putString(key, Task.listToJson(tasks).toString()).apply()
        fire()
    }

    private fun defaultLists(): List<CustomList> = listOf(
        CustomList(id = "general", title = "General"),
    )

    // ── Mutators ────────────────────────────────────────────────────────────

    fun addTodayTask(title: String, reminderAt: String?): Task {
        val task = newTask(title, meta = "Today", reminderAt = reminderAt)
        saveToday(listOf(task) + today())
        return task
    }

    fun toggleTask(scope: TaskScope, id: Long) = mutate(scope, id) {
        it.copy(done = !it.done)
    }

    fun cyclePriority(scope: TaskScope, id: Long) = mutate(scope, id) {
        it.copy(priority = it.priority.next())
    }

    fun editTitle(scope: TaskScope, id: Long, title: String) = mutate(scope, id) {
        it.copy(title = title.trim().ifBlank { it.title })
    }

    fun cycleReminder(scope: TaskScope, id: Long) = mutate(scope, id) { task ->
        task.copy(reminderAt = nextQuickReminder(task.reminderAt))
    }

    fun snoozeReminder(scope: TaskScope, id: Long, minutes: Int = 10) = mutate(scope, id) { task ->
        val base = task.reminderAt?.let { runCatching { ReminderClock.parse(it) }.getOrNull() }
            ?: System.currentTimeMillis()
        task.copy(reminderAt = ReminderClock.format(base + minutes * 60_000L))
    }

    fun removeTask(scope: TaskScope, id: Long) {
        when (scope) {
            TaskScope.Today -> saveToday(today().filterNot { it.id == id })
            TaskScope.Scheduled -> saveScheduled(scheduled().filterNot { it.id == id })
            is TaskScope.CustomList -> {
                saveCustomLists(customLists().map { list ->
                    if (list.id == scope.listId) {
                        list.copy(tasks = list.tasks.filterNot { it.id == id })
                    } else list
                })
            }
        }
    }

    fun clearCompletedToday() {
        saveToday(today().filterNot { it.done })
    }

    fun addScheduled(title: String, meta: String, reminderAt: String, kind: TaskKind): Task {
        val task = newTask(title = title, meta = meta, reminderAt = reminderAt, kind = kind)
        saveScheduled(listOf(task) + scheduled())
        return task
    }

    fun addToCustomList(listId: String, title: String, reminderAt: String?): Task {
        val listTitle = customLists().firstOrNull { it.id == listId }?.title ?: "List"
        val task = newTask(title = title, meta = listTitle, reminderAt = reminderAt)
        saveCustomLists(customLists().map { list ->
            if (list.id == listId) list.copy(tasks = listOf(task) + list.tasks) else list
        })
        return task
    }

    fun createCustomList(title: String): CustomList {
        val list = CustomList(id = UUID.randomUUID().toString(), title = title.trim().ifBlank { "List" })
        saveCustomLists(customLists() + list)
        return list
    }

    fun renameCustomList(listId: String, title: String) {
        saveCustomLists(customLists().map { list ->
            if (list.id == listId) list.copy(title = title.trim().ifBlank { list.title }) else list
        })
    }

    fun toggleListCollapsed(listId: String) {
        saveCustomLists(customLists().map { list ->
            if (list.id == listId) list.copy(collapsed = !list.collapsed) else list
        })
    }

    fun toggleListPinOnToday(listId: String) {
        saveCustomLists(customLists().map { list ->
            if (list.id == listId) list.copy(showOnToday = !list.showOnToday) else list
        })
    }

    fun deleteCustomList(listId: String) {
        saveCustomLists(customLists().filterNot { it.id == listId })
    }

    private fun newTask(
        title: String,
        meta: String,
        reminderAt: String? = null,
        kind: TaskKind = TaskKind.TASK,
    ): Task = Task(
        id = System.currentTimeMillis(),
        title = title.trim(),
        meta = meta,
        priority = Priority.NORMAL,
        kind = kind,
        reminderAt = reminderAt,
    )

    private fun mutate(scope: TaskScope, id: Long, transform: (Task) -> Task) {
        when (scope) {
            TaskScope.Today -> saveToday(today().map { if (it.id == id) transform(it) else it })
            TaskScope.Scheduled -> saveScheduled(scheduled().map { if (it.id == id) transform(it) else it })
            is TaskScope.CustomList -> saveCustomLists(customLists().map { list ->
                if (list.id == scope.listId) {
                    list.copy(tasks = list.tasks.map { if (it.id == id) transform(it) else it })
                } else list
            })
        }
    }

    companion object {
        private const val PREFS_NAME = "todobar.v2"
        private const val KEY_TODAY = "today.v1"
        private const val KEY_MONTH = "month.v1"
        private const val KEY_LISTS = "lists.v1"
        private const val KEY_SETTINGS = "settings.v27"
        private const val KEY_NOTIFIED = "notified.v1"

        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store {
            return instance ?: synchronized(this) {
                instance ?: Store(context).also { instance = it }
            }
        }

        fun sortTasks(tasks: List<Task>, mode: TaskSortMode): List<Task> {
            return tasks.sortedWith(Comparator { a, b ->
                if (a.done != b.done) return@Comparator if (a.done) 1 else -1
                when (mode) {
                    TaskSortMode.NEWEST -> b.id.compareTo(a.id)
                    TaskSortMode.OLDEST -> a.id.compareTo(b.id)
                    TaskSortMode.PRIORITY -> {
                        val d = a.priority.ordinal - b.priority.ordinal
                        if (d != 0) d else b.id.compareTo(a.id)
                    }
                }
            })
        }

        private fun nextQuickReminder(current: String?): String? {
            if (current == null) {
                val now = System.currentTimeMillis()
                return ReminderClock.format(now + 30 * 60_000L)
            }
            val parsed = runCatching { ReminderClock.parse(current) }.getOrNull() ?: return null
            val tomorrow9 = ReminderClock.tomorrowAt(9, 0)
            return if (parsed < tomorrow9) ReminderClock.format(tomorrow9) else null
        }
    }
}

sealed interface TaskScope {
    data object Today : TaskScope
    data object Scheduled : TaskScope
    data class CustomList(val listId: String) : TaskScope
}
