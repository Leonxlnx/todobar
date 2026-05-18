package dev.todobar.mobile.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.Outline
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import dev.todobar.mobile.R
import dev.todobar.mobile.model.DockEdge
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
 * Hosts the expanded sidebar panel. The desktop layout is mirrored 1:1 —
 *   ┌─────────────────────────────────────────┬──┐
 *   │ Todobar (app-lockup)            close │R│
 *   │ ─── focus strip ────                   │A│
 *   │ section-heading (Today · counts)       │I│
 *   │ quick-add row                          │L│
 *   │ task list / pinned lists / …           │ │
 *   └─────────────────────────────────────────┴──┘
 *
 * Built programmatically so the 12 desktop visual presets and live theme
 * swaps work without static XML.
 */
class SidebarOverlayController(
    serviceContext: Context,
    private val windowManager: WindowManager,
    private val overlayType: Int,
    private val onDismiss: () -> Unit,
    private val onRequestBackdropPicker: (() -> Unit)? = null,
) {

    /**
     * Wrap the Service context in a ContextThemeWrapper using the app's
     * Material3 theme. Without this, EditText/SeekBar/AlertDialog created
     * from a Service context can throw because they expect Material
     * attributes (`?attr/colorControlActivated` etc).
     */
    private val context: Context = ContextThemeWrapper(serviceContext, R.style.Theme_Todobar)

    private val store = Store.get(serviceContext)
    private val storeListener: () -> Unit = {
        rootView?.post { applyState() }
        Unit
    }

    private var rootView: FrameLayout? = null
    private var panel: LinearLayout? = null
    private var scrim: View? = null

    private var headerLockupTitle: TextView? = null
    private var headerLockupSub: TextView? = null
    private var headerCloseBtn: ImageButton? = null
    private var focusStripContainer: View? = null
    private var sectionContent: FrameLayout? = null

    private var railColumn: LinearLayout? = null

    private var footerStatus: TextView? = null
    private var footerClearBtn: ImageButton? = null

    private val sectionViews = mutableMapOf<SectionId, SectionView>()
    private var settingsView: SettingsView? = null
    private var activeSection: SectionId = SectionId.TODAY
    private var settingsOpen: Boolean = false

    fun show(): Boolean {
        if (rootView != null) return true
        val ctx = context
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                0,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                @Suppress("DEPRECATION")
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            val root = FrameLayout(ctx)
            rootView = root

            // Lighter scrim than before — the previous 50% dim made the
            // rounded corners stand out as dark patches against the home
            // screen. The desktop sidebar has no scrim at all; this gives
            // just enough contrast so the panel reads as a foreground card.
            val scrimView = View(ctx).apply {
                setBackgroundColor(Color.argb(80, 0, 0, 0))
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
            val viewportWidth = ctx.resources.displayMetrics.widthPixels
            val desiredWidth = dp(ctx, store.settings().panelWidth.toFloat())
            val panelWidth = desiredWidth.coerceAtMost(viewportWidth - dp(ctx, 16f))
            val panelParams = FrameLayout.LayoutParams(
                panelWidth,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            // Panel is full-height and snapped flush to the dock edge — no
            // CENTER_VERTICAL because that would leave a strip of scrim above
            // and below it where the rounded corners bleed transparent.
            panelParams.gravity = when (store.settings().dockEdge) {
                DockEdge.LEFT -> Gravity.START or Gravity.TOP
                DockEdge.TOP -> Gravity.TOP or Gravity.START
                else -> Gravity.END or Gravity.TOP
            }
            panelView.layoutParams = panelParams
            // Swallow stray taps inside the panel so empty areas don't fall
            // through to the scrim (which would dismiss the sidebar).
            panelView.isClickable = true
            panelView.isFocusable = true
            // Hardware accelerated soft shadow under the panel — the desktop
            // applies `-28px 0 72px rgba(19,25,35,0.16)` so we approximate it
            // with a 22dp elevation + an outline that round-rects the inner
            // edge. The outer corners are at the screen edge so the matching
            // shadow on that side is clipped automatically.
            panelView.elevation = dp(ctx, 22f).toFloat()
            root.addView(panelView)

            buildContent(panelView)
            buildRail(panelView)

            val added = runCatching { windowManager.addView(root, params) }
            if (added.isFailure) {
                Log.e(TAG, "panel addView failed", added.exceptionOrNull())
                rootView = null
                panel = null
                scrim = null
                return false
            }

            store.addListener(storeListener)
            applyState()

            // Pre-warm the view tree so the open animation starts on a fully
            // measured panel — otherwise the first frame is the cost of
            // inflating ~70 child views and the slide stutters visibly.
            panelView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            scrimView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            val motionAxis = motionAxisFor(store.settings().dockEdge, panelWidth.toFloat(), root.height.toFloat())
            if (motionAxis.horizontal) {
                panelView.translationX = motionAxis.closedOffset
            } else {
                panelView.translationY = motionAxis.closedOffset
            }
            val motion = store.settings().motionMs.toLong().coerceAtLeast(180L)
            panelView.animate()
                .translationX(0f)
                .translationY(0f)
                .setDuration(motion)
                .setInterpolator(MOTION_DECELERATE)
                .withEndAction { panelView.setLayerType(View.LAYER_TYPE_NONE, null) }
                .start()
            scrimView.alpha = 0f
            scrimView.animate()
                .alpha(1f)
                .setDuration(motion)
                .setInterpolator(MOTION_DECELERATE)
                .withEndAction { scrimView.setLayerType(View.LAYER_TYPE_NONE, null) }
                .start()
            return true
        } catch (t: Throwable) {
            // Service crashes are silent on the user device — log and clean up
            // instead of taking the bubble down with us.
            Log.e(TAG, "panel show failed", t)
            store.removeListener(storeListener)
            runCatching { rootView?.let(windowManager::removeView) }
            rootView = null
            panel = null
            scrim = null
            return false
        }
    }

    // ─── Content (left column of the panel) ────────────────────────────────
    private fun buildContent(parent: LinearLayout) {
        val ctx = context

        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setPadding(dp(ctx, 20f), dp(ctx, 16f), dp(ctx, 14f), dp(ctx, 12f))
        }
        parent.addView(column)

        // ── Header: app-lockup ┄┄ close ───────────────────────────────────
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val lockup = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val iconBadge = View(ctx).apply {
            background = makeAppIconBadge()
        }
        val badgeParams = LinearLayout.LayoutParams(dp(ctx, 32f), dp(ctx, 32f))
        iconBadge.layoutParams = badgeParams
        // Render a checkmark on top of the badge using a FrameLayout.
        val badgeFrame = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 32f), dp(ctx, 32f))
        }
        badgeFrame.addView(iconBadge, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        val badgeCheck = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(ctx, 7f))
        }
        badgeFrame.addView(badgeCheck, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        ))
        lockup.addView(badgeFrame)
        val lockupText = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.setMargins(dp(ctx, 10f), 0, 0, 0)
            layoutParams = p
        }
        val titleTv = TextView(ctx).apply {
            text = "Todobar"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val subTv = TextView(ctx).apply {
            text = "Today · 0 open · 0 done"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        lockupText.addView(titleTv)
        lockupText.addView(subTv)
        lockup.addView(lockupText)
        header.addView(lockup)
        headerLockupTitle = titleTv
        headerLockupSub = subTv

        val closeBtn = makeChromeButton(Icon.CLOSE) { dismiss() }
        header.addView(closeBtn)
        headerCloseBtn = closeBtn

        column.addView(header)
        column.addView(spacer(8))

        // ── Focus strip ───────────────────────────────────────────────────
        val strip = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 6f))
        }
        focusStripContainer = strip
        column.addView(strip)
        column.addView(spacer(10))

        // ── View container (Today / Calendar / Lists / Settings) ──────────
        val container = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        column.addView(container)
        sectionContent = container

        // ── Footer status strip + clear-completed action ──────────────────
        val footer = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.setMargins(0, dp(ctx, 8f), 0, 0)
            layoutParams = p
        }
        val fs = TextView(ctx).apply {
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        footer.addView(fs)
        val clearBtn = makeChromeButton(Icon.BROOM) { store.clearCompletedToday() }
        footer.addView(clearBtn)
        column.addView(footer)
        footerStatus = fs
        footerClearBtn = clearBtn
    }

    // ─── Rail (right-edge column of the panel) ─────────────────────────────
    private fun buildRail(parent: LinearLayout) {
        val ctx = context
        val rail = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 50f), LinearLayout.LayoutParams.MATCH_PARENT)
            setPadding(dp(ctx, 6f), dp(ctx, 14f), dp(ctx, 6f), dp(ctx, 13f))
        }
        parent.addView(rail)
        railColumn = rail
    }

    private fun rebuildRail(settings: SidebarSettings, palette: Palette) {
        val rail = railColumn ?: return
        val ctx = context
        rail.removeAllViews()
        // Stack of section buttons
        val stack = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val stackParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        stack.layoutParams = stackParams
        settings.sectionOrder.forEach { id ->
            val iconRes = when (id) {
                SectionId.TODAY -> R.drawable.ic_today
                SectionId.CALENDAR -> R.drawable.ic_calendar
                SectionId.LISTS -> R.drawable.ic_lists
            }
            val active = !settingsOpen && activeSection == id
            val btn = makeRailButton(iconRes, active, palette) {
                activeSection = id
                settingsOpen = false
                applyState()
            }
            val p = LinearLayout.LayoutParams(dp(ctx, 38f), dp(ctx, 38f))
            p.setMargins(0, 0, 0, dp(ctx, 7f))
            stack.addView(btn, p)
        }
        rail.addView(stack)
        // Spacer pushes settings to bottom
        val gap = View(ctx)
        gap.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        rail.addView(gap)

        val sb = makeRailButton(R.drawable.ic_settings, settingsOpen, palette) {
            settingsOpen = !settingsOpen
            applyState()
        }
        val sbParams = LinearLayout.LayoutParams(dp(ctx, 38f), dp(ctx, 38f))
        rail.addView(sb, sbParams)
    }

    /** A 38x38 icon-only button in the right-edge rail. */
    private fun makeRailButton(
        iconRes: Int,
        active: Boolean,
        palette: Palette,
        onClick: () -> Unit,
    ): ImageButton {
        val ctx = context
        val tint = if (active) palette.accent else palette.muted
        val btn = ImageButton(ctx)
        btn.setImageResource(iconRes)
        btn.imageTintList = android.content.res.ColorStateList.valueOf(tint)
        btn.scaleType = ImageView.ScaleType.FIT_CENTER
        btn.setPadding(dp(ctx, 8f))
        btn.background = Drawables.railButton(
            context = ctx,
            palette = palette,
            themeMode = Drawables.inferTheme(palette),
            active = active,
        )
        // Slight elevation for active state so the active button visibly
        // pops out of the rail — matches `.sidebar-rail button.is-active`
        // shadow on desktop.
        btn.elevation = if (active) dp(ctx, 2f).toFloat() else 0f
        btn.setOnClickListener { onClick() }
        return btn
    }

    // ─── Header chrome button (close, broom) — looks like desktop ──────────
    private fun makeChromeButton(icon: Icon, onClick: () -> Unit): ImageButton {
        val ctx = context
        val btn = ImageButton(ctx)
        btn.setImageDrawable(icon.load(ctx))
        btn.scaleType = ImageView.ScaleType.FIT_CENTER
        val p = LinearLayout.LayoutParams(dp(ctx, 30f), dp(ctx, 30f))
        btn.layoutParams = p
        btn.setPadding(dp(ctx, 6f))
        btn.setOnClickListener { onClick() }
        return btn
    }

    // ─── App-icon badge: dark gradient square with subtle inner highlight ──
    private fun makeAppIconBadge(): android.graphics.drawable.Drawable {
        val outer = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 11f).toFloat()
            colors = intArrayOf(0xFF202126.toInt(), 0xFF0C0D10.toInt())
            orientation = GradientDrawable.Orientation.TL_BR
            setStroke(dp(context, 1f), 0x24FFFFFF)
        }
        return outer
    }

    private fun spacer(heightDp: Int): View {
        val v = View(context)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(context, heightDp.toFloat()),
        )
        return v
    }

    // ─── State application ─────────────────────────────────────────────────
    private fun applyState() {
        val ctx = context
        val settings = store.settings()
        val palette = Palette.resolve(settings)
        val panelView = panel ?: return

        // Panel surface + gravity
        panelView.background = makePanelBackground(palette, settings)
        // Update the elevation outline so the soft shadow follows the panel's
        // current size + corner radius. We use an asymmetric round-rect: only
        // the inner edge is rounded — the outer edge sits flush at the screen
        // boundary and its shadow is clipped automatically.
        val r = dp(ctx, settings.panelRadius.toFloat()).toFloat()
        panelView.clipToOutline = false
        panelView.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val w = view.width
                val h = view.height
                if (w <= 0 || h <= 0) return
                when (settings.dockEdge) {
                    DockEdge.RIGHT -> outline.setRoundRect(-r.toInt(), 0, w, h, r)
                    DockEdge.LEFT -> outline.setRoundRect(0, 0, w + r.toInt(), h, r)
                    DockEdge.TOP -> outline.setRoundRect(0, -r.toInt(), w, h, r)
                }
            }
        }
        val viewportWidth = ctx.resources.displayMetrics.widthPixels
        val desiredWidth = dp(ctx, settings.panelWidth.toFloat())
        val panelWidth = desiredWidth.coerceAtMost(viewportWidth - dp(ctx, 16f))
        val params = panelView.layoutParams as FrameLayout.LayoutParams
        if (params.width != panelWidth) {
            params.width = panelWidth
            panelView.layoutParams = params
        }
        val gravity = when (settings.dockEdge) {
            DockEdge.LEFT -> Gravity.START or Gravity.TOP
            DockEdge.TOP -> Gravity.TOP or Gravity.START
            else -> Gravity.END or Gravity.TOP
        }
        if (params.gravity != gravity) {
            params.gravity = gravity
            panelView.layoutParams = params
        }

        // Header lockup
        headerLockupTitle?.setTextColor(palette.ink)
        headerLockupSub?.setTextColor(palette.muted)
        val today = store.today()
        val open = today.count { !it.done }
        val complete = today.count { it.done }
        val total = today.size
        headerLockupSub?.text = "Today · $open open · $complete done"

        // Close button tint
        headerCloseBtn?.let {
            it.colorFilter = PorterDuffColorFilter(palette.muted, PorterDuff.Mode.SRC_IN)
            it.background = Drawables.chromeButton(
                context = ctx,
                palette = palette,
                themeMode = Drawables.inferTheme(palette),
                radiusDp = 10f,
            )
        }

        // Focus strip
        val track = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(ctx, 999f).toFloat()
            setColor(palette.controlBg)
            setStroke(dp(ctx, 1f), palette.taskBorder)
        }
        val progress = if (total == 0) 0f else (complete.toFloat() / total).coerceIn(0f, 1f)
        if (progress <= 0f) {
            focusStripContainer?.background = track
        } else {
            val fill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(ctx, 999f).toFloat()
                setColor(palette.accent)
            }
            val layer = LayerDrawable(arrayOf(track, fill))
            val inset = ((1f - progress) * 1000f).toInt()
            layer.setLayerInset(1, 0, 0, inset, 0)
            focusStripContainer?.background = layer
        }

        // Footer
        footerStatus?.setTextColor(palette.muted)
        footerStatus?.text = "Tap a task to edit · long-press to expand"
        footerClearBtn?.let {
            it.colorFilter = PorterDuffColorFilter(palette.muted, PorterDuff.Mode.SRC_IN)
            it.background = Drawables.chromeButton(
                context = ctx,
                palette = palette,
                themeMode = Drawables.inferTheme(palette),
                radiusDp = 10f,
            )
        }

        // Rail
        railColumn?.let {
            val gradTop = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x36FFFFFF, Color.TRANSPARENT),
            )
            val rail = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(palette.railBg)
            }
            val border = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.TRANSPARENT)
                setStroke(dp(ctx, 1f), palette.line)
            }
            it.background = if (settings.theme == ThemeMode.LIGHT) {
                LayerDrawable(arrayOf(rail, gradTop, border))
            } else {
                LayerDrawable(arrayOf(rail, border))
            }
        }
        rebuildRail(settings, palette)

        // Section content
        if (settingsOpen) mountSettings(palette, settings) else mountSection(activeSection, palette, settings)
    }

    private fun makePanelBackground(palette: Palette, settings: SidebarSettings): android.graphics.drawable.Drawable {
        val ctx = context
        val surface = Drawables.panelSurface(
            context = ctx,
            palette = palette,
            themeMode = Drawables.inferTheme(palette),
            dockEdge = settings.dockEdge,
            radiusDp = settings.panelRadius,
        )
        val backdropUri = settings.backdropImageUri
        if (backdropUri.isBlank()) return surface
        val bitmap = runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(backdropUri))?.use {
                android.graphics.BitmapFactory.decodeStream(it)
            }
        }.getOrNull() ?: return surface
        val bd = BitmapDrawable(ctx.resources, bitmap).apply {
            alpha = (settings.backdropOpacity * 2.55f).toInt().coerceIn(0, 255)
            gravity = Gravity.CENTER
        }
        val dim = GradientDrawable().apply {
            setColor(Color.argb((settings.backdropDim * 2.55f).toInt().coerceIn(0, 255), 0, 0, 0))
            cornerRadius = dp(ctx, settings.panelRadius.toFloat()).toFloat()
        }
        return LayerDrawable(arrayOf(surface, bd, dim))
    }

    private fun mountSection(section: SectionId, palette: Palette, settings: SidebarSettings) {
        val container = sectionContent ?: return
        val ctx = context
        val view = try {
            sectionViews.getOrPut(section) {
                when (section) {
                    SectionId.TODAY -> TodayView(ctx, store, palette, settings)
                    SectionId.CALENDAR -> CalendarView(ctx, store, palette, settings)
                    SectionId.LISTS -> ListsView(ctx, store, palette, settings)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "section build failed: $section", t)
            return
        }
        if (container.indexOfChild(view) == -1) {
            container.removeAllViews()
            container.addView(view)
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        runCatching { view.applyTheme(palette, settings) }
            .onFailure { Log.e(TAG, "section applyTheme failed: $section", it) }
    }

    private fun mountSettings(palette: Palette, settings: SidebarSettings) {
        val container = sectionContent ?: return
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
        runCatching { view.applyTheme(palette, settings) }
            .onFailure { Log.e(TAG, "settings applyTheme failed", it) }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        rootView?.post { applyState() }
    }

    fun dismiss() {
        val root = rootView ?: return
        val panelView = panel
        val scrimView = scrim
        store.removeListener(storeListener)
        val motion = store.settings().motionMs.toLong().coerceAtLeast(180L)
        scrimView?.let {
            it.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            it.animate()
                .alpha(0f)
                .setDuration(motion)
                .setInterpolator(MOTION_ACCELERATE)
                .start()
        }
        if (panelView != null) {
            panelView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            val motionAxis = motionAxisFor(
                store.settings().dockEdge,
                panelView.width.toFloat(),
                root.height.toFloat(),
            )
            panelView.animate()
                .translationX(if (motionAxis.horizontal) motionAxis.closedOffset else 0f)
                .translationY(if (motionAxis.horizontal) 0f else motionAxis.closedOffset)
                .setDuration(motion)
                .setInterpolator(MOTION_ACCELERATE)
                .withEndAction { teardown() }
                .start()
        } else teardown()
    }

    private fun teardown() {
        rootView?.let { runCatching { windowManager.removeView(it) } }
        rootView = null
        panel = null
        scrim = null
        sectionContent = null
        sectionViews.clear()
        settingsView = null
        onDismiss()
    }

    private data class PanelMotionAxis(
        val closedOffset: Float,
        val horizontal: Boolean,
    )

    private fun motionAxisFor(edge: DockEdge, widthPx: Float, heightPx: Float): PanelMotionAxis =
        when (edge) {
            DockEdge.LEFT -> PanelMotionAxis(-(widthPx + 24f), horizontal = true)
            DockEdge.TOP -> PanelMotionAxis(-(heightPx + 24f), horizontal = false)
            DockEdge.RIGHT -> PanelMotionAxis(widthPx + 24f, horizontal = true)
        }

    companion object {
        private const val TAG = "TodoSidebar"
        // Matches the desktop's `cubic-bezier(0.2, 0.8, 0.2, 1)` ease-out
        // curve so the open transition feels identical on phone and desktop.
        private val MOTION_DECELERATE = PathInterpolator(0.2f, 0.8f, 0.2f, 1f)
        // Faster ease-in for close — `cubic-bezier(0.55, 0, 0.7, 0.3)`.
        private val MOTION_ACCELERATE = PathInterpolator(0.55f, 0f, 0.7f, 0.3f)
    }
}
