# Todobar

[![CI](https://github.com/Leonxlnx/todobar/actions/workflows/ci.yml/badge.svg)](https://github.com/Leonxlnx/todobar/actions/workflows/ci.yml)
[![Release](https://github.com/Leonxlnx/todobar/actions/workflows/release.yml/badge.svg)](https://github.com/Leonxlnx/todobar/actions/workflows/release.yml)

Todobar is a native dockable todo sidebar for macOS and Windows. It stays out of
the way as a small edge handle, opens into a clean task panel, and is built for
fast capture while you work in other apps.

The app is early, but the foundation is real: Tauri v2, React, local persisted
tasks, reminders, native notifications, global shortcuts, autostart,
single-instance behavior, and release automation for Windows and macOS.

## Why

Most todo apps are full windows. Todobar is designed as a desktop utility:

- always reachable from the edge of the screen
- fast enough for one-line capture
- clean enough to leave visible during focused work
- local-first by default
- ready for future AI and MCP integrations without making AI mandatory

## Current Features

- Dockable native sidebar for Windows and macOS
- Right, left, and wider top dock placement modes
- Small draggable handle that opens and closes the panel
- Global shortcut: `Alt + T`, with `Alt + Shift + T` as a fallback
- Native tray/menu-bar control for open, settings, and quit
- Autostart on system login
- Single-instance desktop behavior
- Always-on-top frameless window
- Click-through transparent area outside the handle and open panel
- Compact icon command rail for Today, Calendar, Lists, and setup
- Planning status strip with today's progress, open task count, and list count
- Today tasks
- Real calendar view with month navigation, selected-day capture, and scheduled
  task actions
- Custom task lists
- Pin custom lists onto Today as goal groups
- Add, edit, complete, prioritize, collapse, and delete tasks
- Rename and delete custom lists
- Add reminder times while capturing tasks
- Quick reminder cycling on existing tasks
- Clean icon rail for Today, Calendar, Lists, and Settings
- Native notification plugin support with browser fallback in preview
- Optional completed-task visibility
- Local persistence through browser storage
- Light and dark modes with matching mode-specific theme presets
- Custom theme dropdown with six matching light presets and six matching dark
  presets
- Rearrange Today, Calendar, and Lists from settings
- Adjustable panel width, visible tab size, handle height, edge position,
  motion speed, corner radius, task row height, task spacing, task text size,
  completed-task visibility, launch-at-login, notifications, and surface opacity
- Responsive sizing for different monitor heights and widths

## Platform Status

| Platform | Status | Notes |
| --- | --- | --- |
| Windows | Working | Built locally and in GitHub Actions. Release includes `.exe` and `.msi`. |
| macOS Apple Silicon | Built in CI | Release includes `.dmg`. Unsigned until Apple signing is configured. |
| macOS Intel | Built in CI | Release includes `.dmg`. Unsigned until Apple signing is configured. |

## Verification Status

The current `main` branch is checked by GitHub Actions on Windows and macOS.
The release process publishes platform artifacts only after those workflows pass.

What is verified:

- TypeScript and Vite production build
- ESLint
- Project consistency checks for versions, docs links, scripts, and workflows
- Playwright sidebar smoke test
- Responsive sidebar layout checks for desktop, narrow, and short viewports
- Rust check
- Tauri no-bundle native build smoke test

What is not fully verified yet:

- Physical macOS QA on real hardware
- Windows certificate signing and Apple notarization
- Signed auto-update
- Full keyboard/accessibility pass

## Roadmap

Todobar is planned in product layers:

- Version 0: dockable planner with local Today, Calendar, custom lists,
  launch-at-login, global shortcut, tray/menu-bar control, and responsive native
  shell.
- Version 1: better planner with editable tasks, undo, reminders,
  notifications, recurring tasks, keyboard-first triage, SQLite storage, and
  import/export.
- Version 2: assistant layer with AI suggestions, task splitting, Today
  planning, source/confidence display, approval flow, and audit log.
- Version 3: MCP/context connectors for local files, GitHub, Gmail, calendar,
  Notion/docs, and issue trackers.
- Companion manager: a larger settings/planning window for connectors,
  notification rules, provider setup, bulk editing, backups, and assistant
  history.

The detailed plan lives in [Product vision](docs/product-vision.md) and
[Roadmap](docs/roadmap.md).

## Install

Download the latest Windows or macOS build from
[GitHub Releases](https://github.com/Leonxlnx/todobar/releases).

Current stable release: `v0.1.5`.

Windows:

- Prefer `Todobar_<version>_x64-setup.exe` for a normal installer.
- Use `Todobar_<version>_x64_en-US.msi` for MSI-based install flows.

macOS:

- Use `Todobar_<version>_aarch64.dmg` on Apple Silicon Macs.
- Use `Todobar_<version>_x64.dmg` on Intel Macs.

macOS builds from public CI are currently unsigned until code signing and
notarization certificates are configured. You may need to right-click the app and
choose Open.

## Usage

- Click the edge handle to open or close Todobar.
- Drag the handle along its docked edge to change its position.
- Press `Alt + T` to toggle it from any app.
- If that shortcut is already owned by another app, use `Alt + Shift + T`.
- Use the tray/menu-bar icon to toggle Todobar, open settings, or quit.
- Press `Esc` to close the panel.
- Use the command rail to jump between Today, Calendar, Lists, and setup
  without turning the sidebar into a full dashboard.
- Use the settings button to adjust appearance, dock edge, desktop startup,
  window size, handle shape, task density, motion, radius, and opacity.

## Data and Privacy

Todobar is local-first in this prototype. Task data and settings are stored in
local browser storage inside the app webview. There is no analytics layer and no
network sync.

Future AI and MCP features should stay optional, permissioned, and visible. The
core task app must remain useful without AI.

## Development

Requirements:

- Node.js LTS
- Rust stable
- Platform prerequisites for Tauri v2

Install dependencies:

```bash
npm install
```

Run the web preview:

```bash
npm run dev
```

Run the native desktop app:

```bash
npm run tauri:dev
```

Build the app locally:

```bash
npm run tauri:build
```

Validate the project:

```bash
npm run verify
npm run build
npm run lint
npm run test:smoke
npm run test:native
cargo check --manifest-path src-tauri/Cargo.toml
```

`npm run test:smoke` runs a browser smoke test against the Vite preview. If
Chromium is missing locally, run `npx playwright install chromium` once.

`npm run test:native` runs a no-bundle Tauri release build. It validates the
native desktop shell without creating installers.

## Release Builds

The repository includes GitHub Actions workflows:

- `ci.yml` checks TypeScript, linting, browser smoke tests, Rust, and no-bundle
  native Tauri builds on Windows and macOS.
- `release.yml` builds release artifacts for:
  - Windows
  - macOS Apple Silicon
  - macOS Intel

Create a release by pushing a version tag:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

Before tagging, bump the version in `package.json`, `package-lock.json`,
`src-tauri/Cargo.toml`, `src-tauri/Cargo.lock`, and
`src-tauri/tauri.conf.json`.

The Tauri release workflow uploads the platform installers to GitHub Releases.

Current release workflow output:

- Windows setup `.exe`
- Windows `.msi`
- macOS Apple Silicon `.dmg`
- macOS Intel `.dmg`
- macOS `.app.tar.gz` archives

## Architecture

Todobar uses:

- React + TypeScript for the UI
- Tauri v2 for native desktop APIs
- `@tauri-apps/plugin-global-shortcut` for the open/close shortcut
- `@tauri-apps/plugin-autostart` for launch-at-login
- Tauri tray/menu-bar APIs for desktop control while the window is skipped from
  the taskbar
- `tauri-plugin-single-instance` to prevent duplicate sidebars
- local browser storage for the current prototype task store

Native window behavior is intentionally kept in a small layer:

- the Tauri window is positioned per dock edge
- the visible tab remains docked to the chosen screen edge
- transparent regions pass clicks through to the app behind Todobar
- panel width is clamped so it still fits on narrow monitors

The long-term architecture keeps native integrations behind adapters so the UI
can stay portable while desktop capabilities remain explicit and permissioned.

## Open Source

Todobar is licensed under the Apache License 2.0.

Roadmap and release follow-ups are kept in the docs so the public issue tracker
stays focused on real user reports:

- physical macOS QA
- Windows signing and macOS notarization
- signed auto-update channel
- keyboard and accessibility hardening

Contributions are welcome, especially around:

- Windows and macOS edge-window behavior
- accessibility and keyboard-first task flows
- local-first data storage
- release signing and auto-update
- MCP connector design
- AI planning UX with strong privacy boundaries

## Documentation

Planning and research notes live in [`docs/`](docs/).

Useful starting points:

- [Architecture](docs/architecture.md)
- [Product vision](docs/product-vision.md)
- [UI direction](docs/ui-direction.md)
- [Platform support](docs/platform-support.md)
- [Release verification](docs/release-verification-v0.1.5.md)
- [Roadmap](docs/roadmap.md)
- [Native test matrix](docs/native-test-matrix.md)
- [Security model](docs/security-model.md)
- [Open source plan](docs/open-source-plan.md)
