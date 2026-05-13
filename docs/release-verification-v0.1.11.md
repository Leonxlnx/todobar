# Release Verification v0.1.11

This page records the checks for the `v0.1.11` Android companion release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.11
- Release workflow: https://github.com/Leonxlnx/todobar/actions/runs/25798182640
- Android workflow: https://github.com/Leonxlnx/todobar/actions/runs/25798182656
- Workflow status: passed

Published assets:

- `Todobar_0.1.11_x64-setup.exe` - 3,087,870 bytes
- `Todobar_0.1.11_x64_en-US.msi` - 4,476,928 bytes
- `Todobar_0.1.11_aarch64.dmg` - 5,052,781 bytes
- `Todobar_0.1.11_x64.dmg` - 5,210,983 bytes
- `Todobar_aarch64.app.tar.gz` - 4,786,574 bytes
- `Todobar_x64.app.tar.gz` - 4,946,173 bytes
- `todobar-android-v0.1.11.apk` - 4,413,526 bytes

## Release Scope

`v0.1.11` adds the Android companion APK to the public release flow and fixes the
first pass of native Android overlay behavior.

Included changes:

- Android release APK build.
- Android floating edge bubble with right, left, and top dock behavior.
- Android Today, Calendar, Lists, and Settings views in English.
- Android local task capture, reminders, custom lists, and theme presets.
- Fixed Android overlay service state, handle sizing, dock animation direction,
  drag axis behavior, and reminder snooze routing.
- Desktop version metadata aligned to the same release tag.

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
  `EEF4650B3CEA710CF6D79018D6D9EECA229E11CA20944CC6DC09AA7274245BE6`

## CI Verification

GitHub Actions CI is configured to run on:

- `windows-latest`
- `macos-latest`
- Android release APK build through `android.yml`

The release workflows should produce platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel
- Android

## Known Follow-Ups

- Android is not yet verified on a physical device in this pass.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Gmail OAuth remains hidden until Google app verification and rollout are
  ready.
