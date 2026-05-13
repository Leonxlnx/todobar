package dev.todobar.mobile.ui.views

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import dev.todobar.mobile.R
import dev.todobar.mobile.model.DockEdge
import dev.todobar.mobile.model.SectionId
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.model.TaskSortMode
import dev.todobar.mobile.model.ThemeMode
import dev.todobar.mobile.model.ThemePreset
import dev.todobar.mobile.store.Store
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.Drawables
import dev.todobar.mobile.ui.Drawables.dp
import dev.todobar.mobile.ui.Views

/**
 * Mirrors the desktop "Settings drawer". Every setting maps 1:1 to the
 * desktop's `sidebarSettings`. Some settings (launch-at-login,
 * tabVisibility=hover) are skipped — they don't apply on Android.
 */
class SettingsView(
    context: Context,
    private val store: Store,
    private var palette: Palette,
    private var settings: SidebarSettings,
    private val onPickBackdrop: () -> Unit,
) : SectionView(context) {

    private val scroll = ScrollView(context)
    private val body = Views.column(context)

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
        body.setPadding(dp(context, 14f), dp(context, 10f), dp(context, 14f), dp(context, 40f))
    }

    override fun applyTheme(palette: Palette, settings: SidebarSettings) {
        this.palette = palette
        this.settings = settings
        rebuild()
    }

    override fun refresh() = rebuild()

    private fun rebuild() {
        body.removeAllViews()
        section("Appearance")
        themeMode()
        themePresets()
        backdrop()
        slider("Surface opacity", "%", settings.surfaceAlpha, 58, 100) { v ->
            store.saveSettings(settings.copy(surfaceAlpha = v))
        }
        slider("Panel radius", "px", settings.panelRadius, 12, 28) { v ->
            store.saveSettings(settings.copy(panelRadius = v))
        }

        section("Layout")
        edgeSelector()
        slider("Panel width", "dp", settings.panelWidth, 320, 560, step = 4) { v ->
            store.saveSettings(settings.copy(panelWidth = v))
        }
        slider("Tab width", "dp", settings.tabWidth, 22, 112) { v ->
            store.saveSettings(settings.copy(tabWidth = v))
        }
        slider("Handle height", "dp", settings.handleHeight, 56, 176) { v ->
            store.saveSettings(settings.copy(handleHeight = v))
        }
        slider("Handle position", "%", settings.handleY, 0, 100) { v ->
            store.saveSettings(settings.copy(handleY = v))
        }
        slider("Motion duration", "ms", settings.motionMs, 140, 360, step = 10) { v ->
            store.saveSettings(settings.copy(motionMs = v))
        }

        section("Tasks")
        sortMode()
        slider("Task row height", "dp", settings.taskRowHeight, 40, 62) { v ->
            store.saveSettings(settings.copy(taskRowHeight = v))
        }
        slider("Task spacing", "dp", settings.taskGap, 4, 14) { v ->
            store.saveSettings(settings.copy(taskGap = v))
        }
        sliderFloat("Task text size", "sp", settings.taskTextSize, 11f, 14f) { v ->
            store.saveSettings(settings.copy(taskTextSize = v))
        }
        body.addView(
            Views.toggle(context, "Show completed tasks", settings.showCompleted, palette) { v ->
                store.saveSettings(settings.copy(showCompleted = v))
            }.also { sectionRowParams(it) },
        )
        body.addView(
            Views.toggle(context, "Reminder notifications", settings.notificationsEnabled, palette) { v ->
                store.saveSettings(settings.copy(notificationsEnabled = v))
            }.also { sectionRowParams(it) },
        )

        section("Section order")
        sectionOrder()

        section("Maintenance")
        primaryButton("Clear completed tasks") {
            store.clearCompletedToday()
        }
        secondaryButton("Reset to defaults") {
            store.saveSettings(SidebarSettings())
        }
    }

    private fun section(title: String) {
        val tv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(palette.muted)
            setPadding(0, dp(context, 14f), 0, dp(context, 6f))
            isAllCaps = true
            letterSpacing = 0.08f
        }
        body.addView(tv)
    }

    private fun themeMode() {
        val row = Views.row(context, Gravity.CENTER_VERTICAL).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f))
        }
        sectionRowParams(row)

        val label = Views.text(context, "Theme", sizeSp = 12.5f, color = palette.ink)
        label.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        row.addView(label)

        val light = Views.chip(context, "Light", palette, active = settings.theme == ThemeMode.LIGHT) {
            val nextStyle = if (ThemePreset.byMode[ThemeMode.LIGHT]?.contains(settings.visualStyle) == true) {
                settings.visualStyle
            } else ThemePreset.byMode[ThemeMode.LIGHT]?.first() ?: ThemePreset.CODEX
            store.saveSettings(settings.copy(theme = ThemeMode.LIGHT, visualStyle = nextStyle))
        }
        row.addView(light)
        val gap = View(context); gap.layoutParams = LayoutParams(dp(context, 6f), 1); row.addView(gap)
        val dark = Views.chip(context, "Dark", palette, active = settings.theme == ThemeMode.DARK) {
            val nextStyle = if (ThemePreset.byMode[ThemeMode.DARK]?.contains(settings.visualStyle) == true) {
                settings.visualStyle
            } else ThemePreset.byMode[ThemeMode.DARK]?.first() ?: ThemePreset.CODEX
            store.saveSettings(settings.copy(theme = ThemeMode.DARK, visualStyle = nextStyle))
        }
        row.addView(dark)
        body.addView(row)
    }

    private fun themePresets() {
        val allowed = ThemePreset.byMode[settings.theme].orEmpty()
        val grid = Views.column(context).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 12f))
        }
        sectionRowParams(grid)
        val title = Views.text(context, "Visual style", sizeSp = 12f, color = palette.muted)
        title.setPadding(0, 0, 0, dp(context, 8f))
        grid.addView(title)

        var row: LinearLayout? = null
        var inRow = 0
        allowed.forEach { preset ->
            if (row == null || inRow >= 2) {
                row = Views.row(context, Gravity.CENTER_VERTICAL).apply {
                    val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 0, 0, dp(context, 8f))
                    layoutParams = params
                }
                grid.addView(row)
                inRow = 0
            }
            val active = settings.visualStyle == preset
            val chip = Views.chip(context, ThemePreset.displayLabel(preset, settings.theme), palette, active) {
                store.saveSettings(settings.copy(visualStyle = preset))
            }
            val chipParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            chipParams.setMargins(dp(context, if (inRow == 0) 0f else 4f), 0, dp(context, if (inRow == 0) 4f else 0f), 0)
            chip.layoutParams = chipParams
            row!!.addView(chip)
            inRow++
        }
        body.addView(grid)
    }

    private fun backdrop() {
        val container = Views.column(context).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 12f))
        }
        sectionRowParams(container)
        val title = Views.text(context, "Backdrop image", sizeSp = 12.5f, color = palette.ink, bold = true)
        container.addView(title)
        val current = if (settings.backdropImageUri.isBlank()) {
            "Choose an image to layer behind the panel."
        } else {
            settings.backdropImageName.ifBlank { "Custom backdrop set" }
        }
        val sub = Views.text(context, current, sizeSp = 11.5f, color = palette.muted)
        sub.setPadding(0, dp(context, 4f), 0, dp(context, 10f))
        container.addView(sub)

        val actions = Views.row(context, Gravity.CENTER_VERTICAL)
        val pickBtn = Views.chip(context, "Choose image", palette, active = true) { onPickBackdrop() }
        actions.addView(pickBtn)
        if (settings.backdropImageUri.isNotBlank()) {
            val gap = View(context); gap.layoutParams = LinearLayout.LayoutParams(dp(context, 8f), 1)
            actions.addView(gap)
            val clearBtn = Views.chip(context, "Remove", palette) {
                store.saveSettings(settings.copy(backdropImageUri = "", backdropImageName = ""))
            }
            actions.addView(clearBtn)
        }
        container.addView(actions)

        if (settings.backdropImageUri.isNotBlank()) {
            slider("Backdrop opacity", "%", settings.backdropOpacity, 0, 100) { v ->
                store.saveSettings(settings.copy(backdropOpacity = v))
            }
            slider("Backdrop blur", "px", settings.backdropBlur, 0, 24) { v ->
                store.saveSettings(settings.copy(backdropBlur = v))
            }
            slider("Backdrop dim", "%", settings.backdropDim, 0, 80) { v ->
                store.saveSettings(settings.copy(backdropDim = v))
            }
        }

        body.addView(container)
    }

    private fun slider(
        label: String, unit: String, value: Int, min: Int, max: Int,
        step: Int = 1, onChange: (Int) -> Unit,
    ) {
        val row = Views.column(context).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 8f), dp(context, 12f), dp(context, 10f))
        }
        sectionRowParams(row)

        val header = Views.row(context, Gravity.CENTER_VERTICAL)
        val title = Views.text(context, label, sizeSp = 12.5f, color = palette.ink)
        title.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        header.addView(title)
        val valueLabel = TextView(context).apply {
            text = "$value$unit"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(palette.muted)
        }
        header.addView(valueLabel)
        row.addView(header)

        val slider = Views.slider(context, min, max, value, palette) { v ->
            val stepped = (v / step) * step
            valueLabel.text = "$stepped$unit"
            onChange(stepped)
        }
        row.addView(slider)
        body.addView(row)
    }

    private fun sliderFloat(
        label: String, unit: String, value: Float, min: Float, max: Float,
        onChange: (Float) -> Unit,
    ) {
        val multiplier = 2f
        slider(label, unit, (value * multiplier).toInt(), (min * multiplier).toInt(), (max * multiplier).toInt()) { v ->
            onChange(v / multiplier)
        }
    }

    private fun edgeSelector() {
        val row = Views.row(context, Gravity.CENTER_VERTICAL).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f))
        }
        sectionRowParams(row)
        val title = Views.text(context, "Dock edge", sizeSp = 12.5f, color = palette.ink)
        title.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        row.addView(title)
        DockEdge.values().forEach { edge ->
            val chip = Views.chip(context, edge.name.lowercase().replaceFirstChar { it.uppercase() }, palette, settings.dockEdge == edge) {
                store.saveSettings(settings.copy(dockEdge = edge))
            }
            val params = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(dp(context, 6f), 0, 0, 0)
            chip.layoutParams = params
            row.addView(chip)
        }
        body.addView(row)
    }

    private fun sortMode() {
        val row = Views.row(context, Gravity.CENTER_VERTICAL).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 10f))
        }
        sectionRowParams(row)
        val title = Views.text(context, "Sort", sizeSp = 12.5f, color = palette.ink)
        title.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        row.addView(title)
        listOf(
            TaskSortMode.PRIORITY to "Priority",
            TaskSortMode.NEWEST to "Newest",
            TaskSortMode.OLDEST to "Oldest",
        ).forEach { (mode, label) ->
            val chip = Views.chip(context, label, palette, settings.taskSortMode == mode) {
                store.saveSettings(settings.copy(taskSortMode = mode))
            }
            val params = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            params.setMargins(dp(context, 6f), 0, 0, 0)
            chip.layoutParams = params
            row.addView(chip)
        }
        body.addView(row)
    }

    private fun sectionOrder() {
        val container = Views.column(context).apply {
            background = Drawables.roundedSurface(context, palette.settingsRow, 12f, palette.taskBorder)
            setPadding(dp(context, 12f), dp(context, 10f), dp(context, 12f), dp(context, 12f))
        }
        sectionRowParams(container)
        val hint = Views.text(context, "Reorder how Today / Calendar / Lists appear in the icon rail.", sizeSp = 11.5f, color = palette.muted)
        hint.setPadding(0, 0, 0, dp(context, 8f))
        container.addView(hint)

        settings.sectionOrder.forEachIndexed { index, id ->
            val row = Views.row(context, Gravity.CENTER_VERTICAL).apply {
                setPadding(0, dp(context, 6f), 0, dp(context, 6f))
            }
            val label = Views.text(context, "${index + 1}. ${id.title}", sizeSp = 12.5f, color = palette.ink)
            label.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            row.addView(label)
            val upBtn = Views.chip(context, "↑", palette) {
                moveSection(id, -1)
            }
            row.addView(upBtn)
            val gap = View(context); gap.layoutParams = LinearLayout.LayoutParams(dp(context, 6f), 1)
            row.addView(gap)
            val downBtn = Views.chip(context, "↓", palette) {
                moveSection(id, 1)
            }
            row.addView(downBtn)
            container.addView(row)
        }
        body.addView(container)
    }

    private fun moveSection(id: SectionId, delta: Int) {
        val order = settings.sectionOrder.toMutableList()
        val index = order.indexOf(id)
        if (index < 0) return
        val target = (index + delta).coerceIn(0, order.size - 1)
        if (target == index) return
        order.removeAt(index)
        order.add(target, id)
        store.saveSettings(settings.copy(sectionOrder = order))
    }

    private fun primaryButton(label: String, onClick: () -> Unit) {
        val btn = Button(context).apply {
            text = label
            background = Drawables.primaryButton(context, palette)
            setTextColor(palette.sidebarBg)
            isAllCaps = false
            setOnClickListener { onClick() }
        }
        sectionRowParams(btn)
        body.addView(btn)
    }

    private fun secondaryButton(label: String, onClick: () -> Unit) {
        val btn = Button(context).apply {
            text = label
            background = Drawables.secondaryButton(context, palette)
            setTextColor(palette.ink)
            isAllCaps = false
            setOnClickListener { onClick() }
        }
        sectionRowParams(btn)
        body.addView(btn)
    }

    private fun sectionRowParams(view: View) {
        val params = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, dp(context, 8f))
        view.layoutParams = params
    }
}
