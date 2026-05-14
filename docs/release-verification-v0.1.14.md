# Release Verification v0.1.14

This page records the checks for the `v0.1.14` closed tab shadow fix release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.14
- Release workflow: https://github.com/Leonxlnx/todobar/actions/runs/25881217696 (success)
- Android workflow: https://github.com/Leonxlnx/todobar/actions/runs/25881218074 (success)
- Post-release main CI (Windows + macOS validate): https://github.com/Leonxlnx/todobar/actions/runs/25881204946 (success)

Published assets:

- `Todobar_0.1.14_x64-setup.exe` - 3,092,760 bytes
- `Todobar_0.1.14_x64_en-US.msi` - 4,476,928 bytes
- `Todobar_0.1.14_aarch64.dmg` - 5,057,727 bytes
- `Todobar_0.1.14_x64.dmg` - 5,212,936 bytes
- `Todobar_aarch64.app.tar.gz` - 4,789,380 bytes
- `Todobar_x64.app.tar.gz` - 4,948,073 bytes
- `todobar-android-v0.1.14.apk` - 4,416,158 bytes

The rolling Android pre-release was refreshed at the same time:

- `https://github.com/Leonxlnx/todobar/releases/tag/android-latest` →
  `todobar-android-main.apk`

## Release Scope

`v0.1.14` is a small follow-up to `v0.1.13`. The Premium UI Polish release
shipped with the closed native dock tab still casting the open-panel's full
ambient drop-shadow, which bled vertically above and below the small visible
tab and showed as a faint gray stripe on the user's wallpaper.

This release drops the drop-shadow filter on `.native-dock-surface.is-closed`
across every dock edge and theme, so only the actual tab is visible when the
sidebar is closed. The ambient shadow returns the instant the sidebar opens,
since `.is-open` keeps the full filter.

The Android companion APK is rebuilt with the new version code only — Android
uses native Kotlin views that were not affected by the shadow regression.

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

Android compile and release APK check:

```powershell
cd android
.\gradlew.bat :app:compileDebugKotlin :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

Expected local Android output:

- `android/app/build/outputs/apk/release/app-release.apk`

## Rendered UI Check

Checked through the in-app browser at:

- `http://127.0.0.1:5173/?open=1`

Verified:

- Page loads with Todobar visible on both light and dark themes.
- Closed sidebar shows only the tab with no vertical gray stripe above
  or below it.
- Opening the sidebar restores the ambient shadow on the full panel
  without any visible jump.
- No new console warnings or errors versus `v0.1.13`.

## Known Follow-Ups

- Physical macOS QA still needs to be done on real hardware.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
