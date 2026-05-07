# Release Verification v0.1.3

This page records what was checked while preparing the `v0.1.3` responsive
edge-polish release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.3
- CI workflow run for release commit:
  https://github.com/Leonxlnx/todobar/actions/runs/25513255581
- Release workflow run:
  https://github.com/Leonxlnx/todobar/actions/runs/25513703604

Published assets:

- `Todobar_0.1.3_x64-setup.exe`
- `Todobar_0.1.3_x64_en-US.msi`
- `Todobar_0.1.3_aarch64.dmg`
- `Todobar_0.1.3_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

Asset digests:

- `Todobar_0.1.3_x64-setup.exe`:
  `sha256:be10e5a074555d5c023aa187db3754498760df07368bba3c1f2c32c4058eec53`
- `Todobar_0.1.3_x64_en-US.msi`:
  `sha256:7a9e33f596d9c49ce5d37790b1fb8f40206da2e41c80de4465e0032b01db26bf`
- `Todobar_0.1.3_aarch64.dmg`:
  `sha256:8e86b7476def3e48bf77c42cc4ce0f126874d6156534dcd8f388cca204a90faa`
- `Todobar_0.1.3_x64.dmg`:
  `sha256:431c6f2379f39c82469b6f56d3f3f16d41687927864391a5396bd4e3510d1464`
- `Todobar_aarch64.app.tar.gz`:
  `sha256:4a80f6e1244a66de3ad40a6790eba512220ccc79e2da0f8933d81f632c6e0d64`
- `Todobar_x64.app.tar.gz`:
  `sha256:b5a7dc58fc81a247a8aafd2142a7af359a2424dd4fc711b709de7abcba9b22d1`

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
- Tray/menu bar controls are not implemented yet.
