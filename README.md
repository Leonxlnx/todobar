# Todobar

[![CI](https://github.com/Leonxlnx/todobar/actions/workflows/ci.yml/badge.svg)](https://github.com/Leonxlnx/todobar/actions/workflows/ci.yml)
[![Release](https://github.com/Leonxlnx/todobar/actions/workflows/release.yml/badge.svg)](https://github.com/Leonxlnx/todobar/actions/workflows/release.yml)

Todobar is a native right-edge todo sidebar for macOS and Windows. It stays out
of the way as a small docked handle, opens into a clean task panel, and is built
for fast capture while you work in other apps.

The app is early, but the foundation is real: Tauri v2, React, local persisted
tasks, global shortcuts, autostart, single-instance behavior, and release
automation for Windows and macOS.

## Why

Most todo apps are full windows. Todobar is designed as a desktop utility:

- always reachable from the edge of the screen
- fast enough for one-line capture
- clean enough to leave visible during focused work
- local-first by default
- ready for future AI and MCP integrations without making AI mandatory

## Current Features

- Right-edge native sidebar for Windows and macOS
- Small draggable handle that opens and closes the panel
- Global shortcut: `Ctrl/Cmd + Shift + T`
- Autostart on system login
- Single-instance desktop behavior
- Always-on-top frameless window
- Click-through transparent area outside the handle and open panel
- Today tasks
- Month Plan tasks
- Custom task lists
- Add, complete, prioritize, collapse, and delete tasks
- Delete custom lists
- Local persistence through browser storage
- Light and dark themes
- Adjustable panel width, visible tab size, handle height, vertical position,
  motion speed, corner radius, task row height, task spacing, task text size,
  launch-at-login, and surface opacity
- Responsive sizing for different monitor heights and widths

## Platform Status

| Platform | Status | Notes |
| --- | --- | --- |
| Windows | Working | Built locally and in GitHub Actions. Release includes `.exe` and `.msi`. |
| macOS Apple Silicon | Built in CI | Release includes `.dmg`. Unsigned until Apple signing is configured. |
| macOS Intel | Built in CI | Release includes `.dmg`. Unsigned until Apple signing is configured. |

## Roadmap

- Undo after delete
- Task editing and keyboard navigation
- Tray/menu bar controls
- SQLite-backed local database
- Import/export
- Multi-monitor position memory
- Signed installers
- Auto-update
- MCP connectors for local files, GitHub, calendars, Notion, and Linear
- Optional AI planning actions with explicit permission scopes

## Install

Download the latest Windows or macOS build from
[GitHub Releases](https://github.com/Leonxlnx/todobar/releases).

Current stable release: `v0.1.1`.

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

- Click the right-edge handle to open or close Todobar.
- Drag the handle up or down to change its vertical position.
- Press `Ctrl + Shift + T` on Windows or `Cmd + Shift + T` on macOS to toggle it.
- Press `Esc` to close the panel.
- Use the settings button to adjust appearance, desktop startup, window size,
  handle shape, task density, motion, radius, and opacity.

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
npm run build
npm run lint
cargo check --manifest-path src-tauri/Cargo.toml
```

## Release Builds

The repository includes GitHub Actions workflows:

- `ci.yml` checks TypeScript, linting, and Rust on Windows and macOS.
- `release.yml` builds release artifacts for:
  - Windows
  - macOS Apple Silicon
  - macOS Intel

Create a release by pushing a version tag:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

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
- `tauri-plugin-single-instance` to prevent duplicate sidebars
- local browser storage for the current prototype task store

Native window behavior is intentionally kept in a small layer:

- the Tauri window fills the monitor height
- the visible tab remains docked to the right edge
- transparent regions pass clicks through to the app behind Todobar
- panel width is clamped so it still fits on narrow monitors

The long-term architecture keeps native integrations behind adapters so the UI
can stay portable while desktop capabilities remain explicit and permissioned.

## Open Source

Todobar is licensed under the Apache License 2.0.

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
- [Platform support](docs/platform-support.md)
- [Release verification](docs/release-verification-v0.1.1.md)
- [Roadmap](docs/roadmap.md)
- [Native test matrix](docs/native-test-matrix.md)
- [Security model](docs/security-model.md)
- [Open source plan](docs/open-source-plan.md)
