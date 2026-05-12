package dev.todobar.mobile.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.todobar.mobile.R
import dev.todobar.mobile.TodoRepository
import dev.todobar.mobile.model.Task

/**
 * Hosts the expanded sidebar panel. The panel covers the entire screen with a
 * tap-catcher scrim so taps outside the panel close it (matching the desktop
 * "click-through outside the panel" behavior in spirit).
 */
class SidebarOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val overlayType: Int,
    private val onDismiss: () -> Unit,
) {

    private val repo = TodoRepository.get(context)
    private val taskAdapter = TaskAdapter(
        onToggle = { id -> repo.toggle(id) },
        onDelete = { id -> repo.remove(id) },
    )

    private val tasksListener: (List<Task>) -> Unit = { tasks ->
        rootView?.post { renderTasks(tasks) }
    }

    private var rootView: View? = null
    private var panel: View? = null
    private var inputField: EditText? = null
    private var taskList: RecyclerView? = null
    private var emptyState: TextView? = null
    private var headerCount: TextView? = null

    fun show() {
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.overlay_sidebar, FrameLayout(context), false)
        rootView = root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val scrim = root.findViewById<View>(R.id.scrim)
        val panelView = root.findViewById<View>(R.id.panel)
        val closeBtn = root.findViewById<ImageButton>(R.id.close_button)
        val clearCompletedBtn = root.findViewById<ImageButton>(R.id.clear_completed_button)
        val input = root.findViewById<EditText>(R.id.task_input)
        val list = root.findViewById<RecyclerView>(R.id.task_list)
        val empty = root.findViewById<TextView>(R.id.empty_state)
        val countText = root.findViewById<TextView>(R.id.header_count)

        panel = panelView
        inputField = input
        taskList = list
        emptyState = empty
        headerCount = countText

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = taskAdapter

        scrim.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                dismiss()
            }
            true
        }
        // Swallow taps on the panel body so they don't bubble to the scrim.
        panelView.setOnTouchListener { _, _ -> false }

        closeBtn.setOnClickListener { dismiss() }
        clearCompletedBtn.setOnClickListener {
            val updated = repo.clearCompleted()
            renderTasks(updated)
        }

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_GO
                || actionId == EditorInfo.IME_ACTION_SEND
            ) {
                submitInput()
                true
            } else false
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        runCatching { windowManager.addView(root, params) }
            .onFailure { onDismiss(); return }

        repo.addListener(tasksListener)
        renderTasks(repo.load())

        // Slide-in animation matching the desktop motion-ms (~230ms).
        panelView.translationX = panelView.resources.displayMetrics.widthPixels.toFloat()
        panelView.animate()
            .translationX(0f)
            .setDuration(230)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        scrim.alpha = 0f
        scrim.animate().alpha(1f).setDuration(180).start()
    }

    fun dismiss() {
        val root = rootView ?: return
        val panelView = panel
        val scrim = root.findViewById<View>(R.id.scrim)

        repo.removeListener(tasksListener)

        scrim?.animate()?.alpha(0f)?.setDuration(160)?.start()
        if (panelView != null) {
            panelView.animate()
                .translationX(panelView.width.toFloat())
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    teardown()
                }
                .start()
        } else {
            teardown()
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        // Force a re-layout on rotation: the panel always anchors to the
        // trailing edge, so we just resize the entire root.
        val root = rootView ?: return
        runCatching {
            windowManager.updateViewLayout(
                root,
                (root.layoutParams as WindowManager.LayoutParams).apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                },
            )
        }
    }

    private fun teardown() {
        rootView?.let { runCatching { windowManager.removeView(it) } }
        rootView = null
        panel = null
        inputField = null
        taskList = null
        emptyState = null
        onDismiss()
    }

    private fun submitInput() {
        val input = inputField ?: return
        val title = input.text.toString().trim()
        if (title.isEmpty()) return
        val updated = repo.add(title)
        input.setText("")
        renderTasks(updated)
        taskList?.smoothScrollToPosition(0)
    }

    private fun renderTasks(tasks: List<Task>) {
        val sorted = tasks.sortedWith(
            compareBy<Task> { it.done }
                .thenBy { it.priority.ordinal }
                .thenByDescending { it.createdAt },
        )
        taskAdapter.submit(sorted)
        emptyState?.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        taskList?.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
        val openCount = sorted.count { !it.done }
        headerCount?.text = context.resources.getQuantityString(
            R.plurals.open_task_count,
            openCount,
            openCount,
        )
    }
}
