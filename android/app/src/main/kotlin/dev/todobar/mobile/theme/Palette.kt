package dev.todobar.mobile.theme

import android.graphics.Color
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.model.ThemeMode
import dev.todobar.mobile.model.ThemePreset

/**
 * Material-style palette derived directly from the CSS variables in
 * `src/App.css`. Each ARGB int matches the desktop hex (alpha approximations
 * for rgba(...) values are taken at the source's surface alpha). Some
 * shadow/gradient tokens are skipped — they are baked into drawables instead.
 */
data class Palette(
    val workspaceTop: Int,
    val workspaceBottom: Int,
    val ink: Int,
    val muted: Int,
    val line: Int,
    val sidebarBg: Int,
    val sidebarBorder: Int,
    val controlBg: Int,
    val taskBg: Int,
    val taskBorder: Int,
    val accent: Int,
    val accentSoft: Int,
    val accentLine: Int,
    val amber: Int,
    val amberSoft: Int,
    val sectionBg: Int,
    val railBg: Int,
    val settingsSurface: Int,
    val settingsRow: Int,
    val surfaceHover: Int,
    val rowHover: Int,
) {
    fun withSurfaceAlpha(percent: Int): Palette {
        val alpha = (percent.coerceIn(40, 100) * 2.55f).toInt().coerceIn(0, 255)
        return copy(sidebarBg = sidebarBg.replaceAlpha(alpha))
    }

    private fun Int.replaceAlpha(alpha: Int): Int {
        return Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
    }

    companion object {
        // ──── Light presets ─────────────────────────────────────────────────
        private val STUDIO = Palette(
            workspaceTop = 0xFFF7F7F6.toInt(),
            workspaceBottom = 0xFFE6E9EC.toInt(),
            ink = 0xFF111318.toInt(),
            muted = 0xFF687383.toInt(),
            line = 0x1A18181B,
            sidebarBg = 0xFFFCFCFB.toInt(),
            sidebarBorder = 0x1718181B,
            controlBg = 0xFFF5F6F7.toInt(),
            taskBg = 0xFFFFFFFF.toInt(),
            taskBorder = 0x1618181B,
            accent = 0xFF2563EB.toInt(),
            accentSoft = 0x172563EB,
            accentLine = 0x3D2563EB,
            amber = 0xFFF08C2F.toInt(),
            amberSoft = 0x1CF08C2F,
            sectionBg = 0xB8FFFFFF.toInt(),
            railBg = 0xE0F6F7F8.toInt(),
            settingsSurface = 0xFFFCFCFB.toInt(),
            settingsRow = 0xFFF5F6F7.toInt(),
            surfaceHover = 0xFFEFF3F7.toInt(),
            rowHover = 0x0C2563EB,
        )

        private val PORCELAIN = Palette(
            workspaceTop = 0xFFFBFBFB.toInt(),
            workspaceBottom = 0xFFE7EAEE.toInt(),
            ink = 0xFF16181D.toInt(),
            muted = 0xFF66717E.toInt(),
            line = 0x1B16181D,
            sidebarBg = 0xFFFAFBFC.toInt(),
            sidebarBorder = 0x1716181D,
            controlBg = 0xFFF0F2F4.toInt(),
            taskBg = 0xFFFFFFFF.toInt(),
            taskBorder = 0x1416181D,
            accent = 0xFF1F2937.toInt(),
            accentSoft = 0x141F2937,
            accentLine = 0x331F2937,
            amber = 0xFFF08C2F.toInt(),
            amberSoft = 0x1CF08C2F,
            sectionBg = 0xC2FFFFFF.toInt(),
            railBg = 0xE6F0F2F4.toInt(),
            settingsSurface = 0xFFFAFBFC.toInt(),
            settingsRow = 0xFFF0F2F4.toInt(),
            surfaceHover = 0xFFE8EDF2.toInt(),
            rowHover = 0x0A16181D,
        )

        private val FROST = Palette(
            workspaceTop = 0xFFF5FBFF.toInt(),
            workspaceBottom = 0xFFE3EEF9.toInt(),
            ink = 0xFF102033.toInt(),
            muted = 0xFF60768D.toInt(),
            line = 0x21244C76,
            sidebarBg = 0xFFF1F8FF.toInt(),
            sidebarBorder = 0x21244C76,
            controlBg = 0xFFE8F2FB.toInt(),
            taskBg = 0xFFF8FCFF.toInt(),
            taskBorder = 0x21244C76,
            accent = 0xFF2676D9.toInt(),
            accentSoft = 0x1A2676D9,
            accentLine = 0x422676D9,
            amber = 0xFFF08C2F.toInt(),
            amberSoft = 0x1CF08C2F,
            sectionBg = 0xB8F8FCFF.toInt(),
            railBg = 0xFFE9F3FC.toInt(),
            settingsSurface = 0xFFF1F8FF.toInt(),
            settingsRow = 0xFFE8F2FB.toInt(),
            surfaceHover = 0xFFDDECFA.toInt(),
            rowHover = 0x142676D9,
        )

        private val PAPER = Palette(
            workspaceTop = 0xFFFBFAF6.toInt(),
            workspaceBottom = 0xFFECE5D8.toInt(),
            ink = 0xFF231F19.toInt(),
            muted = 0xFF756C60.toInt(),
            line = 0x1F403222,
            sidebarBg = 0xFFFCFAF4.toInt(),
            sidebarBorder = 0x1F403222,
            controlBg = 0xFFF3EFE6.toInt(),
            taskBg = 0xFFFFFDF8.toInt(),
            taskBorder = 0x1F403222,
            accent = 0xFF7C5C2F.toInt(),
            accentSoft = 0x1A7C5C2F,
            accentLine = 0x3D7C5C2F,
            amber = 0xFFB56B38.toInt(),
            amberSoft = 0x1AB56B38,
            sectionBg = 0xB8FFFDF8.toInt(),
            railBg = 0xFFF3EFE6.toInt(),
            settingsSurface = 0xFFFCFAF4.toInt(),
            settingsRow = 0xFFEFE9DC.toInt(),
            surfaceHover = 0xFFECE6DA.toInt(),
            rowHover = 0x0F7C5C2F,
        )

        private val CLAY_LIGHT = Palette(
            workspaceTop = 0xFFF7F1EC.toInt(),
            workspaceBottom = 0xFFE8D8CD.toInt(),
            ink = 0xFF261D19.toInt(),
            muted = 0xFF78655B.toInt(),
            line = 0x24573A2B,
            sidebarBg = 0xFFFAF4EF.toInt(),
            sidebarBorder = 0x24573A2B,
            controlBg = 0xFFEFE2D9.toInt(),
            taskBg = 0xFFFFF9F5.toInt(),
            taskBorder = 0x24573A2B,
            accent = 0xFF9E5D3F.toInt(),
            accentSoft = 0x1C9E5D3F,
            accentLine = 0x409E5D3F,
            amber = 0xFFB56B38.toInt(),
            amberSoft = 0x1AB56B38,
            sectionBg = 0xB3FFF9F5.toInt(),
            railBg = 0xFFF0E3DB.toInt(),
            settingsSurface = 0xFFFAF4EF.toInt(),
            settingsRow = 0xFFEFE2D9.toInt(),
            surfaceHover = 0xFFEAD9CF.toInt(),
            rowHover = 0x129E5D3F,
        )

        private val BLUEPRINT_LIGHT = Palette(
            workspaceTop = 0xFFF6F9FF.toInt(),
            workspaceBottom = 0xFFDFEAFA.toInt(),
            ink = 0xFF0F1B30.toInt(),
            muted = 0xFF5E708A.toInt(),
            line = 0x2E234F93,
            sidebarBg = 0xFFF4F8FF.toInt(),
            sidebarBorder = 0x2E234F93,
            controlBg = 0xFFEAF1FB.toInt(),
            taskBg = 0xFFF9FBFF.toInt(),
            taskBorder = 0x2E234F93,
            accent = 0xFF1D4ED8.toInt(),
            accentSoft = 0x171D4ED8,
            accentLine = 0x4D1D4ED8,
            amber = 0xFFF08C2F.toInt(),
            amberSoft = 0x1CF08C2F,
            sectionBg = 0xC7FAFCFF.toInt(),
            railBg = 0xE6E6EFFC.toInt(),
            settingsSurface = 0xFFF4F8FF.toInt(),
            settingsRow = 0xFFE6EFFC.toInt(),
            surfaceHover = 0xFFDFEAF8.toInt(),
            rowHover = 0x101D4ED8,
        )

        // ──── Dark presets ─────────────────────────────────────────────────
        private val OBSIDIAN = Palette(
            workspaceTop = 0xFF0D0D0E.toInt(),
            workspaceBottom = 0xFF18181B.toInt(),
            ink = 0xFFF4F4F5.toInt(),
            muted = 0xFF9B9CA3.toInt(),
            line = 0x14FFFFFF,
            sidebarBg = 0xFF0D0D0E.toInt(),
            sidebarBorder = 0x14FFFFFF,
            controlBg = 0xFF171719.toInt(),
            taskBg = 0xFF151517.toInt(),
            taskBorder = 0x14FFFFFF,
            accent = 0xFF74A8FF.toInt(),
            accentSoft = 0x2274A8FF,
            accentLine = 0x4774A8FF,
            amber = 0xFFFFB267.toInt(),
            amberSoft = 0x1FFFB267,
            sectionBg = 0x0CFFFFFF,
            railBg = 0x09FFFFFF,
            settingsSurface = 0xFF0D0D0E.toInt(),
            settingsRow = 0xFF18181B.toInt(),
            surfaceHover = 0xFF1A1A1D.toInt(),
            rowHover = 0x1474A8FF,
        )

        private val CARBON = Palette(
            workspaceTop = 0xFF050506.toInt(),
            workspaceBottom = 0xFF121418.toInt(),
            ink = 0xFFF7F8F9.toInt(),
            muted = 0xFFA0A6AF.toInt(),
            line = 0x18FFFFFF,
            sidebarBg = 0xFF08090B.toInt(),
            sidebarBorder = 0x16FFFFFF,
            controlBg = 0xFF14161A.toInt(),
            taskBg = 0xFF101216.toInt(),
            taskBorder = 0x16FFFFFF,
            accent = 0xFFD8DDE5.toInt(),
            accentSoft = 0x17FFFFFF,
            accentLine = 0x2EFFFFFF,
            amber = 0xFFFFB267.toInt(),
            amberSoft = 0x1FFFB267,
            sectionBg = 0x0AFFFFFF,
            railBg = 0x09FFFFFF,
            settingsSurface = 0xFF08090B.toInt(),
            settingsRow = 0xFF14161A.toInt(),
            surfaceHover = 0x14FFFFFF,
            rowHover = 0x0CFFFFFF,
        )

        private val GRAPHITE = Palette(
            workspaceTop = 0xFF07080A.toInt(),
            workspaceBottom = 0xFF191B21.toInt(),
            ink = 0xFFF6F7F8.toInt(),
            muted = 0xFF969DA8.toInt(),
            line = 0x18FFFFFF,
            sidebarBg = 0xFF0A0B0E.toInt(),
            sidebarBorder = 0x17FFFFFF,
            controlBg = 0xFF15171C.toInt(),
            taskBg = 0xFF121419.toInt(),
            taskBorder = 0x17FFFFFF,
            accent = 0xFFE8EDF4.toInt(),
            accentSoft = 0x18FFFFFF,
            accentLine = 0x2EFFFFFF,
            amber = 0xFFD6AA64.toInt(),
            amberSoft = 0x1AD6AA64,
            sectionBg = 0x0AFFFFFF,
            railBg = 0x09FFFFFF,
            settingsSurface = 0xFF0A0B0E.toInt(),
            settingsRow = 0xFF15171C.toInt(),
            surfaceHover = 0x14FFFFFF,
            rowHover = 0x0DFFFFFF,
        )

        private val MIDNIGHT = Palette(
            workspaceTop = 0xFF050815.toInt(),
            workspaceBottom = 0xFF0F1B31.toInt(),
            ink = 0xFFEEF5FF.toInt(),
            muted = 0xFF91A3BC.toInt(),
            line = 0x1F94ACCF,
            sidebarBg = 0xFF060B16.toInt(),
            sidebarBorder = 0x1F94ACCF,
            controlBg = 0xFF0D1627.toInt(),
            taskBg = 0xFF0A1220.toInt(),
            taskBorder = 0x1F94ACCF,
            accent = 0xFF8FBAFF.toInt(),
            accentSoft = 0x1F8FBAFF,
            accentLine = 0x3D8FBAFF,
            amber = 0xFFFFB267.toInt(),
            amberSoft = 0x1FFFB267,
            sectionBg = 0x0B94ACCF,
            railBg = 0x0994ACCF,
            settingsSurface = 0xFF060B16.toInt(),
            settingsRow = 0xFF0D1627.toInt(),
            surfaceHover = 0x1494ACCF,
            rowHover = 0x1360A5FA,
        )

        private val CLAY_DARK = Palette(
            workspaceTop = 0xFF120C0A.toInt(),
            workspaceBottom = 0xFF251713.toInt(),
            ink = 0xFFFBF1EC.toInt(),
            muted = 0xFFB59A8D.toInt(),
            line = 0x1CFFE0D2,
            sidebarBg = 0xFF140D0B.toInt(),
            sidebarBorder = 0x18FFE0D2,
            controlBg = 0xFF211511.toInt(),
            taskBg = 0xFF1D130F.toInt(),
            taskBorder = 0x1AFFE0D2,
            accent = 0xFFF2A47F.toInt(),
            accentSoft = 0x1FF2A47F,
            accentLine = 0x40F2A47F,
            amber = 0xFFFFB267.toInt(),
            amberSoft = 0x1FFFB267,
            sectionBg = 0x0BFFE0D2,
            railBg = 0x09FFE0D2,
            settingsSurface = 0xFF140D0B.toInt(),
            settingsRow = 0xFF211511.toInt(),
            surfaceHover = 0x14FFE0D2,
            rowHover = 0x17C77956,
        )

        private val BLUEPRINT_DARK = Palette(
            workspaceTop = 0xFF07101E.toInt(),
            workspaceBottom = 0xFF101B30.toInt(),
            ink = 0xFFEEF5FF.toInt(),
            muted = 0xFF94ACCF.toInt(),
            line = 0x1F74A8FF,
            sidebarBg = 0xFF08111F.toInt(),
            sidebarBorder = 0x1F74A8FF,
            controlBg = 0x1274A8FF,
            taskBg = 0x1074A8FF,
            taskBorder = 0x1F74A8FF,
            accent = 0xFF74A8FF.toInt(),
            accentSoft = 0x2074A8FF,
            accentLine = 0x4074A8FF,
            amber = 0xFFFFB267.toInt(),
            amberSoft = 0x1FFFB267,
            sectionBg = 0x0F74A8FF,
            railBg = 0x0B74A8FF,
            settingsSurface = 0xFF08111F.toInt(),
            settingsRow = 0x1474A8FF,
            surfaceHover = 0x1874A8FF,
            rowHover = 0x1474A8FF,
        )

        fun resolve(settings: SidebarSettings): Palette {
            val base = when (settings.theme) {
                ThemeMode.LIGHT -> when (settings.visualStyle) {
                    ThemePreset.PORCELAIN -> PORCELAIN
                    ThemePreset.FROST -> FROST
                    ThemePreset.PAPER -> PAPER
                    ThemePreset.CLAY -> CLAY_LIGHT
                    ThemePreset.BLUEPRINT -> BLUEPRINT_LIGHT
                    else -> STUDIO
                }
                ThemeMode.DARK -> when (settings.visualStyle) {
                    ThemePreset.CARBON -> CARBON
                    ThemePreset.GRAPHITE -> GRAPHITE
                    ThemePreset.MIDNIGHT -> MIDNIGHT
                    ThemePreset.CLAY -> CLAY_DARK
                    ThemePreset.BLUEPRINT -> BLUEPRINT_DARK
                    else -> OBSIDIAN
                }
            }
            return base.withSurfaceAlpha(settings.surfaceAlpha)
        }
    }
}
