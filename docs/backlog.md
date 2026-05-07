# Backlog

This backlog is ordered for the next implementation phase.

## P0 - Keep The Product Spine Clean

- Move task types into `src/domain`.
- Add command definitions and command runner.
- Add storage repository interface.
- Move localStorage behind `webTaskRepository`.
- Add runtime adapter interface.
- Keep sidebar behavior unchanged while refactoring.
- Add command palette skeleton.
- Add suggestion queue model.

## P1 - Improve The Current Preview

- Add Inbox/Today/Month/Later view switching.
- Add create task target list.
- Add basic edit/defer/delete controls.
- Add keyboard navigation.
- Add undo for task mutations.
- Add reduced-motion CSS.
- Add empty states that are useful but not wordy.

## P2 - Native Tauri Spike

- Add Tauri to existing Vite app.
- Create sidebar and settings windows.
- Register editable global shortcut.
- Add tray/menu bar.
- Add launch-at-login setting.
- Add monitor positioning.
- Add SQLite repository.
- Build Windows/macOS test artifacts.

## P3 - AI/MCP Foundations

- Add suggestion batch schema validation.
- Add local suggestion queue.
- Add provider adapter interface.
- Add MCP connector manifest shape.
- Add permission screen.
- Add audit event model.
- Add first local-only mock connector.

## P4 - Public Project Readiness

- Add `LICENSE`.
- Add `CONTRIBUTING.md`.
- Add `SECURITY.md`.
- Add issue templates.
- Add privacy policy draft.
- Add release checklist.
- Add CI build/lint workflow.

