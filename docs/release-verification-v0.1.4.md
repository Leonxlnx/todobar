# Release Verification v0.1.4

This page records what was checked while preparing the `v0.1.4` desktop-control
and product-vision release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.4
- CI workflow run for release commit:
  https://github.com/Leonxlnx/todobar/actions/runs/25515795904
- Release workflow run:
  https://github.com/Leonxlnx/todobar/actions/runs/25516231870

Published assets:

- `Todobar_0.1.4_x64-setup.exe`
- `Todobar_0.1.4_x64_en-US.msi`
- `Todobar_0.1.4_aarch64.dmg`
- `Todobar_0.1.4_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

Asset digests:

- `Todobar_0.1.4_x64-setup.exe`:
  `sha256:52f26f2736ed3e844b5ad42935e628b28649cfd665cc788c97c0f1f9dee7d9bc`
- `Todobar_0.1.4_x64_en-US.msi`:
  `sha256:3e53df35d4a8b17bd509d96249067bbea5ee2c6fc7344c9b45bb5186ad4fa749`
- `Todobar_0.1.4_aarch64.dmg`:
  `sha256:0d6985d906c980823cd99296bb64f40f73bf9b7d82a452c37db7ce3a8e22213e`
- `Todobar_0.1.4_x64.dmg`:
  `sha256:64fb0040db7ef0387d671f2d668a1b0fe4ef9eae2022e2c3fab02e98b1a43e52`
- `Todobar_aarch64.app.tar.gz`:
  `sha256:ed86a8fc861e26e35f390b421fc7ce2be11aacdd47ef6ef1a48eb749531a8f0c`
- `Todobar_x64.app.tar.gz`:
  `sha256:8ce4ba2414fac9e8cbafdce88ad3f98a9e094a1d2e92ef21011d9cac3e001334`

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
- The open handle stays inside the viewport in narrow layouts.
- Settings remain reachable in responsive preview sizes.
- Console warnings/errors are empty for the tested flow.

The browser preview is not the product target, but it verifies the React task
surface and responsive shell before native packaging.

## Native Desktop QA

The native build now includes:

- launch-at-login support
- global shortcut support
- single-instance behavior
- skip-taskbar right-edge window
- tray/menu-bar control for open, settings, and quit

The tray/menu-bar code is compiled by Rust check and the Tauri no-bundle native
build smoke test. Physical tray/menu-bar click QA still needs a manual pass on
real Windows and macOS desktops before calling the product fully finished.

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

## Known Follow-Ups At Release Time

- macOS artifacts are CI-built but still need physical Mac QA.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Auto-update is not configured yet.
- Keyboard and accessibility flow needs hardening.
