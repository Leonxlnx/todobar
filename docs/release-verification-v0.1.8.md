# Release Verification v0.1.8

This page records what was checked while preparing the `v0.1.8` native dock
polish release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.8
- Release workflow: created from the `v0.1.8` tag

Expected published assets:

- `Todobar_0.1.8_x64-setup.exe`
- `Todobar_0.1.8_x64_en-US.msi`
- `Todobar_0.1.8_aarch64.dmg`
- `Todobar_0.1.8_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

## Release Scope

`v0.1.8` updates the native edge experience after the `v0.1.7` personalization
release.

Included polish:

- hover-only native reveal works from the full screen edge
- native click-through hit testing is less aggressive while idle
- native dock/tab surface blends with the sidebar instead of looking like a
  separate rectangle
- custom backdrop imagery is dimmed on the native tab and in settings
- pinned Today list spacing is cleaner
- task rows, pinned lists, settings scrollbars, and dark-mode surfaces are
  more consistent
- the experimental panel-edge resize handle was removed
- settings group state, theme picker, Gmail MCP setup surface, and section
  reordering remain documented and tested

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run lint
npm run build
npm run test:smoke
npm run test:native
```

The local native no-bundle build produces:

- `src-tauri/target/release/todobar.exe`

## Browser Preview QA

Checked through the Playwright smoke test:

- App loads without browser console warnings or errors.
- Sidebar settings open from the rail.
- Theme preset switching stays mode-aware.
- Hover-only tab setting persists and hides the visible handle.
- Native hover-only reveal zone is positioned inside the visible tab strip.
- Native sidebar background stays transparent so the SVG dock surface owns the
  rounded tab shape.
- Responsive sidebar layout stays inside desktop, narrow, and short viewports.
- Pinned list groups keep their lightweight border and spacing.
- Reminder toasts expose Open, Snooze, and Dismiss controls.
- Closed reminders badge the handle without opening the sidebar.

## Native Desktop QA

The native no-bundle build includes:

- launch-at-login support
- native global shortcut registration
- fallback global shortcut registration
- single-instance behavior
- skip-taskbar edge window
- tray/menu-bar control for open, settings, and quit
- cursor-monitor startup targeting for multi-monitor setups
- native hover/click-through hit testing for right, left, and top docks

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
