# Release Verification v0.1.5

This page records what was checked while preparing the `v0.1.5` native shortcut
fix release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.5
- CI workflow run for release commit:
  https://github.com/Leonxlnx/todobar/actions/runs/25524903615
- Release workflow run:
  https://github.com/Leonxlnx/todobar/actions/runs/25525264541

Published assets:

- `Todobar_0.1.5_x64-setup.exe`
  `sha256:7083ce035027e3464589d28e67afff6866570d1b9a3e2e59dedd1a5339f18abf`
- `Todobar_0.1.5_x64_en-US.msi`
  `sha256:116cb83a8c226098a3dadf61758198befadb0ca1eb9e443cad58200763b1f571`
- `Todobar_0.1.5_aarch64.dmg`
  `sha256:04fe367ad86b3934ee8614bf99cc4d0c4987a898f83f8487cf1812701cbc75a7`
- `Todobar_0.1.5_x64.dmg`
  `sha256:bc4bb3188e9737bab17a92799c8499296619b26050fd080163ea7d8edf5b4ab4`
- `Todobar_aarch64.app.tar.gz`
  `sha256:8edf533d8c3d8ef62241960b979d5dd8d53aa533c9a7caccb5d79366cbf3dee4`
- `Todobar_x64.app.tar.gz`
  `sha256:3c5b24d616d59713a23e4a38dd3ae92a4a6d77e4a0bfe91d436a9dafd1d2c57e`

## Fix Scope

`v0.1.4` still depended on the focused webview for shortcut registration in
practice. This meant the shortcut could work after clicking Todobar, but not
reliably from another app.

`v0.1.5` moved shortcut handling into the native Tauri layer. The current
toggle shortcut is:

- `CommandOrControl+Alt+T`

The shortcut emits the same internal toggle event used by the tray/menu-bar
control. Browser tab-restore shortcut chords are intentionally not used.

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

## Known Follow-Ups

- macOS artifacts are CI-built but still need physical Mac QA.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Auto-update is not configured yet.
- Keyboard and accessibility flow needs hardening.
