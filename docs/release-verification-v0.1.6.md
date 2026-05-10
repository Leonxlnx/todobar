# Release Verification v0.1.6

This page records what was checked while preparing the `v0.1.6` sidebar polish
and reminder badge release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.6
- CI workflow run for release commit:
  https://github.com/Leonxlnx/todobar/actions/runs/25632225284
- Release workflow run:
  https://github.com/Leonxlnx/todobar/actions/runs/25632428998

Published assets:

- `Todobar_0.1.6_x64-setup.exe`
  `sha256:5a67757b94b6133f9fb9b9761315ac430d84131669310ec06b1b9ae9eeaf0199`
- `Todobar_0.1.6_x64_en-US.msi`
  `sha256:9dacd40b2e70d38e6aad2e32c4b276616328d61d162d5a1ee408e1d9783bbb7a`
- `Todobar_0.1.6_aarch64.dmg`
  `sha256:ee4678fab3e37c6243503daa0f17eda7532a49853113c311f800d44352bae116`
- `Todobar_0.1.6_x64.dmg`
  `sha256:93fbc1faec6a121e430acc92b56d5106607deaf99c23d1f3d7d1697e720bd8ab`
- `Todobar_aarch64.app.tar.gz`
  `sha256:3afa366a8af07dcf0134d05f21cef358c71c5b74d456c4403ad6256e0e9dc1af`
- `Todobar_x64.app.tar.gz`
  `sha256:b3098a80f6da01d6ae7a46cd30804ee95153589c7b6feb697cd9c1b0a8e24c6f`

## Release Scope

`v0.1.6` focuses on making the current desktop planner feel safer and more
stable before adding larger assistant features.

Included polish:

- one top Today progress bar instead of duplicate progress UI
- square rail and settings buttons with calmer soft-ui states
- bottom reminder toasts plus a handle badge while the sidebar is closed
- better reminder editor spacing and toggle behavior
- draggable Today, Calendar, and Lists ordering in settings
- native startup monitor targeting based on the cursor monitor first

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run lint
npm run build
npm run test:smoke
npm run test:native
```

The local native no-bundle build should produce:

- `src-tauri/target/release/todobar.exe`

## Browser Preview QA

Checked through the Playwright smoke test against the local Vite preview:

- App loads without a framework overlay.
- Sidebar opens from the edge handle.
- Settings dialog opens.
- Completed tasks can be hidden without deleting them.
- Completed tasks can be shown again.
- Sidebar layout stays inside desktop, narrow, and short browser viewports.
- Settings remain reachable in responsive preview sizes.
- The sidebar has a single top progress meter.
- Closed reminder state shows a handle badge without opening the sidebar.
- Rail buttons keep square hit targets.

## Native Desktop QA

The native build includes:

- launch-at-login support
- native global shortcut registration
- fallback global shortcut registration
- single-instance behavior
- skip-taskbar edge window
- tray/menu-bar control for open, settings, and quit
- cursor-monitor startup targeting for multi-monitor setups

Full global keyboard and physical multi-monitor QA still benefit from a normal
desktop session because CI runners cannot prove every OS-level focus and display
edge case.

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
- Keyboard and accessibility flow needs hardening.
