# Changelog

## Unreleased

### Added

- Added a redesigned sidebar shell with a right-side command rail and compact
  planning status strip.
- Added a theme dropdown with Codex, Quartz Glass, Frost, Paper, Graphite,
  Midnight, Clay, and Blueprint presets on top of light and dark color modes.
- Added reminder capture controls, quick reminder cycling on task rows, and
  native Tauri notification support with browser preview fallback.
- Added section ordering controls so Today, Month Plan, and Lists can be
  rearranged from settings.

### Changed

- Removed the browser tab-restore shortcut chord as a default. `Alt + T` is now
  the primary toggle shortcut, with `Ctrl/Cmd + Alt + T` as a fallback.
- Reworked task sections from stacked cards into calmer command rows with
  subtler action chrome and no hover lift.
- Increased the default sidebar width for a less cramped open-source first-run
  experience while keeping panel width configurable.
- Made task and quick-add hover states calmer so controls no longer float upward
  during normal pointer movement.
- Memoized task rows and added lightweight rendering containment for long task
  lists.

## 0.1.5 - Native Shortcut Fix

### Fixed

- Moved the toggle shortcut registration from the focused React webview into the
  native Tauri layer, so the global toggle can open Todobar from another app
  without clicking the sidebar first.

### Added

- Added `Ctrl/Cmd + Alt + T` as a global toggle fallback.
- Extended project verification so native shortcut registration remains covered.

### Verified

- `v0.1.5` artifacts were published for Windows, macOS Apple Silicon, and
  macOS Intel, with release digests recorded in the verification notes.

## 0.1.4 - Desktop Control and Product Vision

### Added

- Native tray/menu-bar control for opening Todobar, opening settings, and
  quitting the app while the sidebar stays out of the taskbar.
- Product vision documentation covering Version 0, planner upgrades,
  notifications, AI assistant direction, MCP/Gmail/context connectors, and the
  future companion manager surface.

### Verified

- `v0.1.4` artifacts were published for Windows, macOS Apple Silicon, and
  macOS Intel, with release digests recorded in the verification notes.

## 0.1.3 - Responsive Edge Polish

### Fixed

- Fixed a 2px handle clipping issue in narrow open-sidebar browser preview
  layouts.

### Added

- Responsive Playwright checks for desktop, narrow, and short viewports.
- Release verification documentation for the responsive edge-polish release.

### Verified

- Browser smoke tests now cover the task flow plus responsive sidebar bounds.
- `v0.1.3` artifacts were published for Windows, macOS Apple Silicon, and
  macOS Intel, with release digests recorded in the verification notes.

## 0.1.2 - Reliability and Open Source Polish

### Added

- Completed-task visibility setting so finished tasks can be hidden without
  deleting them.
- Playwright smoke test for the sidebar, settings drawer, and completed-task
  visibility flow.
- No-bundle native Tauri build smoke test for Windows and macOS CI.
- Release verification documentation for the stable release.

### Changed

- Updated GitHub Actions workflow dependencies.
- Made install instructions version-agnostic to avoid stale release filenames.
- Linked release gaps to public GitHub Issues.
- Expanded project verification to guard scripts, docs links, release versions,
  and workflow expectations.

### Verified

- CI runs project verification, TypeScript/Vite build, ESLint, Playwright smoke
  tests, Rust check, and native Tauri build smoke on Windows and macOS.
- `v0.1.2` release artifacts were published for Windows, macOS Apple Silicon,
  and macOS Intel.

## 0.1.1

- Added launch-at-login control in settings.
- Added task row height, row gap, and text size customization.
- Improved narrow viewport task action spacing.
- Clarified Windows and macOS installation docs.
- Added platform support documentation.
- Added GitHub issue and pull request templates.

## 0.1.0

- Added native right-edge sidebar shell.
- Added draggable open/close handle.
- Added global shortcut support.
- Added autostart on login.
- Added single-instance desktop behavior.
- Added Today, Month Plan, and custom lists.
- Added add, complete, priority, collapse, and delete actions.
- Added light and dark themes.
- Added local persistence for prototype task data.
- Added Windows and macOS release workflows.
