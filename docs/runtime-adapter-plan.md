# Runtime Adapter Plan

The UI should stay framework-native React while platform behavior is isolated
behind adapters.

## Adapter Shape

```ts
type RuntimePlatform = 'web' | 'tauri' | 'electron'

type ShortcutCommand =
  | 'sidebar.toggle'
  | 'capture.open'
  | 'command.open'
  | 'task.completeFocused'

type DesktopRuntime = {
  platform: RuntimePlatform
  capabilities: {
    globalShortcuts: boolean
    tray: boolean
    autostart: boolean
    overlayWindow: boolean
    sqlite: boolean
    mcpSidecars: boolean
    secureSecrets: boolean
  }
  sidebar: {
    show(): Promise<void>
    hide(): Promise<void>
    toggle(): Promise<void>
    setEdge(edge: 'left' | 'right'): Promise<void>
    setWidth(width: number): Promise<void>
  }
  shortcuts: {
    register(shortcut: string, command: ShortcutCommand): Promise<void>
    unregister(shortcut: string): Promise<void>
    list(): Promise<Array<{ shortcut: string; command: ShortcutCommand }>>
  }
  app: {
    openSettings(): Promise<void>
    setLaunchAtLogin(enabled: boolean): Promise<void>
    getLaunchAtLogin(): Promise<boolean>
  }
}
```

## Web Adapter

The web adapter is for development only:

- uses browser keyboard events
- stores tasks in localStorage
- cannot register true global shortcuts
- cannot position native windows
- cannot run MCP sidecars

## Tauri Adapter

The Tauri adapter should own:

- global shortcut plugin
- window positioning
- tray/menu bar
- autostart plugin
- SQL plugin
- shell/sidecar process management for local MCP
- updater integration

## Electron Adapter

Only build this if the Tauri spike fails on core overlay behavior.

The Electron adapter would own:

- BrowserWindow creation
- globalShortcut registration
- Tray
- autoUpdater
- screen/display APIs
- secure storage integration

## Rule

React components should never import Tauri or Electron packages directly.
Components dispatch commands; adapters execute platform behavior.
