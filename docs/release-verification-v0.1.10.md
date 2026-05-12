# Release Verification v0.1.10

This page records the checks for the `v0.1.10` quality release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.10
- Release workflow: pending tag build
- Workflow status: pending

Expected published assets:

- `Todobar_0.1.10_x64-setup.exe`
- `Todobar_0.1.10_x64_en-US.msi`
- `Todobar_0.1.10_aarch64.dmg`
- `Todobar_0.1.10_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

## Release Scope

`v0.1.10` is a quality pass focused on UI polish, settings clarity, native dock
behavior, and safer visual defaults.

Included changes:

- Six curated theme presets per color mode.
- Cleaner settings group hierarchy, theme picker, and panel width apply flow.
- Softer task completion animation without full-row flash.
- Cleaner pinned list separators and Today empty state.
- Calmer dark-mode scrollbars, action buttons, and rail buttons.
- Neutral custom backdrop tab tint and separated native dock closed-state
  styling.
- Faster native cursor passthrough checks for fewer blocked transparent areas.

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run lint
npm run build
npm run test:smoke
cargo check
npm run test:native
```

The local native no-bundle build produces:

- `src-tauri/target/release/todobar.exe`

## CI Verification

GitHub Actions CI is configured to run on:

- `windows-latest`
- `macos-latest`

The release workflow should produce platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel

## Known Follow-Ups

- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Gmail OAuth remains hidden until Google app verification and rollout are
  ready.
