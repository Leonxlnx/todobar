# Todobar Android

A floating-bubble companion to the [Todobar](../README.md) desktop sidebar.

This Android app brings the same right-edge handle and pop-out task panel to
Android phones. A small handle floats over every screen, drag it up or down
along the right edge, tap to expand the sidebar, capture a task in one line,
swipe the handle away when you are done.

> The Android app lives in this `android/` folder and is built independently
> from the desktop Tauri app. No desktop code is shared yet — the persistence
> model and visual language mirror the desktop sidebar so the two can converge
> later.

## Status

Local-first, no network access. The Android app is now a native companion for
the desktop sidebar and should feel like the same product adapted to a phone:

- Always-reachable handle docked to the right edge of the screen
- Drag the handle vertically to reposition it
- Tap to slide in a clean task panel sized for one-handed use
- Add, complete, delete tasks
- Clear all completed tasks in one tap
- Sort: open before completed, then by priority, then most recent
- Light and dark theme matched to the desktop "Studio" / "Obsidian" presets
- Six light and six dark visual presets mirrored from desktop
- Today, Calendar, Lists, and Settings views inside the overlay
- Reminder toasts with snooze support for Today, Calendar, and custom lists
- Local persistence via SharedPreferences
- Autostarts the bubble after reboot once the overlay permission has been
  granted

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Required to draw the floating bubble and sidebar on top of other apps. |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the bubble service alive while you use other apps. |
| `POST_NOTIFICATIONS` | Android 13+ requires this so the foreground service can show its silent ongoing notification. |
| `RECEIVE_BOOT_COMPLETED` | Restarts the bubble after reboot (only fires if you already granted the overlay permission). |

## Build

Requirements: JDK 17, Android SDK platform 34, build-tools 34.0.0.

```bash
cd android
./gradlew assembleRelease
```

The signed release APK lands in `app/build/outputs/apk/release/`.

On Windows from the repository root:

```powershell
cd android
.\gradlew.bat assembleRelease
```

Setting `TODOBAR_KEYSTORE_PATH`, `TODOBAR_KEYSTORE_PASSWORD`, `TODOBAR_KEY_ALIAS`
and `TODOBAR_KEY_PASSWORD` switches the release build to your own keystore;
otherwise the build falls back to the Android debug keystore so the APK stays
installable.

## CI / Releases

`/.github/workflows/android.yml` builds the APK on every push to `main`, on
every tag matching `v*`, and on demand via "Run workflow":

- **`v*` tag pushes** attach the APK to the matching GitHub release alongside
  the desktop Tauri artifacts.
- **Pushes to `main`** publish a rolling `android-latest` prerelease that
  always points at the latest APK so you can install the bleeding edge without
  cutting a desktop tag.
- **PRs touching `android/**`** upload the APK as a workflow artifact for
  review installs.

## Repo layout

```
android/
  app/
    src/main/
      AndroidManifest.xml
      kotlin/dev/todobar/mobile/
        MainActivity.kt          # onboarding + start/stop controls
        BubbleService.kt         # foreground service hosting the handle
        BootReceiver.kt          # restarts the bubble after reboot
        TodoRepository.kt        # local task persistence
        model/Task.kt
        ui/SidebarOverlayController.kt
        ui/TaskAdapter.kt
      res/
        layout/                  # bubble, sidebar, settings, task row
        drawable/                # handle, panel, dots, buttons, icons
        values/, values-night/   # Studio (light) + Obsidian (dark) palettes
  build.gradle.kts
  settings.gradle.kts
```
