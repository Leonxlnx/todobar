# Release Verification v0.1.12

This page records the checks for the `v0.1.12` desktop motion release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.12
- Release workflow: https://github.com/Leonxlnx/todobar/actions/runs/25801672178
- Android workflow: https://github.com/Leonxlnx/todobar/actions/runs/25801672422
- Workflow status: passed

Published assets:

- `Todobar_0.1.12_x64-setup.exe` - 3,088,120 bytes
- `Todobar_0.1.12_x64_en-US.msi` - 4,476,928 bytes
- `Todobar_0.1.12_aarch64.dmg` - 5,055,277 bytes
- `Todobar_0.1.12_x64.dmg` - 5,214,503 bytes
- `Todobar_aarch64.app.tar.gz` - 4,786,606 bytes
- `Todobar_x64.app.tar.gz` - 4,949,422 bytes
- `todobar-android-v0.1.12.apk` - 4,413,526 bytes

## Release Scope

`v0.1.12` focuses on Windows and macOS desktop polish. The release uses the same
React frontend bundle for both platforms, so the Today, Calendar, and Lists
section transitions render from the same source on Windows and macOS.

Included changes:

- Direction-aware motion when switching sidebar sections.
- Staggered entry animation for headings, quick-add controls, calendar boards,
  list groups, and task rows.
- Smoke coverage for the rendered animation hooks.

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run lint
npm run build
npm run test:smoke
cargo check --manifest-path src-tauri/Cargo.toml
npm run test:native
```

Android APK check:

```powershell
cd android
.\gradlew.bat assembleRelease
```

Expected local Android output:

- `android/app/build/outputs/apk/release/app-release.apk`
- Size: `4,413,526` bytes
- SHA256:
  `143506150205A4EEAEA05D61F7828E9FC6D824B3DB9370C38C1E2B82DB0516FD`

## Rendered UI Check

Checked through the in-app browser at:

- `http://127.0.0.1:5173/?open=1`

Verified:

- Page loads with Todobar visible.
- No relevant console warnings or errors.
- Calendar and Lists rail buttons respond.
- Calendar and Lists sections become visible after navigation.
- Section transition animation hooks are present in the rendered DOM.

## Known Follow-Ups

- Physical macOS QA still needs to be done on real hardware.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Android is included in the release workflow, but this release is primarily a
  desktop motion polish pass.
