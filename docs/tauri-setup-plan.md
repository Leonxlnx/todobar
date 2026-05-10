# Tauri Setup Plan

This is the practical path for turning the current Vite/React preview into a
native desktop spike.

## Prerequisites

Windows development needs:

- Microsoft C++ Build Tools
- WebView2
- Rust toolchain
- Node/npm

macOS development needs:

- Xcode or Xcode Command Line Tools
- Rust toolchain
- Node/npm
- Apple Developer account later for signing/notarization

## Add Tauri To Existing Vite App

Use the existing React/Vite app as the frontend. Do not scaffold a separate app.

Expected high-level steps:

```bash
npm install -D @tauri-apps/cli
npx tauri init
```

Then align:

- frontend dev URL with Vite
- dist directory with Vite build output
- app identifier
- product name
- icons

## Plugins To Add In Native Spike

Minimum:

- global shortcut
- autostart
- SQL
- shell, only if sidecars/MCP process spike is included

Possible soon:

- dialog for import/export
- opener for file paths/URLs
- separate notification window only if in-app reminder toasts need to appear
  while the sidebar remains fully closed

## Window Configuration

Create two windows:

### Sidebar Window

- frameless
- right-edge placement
- hidden at startup or visible in dev
- skip taskbar if platform behavior is clean
- always on top, if reliable
- fixed desktop width
- no transparency dependency

### Settings Window

- normal decorated window
- taskbar/dock visible
- opens from tray/sidebar command
- contains shortcuts, launch at login, data, AI, connectors

## Capabilities

Keep capabilities narrow by window.

Sidebar should get only:

- basic window commands
- shortcut-triggered state events
- task data APIs

Settings can get:

- shortcut registration settings
- autostart enable/disable
- export/import dialogs
- connector configuration

MCP/sidecar permissions should be separate and disabled until the user enables a
connector.

## First Native Commands

Rust/Tauri commands should stay thin:

- get monitor info
- show sidebar
- hide sidebar
- move sidebar to monitor edge
- register shortcut
- unregister shortcut
- open settings
- get app paths

Business logic stays in TypeScript domain/storage until SQLite needs Rust-side
help.

## SQLite

Use Tauri SQL plugin with SQLite first.

Start with migrations for:

- tasks
- lists
- task_events
- sources
- suggestions
- audit_events

Keep web preview storage behind the same repository interface.

## MCP Sidecar Spike

Only after sidebar/window behavior passes:

- bundle one tiny local sidecar
- run via Tauri sidecar/shell capability
- communicate over stdio JSON-RPC
- kill/restart sidecar from settings
- log process lifecycle only, not private output by default

## Acceptance

The native spike is successful only when:

- shortcut opens from another app
- sidebar appears at the right screen edge
- closing returns control to the previous app
- task data persists through restart
- launch-at-login setting works
- packaging produces test artifacts

## Sources

- Tauri prerequisites: https://v2.tauri.app/start/prerequisites/
- Tauri create project: https://v2.tauri.app/start/create-project/
- Tauri global shortcut: https://v2.tauri.app/plugin/global-shortcut/
- Tauri autostart: https://v2.tauri.app/plugin/autostart/
- Tauri SQL: https://v2.tauri.app/plugin/sql/
- Tauri capabilities: https://v2.tauri.app/security/capabilities/
- Tauri window API: https://v2.tauri.app/reference/javascript/api/namespacewindow/
- Tauri sidecars: https://tauri.app/develop/sidecar/
