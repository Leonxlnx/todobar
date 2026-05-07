# Release Verification v0.1.2

This page records what was checked while preparing and publishing the `v0.1.2`
release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.2
- CI workflow run for release commit: `25511520319`
- Release workflow run: `25512070832`

Published assets:

- `Todobar_0.1.2_x64-setup.exe`
- `Todobar_0.1.2_x64_en-US.msi`
- `Todobar_0.1.2_aarch64.dmg`
- `Todobar_0.1.2_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

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

The local native no-bundle build produced:

- `src-tauri/target/release/todobar.exe`

## Browser Preview QA

Checked through the Playwright smoke test against the local Vite preview:

- App loads without a framework overlay.
- Sidebar opens from the edge handle.
- Settings dialog opens.
- Completed tasks can be hidden without deleting them.
- Completed tasks can be shown again.
- Console warnings/errors are empty for the tested flow.

The browser preview is not the product target, but it verifies the React task
surface before native packaging.

## CI Verification

GitHub Actions CI is configured to run on:

- `windows-latest`
- `macos-latest`

The CI workflow runs:

- Project verification
- TypeScript and Vite build
- ESLint
- Playwright browser smoke test
- Rust check
- Tauri no-bundle native build smoke test

The release workflow produces platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel

## Known Follow-Ups At Release Time

- macOS artifacts are CI-built but still need physical Mac QA.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Auto-update is not configured yet.
- Keyboard and accessibility flow needs hardening.
- Tray/menu bar controls are not implemented yet.
