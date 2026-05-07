# Release Verification v0.1.5

This page records what was checked while preparing the `v0.1.5` native shortcut
fix release.

## Release

- Repository: `Leonxlnx/todobar`
- Planned release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.5
- CI workflow run for release commit: pending until the `v0.1.5` tag is pushed.
- Release workflow run: pending until the `v0.1.5` tag is pushed.

Expected published assets:

- `Todobar_0.1.5_x64-setup.exe`
- `Todobar_0.1.5_x64_en-US.msi`
- `Todobar_0.1.5_aarch64.dmg`
- `Todobar_0.1.5_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

## Fix Scope

`v0.1.4` still depended on the focused webview for shortcut registration in
practice. This meant the shortcut could work after clicking Todobar, but not
reliably from another app.

`v0.1.5` registers the toggle shortcuts in the native Tauri layer:

- `CommandOrControl+Shift+T`
- `CommandOrControl+Alt+T`

Both shortcuts emit the same internal toggle event used by the tray/menu-bar
control.

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run build
npm run lint
npm run test:smoke
cargo check --manifest-path src-tauri/Cargo.toml
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

The browser preview is not the product target, but it verifies the React task
surface and responsive shell before native packaging.

## Native Desktop QA

The native build includes:

- launch-at-login support
- native global shortcut registration
- fallback global shortcut registration
- single-instance behavior
- skip-taskbar right-edge window
- tray/menu-bar control for open, settings, and quit

The shortcut code is compiled by Rust check and the Tauri no-bundle native build
smoke test. Full global keyboard QA still benefits from manual testing in a
normal desktop session because CI runners cannot reliably prove OS-level hotkey
delivery across arbitrary focused applications.

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

## Known Gaps

- macOS artifacts are CI-built but still need physical Mac QA:
  [#1](https://github.com/Leonxlnx/todobar/issues/1)
- macOS artifacts are unsigned and not notarized:
  [#2](https://github.com/Leonxlnx/todobar/issues/2)
- Windows artifacts are unsigned:
  [#2](https://github.com/Leonxlnx/todobar/issues/2)
- Auto-update is not configured yet:
  [#3](https://github.com/Leonxlnx/todobar/issues/3)
- Keyboard and accessibility flow needs hardening:
  [#4](https://github.com/Leonxlnx/todobar/issues/4)
