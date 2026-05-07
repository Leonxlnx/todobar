import {
  CalendarDays,
  Check,
  Circle,
  Clock3,
  Inbox,
  ListTodo,
  Minus,
  RotateCcw,
  PanelRightClose,
  PanelRightOpen,
  Plus,
  Search,
  Settings,
  Trash2,
  X,
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import type { CSSProperties, KeyboardEvent, PointerEvent } from 'react'
import './App.css'
import { useSidebarSettings } from './sidebarSettings'
import type { SidebarSettings } from './sidebarSettings'
import { initialToday, monthPlan } from './tasks'
import type { Task } from './tasks'
import { usePersistentTasks } from './usePersistentTasks'

const DRAG_THRESHOLD = 5
const TASK_STORAGE_KEYS = {
  today: 'todobar.today.v1',
  month: 'todobar.month.v1',
} as const
const CUSTOM_LISTS_STORAGE_KEY = 'todobar.custom-lists.v1'
const PRIORITY_ORDER: Record<Task['priority'], number> = {
  focus: 0,
  normal: 1,
  later: 2,
}

type TaskListId = keyof typeof TASK_STORAGE_KEYS
type TaskDrafts = Record<TaskListId, string>
type CollapsedSections = Record<TaskListId, boolean>
type CustomTaskList = {
  id: string
  title: string
  tasks: Task[]
  collapsed?: boolean
}

const defaultCustomLists: CustomTaskList[] = [
  {
    id: 'general',
    title: 'General',
    tasks: [],
    collapsed: false,
  },
]

const isTauriRuntime = () =>
  new URLSearchParams(window.location.search).get('runtime') === 'tauri' ||
  window.location.protocol === 'tauri:' ||
  navigator.userAgent.includes('Tauri') ||
  '__TAURI_INTERNALS__' in window ||
  '__TAURI__' in window

type NativeWindowHandle<Position> = {
  setPosition: (position: Position) => Promise<void>
}
type HandleDragState = {
  startScreenY: number
  startHandleY: number
  height: number
  moved: boolean
  latestHandleY: number | null
}

function sortTasks(tasks: Task[]) {
  return [...tasks].sort((a, b) => {
    if (Boolean(a.done) !== Boolean(b.done)) {
      return a.done ? 1 : -1
    }

    const priorityDelta = PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority]

    if (priorityDelta !== 0) {
      return priorityDelta
    }

    return b.id - a.id
  })
}

function nextPriority(priority: Task['priority']): Task['priority'] {
  if (priority === 'normal') {
    return 'focus'
  }

  if (priority === 'focus') {
    return 'later'
  }

  return 'normal'
}

function createTask(title: string, meta: string): Task {
  return {
    id: Date.now(),
    title,
    meta,
    priority: 'normal',
  }
}

function loadCustomLists() {
  try {
    const stored = window.localStorage.getItem(CUSTOM_LISTS_STORAGE_KEY)

    if (!stored) {
      return defaultCustomLists
    }

    const parsed = JSON.parse(stored) as CustomTaskList[]

    if (!Array.isArray(parsed)) {
      return defaultCustomLists
    }

    return parsed
      .filter((list) => list && typeof list.id === 'string')
      .map((list) => ({
        id: list.id,
        title: typeof list.title === 'string' ? list.title : 'List',
        tasks: Array.isArray(list.tasks) ? list.tasks : [],
        collapsed: Boolean(list.collapsed),
      }))
  } catch {
    return defaultCustomLists
  }
}

function animateNativeX<Position>(
  windowHandle: NativeWindowHandle<Position>,
  createPosition: (x: number, y: number) => Position,
  startX: number,
  endX: number,
  y: number,
  duration: number,
) {
  return new Promise<void>((resolve) => {
    const startTime = performance.now()
    let lastX = Number.NaN

    const step = (time: number) => {
      const progress = Math.min((time - startTime) / duration, 1)
      const eased = progress * progress * (3 - 2 * progress)
      const nextX = Math.round(startX + (endX - startX) * eased)

      if (nextX !== lastX) {
        lastX = nextX
        void windowHandle.setPosition(createPosition(nextX, y))
      }

      if (progress < 1) {
        requestAnimationFrame(step)
      } else {
        resolve()
      }
    }

    requestAnimationFrame(step)
  })
}

function App() {
  const [isNative] = useState(() => isTauriRuntime())
  const [isOpen, setIsOpen] = useState(
    () => new URLSearchParams(window.location.search).get('open') === '1',
  )
  const [isSettingsOpen, setIsSettingsOpen] = useState(
    () => new URLSearchParams(window.location.search).get('settings') === '1',
  )
  const [settings, updateSettings, resetSettings] = useSidebarSettings()
  const [viewportWidth, setViewportWidth] = useState(() => window.innerWidth)
  const [viewportHeight, setViewportHeight] = useState(() => window.innerHeight)
  const [dragHandleY, setDragHandleY] = useState<number | null>(null)
  const didNativeLayout = useRef(false)
  const previousOpenState = useRef(isOpen)
  const dragState = useRef<HandleDragState | null>(null)
  const suppressNextClick = useRef(false)
  const [todayTasks, setTodayTasks] = usePersistentTasks(
    initialToday,
    TASK_STORAGE_KEYS.today,
  )
  const [monthTasks, setMonthTasks] = usePersistentTasks(
    monthPlan,
    TASK_STORAGE_KEYS.month,
  )
  const [customLists, setCustomLists] = useState<CustomTaskList[]>(loadCustomLists)
  const [drafts, setDrafts] = useState<TaskDrafts>({ today: '', month: '' })
  const [customDrafts, setCustomDrafts] = useState<Record<string, string>>({})
  const [newListDraft, setNewListDraft] = useState('')
  const [collapsedSections, setCollapsedSections] =
    useState<CollapsedSections>({
      today: false,
      month: false,
    })
  const completed = useMemo(
    () => todayTasks.filter((task) => task.done).length,
    [todayTasks],
  )
  const progressPercent =
    todayTasks.length === 0 ? 0 : Math.round((completed / todayTasks.length) * 100)
  const sortedTodayTasks = useMemo(() => sortTasks(todayTasks), [todayTasks])
  const sortedMonthTasks = useMemo(() => sortTasks(monthTasks), [monthTasks])
  const visibleHandleY = dragHandleY ?? settings.handleY
  const effectivePanelWidth = useMemo(() => {
    const availableWidth = Math.max(280, viewportWidth - settings.tabWidth - 8)

    return Math.min(settings.panelWidth, availableWidth)
  }, [settings.panelWidth, settings.tabWidth, viewportWidth])
  const nativeHandleCenter = useMemo(() => {
    const height = Math.max(settings.handleHeight, viewportHeight || 0)
    const half = settings.handleHeight / 2
    const travel = Math.max(0, height - settings.handleHeight)

    return Math.min(
      height - half - 8,
      Math.max(half + 8, half + (travel * visibleHandleY) / 100),
    )
  }, [settings.handleHeight, visibleHandleY, viewportHeight])
  const dockSurface = useMemo(() => {
    const width = settings.tabWidth + effectivePanelWidth
    const height = Math.max(settings.handleHeight, viewportHeight || 0)
    const x = settings.tabWidth
    const handleRadius = Math.max(
      0,
      Math.min(14, settings.tabWidth / 2 - 1, settings.handleHeight / 2 - 1),
    )
    const half = settings.handleHeight / 2
    const travel = Math.max(0, height - settings.handleHeight)
    const center = Math.min(
      height - half - 8,
      Math.max(half + 8, half + (travel * visibleHandleY) / 100),
    )
    const top = center - half
    const bottom = center + half
    const topDockRadius = Math.max(
      0,
      Math.min(16, settings.tabWidth * 0.55, top),
    )
    const bottomDockRadius = Math.max(
      0,
      Math.min(16, settings.tabWidth * 0.55, height - bottom),
    )
    const topPanelRadius = Math.max(
      0,
      Math.min(settings.panelRadius, top - topDockRadius),
    )
    const bottomPanelRadius = Math.max(
      0,
      Math.min(settings.panelRadius, height - bottom - bottomDockRadius),
    )
    return {
      height,
      path: [
        `M ${x + topPanelRadius} 0`,
        `H ${width}`,
        `V ${height}`,
        `H ${x + bottomPanelRadius}`,
        `Q ${x} ${height} ${x} ${height - bottomPanelRadius}`,
        `V ${bottom + bottomDockRadius}`,
        `Q ${x} ${bottom} ${x - bottomDockRadius} ${bottom}`,
        `H ${handleRadius}`,
        `Q 0 ${bottom} 0 ${bottom - handleRadius}`,
        `V ${top + handleRadius}`,
        `Q 0 ${top} ${handleRadius} ${top}`,
        `H ${x - topDockRadius}`,
        `Q ${x} ${top} ${x} ${top - topDockRadius}`,
        `V ${topPanelRadius}`,
        `Q ${x} 0 ${x + topPanelRadius} 0`,
        'Z',
      ].join(' '),
      width,
    }
  }, [
    settings.handleHeight,
    settings.panelRadius,
    settings.tabWidth,
    effectivePanelWidth,
    visibleHandleY,
    viewportHeight,
  ])

  useEffect(() => {
    const onKeyDown = (event: globalThis.KeyboardEvent) => {
      const shortcut =
        event.shiftKey &&
        event.key.toLowerCase() === 't' &&
        (event.metaKey || event.ctrlKey)

      if (shortcut || event.key === 'Escape') {
        event.preventDefault()
        setIsOpen((current) => (event.key === 'Escape' ? false : !current))
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  useEffect(() => {
    const syncViewportSize = () => {
      setViewportWidth(window.innerWidth)
      setViewportHeight(window.innerHeight)
    }

    syncViewportSize()
    window.addEventListener('resize', syncViewportSize)
    return () => window.removeEventListener('resize', syncViewportSize)
  }, [])

  useEffect(() => {
    document.documentElement.dataset.runtime = isNative ? 'tauri' : 'web'

    return () => {
      delete document.documentElement.dataset.runtime
    }
  }, [isNative])

  useEffect(() => {
    try {
      window.localStorage.setItem(
        CUSTOM_LISTS_STORAGE_KEY,
        JSON.stringify(customLists),
      )
    } catch {
      // Desktop builds can move custom lists into the same local store as tasks.
    }
  }, [customLists])

  useEffect(() => {
    let unregisterShortcut: (() => void) | undefined

    const setupNativeShortcut = async () => {
      if (!isTauriRuntime()) {
        return
      }

      try {
        const { register, unregister } = await import(
          '@tauri-apps/plugin-global-shortcut'
        )
        const shortcut = 'CommandOrControl+Shift+T'

        await register(shortcut, (event) => {
          if (event.state === 'Pressed') {
            setIsOpen((current) => !current)
          }
        })

        unregisterShortcut = () => {
          void unregister(shortcut)
        }

      } catch {
        // Shortcut registration can fail when another app owns the chord.
      }
    }

    void setupNativeShortcut()

    return () => {
      unregisterShortcut?.()
    }
  }, [])

  useEffect(() => {
    if (!isNative) {
      return
    }

    const enableAutostart = async () => {
      try {
        const { enable, isEnabled } = await import('@tauri-apps/plugin-autostart')

        if (!(await isEnabled())) {
          await enable()
        }
      } catch {
        // Autostart is best-effort; the UI stays usable if the OS denies it.
      }
    }

    void enableAutostart()
  }, [isNative])

  useEffect(() => {
    if (!isNative) {
      return
    }

    const syncNativeWindow = async () => {
      try {
        const {
          PhysicalPosition,
          PhysicalSize,
          currentMonitor,
          getCurrentWindow,
        } = await import('@tauri-apps/api/window')
        const appWindow = getCurrentWindow()
        const monitor = await currentMonitor()

        if (!monitor) {
          return
        }

        const workArea = monitor.workArea
        const scaleFactor = monitor.scaleFactor || 1
        const fullCssWidth = Math.round(workArea.size.width / scaleFactor)
        const panelCssWidth = Math.min(
          settings.panelWidth,
          Math.max(280, fullCssWidth - settings.tabWidth - 8),
        )
        const windowWidth = Math.round(
          (settings.tabWidth + panelCssWidth) * scaleFactor,
        )
        const tabWidth = Math.round(settings.tabWidth * scaleFactor)
        const openX = workArea.position.x + workArea.size.width - windowWidth
        const closedX =
          workArea.position.x +
          workArea.size.width -
          tabWidth +
          Math.round(2 * scaleFactor)
        const openY = workArea.position.y
        const targetX = isOpen ? openX : closedX
        const startX = isOpen ? closedX : openX
        const targetY = openY
        const targetHeight = workArea.size.height
        const fullCssHeight = Math.round(workArea.size.height / scaleFactor)

        const setTarget = async () => {
          await appWindow.setSize(new PhysicalSize(windowWidth, targetHeight))
          await appWindow.setPosition(new PhysicalPosition(targetX, targetY))
        }
        const didOpenStateChange = previousOpenState.current !== isOpen
        previousOpenState.current = isOpen

        if (!didNativeLayout.current) {
          didNativeLayout.current = true
          setViewportWidth(fullCssWidth)
          setViewportHeight(fullCssHeight)
          await setTarget()
          return
        }

        if (!didOpenStateChange) {
          setViewportWidth(fullCssWidth)
          setViewportHeight(fullCssHeight)
          await setTarget()
          return
        }

        if (isOpen) {
          setViewportWidth(fullCssWidth)
          setViewportHeight(fullCssHeight)
          await Promise.all([
            appWindow.setSize(new PhysicalSize(windowWidth, workArea.size.height)),
            appWindow.setPosition(new PhysicalPosition(closedX, openY)),
          ])
          await animateNativeX(
            appWindow,
            (x, y) => new PhysicalPosition(x, y),
            startX,
            openX,
            openY,
            settings.motionMs,
          )
          await appWindow.setFocus()
          return
        }

        setViewportWidth(fullCssWidth)
        setViewportHeight(fullCssHeight)
        await animateNativeX(
          appWindow,
          (x, y) => new PhysicalPosition(x, y),
          startX,
          closedX,
          openY,
          settings.motionMs,
        )
        await setTarget()
      } catch {
        // Native window control can fail when the app is inspected in a browser.
      }
    }

    void syncNativeWindow()
  }, [isNative, isOpen, settings.motionMs, settings.panelWidth, settings.tabWidth])

  useEffect(() => {
    if (!isNative) {
      return
    }

    let cancelled = false
    let isChecking = false
    let lastIgnored: boolean | null = null

    const syncHitTest = async () => {
      if (isChecking) {
        return
      }

      isChecking = true

      try {
        const { cursorPosition, currentMonitor, getCurrentWindow } =
          await import('@tauri-apps/api/window')
        const appWindow = getCurrentWindow()
        const [cursor, position, monitor] = await Promise.all([
          cursorPosition(),
          appWindow.outerPosition(),
          currentMonitor(),
        ])
        const scaleFactor = monitor?.scaleFactor || 1
        const relativeX = cursor.x - position.x
        const relativeY = cursor.y - position.y
        const tabWidth = settings.tabWidth * scaleFactor
        const panelWidth = effectivePanelWidth * scaleFactor
        const handleTop =
          (nativeHandleCenter - settings.handleHeight / 2) * scaleFactor
        const handleBottom =
          (nativeHandleCenter + settings.handleHeight / 2) * scaleFactor
        const handleHitSlop = 3 * scaleFactor
        const isOnHandle =
          relativeX >= -handleHitSlop &&
          relativeX <= tabWidth + handleHitSlop &&
          relativeY >= handleTop - handleHitSlop &&
          relativeY <= handleBottom + handleHitSlop
        const isOnPanel =
          isOpen &&
          relativeX >= tabWidth &&
          relativeX <= tabWidth + panelWidth &&
          relativeY >= 0
        const shouldIgnore = dragState.current
          ? false
          : !(isOnHandle || isOnPanel)

        if (!cancelled && shouldIgnore !== lastIgnored) {
          lastIgnored = shouldIgnore
          await appWindow.setIgnoreCursorEvents(shouldIgnore)
        }
      } catch {
        // Pointer passthrough is best-effort; the app still works without it.
      } finally {
        isChecking = false
      }
    }

    void syncHitTest()
    const interval = window.setInterval(syncHitTest, 32)

    return () => {
      cancelled = true
      window.clearInterval(interval)
      void import('@tauri-apps/api/window')
        .then(({ getCurrentWindow }) =>
          getCurrentWindow().setIgnoreCursorEvents(false),
        )
        .catch(() => undefined)
    }
  }, [
    isNative,
    isOpen,
    nativeHandleCenter,
    settings.handleHeight,
    settings.tabWidth,
    effectivePanelWidth,
  ])

  const updateDraft = (listId: TaskListId, value: string) => {
    setDrafts((current) => ({ ...current, [listId]: value }))
  }

  const updateTasks = (
    listId: TaskListId,
    updater: (tasks: Task[]) => Task[],
  ) => {
    const setter = listId === 'today' ? setTodayTasks : setMonthTasks

    setter(updater)
  }

  const addTask = (listId: TaskListId) => {
    const title = drafts[listId].trim()

    if (!title) {
      return
    }

    updateTasks(listId, (tasks) => [
      createTask(title, listId === 'today' ? 'Today' : 'Month Plan'),
      ...tasks,
    ])
    setDrafts((current) => ({ ...current, [listId]: '' }))
    setCollapsedSections((current) => ({ ...current, [listId]: false }))
    setIsOpen(true)
  }

  const onDraftKeyDown = (
    event: KeyboardEvent<HTMLInputElement>,
    listId: TaskListId,
  ) => {
    if (event.key === 'Enter') {
      addTask(listId)
    }
  }

  const toggleTask = (listId: TaskListId, id: number) => {
    updateTasks(listId, (tasks) =>
      tasks.map((task) =>
        task.id === id ? { ...task, done: !task.done } : task,
      ),
    )
  }

  const cycleTaskPriority = (listId: TaskListId, id: number) => {
    updateTasks(listId, (tasks) =>
      tasks.map((task) =>
        task.id === id
          ? { ...task, priority: nextPriority(task.priority) }
          : task,
      ),
    )
  }

  const deleteTask = (listId: TaskListId, id: number) => {
    updateTasks(listId, (tasks) => tasks.filter((task) => task.id !== id))
  }

  const toggleSection = (listId: TaskListId) => {
    setCollapsedSections((current) => ({
      ...current,
      [listId]: !current[listId],
    }))
  }

  const addCustomList = () => {
    const title = newListDraft.trim()

    if (!title) {
      return
    }

    const id = `${Date.now()}`

    setCustomLists((lists) => [
      ...lists,
      {
        id,
        title,
        tasks: [],
        collapsed: false,
      },
    ])
    setCustomDrafts((current) => ({ ...current, [id]: '' }))
    setNewListDraft('')
  }

  const onNewListKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      addCustomList()
    }
  }

  const updateCustomDraft = (listId: string, value: string) => {
    setCustomDrafts((current) => ({ ...current, [listId]: value }))
  }

  const updateCustomListTasks = (
    listId: string,
    updater: (tasks: Task[]) => Task[],
  ) => {
    setCustomLists((lists) =>
      lists.map((list) =>
        list.id === listId ? { ...list, tasks: updater(list.tasks) } : list,
      ),
    )
  }

  const addCustomTask = (listId: string) => {
    const title = (customDrafts[listId] ?? '').trim()
    const list = customLists.find((item) => item.id === listId)

    if (!title || !list) {
      return
    }

    updateCustomListTasks(listId, (tasks) => [
      createTask(title, list.title),
      ...tasks,
    ])
    setCustomDrafts((current) => ({ ...current, [listId]: '' }))
    setCustomLists((lists) =>
      lists.map((item) =>
        item.id === listId ? { ...item, collapsed: false } : item,
      ),
    )
  }

  const onCustomDraftKeyDown = (
    event: KeyboardEvent<HTMLInputElement>,
    listId: string,
  ) => {
    if (event.key === 'Enter') {
      addCustomTask(listId)
    }
  }

  const toggleCustomTask = (listId: string, taskId: number) => {
    updateCustomListTasks(listId, (tasks) =>
      tasks.map((task) =>
        task.id === taskId ? { ...task, done: !task.done } : task,
      ),
    )
  }

  const cycleCustomTaskPriority = (listId: string, taskId: number) => {
    updateCustomListTasks(listId, (tasks) =>
      tasks.map((task) =>
        task.id === taskId
          ? { ...task, priority: nextPriority(task.priority) }
          : task,
      ),
    )
  }

  const deleteCustomTask = (listId: string, taskId: number) => {
    updateCustomListTasks(listId, (tasks) =>
      tasks.filter((task) => task.id !== taskId),
    )
  }

  const toggleCustomList = (listId: string) => {
    setCustomLists((lists) =>
      lists.map((list) =>
        list.id === listId ? { ...list, collapsed: !list.collapsed } : list,
      ),
    )
  }

  const deleteCustomList = (listId: string) => {
    setCustomLists((lists) => lists.filter((list) => list.id !== listId))
    setCustomDrafts((current) => {
      const next = { ...current }
      delete next[listId]
      return next
    })
  }

  const onHandlePointerDown = (event: PointerEvent<HTMLButtonElement>) => {
    if (!isNative) {
      return
    }

    event.currentTarget.setPointerCapture(event.pointerId)
    suppressNextClick.current = false

    const drag: HandleDragState = {
      startScreenY: event.screenY,
      startHandleY: visibleHandleY,
      height: window.innerHeight,
      moved: false,
      latestHandleY: null,
    }

    dragState.current = drag
  }

  const onHandlePointerMove = (event: PointerEvent<HTMLButtonElement>) => {
    const drag = dragState.current

    if (!drag) {
      return
    }

    const deltaY = event.screenY - drag.startScreenY

    if (Math.abs(deltaY) < DRAG_THRESHOLD && !drag.moved) {
      return
    }

    drag.moved = true
    suppressNextClick.current = true

    const travel = Math.max(1, drag.height - settings.handleHeight)
    const nextHandleY = Math.min(
      100,
      Math.max(0, drag.startHandleY + (deltaY / travel) * 100),
    )

    drag.latestHandleY = Math.round(nextHandleY)
    setDragHandleY(nextHandleY)
  }

  const onHandlePointerUp = (event: PointerEvent<HTMLButtonElement>) => {
    const drag = dragState.current
    const shouldToggle = drag && !drag.moved

    if (drag?.moved && drag.latestHandleY !== null) {
      updateSettings({ handleY: drag.latestHandleY })
    }

    setDragHandleY(null)
    dragState.current = null

    try {
      event.currentTarget.releasePointerCapture(event.pointerId)
    } catch {
      // Pointer capture can already be gone after native pointer handoff.
    }

    if (shouldToggle) {
      suppressNextClick.current = true
      setIsOpen((current) => !current)
    }
  }

  const onHandleClick = () => {
    if (suppressNextClick.current) {
      suppressNextClick.current = false
      return
    }

    setIsOpen((current) => !current)
  }

  const appStyle = {
    '--panel-width': `${effectivePanelWidth}px`,
    '--tab-width': `${settings.tabWidth}px`,
    '--handle-height': `${settings.handleHeight}px`,
    '--handle-half': `${settings.handleHeight / 2}px`,
    '--handle-y': `${visibleHandleY}%`,
    '--native-handle-y-px': `${nativeHandleCenter}px`,
    '--motion-ms': `${settings.motionMs}ms`,
    '--panel-radius': `${settings.panelRadius}px`,
    '--surface-alpha': `${settings.surfaceAlpha / 100}`,
  } as CSSProperties

  return (
    <main
      className={`workspace ${isNative ? 'is-native' : 'is-web-preview'} ${
        isOpen ? 'is-sidebar-open' : 'is-sidebar-closed'
      } theme-${settings.theme}`}
      style={appStyle}
    >
      <section
        className="desktop-preview"
        aria-label="Todobar desktop preview"
        aria-hidden={isNative}
      >
        <nav className="system-bar" aria-label="Desktop menu">
          <div className="window-dots" aria-hidden="true">
            <span />
            <span />
            <span />
          </div>
          <div className="system-title">
            <ListTodo size={16} />
            <span>Todobar</span>
          </div>
          <div className="system-actions">
            <button type="button" aria-label="Search">
              <Search size={16} />
            </button>
            <button type="button" aria-label="Settings">
              <Settings size={16} />
            </button>
          </div>
        </nav>

        <section className="canvas" aria-label="Desktop workspace">
          <div className="desk-copy">
            <div className="mark">
              <PanelRightOpen size={22} />
            </div>
            <h1>Right-edge todo bar for deep work.</h1>
            <p>
              The browser view is only the dev preview. The product target is a
              native macOS and Windows utility with shortcuts, overlay windows,
              MCP connectors, and AI planning.
            </p>
            <div className="shortcut-row" aria-label="Keyboard shortcut">
              <kbd>Cmd</kbd>
              <kbd>Ctrl</kbd>
              <kbd>Shift</kbd>
              <kbd>T</kbd>
            </div>
          </div>

          <div className="mock-window mock-window-a">
            <div />
            <span />
            <span />
          </div>
          <div className="mock-window mock-window-b">
            <span />
            <span />
            <span />
          </div>
        </section>
      </section>

      {isNative ? (
        <svg
          className="native-dock-surface"
          width={dockSurface.width}
          height={dockSurface.height}
          viewBox={`0 0 ${dockSurface.width} ${dockSurface.height}`}
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <path d={dockSurface.path} />
        </svg>
      ) : null}

      <button
        className={`edge-handle ${isOpen ? 'is-open' : ''}`}
        type="button"
        aria-label={isOpen ? 'Close Todobar' : 'Open Todobar'}
        aria-expanded={isOpen}
        onPointerDown={onHandlePointerDown}
        onPointerMove={onHandlePointerMove}
        onPointerUp={onHandlePointerUp}
        onPointerCancel={onHandlePointerUp}
        onClick={onHandleClick}
      >
        <span className="handle-symbol" aria-hidden="true">
          <PanelRightOpen className="handle-icon-open" size={15} />
          <PanelRightClose className="handle-icon-close" size={15} />
        </span>
      </button>

      <aside
        className={`todo-sidebar ${isOpen ? 'is-open' : ''} ${
          isSettingsOpen ? 'is-settings-open' : ''
        }`}
        aria-label="Todobar sidebar"
        aria-hidden={!isOpen}
      >
        {isSettingsOpen ? (
          <div className="settings-drawer" role="dialog" aria-label="Settings">
            <SidebarSettingsPanel
              settings={settings}
              onChange={updateSettings}
              onReset={resetSettings}
              onClose={() => setIsSettingsOpen(false)}
            />
          </div>
        ) : (
          <>
        <header className="sidebar-header">
          <div className="app-lockup">
            <span className="app-icon">
              <Check size={15} />
            </span>
            <strong>Todobar</strong>
          </div>
          <div className="icon-cluster">
            <button
              type="button"
              className={isSettingsOpen ? 'is-active' : ''}
              aria-label="Sidebar settings"
              aria-pressed={isSettingsOpen}
              onClick={() => setIsSettingsOpen((current) => !current)}
            >
              <Settings size={16} />
            </button>
          </div>
        </header>

        <section className="panel-section" aria-labelledby="today-heading">
          <div className="section-heading">
            <div>
              <span id="today-heading">
                <Clock3 size={15} />
                Today
                <em>
                  {completed} done · {todayTasks.length} total
                </em>
              </span>
            </div>
            <button
              type="button"
              aria-label={
                collapsedSections.today ? 'Expand Today' : 'Collapse Today'
              }
              aria-expanded={!collapsedSections.today}
              onClick={() => toggleSection('today')}
            >
              {collapsedSections.today ? <Plus size={15} /> : <Minus size={15} />}
            </button>
          </div>
          <div className="section-meter" aria-hidden="true">
            <span style={{ width: `${progressPercent}%` }} />
          </div>
          <div
            className={`section-content ${
              collapsedSections.today ? 'is-collapsed' : ''
            }`}
            aria-hidden={collapsedSections.today}
          >
            <div className="section-content-inner">
              <QuickAdd
                ariaLabel="Add a task to Today"
                value={drafts.today}
                placeholder="Add task..."
                onChange={(value) => updateDraft('today', value)}
                onSubmit={() => addTask('today')}
                onKeyDown={(event) => onDraftKeyDown(event, 'today')}
              />
              <div className="task-list">
                {sortedTodayTasks.map((task, index) => (
                  <TaskRow
                    key={task.id}
                    task={task}
                    index={index}
                    onToggle={(id) => toggleTask('today', id)}
                    onPriority={(id) => cycleTaskPriority('today', id)}
                    onDelete={(id) => deleteTask('today', id)}
                  />
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="panel-section" aria-labelledby="month-heading">
          <div className="section-heading">
            <div>
              <span id="month-heading">
                <CalendarDays size={15} />
                Month Plan
                <em>{monthTasks.length} tasks</em>
              </span>
            </div>
            <button
              type="button"
              aria-label={
                collapsedSections.month
                  ? 'Expand Month Plan'
                  : 'Collapse Month Plan'
              }
              aria-expanded={!collapsedSections.month}
              onClick={() => toggleSection('month')}
            >
              {collapsedSections.month ? <Plus size={15} /> : <Minus size={15} />}
            </button>
          </div>
          <div
            className={`section-content ${
              collapsedSections.month ? 'is-collapsed' : ''
            }`}
            aria-hidden={collapsedSections.month}
          >
            <div className="section-content-inner">
              <QuickAdd
                ariaLabel="Add a task to Month Plan"
                value={drafts.month}
                placeholder="Add month task..."
                onChange={(value) => updateDraft('month', value)}
                onSubmit={() => addTask('month')}
                onKeyDown={(event) => onDraftKeyDown(event, 'month')}
              />
              <div className="task-list month-list">
                {sortedMonthTasks.map((task, index) => (
                  <TaskRow
                    key={task.id}
                    task={task}
                    index={index}
                    onToggle={(id) => toggleTask('month', id)}
                    onPriority={(id) => cycleTaskPriority('month', id)}
                    onDelete={(id) => deleteTask('month', id)}
                  />
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="panel-section list-section" aria-labelledby="lists-heading">
          <div className="section-heading">
            <div>
              <span id="lists-heading">
                <ListTodo size={15} />
                Lists
                <em>{customLists.length} custom</em>
              </span>
            </div>
          </div>

          <div className="quick-add list-create">
            <ListTodo size={16} />
            <input
              aria-label="Create a custom list"
              placeholder="New list..."
              value={newListDraft}
              onChange={(event) => setNewListDraft(event.target.value)}
              onKeyDown={onNewListKeyDown}
            />
            <button type="button" aria-label="Create list" onClick={addCustomList}>
              <Plus size={16} />
            </button>
          </div>

          <div className="custom-list-stack">
            {customLists.map((list) => {
              const sortedTasks = sortTasks(list.tasks)

              return (
                <section className="custom-list" key={list.id}>
                  <div className="custom-list-header">
                    <button
                      type="button"
                      className="custom-list-title"
                      aria-expanded={!list.collapsed}
                      onClick={() => toggleCustomList(list.id)}
                    >
                      <span>{list.title}</span>
                      <em>{list.tasks.length}</em>
                    </button>
                    <button
                      type="button"
                      className="custom-list-toggle"
                      aria-label={
                        list.collapsed
                          ? `Expand ${list.title}`
                          : `Collapse ${list.title}`
                      }
                      onClick={() => toggleCustomList(list.id)}
                    >
                      {list.collapsed ? <Plus size={14} /> : <Minus size={14} />}
                    </button>
                    <button
                      type="button"
                      className="delete-button custom-list-delete"
                      aria-label={`Delete ${list.title}`}
                      onClick={() => deleteCustomList(list.id)}
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>

                  <div
                    className={`section-content ${
                      list.collapsed ? 'is-collapsed' : ''
                    }`}
                    aria-hidden={Boolean(list.collapsed)}
                  >
                    <div className="section-content-inner">
                      <QuickAdd
                        ariaLabel={`Add a task to ${list.title}`}
                        value={customDrafts[list.id] ?? ''}
                        placeholder="Add task..."
                        onChange={(value) => updateCustomDraft(list.id, value)}
                        onSubmit={() => addCustomTask(list.id)}
                        onKeyDown={(event) =>
                          onCustomDraftKeyDown(event, list.id)
                        }
                      />
                      {sortedTasks.length > 0 ? (
                        <div className="task-list">
                          {sortedTasks.map((task, index) => (
                            <TaskRow
                              key={task.id}
                              task={task}
                              index={index}
                              onToggle={(id) => toggleCustomTask(list.id, id)}
                              onPriority={(id) =>
                                cycleCustomTaskPriority(list.id, id)
                              }
                              onDelete={(id) => deleteCustomTask(list.id, id)}
                            />
                          ))}
                        </div>
                      ) : null}
                    </div>
                  </div>
                </section>
              )
            })}
          </div>
        </section>
          </>
        )}
      </aside>
    </main>
  )
}

function QuickAdd({
  ariaLabel,
  value,
  placeholder,
  onChange,
  onSubmit,
  onKeyDown,
}: {
  ariaLabel: string
  value: string
  placeholder: string
  onChange: (value: string) => void
  onSubmit: () => void
  onKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void
}) {
  return (
    <div className="quick-add">
      <Inbox size={16} />
      <input
        aria-label={ariaLabel}
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={onKeyDown}
      />
      <button
        type="button"
        aria-label="Add task"
        onClick={onSubmit}
      >
        <Plus size={16} />
      </button>
    </div>
  )
}

function SidebarSettingsPanel({
  settings,
  onChange,
  onReset,
  onClose,
}: {
  settings: SidebarSettings
  onChange: (patch: Partial<SidebarSettings>) => void
  onReset: () => void
  onClose: () => void
}) {
  return (
    <section className="settings-panel" aria-label="Sidebar settings">
      <div className="settings-panel-header">
        <div>
          <strong>Todo settings</strong>
        </div>
        <div className="settings-actions">
          <button type="button" aria-label="Reset settings" onClick={onReset}>
            <RotateCcw size={15} />
          </button>
          <button type="button" aria-label="Close settings" onClick={onClose}>
            <X size={15} />
          </button>
        </div>
      </div>

      <div className="settings-group settings-appearance">
        <div className="settings-group-title">Appearance</div>
        <div className="segmented-control" aria-label="Appearance">
          <button
            type="button"
            className={settings.theme === 'light' ? 'is-selected' : ''}
            onClick={() => onChange({ theme: 'light' })}
          >
            Light
          </button>
          <button
            type="button"
            className={settings.theme === 'dark' ? 'is-selected' : ''}
            onClick={() => onChange({ theme: 'dark' })}
          >
            Dark
          </button>
        </div>
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Window</div>
        <SliderSetting
          label="Panel width"
          value={settings.panelWidth}
          min={300}
          max={500}
          step={4}
          suffix="px"
          onChange={(panelWidth) => onChange({ panelWidth })}
        />
        <SliderSetting
          label="Visible tab"
          value={settings.tabWidth}
          min={34}
          max={62}
          step={2}
          suffix="px"
          onChange={(tabWidth) => onChange({ tabWidth })}
        />
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Handle</div>
        <SliderSetting
          label="Button height"
          value={settings.handleHeight}
          min={72}
          max={132}
          step={2}
          suffix="px"
          onChange={(handleHeight) => onChange({ handleHeight })}
        />
        <SliderSetting
          label="Vertical position"
          value={settings.handleY}
          min={0}
          max={100}
          step={1}
          suffix="%"
          onChange={(handleY) => onChange({ handleY })}
        />
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Feel</div>
        <SliderSetting
          label="Motion"
          value={settings.motionMs}
          min={140}
          max={360}
          step={10}
          suffix="ms"
          onChange={(motionMs) => onChange({ motionMs })}
        />
        <SliderSetting
          label="Corner radius"
          value={settings.panelRadius}
          min={12}
          max={28}
          step={1}
          suffix="px"
          onChange={(panelRadius) => onChange({ panelRadius })}
        />
        <SliderSetting
          label="Surface"
          value={settings.surfaceAlpha}
          min={86}
          max={100}
          step={1}
          suffix="%"
          onChange={(surfaceAlpha) => onChange({ surfaceAlpha })}
        />
      </div>
    </section>
  )
}

function SliderSetting({
  label,
  value,
  min,
  max,
  step,
  suffix,
  onChange,
}: {
  label: string
  value: number
  min: number
  max: number
  step: number
  suffix: string
  onChange: (value: number) => void
}) {
  return (
    <label className="slider-setting">
      <span>
        <strong>{label}</strong>
        <em>
          {value}
          {suffix}
        </em>
      </span>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
      />
    </label>
  )
}

function TaskRow({
  task,
  index = 0,
  onToggle,
  onPriority,
  onDelete,
}: {
  task: Task
  index?: number
  onToggle?: (id: number) => void
  onPriority?: (id: number) => void
  onDelete?: (id: number) => void
}) {
  const priorityLabel =
    task.priority === 'focus'
      ? 'Focus'
      : task.priority === 'later'
        ? 'Later'
        : 'Task'

  return (
    <article
      className={`task-row priority-${task.priority} ${
        task.done ? 'is-complete' : ''
      }`}
      style={{ '--row-delay': `${Math.min(index, 8) * 18}ms` } as CSSProperties}
    >
      <button
        type="button"
        className="check-button"
        aria-label={task.done ? `Mark ${task.title} open` : `Complete ${task.title}`}
        onClick={() => onToggle?.(task.id)}
      >
        {task.done ? <Check size={13} /> : <Circle size={13} />}
      </button>
      <div className="task-body">
        <strong className={task.done ? 'is-done' : ''}>{task.title}</strong>
        <span>{task.meta}</span>
      </div>
      <div className="row-actions">
        <button
          type="button"
          className="priority-button"
          aria-label={`Priority: ${priorityLabel}. Click to change`}
          onClick={() => onPriority?.(task.id)}
        >
          <span className="priority-dot" aria-hidden="true" />
        </button>
        <button
          type="button"
          className="delete-button"
          aria-label={`Delete ${task.title}`}
          onClick={() => onDelete?.(task.id)}
        >
          <Trash2 size={13} />
        </button>
      </div>
    </article>
  )
}

export default App
