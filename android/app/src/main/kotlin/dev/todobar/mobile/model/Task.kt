package dev.todobar.mobile.model

import org.json.JSONArray
import org.json.JSONObject

enum class Priority { FOCUS, NORMAL, LATER;

    fun next(): Priority = when (this) {
        NORMAL -> FOCUS
        FOCUS -> LATER
        LATER -> NORMAL
    }
}

enum class TaskKind { TASK, EVENT }

data class TaskSource(
    val type: String,
    val from: String? = null,
    val threadId: String? = null,
    val url: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        from?.let { put("from", it) }
        threadId?.let { put("threadId", it) }
        url?.let { put("url", it) }
    }

    companion object {
        fun fromJson(obj: JSONObject): TaskSource = TaskSource(
            type = obj.optString("type"),
            from = obj.optString("from").ifBlank { null },
            threadId = obj.optString("threadId").ifBlank { null },
            url = obj.optString("url").ifBlank { null },
        )
    }
}

/**
 * Mirrors the desktop Task model (src/tasks.ts) so persistence can converge later.
 * `id` is a millisecond-resolution stamp (the desktop uses Date.now()), `meta` is a
 * free-form caption like "Today · 40 min" or "May · Milestone 0.1".
 */
data class Task(
    val id: Long,
    val title: String,
    val meta: String = "",
    val priority: Priority = Priority.NORMAL,
    val kind: TaskKind = TaskKind.TASK,
    val done: Boolean = false,
    val reminderAt: String? = null,
    val source: TaskSource? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("meta", meta)
        put("priority", priority.name.lowercase())
        put("kind", kind.name.lowercase())
        put("done", done)
        reminderAt?.let { put("reminderAt", it) }
        source?.let { put("source", it.toJson()) }
    }

    companion object {
        fun fromJson(obj: JSONObject): Task = Task(
            id = obj.optLong("id", System.currentTimeMillis()),
            title = obj.optString("title"),
            meta = obj.optString("meta"),
            priority = parsePriority(obj.optString("priority")),
            kind = parseKind(obj.optString("kind")),
            done = obj.optBoolean("done", false),
            reminderAt = obj.optString("reminderAt").ifBlank { null },
            source = obj.optJSONObject("source")?.let { TaskSource.fromJson(it) },
        )

        fun listFromJson(arr: JSONArray): List<Task> = buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(fromJson(item))
            }
        }

        fun listToJson(tasks: List<Task>): JSONArray = JSONArray().apply {
            tasks.forEach { put(it.toJson()) }
        }

        private fun parsePriority(value: String?): Priority = when (value?.uppercase()) {
            "FOCUS" -> Priority.FOCUS
            "LATER" -> Priority.LATER
            else -> Priority.NORMAL
        }

        private fun parseKind(value: String?): TaskKind = when (value?.uppercase()) {
            "EVENT" -> TaskKind.EVENT
            else -> TaskKind.TASK
        }
    }
}

data class CustomList(
    val id: String,
    val title: String,
    val tasks: List<Task> = emptyList(),
    val collapsed: Boolean = false,
    val showOnToday: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("tasks", Task.listToJson(tasks))
        put("collapsed", collapsed)
        put("showOnToday", showOnToday)
    }

    companion object {
        fun fromJson(obj: JSONObject): CustomList = CustomList(
            id = obj.optString("id"),
            title = obj.optString("title").ifBlank { "List" },
            tasks = obj.optJSONArray("tasks")?.let { Task.listFromJson(it) } ?: emptyList(),
            collapsed = obj.optBoolean("collapsed", false),
            showOnToday = obj.optBoolean("showOnToday", false),
        )

        fun listFromJson(arr: JSONArray): List<CustomList> = buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(fromJson(obj))
            }
        }

        fun listToJson(lists: List<CustomList>): JSONArray = JSONArray().apply {
            lists.forEach { put(it.toJson()) }
        }
    }
}
