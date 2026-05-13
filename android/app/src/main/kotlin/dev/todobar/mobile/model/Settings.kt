package dev.todobar.mobile.model

import org.json.JSONArray
import org.json.JSONObject

enum class DockEdge { RIGHT, LEFT, TOP }
enum class ThemeMode { LIGHT, DARK }
enum class TaskSortMode { PRIORITY, NEWEST, OLDEST }
enum class TabVisibility { ALWAYS, HOVER }
enum class SectionId { TODAY, CALENDAR, LISTS;

    val title: String
        get() = when (this) {
            TODAY -> "Today"
            CALENDAR -> "Calendar"
            LISTS -> "Lists"
        }
}

enum class ThemePreset {
    CODEX, PORCELAIN, FROST, PAPER, CARBON, GRAPHITE, MIDNIGHT, CLAY, BLUEPRINT;

    companion object {
        val byMode = mapOf(
            ThemeMode.DARK to listOf(CODEX, CARBON, GRAPHITE, MIDNIGHT, CLAY, BLUEPRINT),
            ThemeMode.LIGHT to listOf(CODEX, PORCELAIN, FROST, PAPER, CLAY, BLUEPRINT),
        )

        fun displayLabel(preset: ThemePreset, mode: ThemeMode): String = when (mode) {
            ThemeMode.LIGHT -> when (preset) {
                CODEX -> "Studio"
                PORCELAIN -> "Porcelain"
                FROST -> "Frostline"
                PAPER -> "Paper Trail"
                CLAY -> "Terra"
                BLUEPRINT -> "Blueprint"
                else -> preset.name.lowercase().replaceFirstChar { it.uppercase() }
            }
            ThemeMode.DARK -> when (preset) {
                CODEX -> "Obsidian"
                CARBON -> "Carbon"
                GRAPHITE -> "Graphite"
                MIDNIGHT -> "Nightfall"
                CLAY -> "Ember"
                BLUEPRINT -> "Gridlock"
                else -> preset.name.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
    }
}

/**
 * Mirrors the desktop SidebarSettings (src/sidebarSettings.ts). Field
 * names are kept identical so eventual sync logic is trivial. Comments
 * include the desktop clamp ranges.
 */
data class SidebarSettings(
    val dockEdge: DockEdge = DockEdge.RIGHT,
    val panelWidth: Int = 400, // 320-560
    val tabVisibility: TabVisibility = TabVisibility.ALWAYS,
    val tabWidth: Int = 42, // 22-112
    val handleHeight: Int = 84, // 56-176
    val handleY: Int = 50, // 0-100
    val motionMs: Int = 230, // 140-360
    val panelRadius: Int = 18, // 12-28
    val surfaceAlpha: Int = 96, // 58-100
    val taskRowHeight: Int = 44, // 40-62
    val taskGap: Int = 7, // 4-14
    val taskTextSize: Float = 12.5f, // 11-14
    val showCompleted: Boolean = true,
    val launchAtLogin: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val taskSortMode: TaskSortMode = TaskSortMode.PRIORITY,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val visualStyle: ThemePreset = ThemePreset.CODEX,
    val sectionOrder: List<SectionId> = listOf(SectionId.TODAY, SectionId.CALENDAR, SectionId.LISTS),
    val backdropImageUri: String = "",
    val backdropImageName: String = "",
    val backdropOpacity: Int = 76, // 0-100
    val backdropBlur: Int = 0, // 0-24
    val backdropDim: Int = 18, // 0-80
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("dockEdge", dockEdge.name.lowercase())
        put("panelWidth", panelWidth)
        put("tabVisibility", tabVisibility.name.lowercase())
        put("tabWidth", tabWidth)
        put("handleHeight", handleHeight)
        put("handleY", handleY)
        put("motionMs", motionMs)
        put("panelRadius", panelRadius)
        put("surfaceAlpha", surfaceAlpha)
        put("taskRowHeight", taskRowHeight)
        put("taskGap", taskGap)
        put("taskTextSize", taskTextSize.toDouble())
        put("showCompleted", showCompleted)
        put("launchAtLogin", launchAtLogin)
        put("notificationsEnabled", notificationsEnabled)
        put("taskSortMode", taskSortMode.name.lowercase())
        put("theme", theme.name.lowercase())
        put("visualStyle", visualStyle.name.lowercase())
        put("sectionOrder", JSONArray().apply {
            sectionOrder.forEach { put(it.name.lowercase()) }
        })
        put("backdropImageUri", backdropImageUri)
        put("backdropImageName", backdropImageName)
        put("backdropOpacity", backdropOpacity)
        put("backdropBlur", backdropBlur)
        put("backdropDim", backdropDim)
    }

    companion object {
        private fun clamp(value: Int, lo: Int, hi: Int) = value.coerceIn(lo, hi)
        private fun clamp(value: Float, lo: Float, hi: Float) = value.coerceIn(lo, hi)

        fun fromJson(obj: JSONObject): SidebarSettings {
            val theme = when (obj.optString("theme")) {
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.LIGHT
            }
            val visualStyle = parsePreset(obj.optString("visualStyle"))
            val safePreset = if (ThemePreset.byMode[theme]?.contains(visualStyle) == true) {
                visualStyle
            } else {
                ThemePreset.byMode[theme]?.firstOrNull() ?: ThemePreset.CODEX
            }
            val sortMode = when (obj.optString("taskSortMode")) {
                "newest" -> TaskSortMode.NEWEST
                "oldest" -> TaskSortMode.OLDEST
                else -> TaskSortMode.PRIORITY
            }
            val dockEdge = when (obj.optString("dockEdge")) {
                "left" -> DockEdge.LEFT
                "top" -> DockEdge.TOP
                "bottom" -> DockEdge.TOP
                else -> DockEdge.RIGHT
            }
            val tabVisibility = when (obj.optString("tabVisibility")) {
                "hover" -> TabVisibility.HOVER
                else -> TabVisibility.ALWAYS
            }
            val sectionOrder = parseSectionOrder(obj.optJSONArray("sectionOrder"))
            val defaults = SidebarSettings()
            return SidebarSettings(
                dockEdge = dockEdge,
                panelWidth = clamp(obj.optInt("panelWidth", defaults.panelWidth), 320, 560),
                tabVisibility = tabVisibility,
                tabWidth = clamp(obj.optInt("tabWidth", defaults.tabWidth), 22, 112),
                handleHeight = clamp(obj.optInt("handleHeight", defaults.handleHeight), 56, 176),
                handleY = clamp(obj.optInt("handleY", defaults.handleY), 0, 100),
                motionMs = clamp(obj.optInt("motionMs", defaults.motionMs), 140, 360),
                panelRadius = clamp(obj.optInt("panelRadius", defaults.panelRadius), 12, 28),
                surfaceAlpha = clamp(obj.optInt("surfaceAlpha", defaults.surfaceAlpha), 58, 100),
                taskRowHeight = clamp(obj.optInt("taskRowHeight", defaults.taskRowHeight), 40, 62),
                taskGap = clamp(obj.optInt("taskGap", defaults.taskGap), 4, 14),
                taskTextSize = clamp(obj.optDouble("taskTextSize", defaults.taskTextSize.toDouble()).toFloat(), 11f, 14f),
                showCompleted = obj.optBoolean("showCompleted", defaults.showCompleted),
                launchAtLogin = obj.optBoolean("launchAtLogin", defaults.launchAtLogin),
                notificationsEnabled = obj.optBoolean("notificationsEnabled", defaults.notificationsEnabled),
                taskSortMode = sortMode,
                theme = theme,
                visualStyle = safePreset,
                sectionOrder = sectionOrder,
                backdropImageUri = obj.optString("backdropImageUri"),
                backdropImageName = obj.optString("backdropImageName"),
                backdropOpacity = clamp(obj.optInt("backdropOpacity", defaults.backdropOpacity), 0, 100),
                backdropBlur = clamp(obj.optInt("backdropBlur", defaults.backdropBlur), 0, 24),
                backdropDim = clamp(obj.optInt("backdropDim", defaults.backdropDim), 0, 80),
            )
        }

        private fun parsePreset(value: String?): ThemePreset = runCatching {
            ThemePreset.valueOf(value?.uppercase() ?: "")
        }.getOrElse { ThemePreset.CODEX }

        private fun parseSectionOrder(arr: JSONArray?): List<SectionId> {
            val incoming = mutableListOf<SectionId>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val raw = arr.optString(i).uppercase()
                    val mapped = when (raw) {
                        "MONTH" -> SectionId.CALENDAR
                        "TODAY" -> SectionId.TODAY
                        "CALENDAR" -> SectionId.CALENDAR
                        "LISTS" -> SectionId.LISTS
                        else -> null
                    }
                    if (mapped != null && mapped !in incoming) incoming.add(mapped)
                }
            }
            SectionId.values().forEach { if (it !in incoming) incoming.add(it) }
            return incoming
        }
    }
}
