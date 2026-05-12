# Release Verification v0.1.8

This page records what was checked while preparing the `v0.1.8` native dock
polish release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.8
- Release workflow:
  https://github.com/Leonxlnx/todobar/actions/runs/25696559749
- Release workflow result: passed

Published assets:

- `Todobar_0.1.8_x64-setup.exe`
  `sha256:91fdfbb6a8a7d65e13977726f37106fbed168a079ece9c264c76c94b61bd6750`
- `Todobar_0.1.8_x64_en-US.msi`
  `sha256:fe9fdebd967beaadfb151fa176fb93ff5bfbcccd381e3ea55a7859f755cfc1e8`
- `Todobar_0.1.8_aarch64.dmg`
  `sha256:385f99157aaa385b415e3b0216e5da039d2a3c9689144d8fbb3f5311d705bcf5`
- `Todobar_0.1.8_x64.dmg`
  `sha256:63520588e2cde4562084771d3fd231b16236d1dbab10a7d5762a4055ad6dbe4e`
- `Todobar_aarch64.app.tar.gz`
  `sha256:952ac6073b2749d2c233dc406f278cbe47c3c27d41ba138e0db2f94e7953d6e0`
- `Todobar_x64.app.tar.gz`
  `sha256:2fdf6978fefb9752137510b3527647f078f24d1f667093ec0b00a1bd4331f7f0`

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
