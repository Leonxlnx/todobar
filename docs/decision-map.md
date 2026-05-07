# Decision Map

This is the short version of the research and planning pass.

## Product

Todobar is a native right-edge desktop utility, not a browser app. The browser
build stays as a fast development preview.

## Runtime

Start with Tauri v2.

Keep Electron as fallback only if Tauri fails the native window/focus/multi-
monitor spike.

## Architecture

Use runtime and storage adapters:

- React UI
- domain commands
- repository interface
- web adapter for preview
- Tauri adapter for desktop
- possible Electron adapter if needed

## Data

Local-first SQLite is the desktop source of truth.

Use event history and audit events from the beginning so AI, MCP, import/export,
and future sync do not become special cases.

## AI

AI produces structured suggestions.

It does not silently mutate tasks or external systems.

## MCP

MCP connectors are permissioned sources/tools.

Start with local/stdin-stdout style connectors and visible scopes. External
writes need approval.

## Widgets

Do not build OS widgets first.

Build the sidebar, tray/menu bar, shortcuts, and local database first. Later
Windows/macOS widgets should be glanceable companion surfaces.

## Distribution

Early:

- GitHub releases
- Windows NSIS test installer
- macOS DMG test build

Public beta:

- signed Windows build
- signed and notarized macOS build
- updater
- privacy/security docs

## Open Source

Use Apache-2.0 unless there is a strong reason to choose MIT.

Keep contributor surfaces clear and security-sensitive areas maintainer-reviewed.

## Next Build Step

Implement Milestone 0:

- domain model
- command system
- storage adapter
- runtime adapter
- command palette skeleton
- suggestion queue
- keep the current sidebar working

Then add Tauri and run the native spike.

