package dev.todobar.mobile.ui.views

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.theme.Palette

/** Common contract for a navigable section (Today/Calendar/Lists/Settings). */
abstract class SectionView(context: Context) : LinearLayout(context) {
    abstract fun applyTheme(palette: Palette, settings: SidebarSettings)
    abstract fun refresh()
    open fun onShown() {}
    open fun onHidden() {}
}
