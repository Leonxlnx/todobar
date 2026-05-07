# Tauri vs Electron Decision Matrix

This project should start with Tauri v2, but keep the UI runtime-neutral until
the native sidebar behavior is proven on Windows and macOS.

## Recommendation

Use Tauri first for the desktop spike.

Reason: Todobar is a utility app that should feel light, fast, local-first, and
trustworthy. Tauri's smaller shell, permission model, Rust backend, and official
plugins are a strong match for that shape.

Keep Electron as a planned fallback if Tauri cannot deliver reliable edge-window
behavior on real hardware.

## What Matters For Todobar

| Capability | Tauri v2 | Electron | Product impact |
| --- | --- | --- | --- |
| React/Vite UI reuse | Strong | Strong | Current UI can move to either runtime. |
| Global shortcuts | Official plugin, explicit permissions | Mature main-process API | Required for show/hide and quick capture. |
| Frameless sidebar window | Supported through window config/APIs | Very mature BrowserWindow options | Must be tested on Windows/macOS, not assumed. |
| Tray/menu bar | Supported | Mature | Required for a utility app. |
| Local database | SQL plugin supports SQLite and migrations | Many Node SQLite options | Tauri is cleaner for a local-first app. |
| Updater | Official plugin with signed artifacts | Mature ecosystem, but packaging choices matter | Needed before public beta. |
| Security boundary | Capability-based API permissions | Strong if sandboxed/preload is disciplined | Tauri nudges the project toward narrower native access. |
| Bundle size/runtime | Usually smaller | Ships Chromium/Node | Tauri better matches "quick, clean, smooth." |
| Window edge cases | Needs spike validation | More proven in many production apps | Main reason to keep Electron fallback. |
| Contributor familiarity | Rust + web | Node + web | Electron may be easier for some OSS contributors. |

## Tauri Risks To Spike Early

- Always-on-top behavior while other apps are active.
- Focus return after hiding the sidebar.
- Active-monitor detection on multi-monitor setups.
- Per-monitor DPI positioning on Windows.
- Shortcut collisions and failed registration.
- Frameless window shadows and resize behavior.
- Launch-at-login behavior on both platforms.
- Update artifact generation and signing path.

## Electron Risks If Used Later

- Larger app footprint.
- More security footguns if renderer access is not locked down.
- Higher memory usage for a small daily utility.
- Auto-update/signing complexity is still real.
- Easy to overbuild because Node access is convenient.

## Decision Gate

After the Tauri spike, continue with Tauri only if all are true:

- Sidebar opens from another app within a perceptibly instant timeframe.
- Window lands on the expected monitor.
- Hide/show does not create weird focus traps.
- Shortcut registration failure can be handled and edited.
- Packaging works on Windows and macOS in CI.

Run an Electron spike if any of these remain unresolved after focused fixes:

- Tauri cannot reliably show the overlay above normal app windows.
- Focus behavior feels broken in daily use.
- Multi-monitor placement is inconsistent on Windows.
- Required window behavior depends on brittle platform-specific hacks.

## Implementation Guardrail

React components must never import Tauri or Electron packages directly. Native
features go through `DesktopRuntime`, so the product layer survives a runtime
switch.

