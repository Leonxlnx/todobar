package dev.todobar.mobile.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.R
import dev.todobar.mobile.model.SectionId
import dev.todobar.mobile.model.SidebarSettings
import dev.todobar.mobile.model.ThemeMode
import dev.todobar.mobile.store.Store
import dev.todobar.mobile.theme.Palette
import dev.todobar.mobile.ui.Drawables.dp
import dev.todobar.mobile.ui.views.CalendarView
import dev.todobar.mobile.ui.views.ListsView
import dev.todobar.mobile.ui.views.SectionView
import dev.todobar.mobile.ui.views.SettingsView
import dev.todobar.mobile.ui.views.TodayView

/**
 * Hosts the expanded sidebar panel. The desktop layout — icon rail on the
 * leading edge, header on top of the content column, four views (Today,
 * Calendar, Lists, Settings) — is rebuilt in code so theming and the 12 visual
 * presets can be applied at runtime.
 */
class SidebarOverlayController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val overlayType: Int,
    private val onDismiss: () -> Unit,
    private val onRequestBackdropPicker: (() -> Unit)? = null,
) {

    private val store = Store.get(context)
    private val storeListener: () -> Unit = {
        rootView?.post { applyState() }
        Unit
    }

    private var rootView: FrameLayout? = null
    private var panel: LinearLayout? = null
    private var scrim: View? = null
    private var content: FrameLayout? = null
    private var headerTitle: TextView? = null
    private var headerStatus: TextView? = null
    private var rail: LinearLayout? = null

    private val sectionViews = mutableMapOf<SectionId, SectionView>()
    private var settingsView: SettingsView? = null
    private var activeSection: SectionId = SectionId.TODAY
    private var settingsOpen: Boolean = false

    fun show() {
        if (rootView != null) return
        val ctx = context
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

        val root = FrameLayout(ctx)
        rootView = root

        val scrimView = View(ctx).apply {
            setBackgroundColor(Color.argb(150, 0, 0, 0))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        scrim = scrimView
        scrimView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) dismiss()
            true
        }
        root.addView(scrimView)

        val panelView = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        panel = panelView
        // Panel sits at the dock edge (right by default). Width is bounded by
        // device width minus 16dp breathing room.
        val viewportWidth = ctx.resources.displayMetrics.widthPixels
        val desiredWidth = dp(ctx, store.settings().panelWidth.toFloat())
        val panelWidth = desiredWidth.coerceAtMost(viewportWidth - dp(ctx, 16f))
        val panelParams = FrameLayout.LayoutParams(
            panelWidth,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        panelParams.gravity = when (store.settings().dockEdge) {
            dev.todobar.mobile.model.DockEdge.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            dev.todobar.mobile.model.DockEdge.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            else -> Gravity.END or Gravity.CENTER_VERTICAL
        }
        panelView.layoutParams = panelParams
        // Swallow taps inside the panel so they don't bubble to the scrim.
        panelView.setOnTouchListener { _, _ -> false }
        root.addView(panelView)

        buildRail(panelView)
        buildContent(panelView)

        runCatching { windowManager.addView(root, params) }
            .onFailure {
                rootView = null
                onDismiss()
                return
            }

        store.addListener(storeListener)
        applyState()

        // Slide-in animation matching desktop motion-ms
        val travel = panelWidth.toFloat()
        panelView.translationX = travel
        val motion = store.settings().motionMs.toLong()
        panelView.animate()
            .translationX(0f)
            .setDuration(motion)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        scrimView.alpha = 0f
        scrimView.animate().alpha(1f).setDuration(motion - 50).start()
    }

    private fun buildRail(parent: LinearLayout) {
        val ctx = context
        val railView = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 56f), LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding(dp(ctx, 8f), dp(ctx, 18f), dp(ctx, 8f), dp(ctx, 18f))
        }
        rail = railView
        parent.addView(railView)
    }

    private fun buildContent(parent: LinearLayout) {
        val ctx = context
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f,
            )
        }
        parent.addView(column)

        // Header row: title + status + actions
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 14f), dp(ctx, 14f), dp(ctx, 8f), dp(ctx, 6f))
        }
        val titleBlock = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val title = TextView(ctx).apply {
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val status = TextView(ctx).apply {
            textSize = 11.5f
        }
        titleBlock.addView(title)
        titleBlock.addView(status)
        header.addView(titleBlock)
        headerTitle = title
        headerStatus = status

        // Theme toggle button
        val themeBtn = Views.iconButton(ctx, Icon.THEME, Color.WHITE, sizeDp = 34) {
            val cur = store.settings()
            val nextTheme = if (cur.theme == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
            val allowed = dev.todobar.mobile.model.ThemePreset.byMode[nextTheme].orEmpty()
            val nextStyle = if (allowed.contains(cur.visualStyle)) cur.visualStyle else (allowed.firstOrNull() ?: cur.visualStyle)
            store.saveSettings(cur.copy(theme = nextTheme, visualStyle = nextStyle))
        }
        header.addView(themeBtn)
        themeBtn.tag = "themeBtn"

        val settingsBtn = Views.iconButton(ctx, Icon.SETTINGS, Color.WHITE, sizeDp = 34) {
            settingsOpen = !settingsOpen
            applyState()
        }
        header.addView(settingsBtn)
        settingsBtn.tag = "settingsBtn"

        val closeBtn = Views.iconButton(ctx, Icon.CLOSE, Color.WHITE, sizeDp = 34) { dismiss() }
        header.addView(closeBtn)
        closeBtn.tag = "closeBtn"

        column.addView(header)

        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 1f))
        }
        column.addView(divider)
        divider.tag = "headerDivider"

        val container = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f,
            )
        }
        column.addView(container)
        content = container

        // Bottom-attached clear-completed action and "footer status strip"
        val footer = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 14f), dp(ctx, 10f), dp(ctx, 14f), dp(ctx, 12f))
        }
        val footerStatus = TextView(ctx).apply {
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        footer.addView(footerStatus)
        val clearBtn = Views.iconButton(ctx, Icon.BROOM, Color.WHITE, sizeDp = 34) {
            store.clearCompletedToday()
        }
        footer.addView(clearBtn)
        column.addView(footer)
        footer.tag = "footer"
        footerStatus.tag = "footerStatus"
        clearBtn.tag = "clearBtn"
    }

    private fun applyState() {
        val ctx = context
        val settings = store.settings()
        val palette = Palette.resolve(settings)
        val root = rootView ?: return
        val panelView = panel ?: return

        // Panel surface
        panelView.background = makePanelBackground(palette, settings)

        // Re-layout panel width / edge in case settings changed
        val viewportWidth = ctx.resources.displayMetrics.widthPixels
        val desiredWidth = dp(ctx, settings.panelWidth.toFloat())
        val panelWidth = desiredWidth.coerceAtMost(viewportWidth - dp(ctx, 16f))
        val params = panelView.layoutParams as FrameLayout.LayoutParams
        if (params.width != panelWidth) {
            params.width = panelWidth
            panelView.layoutParams = params
        }
        val gravity = when (settings.dockEdge) {
            dev.todobar.mobile.model.DockEdge.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
            dev.todobar.mobile.model.DockEdge.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            else -> Gravity.END or Gravity.CENTER_VERTICAL
        }
        if (params.gravity != gravity) {
            params.gravity = gravity
            panelView.layoutParams = params
        }

        // Header colors
        headerTitle?.setTextColor(palette.ink)
        headerStatus?.setTextColor(palette.muted)

        val today = store.today()
        val open = today.count { !it.done }
        val complete = today.count { it.done }
        headerTitle?.text = if (settingsOpen) "Settings" else activeSection.title
        val lists = store.customLists()
        headerStatus?.text = "$open open · $complete done · ${lists.size} lists"

        // Theme + close + settings button tints
        val headerView = (panelView.getChildAt(1) as LinearLayout).getChildAt(0) as LinearLayout
        for (i in 0 until headerView.childCount) {
            val child = headerView.getChildAt(i)
            if (child is android.widget.ImageButton) {
                child.colorFilter = PorterDuffColorFilter(palette.ink, PorterDuff.Mode.SRC_IN)
            }
        }
        // Footer status + clear button colors
        val column = panelView.getChildAt(1) as LinearLayout
        val footer = column.findViewWithTag<LinearLayout>("footer")
        footer?.let { row ->
            val fs = row.findViewWithTag<TextView>("footerStatus")
            fs?.setTextColor(palette.muted)
            fs?.text = "Tap a row to edit · long-press to drag (coming soon)"
            val cb = row.findViewWithTag<android.widget.ImageButton>("clearBtn")
            cb?.colorFilter = PorterDuffColorFilter(palette.muted, PorterDuff.Mode.SRC_IN)
        }
        // Header divider color
        val headerDivider = column.findViewWithTag<View>("headerDivider")
        headerDivider?.setBackgroundColor(palette.line)
        // Rail background
        rail?.setBackgroundColor(palette.railBg)

        // Rebuild rail with icons from sectionOrder + settings
        buildRailIcons(settings, palette)

        // Build / refresh content
        if (settingsOpen) {
            mountSettings(palette, settings)
        } else {
            mountSection(activeSection, palette, settings)
        }
    }

    private fun makePanelBackground(palette: Palette, settings: SidebarSettings): android.graphics.drawable.Drawable {
        val ctx = context
        val surface = Drawables.panelSurface(ctx, palette, settings.panelRadius)
        val backdropUri = settings.backdropImageUri
        if (backdropUri.isBlank()) return surface
        val bitmap = runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(backdropUri))?.use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
        }.getOrNull() ?: return surface
        val bitmapDrawable = BitmapDrawable(ctx.resources, bitmap).apply {
            alpha = (settings.backdropOpacity * 2.55f).toInt().coerceIn(0, 255)
            gravity = Gravity.CENTER
        }
        val dim = GradientDrawable().apply {
            setColor(Color.argb((settings.backdropDim * 2.55f).toInt().coerceIn(0, 255), 0, 0, 0))
            cornerRadius = dp(ctx, settings.panelRadius.toFloat()).toFloat()
        }
        return LayerDrawable(arrayOf(surface, bitmapDrawable, dim))
    }

    private fun buildRailIcons(settings: SidebarSettings, palette: Palette) {
        val rail = rail ?: return
        rail.removeAllViews()
        val ctx = context

        settings.sectionOrder.forEach { section ->
            val icon = when (section) {
                SectionId.TODAY -> Icon.TODAY
                SectionId.CALENDAR -> Icon.CALENDAR
                SectionId.LISTS -> Icon.LISTS
            }
            val active = !settingsOpen && activeSection == section
            val btn = Views.iconButton(ctx, icon, if (active) palette.accent else palette.muted, sizeDp = 40) {
                activeSection = section
                settingsOpen = false
                applyState()
            }
            btn.background = if (active) {
                Drawables.roundedSurface(ctx, palette.accentSoft, 12f, palette.accentLine)
            } else null
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 44f),
            )
            params.setMargins(0, 0, 0, dp(ctx, 8f))
            btn.layoutParams = params
            rail.addView(btn)
        }

        // Spacer to push the settings icon to the bottom
        val spacer = View(ctx)
        spacer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        rail.addView(spacer)

        val activeSettings = settingsOpen
        val settingsBtn = Views.iconButton(
            ctx, Icon.SETTINGS,
            if (activeSettings) palette.accent else palette.muted,
            sizeDp = 40,
        ) {
            settingsOpen = !settingsOpen
            applyState()
        }
        settingsBtn.background = if (activeSettings) {
            Drawables.roundedSurface(ctx, palette.accentSoft, 12f, palette.accentLine)
        } else null
        val sParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 44f))
        settingsBtn.layoutParams = sParams
        rail.addView(settingsBtn)
    }

    private fun mountSection(section: SectionId, palette: Palette, settings: SidebarSettings) {
        val container = content ?: return
        val ctx = context
        val view = sectionViews.getOrPut(section) {
            when (section) {
                SectionId.TODAY -> TodayView(ctx, store, palette, settings)
                SectionId.CALENDAR -> CalendarView(ctx, store, palette, settings)
                SectionId.LISTS -> ListsView(ctx, store, palette, settings)
            }
        }
        if (container.indexOfChild(view) == -1) {
            container.removeAllViews()
            container.addView(view)
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        view.applyTheme(palette, settings)
    }

    private fun mountSettings(palette: Palette, settings: SidebarSettings) {
        val container = content ?: return
        val ctx = context
        val view = settingsView ?: SettingsView(ctx, store, palette, settings) {
            onRequestBackdropPicker?.invoke()
        }.also { settingsView = it }
        if (container.indexOfChild(view) == -1) {
            container.removeAllViews()
            container.addView(view)
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        view.applyTheme(palette, settings)
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        rootView?.post { applyState() }
    }

    fun dismiss() {
        val root = rootView ?: return
        val panelView = panel
        val scrimView = scrim
        store.removeListener(storeListener)
        val motion = store.settings().motionMs.toLong()
        scrimView?.animate()?.alpha(0f)?.setDuration(motion - 50)?.start()
        if (panelView != null) {
            val travel = panelView.width.toFloat()
            panelView.animate()
                .translationX(travel)
                .setDuration(motion)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { teardown() }
                .start()
        } else teardown()
    }

    private fun teardown() {
        rootView?.let { runCatching { windowManager.removeView(it) } }
        rootView = null
        panel = null
        scrim = null
        content = null
        sectionViews.clear()
        settingsView = null
        onDismiss()
    }
}
