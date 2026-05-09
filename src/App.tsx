import {
  ArrowDown,
  ArrowUp,
  Bell,
  BellRing,
  CalendarDays,
  Check,
  ChevronLeft,
  ChevronRight,
  Circle,
  Clock3,
  Inbox,
  ListTodo,
  Moon,
  Minus,
  Palette,
  Pin,
  RotateCcw,
  PanelRightClose,
  PanelRightOpen,
  Plus,
  Search,
  Settings,
  Sun,
  Trash2,
  X,
} from 'lucide-react'
import { memo, useEffect, useMemo, useRef, useState } from 'react'
import type { CSSProperties, KeyboardEvent, PointerEvent } from 'react'
import './App.css'
import { useSidebarSettings } from './sidebarSettings'
import type { SectionId, SidebarSettings, ThemePreset } from './sidebarSettings'
import { initialToday, monthPlan } from './tasks'
import type { Task } from './tasks'
import { usePersistentTasks } from './usePersistentTasks'

const DRAG_THRESHOLD = 5
const TASK_STORAGE_KEYS = {
  today: 'todobar.today.v1',
  month: 'todobar.month.v1',
} as const
const CUSTOM_LISTS_STORAGE_KEY = 'todobar.custom-lists.v1'
const NOTIFIED_REMINDERS_STORAGE_KEY = 'todobar.notified-reminders.v1'
const PRIORITY_ORDER: Record<Task['priority'], number> = {
  focus: 0,
  normal: 1,
  later: 2,
}
const SECTION_LABELS: Record<SectionId, string> = {
  calendar: 'Calendar',
  lists: 'Lists',
  today: 'Today',
}
const THEME_PRESETS = [
  {
    id: 'codex',
    label: 'Codex',
    note: 'Neutral',
  },
  {
    id: 'quartz',
    label: 'Quartz Glass',
    note: 'Clear light',
  },
  {
    id: 'frost',
    label: 'Frost',
    note: 'Blue glass',
  },
  {
    id: 'paper',
    label: 'Paper',
    note: 'Warm light',
  },
  {
    id: 'graphite',
    label: 'Graphite',
    note: 'Deep focus',
  },
  {
    id: 'midnight',
    label: 'Midnight',
    note: 'Blue dark',
  },
  {
    id: 'clay',
    label: 'Clay',
    note: 'Muted color',
  },
  {
    id: 'blueprint',
    label: 'Blueprint',
    note: 'Grid',
  },
] as const satisfies Array<{
  id: ThemePreset
  label: string
  note: string
}>

type TaskListId = keyof typeof TASK_STORAGE_KEYS
type TaskDrafts = Record<TaskListId, string>
type ReminderDrafts = Record<TaskListId, string>
type CollapsedSections = Record<TaskListId, boolean>
type CustomTaskList = {
  id: string
  title: string
  tasks: Task[]
  collapsed?: boolean
  showOnToday?: boolean
}
type CalendarTaskRef = {
  listId?: string
  listTitle: string
  source: TaskListId | 'custom'
  task: Task
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

function createTask(title: string, meta: string, reminderAt?: string): Task {
  return {
    id: Date.now(),
    title,
    meta,
    priority: 'normal',
    reminderAt: reminderAt || undefined,
  }
}

function toLocalDateTimeValue(date: Date) {
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)

  return offsetDate.toISOString().slice(0, 16)
}

function nextQuickReminder(current?: string) {
  if (!current) {
    const next = new Date()
    next.setMinutes(next.getMinutes() + 30)
    next.setSeconds(0, 0)
    return toLocalDateTimeValue(next)
  }

  const next = new Date()
  next.setDate(next.getDate() + 1)
  next.setHours(9, 0, 0, 0)

  const currentTime = new Date(current).getTime()
  const tomorrowTime = next.getTime()

  return Number.isFinite(currentTime) && currentTime < tomorrowTime
    ? toLocalDateTimeValue(next)
    : undefined
}

function formatReminder(reminderAt?: string) {
  if (!reminderAt) {
    return ''
  }

  const date = new Date(reminderAt)

  if (Number.isNaN(date.getTime())) {
    return ''
  }

  return new Intl.DateTimeFormat(undefined, {
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    month: 'short',
  }).format(date)
}

function formatDateKey(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function parseReminderDate(reminderAt?: string) {
  if (!reminderAt) {
    return null
  }

  const date = new Date(reminderAt)

  return Number.isNaN(date.getTime()) ? null : date
}

function formatCalendarMonth(date: Date) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'long',
    year: 'numeric',
  }).format(date)
}

function formatCalendarDay(date: Date) {
  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
  }).format(date)
}

function addMonths(date: Date, amount: number) {
  return new Date(date.getFullYear(), date.getMonth() + amount, 1)
}

function buildCalendarDays(
  cursor: Date,
  tasks: Array<{ listTitle: string; task: Task }>,
) {
  const monthStart = new Date(cursor.getFullYear(), cursor.getMonth(), 1)
  const gridStart = new Date(monthStart)
  const mondayOffset = (monthStart.getDay() + 6) % 7
  const todayKey = formatDateKey(new Date())

  gridStart.setDate(monthStart.getDate() - mondayOffset)

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart)
    date.setDate(gridStart.getDate() + index)

    const key = formatDateKey(date)
    const dayTasks = tasks.filter(({ task }) => {
      const reminderDate = parseReminderDate(task.reminderAt)

      return reminderDate ? formatDateKey(reminderDate) === key : false
    })

    return {
      date,
      doneCount: dayTasks.filter(({ task }) => task.done).length,
      isCurrentMonth: date.getMonth() === cursor.getMonth(),
      isToday: key === todayKey,
      key,
      taskCount: dayTasks.length,
    }
  })
}

function loadNotifiedReminderKeys() {
  try {
    const stored = window.localStorage.getItem(NOTIFIED_REMINDERS_STORAGE_KEY)
    const parsed = stored ? (JSON.parse(stored) as Record<string, boolean>) : {}

    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function moveSectionOrder(
  sections: SectionId[],
  section: SectionId,
  direction: -1 | 1,
) {
  const currentIndex = sections.indexOf(section)
  const nextIndex = currentIndex + direction

  if (
    currentIndex < 0 ||
    nextIndex < 0 ||
    nextIndex >= sections.length
  ) {
    return sections
  }

  const next = [...sections]
  const [item] = next.splice(currentIndex, 1)
  next.splice(nextIndex, 0, item)

  return next
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
        showOnToday: Boolean(list.showOnToday),
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
  const [activeRailSection, setActiveRailSection] = useState<SectionId>('today')
  const [calendarCursor, setCalendarCursor] = useState(() => new Date())
  const [selectedCalendarKey, setSelectedCalendarKey] = useState(() =>
    formatDateKey(new Date()),
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
  const [reminderDrafts, setReminderDrafts] = useState<ReminderDrafts>({
    today: '',
    month: '',
  })
  const [customDrafts, setCustomDrafts] = useState<Record<string, string>>({})
  const [customReminderDrafts, setCustomReminderDrafts] = useState<
    Record<string, string>
  >({})
  const [newListDraft, setNewListDraft] = useState('')
  const notifiedReminderKeys = useRef<Record<string, boolean>>(
    loadNotifiedReminderKeys(),
  )
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
  const visibleTodayTasks = useMemo(
    () =>
      settings.showCompleted
        ? sortedTodayTasks
        : sortedTodayTasks.filter((task) => !task.done),
    [settings.showCompleted, sortedTodayTasks],
  )
  const visibleMonthTasks = useMemo(
    () =>
      settings.showCompleted
        ? sortedMonthTasks
        : sortedMonthTasks.filter((task) => !task.done),
    [settings.showCompleted, sortedMonthTasks],
  )
  const reminderTasks = useMemo(
    (): CalendarTaskRef[] => [
      ...todayTasks.map((task) => ({
        listTitle: 'Today',
        source: 'today' as const,
        task,
      })),
      ...monthTasks.map((task) => ({
        listTitle: 'Calendar',
        source: 'month' as const,
        task,
      })),
      ...customLists.flatMap((list) =>
        list.tasks.map((task) => ({
          listId: list.id,
          listTitle: list.title,
          source: 'custom' as const,
          task,
        })),
      ),
    ],
    [customLists, monthTasks, todayTasks],
  )
  const pinnedTodayLists = useMemo(
    () => customLists.filter((list) => list.showOnToday),
    [customLists],
  )
  const totalOpenTasks = useMemo(
    () =>
      reminderTasks.filter(({ task }) => !task.done).length,
    [reminderTasks],
  )
  const calendarDays = useMemo(
    () => buildCalendarDays(calendarCursor, reminderTasks),
    [calendarCursor, reminderTasks],
  )
  const selectedCalendarDate = useMemo(
    () => new Date(`${selectedCalendarKey}T09:00:00`),
    [selectedCalendarKey],
  )
  const selectedCalendarTasks = useMemo(
    () =>
      reminderTasks.filter(({ task }) => {
        const reminderDate = parseReminderDate(task.reminderAt)

        return reminderDate
          ? formatDateKey(reminderDate) === selectedCalendarKey
          : false
      }),
    [reminderTasks, selectedCalendarKey],
  )
  const unscheduledMonthTasks = useMemo(
    () => monthTasks.filter((task) => !task.reminderAt),
    [monthTasks],
  )
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
      12,
      Math.min(16, settings.tabWidth * 0.4, settings.handleHeight * 0.24),
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
      Math.min(18, settings.tabWidth * 0.5, top),
    )
    const bottomDockRadius = Math.max(
      0,
      Math.min(18, settings.tabWidth * 0.5, height - bottom),
    )
    const topPanelRadius = Math.max(
      0,
      Math.min(settings.panelRadius, top - topDockRadius),
    )
    const bottomPanelRadius = Math.max(
      0,
      Math.min(settings.panelRadius, height - bottom - bottomDockRadius),
    )
    const closedPath = [
      `M ${x} ${top - topDockRadius}`,
      `Q ${x} ${top} ${x - topDockRadius} ${top}`,
      `H ${handleRadius}`,
      `C ${handleRadius * 0.45} ${top} 0 ${top + handleRadius * 0.45} 0 ${top + handleRadius}`,
      `V ${bottom - handleRadius}`,
      `C 0 ${bottom - handleRadius * 0.45} ${handleRadius * 0.45} ${bottom} ${handleRadius} ${bottom}`,
      `H ${x - bottomDockRadius}`,
      `Q ${x} ${bottom} ${x} ${bottom + bottomDockRadius}`,
      'Z',
    ].join(' ')

    return {
      height,
      path: isOpen
        ? [
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
          ].join(' ')
        : closedPath,
      width,
    }
  }, [
    isOpen,
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
        event.altKey &&
        event.key.toLowerCase() === 't' &&
        !event.shiftKey

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
    if (!settings.notificationsEnabled) {
      return
    }

    let cancelled = false

    const notify = async (task: Task, listTitle: string) => {
      const body = `${listTitle} · ${task.meta}`

      try {
        if (isNative) {
          const {
            isPermissionGranted,
            requestPermission,
            sendNotification,
          } = await import('@tauri-apps/plugin-notification')

          let permissionGranted = await isPermissionGranted()

          if (!permissionGranted) {
            permissionGranted = (await requestPermission()) === 'granted'
          }

          if (permissionGranted && !cancelled) {
            sendNotification({ body, title: task.title })
            return true
          }

          return false
        }

        if (!('Notification' in window)) {
          return false
        }

        let permission = Notification.permission

        if (permission === 'default') {
          permission = await Notification.requestPermission()
        }

        if (permission === 'granted' && !cancelled) {
          new Notification(task.title, { body })
          return true
        }
      } catch {
        // Notifications are best-effort; tasks and reminders stay usable.
      }

      return false
    }

    const checkReminders = () => {
      const now = Date.now()

      for (const { listTitle, task } of reminderTasks) {
        if (!task.reminderAt || task.done) {
          continue
        }

        const dueTime = new Date(task.reminderAt).getTime()

        if (!Number.isFinite(dueTime) || dueTime > now) {
          continue
        }

        const key = `${task.id}:${task.reminderAt}`

        if (notifiedReminderKeys.current[key]) {
          continue
        }

        notifiedReminderKeys.current[key] = true
        window.localStorage.setItem(
          NOTIFIED_REMINDERS_STORAGE_KEY,
          JSON.stringify(notifiedReminderKeys.current),
        )
        void notify(task, listTitle)
      }
    }

    checkReminders()
    const interval = window.setInterval(checkReminders, 30000)

    return () => {
      cancelled = true
      window.clearInterval(interval)
    }
  }, [isNative, reminderTasks, settings.notificationsEnabled])

  useEffect(() => {
    if (!isNative) {
      return
    }

    let unlistenToggle: (() => void) | undefined
    let unlistenSettings: (() => void) | undefined

    const setupTrayListeners = async () => {
      try {
        const { listen } = await import('@tauri-apps/api/event')

        unlistenToggle = await listen('todobar-tray-toggle', () => {
          setIsOpen((current) => !current)
        })
        unlistenSettings = await listen('todobar-tray-settings', () => {
          setIsOpen(true)
          setIsSettingsOpen(true)
        })
      } catch {
        // Tray events are only available in the native desktop shell.
      }
    }

    void setupTrayListeners()

    return () => {
      unlistenToggle?.()
      unlistenSettings?.()
    }
  }, [isNative])

  useEffect(() => {
    if (!isNative || import.meta.env.DEV) {
      return
    }

    const enableAutostart = async () => {
      try {
        const { disable, enable, isEnabled } = await import(
          '@tauri-apps/plugin-autostart'
        )
        const enabled = await isEnabled()

        if (settings.launchAtLogin) {
          if (enabled) {
            await disable()
          }

          await enable()
          return
        }

        if (!settings.launchAtLogin && enabled) {
          await disable()
        }
      } catch {
        // Autostart is best-effort; the UI stays usable if the OS denies it.
      }
    }

    void enableAutostart()
  }, [isNative, settings.launchAtLogin])

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

  const updateReminderDraft = (listId: TaskListId, value: string) => {
    setReminderDrafts((current) => ({ ...current, [listId]: value }))
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
      createTask(
        title,
        listId === 'today' ? 'Today' : 'Calendar',
        reminderDrafts[listId],
      ),
      ...tasks,
    ])
    setDrafts((current) => ({ ...current, [listId]: '' }))
    setReminderDrafts((current) => ({ ...current, [listId]: '' }))
    setCollapsedSections((current) => ({ ...current, [listId]: false }))
    setIsOpen(true)
  }

  const addCalendarTask = () => {
    const title = drafts.month.trim()

    if (!title) {
      return
    }

    const fallbackReminder = `${selectedCalendarKey}T09:00`

    setMonthTasks((tasks) => [
      createTask(
        title,
        `${formatCalendarDay(selectedCalendarDate)} · Calendar`,
        reminderDrafts.month || fallbackReminder,
      ),
      ...tasks,
    ])
    setDrafts((current) => ({ ...current, month: '' }))
    setReminderDrafts((current) => ({ ...current, month: '' }))
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

  const onCalendarDraftKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      addCalendarTask()
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

  const cycleTaskReminder = (listId: TaskListId, id: number) => {
    updateTasks(listId, (tasks) =>
      tasks.map((task) =>
        task.id === id
          ? { ...task, reminderAt: nextQuickReminder(task.reminderAt) }
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

  const updateCustomReminderDraft = (listId: string, value: string) => {
    setCustomReminderDrafts((current) => ({ ...current, [listId]: value }))
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
      createTask(title, list.title, customReminderDrafts[listId]),
      ...tasks,
    ])
    setCustomDrafts((current) => ({ ...current, [listId]: '' }))
    setCustomReminderDrafts((current) => ({ ...current, [listId]: '' }))
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

  const cycleCustomTaskReminder = (listId: string, taskId: number) => {
    updateCustomListTasks(listId, (tasks) =>
      tasks.map((task) =>
        task.id === taskId
          ? { ...task, reminderAt: nextQuickReminder(task.reminderAt) }
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
    setCustomReminderDrafts((current) => {
      const next = { ...current }
      delete next[listId]
      return next
    })
  }

  const toggleCustomListOnToday = (listId: string) => {
    setCustomLists((lists) =>
      lists.map((list) =>
        list.id === listId
          ? { ...list, showOnToday: !list.showOnToday }
          : list,
      ),
    )
  }

  const toggleCalendarTask = (
    source: CalendarTaskRef['source'],
    listId: string | undefined,
    taskId: number,
  ) => {
    if (source === 'custom') {
      if (listId) {
        toggleCustomTask(listId, taskId)
      }

      return
    }

    toggleTask(source, taskId)
  }

  const cycleCalendarTaskPriority = (
    source: CalendarTaskRef['source'],
    listId: string | undefined,
    taskId: number,
  ) => {
    if (source === 'custom') {
      if (listId) {
        cycleCustomTaskPriority(listId, taskId)
      }

      return
    }

    cycleTaskPriority(source, taskId)
  }

  const cycleCalendarTaskReminder = (
    source: CalendarTaskRef['source'],
    listId: string | undefined,
    taskId: number,
  ) => {
    if (source === 'custom') {
      if (listId) {
        cycleCustomTaskReminder(listId, taskId)
      }

      return
    }

    cycleTaskReminder(source, taskId)
  }

  const deleteCalendarTask = (
    source: CalendarTaskRef['source'],
    listId: string | undefined,
    taskId: number,
  ) => {
    if (source === 'custom') {
      if (listId) {
        deleteCustomTask(listId, taskId)
      }

      return
    }

    deleteTask(source, taskId)
  }

  const jumpCalendarToToday = () => {
    const today = new Date()

    setCalendarCursor(today)
    setSelectedCalendarKey(formatDateKey(today))
  }

  const focusSection = (section: SectionId) => {
    setActiveRailSection(section)
    setIsSettingsOpen(false)
    setIsOpen(true)
  }

  const openSettings = () => {
    setIsOpen(true)
    setIsSettingsOpen(true)
  }

  const closeSettings = () => {
    setIsSettingsOpen(false)
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
    '--task-row-height': `${settings.taskRowHeight}px`,
    '--task-gap': `${settings.taskGap}px`,
    '--task-title-size': `${settings.taskTextSize}px`,
    '--task-meta-size': `${Math.max(10, settings.taskTextSize - 1.5)}px`,
  } as CSSProperties

  return (
    <main
      className={`workspace ${isNative ? 'is-native' : 'is-web-preview'} ${
        isOpen ? 'is-sidebar-open' : 'is-sidebar-closed'
      } theme-${settings.theme} style-${settings.visualStyle}`}
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
              <kbd>Alt</kbd>
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
              onClose={closeSettings}
            />
          </div>
        ) : (
          <div className="sidebar-content">
            <header className="sidebar-header">
              <div className="app-lockup">
                <span className="app-icon">
                  <Check size={15} />
                </span>
                <div>
                  <strong>Todobar</strong>
                </div>
              </div>
            </header>

            <section className="focus-strip" aria-label="Planning status">
              <div>
                <strong>{progressPercent}%</strong>
                <span>today</span>
              </div>
              <div>
                <strong>{totalOpenTasks}</strong>
                <span>open</span>
              </div>
              <div>
                <strong>{customLists.length}</strong>
                <span>lists</span>
              </div>
            </section>

            <div className="view-stack" data-view={activeRailSection}>
              {activeRailSection === 'today' ? (
                <section
                  className="panel-section"
                  aria-labelledby="today-heading"
                  id="today-section"
                >
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
                        collapsedSections.today
                          ? 'Expand Today'
                          : 'Collapse Today'
                      }
                      aria-expanded={!collapsedSections.today}
                      onClick={() => toggleSection('today')}
                    >
                      {collapsedSections.today ? (
                        <Plus size={15} />
                      ) : (
                        <Minus size={15} />
                      )}
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
                        reminderValue={reminderDrafts.today}
                        placeholder="Add task..."
                        onChange={(value) => updateDraft('today', value)}
                        onReminderChange={(value) =>
                          updateReminderDraft('today', value)
                        }
                        onSubmit={() => addTask('today')}
                        onKeyDown={(event) => onDraftKeyDown(event, 'today')}
                      />
                      <div className="task-list">
                        {visibleTodayTasks.map((task, index) => (
                          <TaskRow
                            key={task.id}
                            task={task}
                            index={index}
                            onToggle={(id) => toggleTask('today', id)}
                            onPriority={(id) => cycleTaskPriority('today', id)}
                            onReminder={(id) => cycleTaskReminder('today', id)}
                            onDelete={(id) => deleteTask('today', id)}
                          />
                        ))}
                      </div>
                      {pinnedTodayLists.length > 0 ? (
                        <section
                          className="today-goals"
                          aria-label="Pinned lists on Today"
                        >
                          <div className="mini-heading">
                            <strong>Today goals</strong>
                            <span>{pinnedTodayLists.length} pinned</span>
                          </div>
                          <div className="pinned-list-stack">
                            {pinnedTodayLists.map((list) => {
                              const sortedTasks = sortTasks(list.tasks)
                              const visibleTasks = settings.showCompleted
                                ? sortedTasks
                                : sortedTasks.filter((task) => !task.done)

                              return (
                                <section
                                  className="today-goal-list"
                                  key={list.id}
                                  aria-label={`${list.title} goals`}
                                >
                                  <div className="today-goal-list-title">
                                    <strong>{list.title}</strong>
                                    <span>{list.tasks.length}</span>
                                  </div>
                                  {visibleTasks.length > 0 ? (
                                    <div className="task-list compact-task-list">
                                      {visibleTasks.map((task, index) => (
                                        <TaskRow
                                          key={task.id}
                                          task={task}
                                          index={index}
                                          onToggle={(id) =>
                                            toggleCustomTask(list.id, id)
                                          }
                                          onPriority={(id) =>
                                            cycleCustomTaskPriority(list.id, id)
                                          }
                                          onReminder={(id) =>
                                            cycleCustomTaskReminder(list.id, id)
                                          }
                                          onDelete={(id) =>
                                            deleteCustomTask(list.id, id)
                                          }
                                        />
                                      ))}
                                    </div>
                                  ) : (
                                    <p className="empty-list-note">
                                      No tasks in this list.
                                    </p>
                                  )}
                                </section>
                              )
                            })}
                          </div>
                        </section>
                      ) : null}
                    </div>
                  </div>
                </section>
              ) : null}

              {activeRailSection === 'calendar' ? (
                <section
                  className="panel-section calendar-section"
                  aria-labelledby="calendar-heading"
                  id="calendar-section"
                >
                  <div className="section-heading">
                    <div>
                      <span id="calendar-heading">
                        <CalendarDays size={15} />
                        Calendar
                        <em>{formatCalendarMonth(calendarCursor)}</em>
                      </span>
                    </div>
                    <div className="calendar-toolbar" aria-label="Calendar month">
                      <div className="calendar-nav">
                        <button
                          type="button"
                          aria-label="Previous month"
                          onClick={() =>
                            setCalendarCursor((current) => addMonths(current, -1))
                          }
                        >
                          <ChevronLeft size={15} />
                        </button>
                        <button
                          type="button"
                          aria-label="Next month"
                          onClick={() =>
                            setCalendarCursor((current) => addMonths(current, 1))
                          }
                        >
                          <ChevronRight size={15} />
                        </button>
                      </div>
                      <button
                        type="button"
                        className="calendar-today-button"
                        aria-label="Jump to today"
                        onClick={jumpCalendarToToday}
                      >
                        Today
                      </button>
                    </div>
                  </div>

                  <div className="calendar-board">
                    <div className="calendar-weekdays" aria-hidden="true">
                      {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map(
                        (day) => (
                          <span key={day}>{day}</span>
                        ),
                      )}
                    </div>
                    <div
                      className="calendar-grid"
                      role="grid"
                      aria-label={formatCalendarMonth(calendarCursor)}
                    >
                      {calendarDays.map((day) => (
                        <button
                          type="button"
                          key={day.key}
                          className={[
                            day.isCurrentMonth ? '' : 'is-muted',
                            day.isToday ? 'is-today' : '',
                            day.key === selectedCalendarKey ? 'is-selected' : '',
                            day.taskCount > 0 ? 'has-task' : '',
                          ]
                            .filter(Boolean)
                            .join(' ')}
                          aria-label={`${formatCalendarDay(day.date)}, ${
                            day.taskCount
                          } scheduled`}
                          aria-selected={day.key === selectedCalendarKey}
                          role="gridcell"
                          onClick={() => {
                            setSelectedCalendarKey(day.key)

                            if (!day.isCurrentMonth) {
                              setCalendarCursor(
                                new Date(
                                  day.date.getFullYear(),
                                  day.date.getMonth(),
                                  1,
                                ),
                              )
                            }
                          }}
                        >
                          <span>{day.date.getDate()}</span>
                          {day.taskCount > 0 ? (
                            <em>
                              {day.doneCount}/{day.taskCount}
                            </em>
                          ) : null}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="calendar-day-panel">
                    <div>
                      <strong>{formatCalendarDay(selectedCalendarDate)}</strong>
                      <span>
                        {selectedCalendarTasks.length === 0
                          ? 'Free'
                          : `${selectedCalendarTasks.length} scheduled`}
                      </span>
                    </div>
                    <QuickAdd
                      ariaLabel="Add a task to selected calendar day"
                      value={drafts.month}
                      reminderValue={
                        reminderDrafts.month || `${selectedCalendarKey}T09:00`
                      }
                      placeholder="Add to this day..."
                      onChange={(value) => updateDraft('month', value)}
                      onReminderChange={(value) =>
                        updateReminderDraft('month', value)
                      }
                      onSubmit={addCalendarTask}
                      onKeyDown={onCalendarDraftKeyDown}
                    />
                    {selectedCalendarTasks.length > 0 ? (
                      <div className="calendar-agenda">
                        <div className="task-list calendar-task-list">
                          {selectedCalendarTasks.map(
                            ({ listId, listTitle, source, task }, index) => (
                              <TaskRow
                                key={`${source}-${listId ?? 'base'}-${task.id}`}
                                task={{
                                  ...task,
                                  meta: `${formatReminder(task.reminderAt)} · ${listTitle}`,
                                }}
                                index={index}
                                onToggle={(id) =>
                                  toggleCalendarTask(source, listId, id)
                                }
                                onPriority={(id) =>
                                  cycleCalendarTaskPriority(source, listId, id)
                                }
                                onReminder={(id) =>
                                  cycleCalendarTaskReminder(source, listId, id)
                                }
                                onDelete={(id) =>
                                  deleteCalendarTask(source, listId, id)
                                }
                              />
                            ),
                          )}
                        </div>
                      </div>
                    ) : null}
                  </div>

                  {unscheduledMonthTasks.length > 0 ? (
                    <div className="calendar-backlog">
                      <div className="mini-heading">
                        <strong>Backlog</strong>
                        <span>{unscheduledMonthTasks.length} open</span>
                      </div>
                      <div className="task-list month-list">
                        {visibleMonthTasks
                          .filter((task) => !task.reminderAt)
                          .map((task, index) => (
                            <TaskRow
                              key={task.id}
                              task={task}
                              index={index}
                              onToggle={(id) => toggleTask('month', id)}
                              onPriority={(id) => cycleTaskPriority('month', id)}
                              onReminder={(id) => cycleTaskReminder('month', id)}
                              onDelete={(id) => deleteTask('month', id)}
                            />
                          ))}
                      </div>
                    </div>
                  ) : null}
                </section>
              ) : null}

              {activeRailSection === 'lists' ? (
                <section
                  className="panel-section list-section"
                  aria-labelledby="lists-heading"
                  id="lists-section"
                >
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
                    <button
                      type="button"
                      aria-label="Create list"
                      onClick={addCustomList}
                    >
                      <Plus size={16} />
                    </button>
                  </div>

                  <div className="custom-list-stack">
                    {customLists.map((list) => {
                      const sortedTasks = sortTasks(list.tasks)
                      const visibleTasks = settings.showCompleted
                        ? sortedTasks
                        : sortedTasks.filter((task) => !task.done)

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
                              className={`custom-list-pin ${
                                list.showOnToday ? 'is-pinned' : ''
                              }`}
                              aria-label={
                                list.showOnToday
                                  ? `Remove ${list.title} from Today`
                                  : `Show ${list.title} on Today`
                              }
                              aria-pressed={Boolean(list.showOnToday)}
                              onClick={() => toggleCustomListOnToday(list.id)}
                            >
                              <Pin size={13} />
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
                              {list.collapsed ? (
                                <Plus size={14} />
                              ) : (
                                <Minus size={14} />
                              )}
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
                                reminderValue={
                                  customReminderDrafts[list.id] ?? ''
                                }
                                placeholder="Add task..."
                                onChange={(value) =>
                                  updateCustomDraft(list.id, value)
                                }
                                onReminderChange={(value) =>
                                  updateCustomReminderDraft(list.id, value)
                                }
                                onSubmit={() => addCustomTask(list.id)}
                                onKeyDown={(event) =>
                                  onCustomDraftKeyDown(event, list.id)
                                }
                              />
                              {visibleTasks.length > 0 ? (
                                <div className="task-list">
                                  {visibleTasks.map((task, index) => (
                                    <TaskRow
                                      key={task.id}
                                      task={task}
                                      index={index}
                                      onToggle={(id) =>
                                        toggleCustomTask(list.id, id)
                                      }
                                      onPriority={(id) =>
                                        cycleCustomTaskPriority(list.id, id)
                                      }
                                      onReminder={(id) =>
                                        cycleCustomTaskReminder(list.id, id)
                                      }
                                      onDelete={(id) =>
                                        deleteCustomTask(list.id, id)
                                      }
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
              ) : null}
            </div>
          </div>
        )}
        <SidebarRail
          activeSection={activeRailSection}
          isSettingsOpen={isSettingsOpen}
          sectionOrder={settings.sectionOrder}
          onFocusSection={focusSection}
          onOpenSettings={openSettings}
        />
      </aside>
    </main>
  )
}

function SidebarRail({
  activeSection,
  isSettingsOpen,
  sectionOrder,
  onFocusSection,
  onOpenSettings,
}: {
  activeSection: SectionId
  isSettingsOpen: boolean
  sectionOrder: SectionId[]
  onFocusSection: (section: SectionId) => void
  onOpenSettings: () => void
}) {
  return (
    <nav className="sidebar-rail" aria-label="Todobar navigation">
      <div className="rail-stack">
        {sectionOrder.map((section) => (
          <button
            type="button"
            key={section}
            className={activeSection === section && !isSettingsOpen ? 'is-active' : ''}
            aria-label={`Jump to ${SECTION_LABELS[section]}`}
            aria-current={activeSection === section && !isSettingsOpen ? 'true' : undefined}
            onClick={() => onFocusSection(section)}
          >
            {section === 'today' ? <Clock3 size={16} /> : null}
            {section === 'calendar' ? <CalendarDays size={16} /> : null}
            {section === 'lists' ? <ListTodo size={16} /> : null}
            <span>{SECTION_LABELS[section].split(' ')[0]}</span>
          </button>
        ))}
      </div>
      <button
        type="button"
        className={isSettingsOpen ? 'is-active' : ''}
        aria-label="Sidebar settings"
        aria-pressed={isSettingsOpen}
        onClick={onOpenSettings}
      >
        <Settings size={16} />
        <span>Setup</span>
      </button>
    </nav>
  )
}

function QuickAdd({
  ariaLabel,
  value,
  reminderValue,
  placeholder,
  onChange,
  onReminderChange,
  onSubmit,
  onKeyDown,
}: {
  ariaLabel: string
  value: string
  reminderValue: string
  placeholder: string
  onChange: (value: string) => void
  onReminderChange: (value: string) => void
  onSubmit: () => void
  onKeyDown: (event: KeyboardEvent<HTMLInputElement>) => void
}) {
  const [isReminderOpen, setIsReminderOpen] = useState(false)
  const hasReminder = Boolean(reminderValue)

  return (
    <div
      className={`quick-add ${isReminderOpen ? 'is-reminder-open' : ''} ${
        hasReminder ? 'has-reminder' : ''
      }`}
    >
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
        className="reminder-toggle"
        aria-label={hasReminder ? 'Edit reminder time' : 'Add reminder time'}
        aria-pressed={hasReminder}
        onClick={() => setIsReminderOpen((current) => !current)}
      >
        {hasReminder ? <BellRing size={15} /> : <Bell size={15} />}
      </button>
      <button
        type="button"
        className="submit-task"
        aria-label="Add task"
        onClick={onSubmit}
      >
        <Plus size={16} />
      </button>
      {isReminderOpen ? (
        <div className="reminder-popover">
          <label>
            <span>Remind</span>
            <input
              aria-label="Reminder time"
              type="datetime-local"
              value={reminderValue}
              onChange={(event) => onReminderChange(event.target.value)}
            />
          </label>
          {hasReminder ? (
            <button
              type="button"
              aria-label="Clear reminder"
              onClick={() => onReminderChange('')}
            >
              <X size={13} />
            </button>
          ) : null}
        </div>
      ) : null}
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
  const selectedTheme =
    THEME_PRESETS.find((preset) => preset.id === settings.visualStyle) ??
    THEME_PRESETS[0]

  return (
    <section className="settings-panel" aria-label="Sidebar settings">
      <div className="settings-panel-header">
        <div>
          <strong>Todo settings</strong>
        </div>
        <div className="settings-actions">
          <button
            type="button"
            className="mode-toggle"
            aria-label={
              settings.theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'
            }
            onClick={() =>
              onChange({ theme: settings.theme === 'dark' ? 'light' : 'dark' })
            }
          >
            {settings.theme === 'dark' ? <Sun size={15} /> : <Moon size={15} />}
          </button>
          <button type="button" aria-label="Reset settings" onClick={onReset}>
            <RotateCcw size={15} />
          </button>
          <button type="button" aria-label="Close settings" onClick={onClose}>
            <X size={15} />
          </button>
        </div>
      </div>

      <div className="settings-group settings-appearance">
        <div className="settings-group-title">
          <Palette size={12} />
          Theme
        </div>
        <div className="theme-select-panel">
          <label className="theme-select-field">
            <span>Preset</span>
            <select
              aria-label="Theme preset"
              value={settings.visualStyle}
              onChange={(event) =>
                onChange({ visualStyle: event.target.value as ThemePreset })
              }
            >
              {THEME_PRESETS.map((preset) => (
                <option key={preset.id} value={preset.id}>
                  {preset.label}
                </option>
              ))}
            </select>
          </label>
          <p>{selectedTheme.note}</p>
        </div>
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Layout</div>
        <SectionOrderSetting
          order={settings.sectionOrder}
          onMove={(section, direction) =>
            onChange({
              sectionOrder: moveSectionOrder(
                settings.sectionOrder,
                section,
                direction,
              ),
            })
          }
        />
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Desktop</div>
        <ToggleSetting
          label="Launch at login"
          checked={settings.launchAtLogin}
          onChange={(launchAtLogin) => onChange({ launchAtLogin })}
        />
        <ToggleSetting
          label="Notifications"
          checked={settings.notificationsEnabled}
          onChange={(notificationsEnabled) => onChange({ notificationsEnabled })}
        />
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Window</div>
        <SliderSetting
          label="Panel width"
          value={settings.panelWidth}
          min={320}
          max={560}
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
        <div className="position-presets" aria-label="Handle position presets">
          <button
            type="button"
            className={settings.handleY <= 25 ? 'is-selected' : ''}
            onClick={() => onChange({ handleY: 10 })}
          >
            Top
          </button>
          <button
            type="button"
            className={
              settings.handleY > 25 && settings.handleY < 75 ? 'is-selected' : ''
            }
            onClick={() => onChange({ handleY: 50 })}
          >
            Middle
          </button>
          <button
            type="button"
            className={settings.handleY >= 75 ? 'is-selected' : ''}
            onClick={() => onChange({ handleY: 90 })}
          >
            Bottom
          </button>
        </div>
      </div>

      <div className="settings-group">
        <div className="settings-group-title">Tasks</div>
        <ToggleSetting
          label="Show completed"
          checked={settings.showCompleted}
          onChange={(showCompleted) => onChange({ showCompleted })}
        />
        <SliderSetting
          label="Row height"
          value={settings.taskRowHeight}
          min={40}
          max={62}
          step={1}
          suffix="px"
          onChange={(taskRowHeight) => onChange({ taskRowHeight })}
        />
        <SliderSetting
          label="Row gap"
          value={settings.taskGap}
          min={4}
          max={14}
          step={1}
          suffix="px"
          onChange={(taskGap) => onChange({ taskGap })}
        />
        <SliderSetting
          label="Text size"
          value={settings.taskTextSize}
          min={11}
          max={14}
          step={0.5}
          suffix="px"
          onChange={(taskTextSize) => onChange({ taskTextSize })}
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

function SectionOrderSetting({
  order,
  onMove,
}: {
  order: SectionId[]
  onMove: (section: SectionId, direction: -1 | 1) => void
}) {
  const labels: Record<SectionId, string> = {
    calendar: 'Calendar',
    lists: 'Lists',
    today: 'Today',
  }

  return (
    <div className="section-order-list" aria-label="Section order">
      {order.map((section, index) => (
        <div className="section-order-row" key={section}>
          <span>{labels[section]}</span>
          <div>
            <button
              type="button"
              aria-label={`Move ${labels[section]} up`}
              disabled={index === 0}
              onClick={() => onMove(section, -1)}
            >
              <ArrowUp size={13} />
            </button>
            <button
              type="button"
              aria-label={`Move ${labels[section]} down`}
              disabled={index === order.length - 1}
              onClick={() => onMove(section, 1)}
            >
              <ArrowDown size={13} />
            </button>
          </div>
        </div>
      ))}
    </div>
  )
}

function ToggleSetting({
  label,
  checked,
  onChange,
}: {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}) {
  return (
    <label className="toggle-setting">
      <span>
        <strong>{label}</strong>
      </span>
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
      />
      <em aria-hidden="true" />
    </label>
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

const TaskRow = memo(function TaskRow({
  task,
  index = 0,
  onToggle,
  onPriority,
  onReminder,
  onDelete,
}: {
  task: Task
  index?: number
  onToggle?: (id: number) => void
  onPriority?: (id: number) => void
  onReminder?: (id: number) => void
  onDelete?: (id: number) => void
}) {
  const priorityLabel =
    task.priority === 'focus'
      ? 'Focus'
      : task.priority === 'later'
        ? 'Later'
        : 'Task'
  const reminderLabel = formatReminder(task.reminderAt)

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
        <span>
          {task.meta}
          {reminderLabel ? (
            <em className="task-reminder">
              <Bell size={10} />
              {reminderLabel}
            </em>
          ) : null}
        </span>
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
          className={`reminder-button ${task.reminderAt ? 'is-set' : ''}`}
          aria-label={
            task.reminderAt
              ? `Reminder set for ${reminderLabel}. Click to change`
              : `Add reminder for ${task.title}`
          }
          onClick={() => onReminder?.(task.id)}
        >
          {task.reminderAt ? <BellRing size={13} /> : <Bell size={13} />}
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
}, areTaskRowsEqual)

function areTaskRowsEqual(
  previous: {
    task: Task
    index?: number
  },
  next: {
    task: Task
    index?: number
  },
) {
  return previous.task === next.task && previous.index === next.index
}

export default App
