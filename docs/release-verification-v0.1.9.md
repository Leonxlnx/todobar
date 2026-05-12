# Release Verification v0.1.9

This page records the checks for the `v0.1.9` release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.9
- Release workflow: created from the `v0.1.9` tag

Expected published assets:

- `Todobar_0.1.9_x64-setup.exe`
- `Todobar_0.1.9_x64_en-US.msi`
- `Todobar_0.1.9_aarch64.dmg`
- `Todobar_0.1.9_x64.dmg`
- `Todobar_aarch64.app.tar.gz`
- `Todobar_x64.app.tar.gz`

## Release Scope

`v0.1.9` keeps the Gmail OAuth foundation in code but hides the visible Gmail
connector UI until Google OAuth verification and a real public rollout are
ready.

Included changes:

- Gmail connector UI is hidden behind an internal feature flag.
- Inbox suggestions are not shown in the normal product UI.
- README clarifies that Gmail is implemented as a hidden foundation, not a
  visible feature.
- Tests assert that Gmail remains hidden while the code stays available for
  future activation.

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

The release workflow produces platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel

## Known Follow-Ups

- Gmail OAuth app registration and Google verification are not complete.
- Gmail remains hidden until the app identity, privacy policy, verification,
  and public connector UX are ready.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
