# Changelog

## Unreleased

### Added

- Added collapsible settings groups to keep the settings drawer easier to scan.
- Added a non-connected Gmail MCP connector surface that documents the
  permission boundary without reading email data.
- Added Calendar entry mode controls so selected-day capture can create tasks
  or events.
- Added a hover-only visible tab setting for users who want the edge handle to
  stay hidden until the screen edge is hovered.
- Added persistent settings group collapse state.

### Changed

- Made task completion and reopening feel smoother without replaying the row
  entrance animation.
- Flattened pinned list and custom list sections to avoid card-in-card layouts.
- Reduced heavy dark-mode gradients, rail icon bevels, and settings control
  contrast.
- Reworked the themed scrollbar styling for the sidebar, settings, and theme
  picker surfaces.
- Reworked panel-width Apply feedback to stay pale instead of switching to a
  bright blue Done state.
- Added pointer-based section reordering in settings so Today, Calendar, and
  Lists can be dragged more reliably.
- Clarified the Surface opacity setting and the Gmail MCP permission boundary.

## 0.1.7 - Performance and Personalization

### Added

- Added task sorting controls so lists can stay priority-first, newest-first,
  or oldest-first without changing the stored tasks.
- Added custom backdrop image support with strength, dim, and blur controls for
  users who want a more personal desktop overlay.
- Added a 10-minute snooze action to custom reminder toasts.

### Changed

- Debounced local task, custom-list, and settings persistence so normal typing
  and slider movement do less synchronous storage work.
- Indexed calendar reminder counts once per render instead of filtering all
  tasks for every visible calendar day.
- Reworked the staged panel-width setting so Apply no longer gets squeezed next
  to the range control.

### Fixed

- Fixed light-mode reminder toast colors so in-app alerts follow the active
  theme instead of looking like a dark OS notification.

## 0.1.6 - Sidebar Polish and Reminder Badges

### Added

- Added a redesigned sidebar shell with a right-side command rail and compact
  planning status strip.
- Added a custom mode-aware theme dropdown with five light presets and five dark
  presets.
- Added reminder capture controls, quick reminder cycling on task rows, and
  custom Todobar reminder toasts that avoid OS notification chrome.
- Added section ordering controls so Today, Calendar, and Lists can be
  rearranged from settings.
- Added real Calendar navigation with selected-day capture and task actions.
- Added right, left, and wider top dock placement modes.
- Added pinned custom lists on Today for goal groups.
- Added inline task editing and custom list renaming.

### Changed

- Removed the browser tab-restore shortcut chord as a default. `Alt + T` is now
  the primary toggle shortcut, with `Alt + Shift + T` as a fallback.
- Removed the translucent Lumen/Smoke preset and kept the picker focused on
  solid, readable light and dark themes.
- Widened the top dock layout and expanded handle size controls.
- Added confirmation UI for reset, task delete, and custom-list delete while
  preserving Shift-click as the immediate delete path.
- Reworked task sections from stacked cards into calmer command rows with
  subtler action chrome and no hover lift.
- Increased the default sidebar width for a less cramped open-source first-run
  experience while keeping panel width configurable.
- Made task and quick-add hover states calmer so controls no longer float upward
  during normal pointer movement.
- Memoized task rows and added lightweight rendering containment for long task
  lists.
- Simplified the sidebar rail into icon-only controls to avoid clipped labels
  and keep the panel calmer.
- Replaced handle top/middle/bottom presets with clearer screen-edge docking
  controls for right, left, and top placement.
- Replaced the top stats strip with a single quiet Today progress bar.
- Reworked settings sizing so panel width is staged with an Apply action instead
  of resizing the window while the slider moves.
- Combined window and handle controls into a tighter settings group and ordered
  Screen Edge controls as Left, Right, then Top.
- Refined task, calendar, settings, and list buttons with square hit targets,
  soft-ui bevels, pressed states, and task action tooltips.
- Removed the duplicate per-section Today meter so the sidebar has one progress
  bar at the top.
- Added drag-and-drop section ordering while keeping the small arrow controls as
  an accessible fallback.
- Moved reminder alerts to a bottom toast position and added a red handle badge
  for due reminders when the sidebar is closed.

### Fixed

- Fixed clipped quick reminder controls by letting the reminder editor expand in
  flow instead of rendering outside its section.
- Fixed reminder date focus styling so it no longer shows a harsh blue ring that
  appears cut off.
- Fixed native monitor targeting by resolving the monitor under the cursor before
  falling back to the current window monitor.
- Fixed reminder alerts opening the full sidebar automatically when they become
  due.
- Fixed task action tooltips and delete confirmations being trapped under task
  row layers.
- Fixed startup monitor targeting in the native shell by using the cursor monitor
  before falling back to the current window monitor.

### Verified

- Local Windows checks cover project verification, linting, production build,
  Playwright smoke tests, and the no-bundle native Tauri build.
- Release automation publishes Windows, macOS Apple Silicon, and macOS Intel
  artifacts from the `v0.1.6` tag.

## 0.1.5 - Native Shortcut Fix

### Fixed

- Moved the toggle shortcut registration from the focused React webview into the
  native Tauri layer, so the global toggle can open Todobar from another app
  without clicking the sidebar first.

### Added

- Added `Alt + Shift + T` as a global toggle fallback.
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
