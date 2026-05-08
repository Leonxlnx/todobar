import { useEffect, useState } from 'react'

const STORAGE_KEY = 'todobar.sidebar.settings.v19'

export type ThemeMode = 'light' | 'dark'
export type VisualStyle = 'minimal' | 'glass' | 'brutal' | 'skeuo'

export type SidebarSettings = {
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
  visualStyle: VisualStyle
}

export const defaultSidebarSettings: SidebarSettings = {
  panelWidth: 340,
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
  visualStyle: 'minimal',
}

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max)

function sanitizeSettings(value: Partial<SidebarSettings>): SidebarSettings {
  return {
    panelWidth: clamp(
      value.panelWidth ?? defaultSidebarSettings.panelWidth,
      300,
      500,
    ),
    tabWidth: clamp(value.tabWidth ?? defaultSidebarSettings.tabWidth, 34, 62),
    handleHeight: clamp(
      value.handleHeight ?? defaultSidebarSettings.handleHeight,
      72,
      132,
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
    theme: value.theme === 'dark' ? 'dark' : 'light',
    visualStyle: ['minimal', 'glass', 'brutal', 'skeuo'].includes(
      value.visualStyle ?? '',
    )
      ? (value.visualStyle as VisualStyle)
      : defaultSidebarSettings.visualStyle,
  }
}

export function useSidebarSettings() {
  const [settings, setSettings] = useState<SidebarSettings>(() => {
    const params = new URLSearchParams(window.location.search)
    const themeParam = params.get('theme')

    try {
      const stored = window.localStorage.getItem(STORAGE_KEY)
      const base = stored
        ? sanitizeSettings(JSON.parse(stored) as Partial<SidebarSettings>)
        : defaultSidebarSettings

      return sanitizeSettings({
        ...base,
        theme:
          themeParam === 'dark' || themeParam === 'light'
            ? themeParam
            : base.theme,
      })
    } catch {
      return sanitizeSettings({
        ...defaultSidebarSettings,
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
