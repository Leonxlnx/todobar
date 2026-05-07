# Release Verification v0.1.2

This page records what was checked while preparing the `v0.1.2` release.

## Release

- Repository: `Leonxlnx/todobar`
- Planned release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.2
- CI workflow run for release commit: pending until the `v0.1.2` tag is pushed.
- Release workflow run: pending until the `v0.1.2` tag is pushed.

Expected published assets:

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
- Tray/menu bar controls are not implemented yet.
