# Technical Decisions

## 1. Tauri First, Electron As Fallback

Use Tauri v2 for the first native spike. The product is a small utility, so
startup speed, memory use, and small bundles matter. Electron remains a fallback
if overlay-window behavior, always-on-top behavior, or multi-monitor behavior
blocks the core UX.

Status: accepted for next milestone.

## 2. React UI, Runtime Adapter Underneath

The UI should not import Tauri or Electron directly. It should call a runtime
adapter.

```ts
type DesktopRuntime = {
  platform: 'web' | 'tauri' | 'electron'
  showSidebar(): Promise<void>
  hideSidebar(): Promise<void>
  toggleSidebar(): Promise<void>
  registerShortcut(shortcut: string, command: string): Promise<void>
  openSettings(): Promise<void>
}
```

Status: accepted.

## 3. Local-First Storage

The web preview can use `localStorage`, but the desktop app should use SQLite
with migrations. The task database is the source of truth. Sync is a later
feature.

Status: accepted.

## 4. AI Creates Suggestions, Not Silent Writes

AI features should return structured proposals. The user approves them before
they become tasks.

Status: accepted.

## 5. MCP Is Permissioned Infrastructure

MCP connectors should flow through a client adapter and permission system.
Todobar should display available tools/resources/prompts, active scopes, and
tool-call audit events.

Status: accepted.

## 6. Native Widgets Are Not Milestone 1

Windows widgets and macOS WidgetKit are separate extension surfaces. The first
product surface is the right-edge overlay. Widgets can later show glanceable
counts or Today items.

Status: accepted.

## 7. Shortcuts Must Be Editable

Global shortcuts can fail if owned by another app or blocked by the operating
system. Todobar must offer editable shortcuts and graceful failure states.

Status: accepted.

## 8. No Hidden External Context

Todobar should never silently read browser history, open tabs, files, email, or
calendar data. Those are future connector scopes and require explicit user
approval.

Status: accepted.
