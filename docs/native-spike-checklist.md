# Native Spike Checklist

The native spike should answer one question: can Tauri deliver the Todobar core
experience cleanly on macOS and Windows?

## Setup

- Add `@tauri-apps/cli`.
- Initialize `src-tauri`.
- Configure Vite dev server at `http://localhost:5173`.
- Configure production frontend output as `dist`.
- Add app identifier, icon placeholders, and app name.

## Required Plugins

- global shortcut
- SQL
- autostart
- updater
- window state or custom placement persistence
- optional: single instance
- optional: notifications
- optional: stronghold/keychain for secrets later

## Sidebar Window

Test these options:

- undecorated/frameless
- fixed width around 380-420px
- full work-area height
- right-edge placement
- always-on-top
- skip taskbar
- focus behavior on open/close
- no transparent-window dependency
- smooth show/hide animation handled by UI where possible

## Shortcut Tests

- show/hide shortcut works while another app is focused
- quick capture shortcut works while another app is focused
- registration failure is detected
- shortcuts are configurable
- shortcuts unregister on quit
- repeated open/close does not leave stale windows

## Multi-Monitor Tests

- single monitor
- two monitors with same DPI
- two monitors with different DPI
- laptop + external monitor
- Windows taskbar left/right/bottom
- macOS Spaces / virtual desktops
- active monitor detection
- remember last monitor and edge

## Focus Tests

- open from editor
- open from browser
- open from fullscreen-ish app
- close with Escape
- click outside behavior
- quick capture focus lands in input
- show/hide does not break current app focus unnecessarily

## Distribution Tests

- unsigned dev build works locally
- Windows installer builds
- macOS app bundle builds
- code signing path documented
- updater artifacts generated
- app launch at login works
- tray/menu bar actions work

## Success Criteria

- Opening feels instant.
- Sidebar appears on the expected screen edge.
- Text input is focused when quick capture opens.
- Closing returns the user to work.
- No visible title bar or awkward native frame.
- No severe jank after repeated toggles.
- Known platform quirks are documented before building more features.
