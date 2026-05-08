import { useEffect, useState } from 'react'

const STORAGE_KEY = 'todobar.sidebar.settings.v18'

export type ThemeMode = 'light' | 'dark' | 'glass'

export type SidebarSettings = {
  panelWidth: number
  tabWidth: number
  handleHeight: number
  handleY: number
  motionMs: number
  panelRadius: number
  translucency: number
  taskRowHeight: number
  taskGap: number
  taskTextSize: number
  showCompleted: boolean
  launchAtLogin: boolean
  theme: ThemeMode
}

export const defaultSidebarSettings: SidebarSettings = {
  panelWidth: 340,
  tabWidth: 42,
  handleHeight: 84,
  handleY: 50,
  motionMs: 230,
  panelRadius: 18,
  translucency: 24,
  taskRowHeight: 44,
  taskGap: 7,
  taskTextSize: 12.5,
  showCompleted: true,
  launchAtLogin: true,
  theme: 'light',
}

const clamp = (value: number, min: number, max: number) =>
  Math.min(Math.max(value, min), max)

const surfaceAlphaToTranslucency = (surfaceAlpha: number) =>
  Math.round(((1 - surfaceAlpha / 100) / 0.95) * 100)

export const translucencyToSurfaceAlpha = (translucency: number) =>
  Number((1 - (clamp(translucency, 0, 100) / 100) * 0.95).toFixed(3))

function readTranslucency(
  value: Partial<SidebarSettings> & { surfaceAlpha?: number },
) {
  if (value.translucency !== undefined) {
    return value.translucency
  }

  if (value.surfaceAlpha !== undefined) {
    return surfaceAlphaToTranslucency(value.surfaceAlpha)
  }

  return defaultSidebarSettings.translucency
}

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
    translucency: clamp(readTranslucency(value), 0, 100),
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
    showCompleted:
      value.showCompleted ?? defaultSidebarSettings.showCompleted,
    launchAtLogin:
      value.launchAtLogin ?? defaultSidebarSettings.launchAtLogin,
    theme:
      value.theme === 'glass'
        ? 'glass'
        : value.theme === 'dark'
          ? 'dark'
          : 'light',
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
          themeParam === 'dark' ||
          themeParam === 'light' ||
          themeParam === 'glass'
            ? themeParam
            : base.theme,
      })
    } catch {
      return sanitizeSettings({
        ...defaultSidebarSettings,
        theme:
          themeParam === 'dark' ||
          themeParam === 'light' ||
          themeParam === 'glass'
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
