package dev.todobar.mobile.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import dev.todobar.mobile.R

/** Resource handle for the icons we use across the sidebar. */
enum class Icon(val resId: Int) {
    TODAY(R.drawable.ic_check),
    CALENDAR(R.drawable.ic_calendar),
    LISTS(R.drawable.ic_lists),
    SETTINGS(R.drawable.ic_settings),
    CLOSE(R.drawable.ic_close),
    PLUS(R.drawable.ic_plus),
    EDIT(R.drawable.ic_edit),
    DELETE(R.drawable.ic_trash),
    BROOM(R.drawable.ic_broom),
    BELL(R.drawable.ic_bell),
    PIN(R.drawable.ic_pin),
    THEME(R.drawable.ic_theme),
    SUN(R.drawable.ic_sun),
    MOON(R.drawable.ic_moon),
    CHEVRON_LEFT(R.drawable.ic_chevron_left),
    CHEVRON_RIGHT(R.drawable.ic_chevron_right),
    CHEVRON_DOWN(R.drawable.ic_chevron_down),
    SNOOZE(R.drawable.ic_snooze),
    SOURCE(R.drawable.ic_source);

    fun load(context: Context): Drawable? = ContextCompat.getDrawable(context, resId)
}
