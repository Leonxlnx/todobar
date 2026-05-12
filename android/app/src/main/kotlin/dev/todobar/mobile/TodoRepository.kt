package dev.todobar.mobile

import android.content.Context
import android.content.SharedPreferences
import dev.todobar.mobile.model.Priority
import dev.todobar.mobile.model.Task
import org.json.JSONArray
import java.util.UUID

/**
 * Local-first task storage backed by SharedPreferences. The same persistence
 * model the desktop sidebar uses (a flat "today" list with title/done/priority
 * fields) so that we can later plug in a sync layer without changing call
 * sites.
 */
class TodoRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val listeners = mutableListOf<(List<Task>) -> Unit>()

    fun load(): List<Task> {
        val raw = prefs.getString(KEY_TODAY, null) ?: return emptyList()
        return runCatching { Task.listFromJson(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    fun save(tasks: List<Task>) {
        prefs.edit().putString(KEY_TODAY, Task.listToJson(tasks).toString()).apply()
        listeners.toList().forEach { it(tasks) }
    }

    fun add(title: String, priority: Priority = Priority.NORMAL): List<Task> {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return load()
        val current = load().toMutableList()
        current.add(
            0,
            Task(
                id = UUID.randomUUID().toString(),
                title = trimmed,
                done = false,
                priority = priority,
            ),
        )
        save(current)
        return current
    }

    fun toggle(id: String): List<Task> {
        val current = load().map {
            if (it.id == id) it.copy(done = !it.done) else it
        }
        save(current)
        return current
    }

    fun remove(id: String): List<Task> {
        val current = load().filterNot { it.id == id }
        save(current)
        return current
    }

    fun clearCompleted(): List<Task> {
        val current = load().filterNot { it.done }
        save(current)
        return current
    }

    fun addListener(listener: (List<Task>) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (List<Task>) -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        private const val PREFS_NAME = "todobar.tasks.v1"
        private const val KEY_TODAY = "today"

        @Volatile
        private var instance: TodoRepository? = null

        fun get(context: Context): TodoRepository {
            return instance ?: synchronized(this) {
                instance ?: TodoRepository(context).also { instance = it }
            }
        }
    }
}
