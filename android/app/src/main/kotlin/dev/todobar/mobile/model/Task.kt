package dev.todobar.mobile.model

import org.json.JSONArray
import org.json.JSONObject

enum class Priority { FOCUS, NORMAL, LATER }

data class Task(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val priority: Priority = Priority.NORMAL,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("done", done)
        put("priority", priority.name)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(obj: JSONObject): Task = Task(
            id = obj.optString("id"),
            title = obj.optString("title"),
            done = obj.optBoolean("done", false),
            priority = runCatching {
                Priority.valueOf(obj.optString("priority", "NORMAL"))
            }.getOrElse { Priority.NORMAL },
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        )

        fun listFromJson(arr: JSONArray): List<Task> {
            val out = ArrayList<Task>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                out.add(fromJson(item))
            }
            return out
        }

        fun listToJson(tasks: List<Task>): JSONArray {
            val arr = JSONArray()
            tasks.forEach { arr.put(it.toJson()) }
            return arr
        }
    }
}
