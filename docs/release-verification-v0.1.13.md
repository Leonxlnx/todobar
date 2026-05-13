# Release Verification v0.1.13

This page records the checks for the `v0.1.13` premium UI polish release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.13
- Release workflow: triggered by tag push to `v0.1.13`
- Android workflow: triggered by tag push to `v0.1.13`

## Release Scope

`v0.1.13` is a focused UI/UX polish pass across desktop (Windows, macOS) and
the Android companion. The same React frontend bundle ships on Windows and
macOS, so all desktop platforms benefit from the same visual refresh, and the
Android UI was tuned to match the new tokens.

Highlights:

- Shared `--ease-out`, `--space-*`, `--radius-*` tokens at the workspace root.
- Sidebar rail and edge handle restyled with a transparent rest state, soft
  surface hover, accent-soft chip on active, and a 2 x 16 px accent rail
  indicator that slides in beside the active section button.
- Task row check button reshaped into a round outline that fills with the
  accent (animated tick) on completion.
- Dock-edge picker, task-sort picker, and calendar entry-mode toggle reworked
  as proper segmented controls (single shared track, lifted segment for the
  selected option).
- Settings drawer gained a sticky title header with backdrop blur and visible
  dividers between collapsible groups, plus a real primary `Apply` pill.
- Calendar refined end-to-end: cleaner board, today as a soft accent fill with
  a bold date, selected day as a solid accent pill, event dots under day
  numbers, and a dedicated agenda card for the selected day.
- Reminder toast, delete confirm, theme dropdown, and reset confirm popovers
  all share a calmer two-layer shadow that adapts to dark mode.
- Touch parity: `@media (hover: none)` keeps row actions visible, grows
  reminder presets to 40 dp, expands rail and calendar hit targets to
  38-44 dp, and disables hover-only tooltips on touch.
- Reduced-motion support added for the section enter and per-row stagger.
- Android mirrors the desktop refresh: thinner bubble-handle stroke and card
  borders, pill-shaped primary buttons, flat task row backgrounds, and
  ink-color sentence-case section headings.

The release ships 90 small, semantically scoped commits between `52b2bb9`
("Match Android bubble handle to desktop edge shape") and the `Prepare
v0.1.13` commit.

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
- Rail "you are here" accent indicator slides in on section change.
- Settings drawer sticky header keeps the title visible while scrolling.
- Calendar today / selected / event-dot states all render correctly.
- Reminder presets, dock-edge picker, and task-sort picker render as proper
  segmented controls in both themes.
- No new console warnings or errors versus `v0.1.12`.

## Known Follow-Ups

- Physical macOS QA still needs to be done on real hardware.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
- Custom backdrop image support in dark mode could still use one more pass
  to make sure every popover stays legible above arbitrary user images.
