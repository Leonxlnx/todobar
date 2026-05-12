import { useEffect, useState } from 'react'
import { scheduleLocalStorageWrite } from './storage'

const STORAGE_KEY = 'todobar.sidebar.settings.v27'

export type DockEdge = 'right' | 'left' | 'top' | 'bottom'
export type ThemeMode = 'light' | 'dark'
export type TaskSortMode = 'priority' | 'newest' | 'oldest'
export type TabVisibility = 'always' | 'hover'
export type ThemePreset =
  | 'codex'
  | 'porcelain'
  | 'frost'
  | 'paper'
  | 'carbon'
  | 'graphite'
  | 'midnight'
  | 'clay'
  | 'blueprint'
export type SectionId = 'today' | 'calendar' | 'lists'

export const themePresetsByMode: Record<ThemeMode, ThemePreset[]> = {
  dark: ['codex', 'carbon', 'graphite', 'midnight', 'clay', 'blueprint'],
  light: ['codex', 'porcelain', 'frost', 'paper', 'clay', 'blueprint'],
}

export type SidebarSettings = {
  dockEdge: DockEdge
  panelWidth: number
  tabVisibility: TabVisibility
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
  taskSortMode: TaskSortMode
  theme: ThemeMode
  visualStyle: ThemePreset
  sectionOrder: SectionId[]
  backdropImage: string
  backdropImageName: string
  backdropOpacity: number
  backdropBlur: number
  backdropDim: number
}

export const defaultSidebarSettings: SidebarSettings = {
  dockEdge: 'right',
  panelWidth: 400,
  tabVisibility: 'always',
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
  taskSortMode: 'priority',
  theme: 'light',
  visualStyle: 'codex',
  sectionOrder: ['today', 'calendar', 'lists'],
  backdropImage: '',
  backdropImageName: '',
  backdropOpacity: 76,
  backdropBlur: 0,
  backdropDim: 18,
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
  const taskSortMode =
    value.taskSortMode === 'newest' || value.taskSortMode === 'oldest'
      ? value.taskSortMode
      : 'priority'
  const tabVisibility = value.tabVisibility === 'hover' ? 'hover' : 'always'
  const backdropImage =
    typeof value.backdropImage === 'string' &&
    value.backdropImage.startsWith('data:image/')
      ? value.backdropImage
      : ''
  const backdropImageName =
    backdropImage && typeof value.backdropImageName === 'string'
      ? value.backdropImageName.slice(0, 80)
      : ''

  return {
    dockEdge,
    panelWidth: clamp(
      value.panelWidth ?? defaultSidebarSettings.panelWidth,
      320,
      560,
    ),
    tabVisibility,
    tabWidth: clamp(value.tabWidth ?? defaultSidebarSettings.tabWidth, 22, 112),
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
      58,
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
    taskSortMode,
    theme,
    visualStyle,
    sectionOrder: sanitizeSectionOrder(value.sectionOrder),
    backdropImage,
    backdropImageName,
    backdropOpacity: clamp(
      value.backdropOpacity ?? defaultSidebarSettings.backdropOpacity,
      30,
      100,
    ),
    backdropBlur: clamp(
      value.backdropBlur ?? defaultSidebarSettings.backdropBlur,
      0,
      18,
    ),
    backdropDim: clamp(
      value.backdropDim ?? defaultSidebarSettings.backdropDim,
      0,
      70,
    ),
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
    return scheduleLocalStorageWrite(STORAGE_KEY, JSON.stringify(settings), 700)
  }, [settings])

  const updateSettings = (patch: Partial<SidebarSettings>) => {
    setSettings((current) => sanitizeSettings({ ...current, ...patch }))
  }

  const resetSettings = () => setSettings(defaultSidebarSettings)

  return [settings, updateSettings, resetSettings] as const
}
