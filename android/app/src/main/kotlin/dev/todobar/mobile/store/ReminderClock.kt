package dev.todobar.mobile.store

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reminder strings are stored in the same local "datetime-local" shape the
 * desktop uses: `YYYY-MM-DDTHH:mm`. We parse/format relative to the device's
 * local zone so saved reminders survive timezone shifts only via re-emit.
 */
object ReminderClock {
    private val ISO = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    }
    private val DISPLAY = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    }
    private val DAY_KEY = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun format(timeMs: Long): String = ISO.get()!!.format(Date(timeMs))

    fun parse(value: String): Long = ISO.get()!!.parse(value)?.time
        ?: throw IllegalArgumentException("not a datetime-local: $value")

    fun displayLabel(value: String?): String {
        if (value.isNullOrBlank()) return ""
        val time = runCatching { parse(value) }.getOrNull() ?: return ""
        return DISPLAY.get()!!.format(Date(time))
    }

    fun tomorrowAt(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun dayKey(timeMs: Long): String = DAY_KEY.get()!!.format(Date(timeMs))
    fun dayKey(value: String): String? = runCatching { dayKey(parse(value)) }.getOrNull()
}
