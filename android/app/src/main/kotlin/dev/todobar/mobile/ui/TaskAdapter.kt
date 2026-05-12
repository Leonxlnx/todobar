package dev.todobar.mobile.ui

import android.text.style.StrikethroughSpan
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.todobar.mobile.R
import dev.todobar.mobile.model.Priority
import dev.todobar.mobile.model.Task

class TaskAdapter(
    private val onToggle: (String) -> Unit,
    private val onDelete: (String) -> Unit,
) : RecyclerView.Adapter<TaskAdapter.VH>() {

    private val items = mutableListOf<Task>()

    fun submit(next: List<Task>) {
        val diff = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize() = items.size
                override fun getNewListSize() = next.size
                override fun areItemsTheSame(o: Int, n: Int) = items[o].id == next[n].id
                override fun areContentsTheSame(o: Int, n: Int) = items[o] == next[n]
            },
        )
        items.clear()
        items.addAll(next)
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = items[position]
        holder.bind(task)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.task_title)
        private val checkbox: View = view.findViewById(R.id.task_checkbox)
        private val priorityDot: View = view.findViewById(R.id.task_priority)
        private val deleteBtn: ImageButton = view.findViewById(R.id.task_delete)
        private val row: View = view.findViewById(R.id.task_row)

        fun bind(task: Task) {
            val context = itemView.context
            if (task.done) {
                val span = SpannableString(task.title)
                span.setSpan(StrikethroughSpan(), 0, span.length, 0)
                titleView.text = span
                titleView.alpha = 0.5f
            } else {
                titleView.text = task.title
                titleView.alpha = 1f
            }
            checkbox.isSelected = task.done
            checkbox.contentDescription = context.getString(
                if (task.done) R.string.cd_uncheck else R.string.cd_check,
            )

            priorityDot.setBackgroundResource(
                when (task.priority) {
                    Priority.FOCUS -> R.drawable.dot_focus
                    Priority.NORMAL -> R.drawable.dot_normal
                    Priority.LATER -> R.drawable.dot_later
                },
            )

            row.setOnClickListener { onToggle(task.id) }
            checkbox.setOnClickListener { onToggle(task.id) }
            deleteBtn.setOnClickListener { onDelete(task.id) }
        }
    }
}
