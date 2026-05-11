# Release Verification v0.1.7

This page records what was checked while preparing the `v0.1.7` performance,
personalization, and reminder polish release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.7
- CI workflow run for release commit:
  https://github.com/Leonxlnx/todobar/actions/runs/25672477149
- Release workflow run:
  https://github.com/Leonxlnx/todobar/actions/runs/25672479025

Published assets:

- `Todobar_0.1.7_x64-setup.exe`
  `sha256:3e415ce9a5f7df8a5fbd112ad83e1e4733a0fc143cd308d80e7ab4289c4158ff`
- `Todobar_0.1.7_x64_en-US.msi`
  `sha256:e7b5a6efd4cfeac859176ebc52e2c768037401fa76401345f2daca289e71e079`
- `Todobar_0.1.7_aarch64.dmg`
  `sha256:f727565fa5ba634c5027029253968208ac7fe6b55355d19bd30f0ee293995982`
- `Todobar_0.1.7_x64.dmg`
  `sha256:5f9b3e22d3b7358b8b3e625bf9854f79445da06eb95c1c4b0f057296cb947c32`
- `Todobar_aarch64.app.tar.gz`
  `sha256:d1ca144d405a996f94aa267ede6e5be6a70c4d89148c1c62ea740c2630ce5bd1`
- `Todobar_x64.app.tar.gz`
  `sha256:a28c7cfd9d4b054f62720c68579981dcab6908a4cc3ff6fa436d40242fa59e8a`

## Release Scope

`v0.1.7` keeps the native sidebar foundation intact and focuses on making the
daily planner feel faster, more personal, and safer to iterate on.

Included polish:

- task sorting by priority, newest, or oldest
- custom backdrop image upload with strength, dim, and blur controls
- custom reminder toasts that match light and dark themes
- 10-minute snooze directly from reminder toasts
- debounced local task, custom-list, and settings persistence
- faster calendar reminder indexing
- cleaner staged panel-width settings layout

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run lint
npm run build
npm run test:smoke
npm run test:native
```

The local native no-bundle build produced:

- `src-tauri/target/release/todobar.exe`

## Browser Preview QA

Checked through the Playwright smoke test and an in-app browser visual pass:

- App loads without browser console warnings or errors.
- Sidebar settings open from the rail.
- Task sort controls are visible and persist through settings.
- Custom backdrop upload marks the workspace and exposes backdrop sliders.
- Reminder toasts stay in-app and expose Open, Snooze, and Dismiss controls.
- Snooze moves a due reminder forward instead of reopening the sidebar.
- Closed reminders badge the handle without opening the sidebar.
- Responsive sidebar layout stays inside desktop, narrow, and short viewports.

## Native Desktop QA

The native no-bundle build includes:

- launch-at-login support
- native global shortcut registration
- fallback global shortcut registration
- single-instance behavior
- skip-taskbar edge window
- tray/menu-bar control for open, settings, and quit
- cursor-monitor startup targeting for multi-monitor setups

Physical multi-monitor and macOS QA are still the most important manual checks
after CI because hosted runners cannot prove every real desktop edge case.

## CI Verification

GitHub Actions CI is configured to run on:

- `windows-latest`
- `macos-latest`

The CI workflow runs:

- Project verification
- TypeScript and Vite build
- ESLint
- Playwright browser smoke test
- Responsive browser layout checks
- Rust check
- Tauri no-bundle native build smoke test

The release workflow produces platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel

## Known Follow-Ups

- macOS artifacts are CI-built but still need physical Mac QA.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Auto-update is not configured yet.
- Full keyboard and accessibility flow needs hardening.
- Gmail, calendar, and other MCP connectors remain planned but require explicit
  permissioned setup instead of placeholder UI.
