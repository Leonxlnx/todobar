# Release Verification v0.1.1

This page records what was actually checked for the `v0.1.1` release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.1
- Release workflow run: `25506012320`
- CI workflow run for release commit: `25506005032`

Published assets:

- `Todobar_0.1.1_x64-setup.exe`
- `Todobar_0.1.1_x64_en-US.msi`
- `Todobar_0.1.1_aarch64.dmg`
- `Todobar_0.1.1_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run build
npm run lint
cargo check --manifest-path src-tauri/Cargo.toml
npm run tauri:build
```

Local Windows bundles were produced:

- `src-tauri/target/release/bundle/nsis/Todobar_0.1.1_x64-setup.exe`
- `src-tauri/target/release/bundle/msi/Todobar_0.1.1_x64_en-US.msi`

Native runtime checks:

- Todobar process was running from `src-tauri/target/debug/todobar.exe`.
- Windows launch-at-login registry entry existed under
  `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`.
- The visible Todobar window occupied full monitor height with a narrow
  right-edge sidebar width.

## Browser Preview QA

Checked through the local Vite preview at `http://127.0.0.1:5173/`:

- App loaded without framework overlay.
- Console warnings/errors were empty for the tested interactions.
- Edge handle opened the sidebar.
- Settings panel opened and rendered customization controls.
- A Today task could be added and then deleted.
- Today section collapsed and expanded.

The browser preview is not the product target, but it verifies the React task
surface before native packaging.

## CI Verification

GitHub Actions CI passed on:

- `windows-latest`
- `macos-latest`

The CI workflow runs:

- TypeScript and Vite build
- ESLint
- Rust check

The release workflow produced platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel

## Known Gaps

- macOS artifacts are CI-built but not tested on a physical Mac in this pass.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Auto-update is not configured yet.
- Tray/menu bar controls are not implemented yet.

