import { useEffect, useState } from 'react'

const STORAGE_KEY = 'todobar.sidebar.settings.v27'

export type DockEdge = 'right' | 'left' | 'top' | 'bottom'
export type ThemeMode = 'light' | 'dark'
export type ThemePreset =
  | 'codex'
  | 'frost'
  | 'paper'
  | 'graphite'
  | 'midnight'
  | 'clay'
  | 'blueprint'
export type SectionId = 'today' | 'calendar' | 'lists'

export const themePresetsByMode: Record<ThemeMode, ThemePreset[]> = {
  dark: ['codex', 'graphite', 'midnight', 'clay', 'blueprint'],
  light: ['codex', 'frost', 'paper', 'clay', 'blueprint'],
}

export type SidebarSettings = {
  dockEdge: DockEdge
  panelWidth: number
  tabWidth: number
  handleHeight: number
  handleY: number
  motionMs: number
  panelRadius: number
  surfaceAlpha: number
  taskRowHeight: number
  taskGap: number
  taskTextSize: number
  showCompleted: boolean
  launchAtLogin: boolean
  notificationsEnabled: boolean
  theme: ThemeMode
  visualStyle: ThemePreset
  sectionOrder: SectionId[]
}

export const defaultSidebarSettings: SidebarSettings = {
  dockEdge: 'right',
  panelWidth: 400,
  tabWidth: 42,
  handleHeight: 84,
  handleY: 50,
  motionMs: 230,
  panelRadius: 18,
  surfaceAlpha: 96,
  taskRowHeight: 44,
  taskGap: 7,
  taskTextSize: 12.5,
  showCompleted: true,
  launchAtLogin: true,
  notificationsEnabled: true,
  theme: 'light',
  visualStyle: 'codex',
  sectionOrder: ['today', 'calendar', 'lists'],
}

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max)

const validSectionIds: SectionId[] = ['today', 'calendar', 'lists']
const legacySectionIds: Record<string, SectionId> = {
  month: 'calendar',
}

function sanitizeSectionOrder(value?: SectionId[]) {
  const incoming = Array.isArray(value) ? value : []
  const normalized = incoming
    .map((section) => legacySectionIds[section] ?? section)
    .filter((section): section is SectionId => validSectionIds.includes(section))
  const unique = normalized.filter(
    (section, index) => normalized.indexOf(section) === index,
  )

  return [
    ...unique,
    ...validSectionIds.filter((section) => !unique.includes(section)),
  ]
}

function sanitizeSettings(value: Partial<SidebarSettings>): SidebarSettings {
  const theme: ThemeMode = value.theme === 'dark' ? 'dark' : 'light'
  const availableThemes = themePresetsByMode[theme]
  const visualStyle = availableThemes.includes(value.visualStyle as ThemePreset)
    ? (value.visualStyle as ThemePreset)
    : availableThemes[0]
  const dockEdge =
    value.dockEdge === 'left' ||
    value.dockEdge === 'right' ||
    value.dockEdge === 'top'
      ? value.dockEdge
      : value.dockEdge === 'bottom'
        ? 'top'
        : 'right'

  return {
    dockEdge,
    panelWidth: clamp(
      value.panelWidth ?? defaultSidebarSettings.panelWidth,
      320,
      560,
    ),
    tabWidth: clamp(value.tabWidth ?? defaultSidebarSettings.tabWidth, 26, 88),
    handleHeight: clamp(
      value.handleHeight ?? defaultSidebarSettings.handleHeight,
      56,
      176,
    ),
    handleY: clamp(value.handleY ?? defaultSidebarSettings.handleY, 0, 100),
    motionMs: clamp(value.motionMs ?? defaultSidebarSettings.motionMs, 140, 360),
    panelRadius: clamp(
      value.panelRadius ?? defaultSidebarSettings.panelRadius,
      12,
      28,
    ),
    surfaceAlpha: clamp(
      value.surfaceAlpha ?? defaultSidebarSettings.surfaceAlpha,
      86,
      100,
    ),
    taskRowHeight: clamp(
      value.taskRowHeight ?? defaultSidebarSettings.taskRowHeight,
      40,
      62,
    ),
    taskGap: clamp(value.taskGap ?? defaultSidebarSettings.taskGap, 4, 14),
    taskTextSize: clamp(
      value.taskTextSize ?? defaultSidebarSettings.taskTextSize,
      11,
      14,
    ),
    showCompleted: value.showCompleted ?? defaultSidebarSettings.showCompleted,
    launchAtLogin:
      value.launchAtLogin ?? defaultSidebarSettings.launchAtLogin,
    notificationsEnabled:
      value.notificationsEnabled ??
      defaultSidebarSettings.notificationsEnabled,
    theme,
    visualStyle,
    sectionOrder: sanitizeSectionOrder(value.sectionOrder),
  }
}

export function useSidebarSettings() {
  const [settings, setSettings] = useState<SidebarSettings>(() => {
    const params = new URLSearchParams(window.location.search)
    const themeParam = params.get('theme')
    const dockParam = params.get('dock')

    try {
      const stored = window.localStorage.getItem(STORAGE_KEY)
      const base = stored
        ? sanitizeSettings(JSON.parse(stored) as Partial<SidebarSettings>)
        : defaultSidebarSettings

      return sanitizeSettings({
        ...base,
        dockEdge:
          dockParam === 'left' ||
          dockParam === 'right' ||
          dockParam === 'top'
            ? dockParam
            : base.dockEdge,
        theme:
          themeParam === 'dark' || themeParam === 'light'
            ? themeParam
            : base.theme,
      })
    } catch {
      return sanitizeSettings({
        ...defaultSidebarSettings,
        dockEdge:
          dockParam === 'left' ||
          dockParam === 'right' ||
          dockParam === 'top'
            ? dockParam
            : defaultSidebarSettings.dockEdge,
        theme:
          themeParam === 'dark' || themeParam === 'light'
            ? themeParam
            : defaultSidebarSettings.theme,
      })
    }
  })

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
    } catch {
      // Desktop builds can later move this into a settings repository.
    }
  }, [settings])

  const updateSettings = (patch: Partial<SidebarSettings>) => {
    setSettings((current) => sanitizeSettings({ ...current, ...patch }))
  }

  const resetSettings = () => setSettings(defaultSidebarSettings)

  return [settings, updateSettings, resetSettings] as const
}
