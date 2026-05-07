# Milestone 0 Implementation Plan

Goal: turn the current web preview into a clean product spine before adding the
native Tauri shell.

## Scope

Milestone 0 is still browser-preview only, but the architecture should already
look like a desktop app:

- domain model separated from components
- runtime adapter separated from UI
- storage adapter separated from task logic
- commands separated from buttons/shortcuts
- AI/MCP represented as suggestions, not real external access yet

## File Plan

Create these modules:

- `src/domain/task.ts`
- `src/domain/list.ts`
- `src/domain/source.ts`
- `src/domain/suggestion.ts`
- `src/domain/commands.ts`
- `src/storage/taskRepository.ts`
- `src/storage/webTaskRepository.ts`
- `src/runtime/DesktopRuntime.ts`
- `src/runtime/webRuntime.ts`
- `src/features/sidebar/Sidebar.tsx`
- `src/features/quickCapture/QuickCapture.tsx`
- `src/features/suggestions/SuggestionQueue.tsx`
- `src/features/commands/CommandPalette.tsx`

Later Tauri modules should live beside the web adapters, not inside feature UI:

- `src/runtime/tauriRuntime.ts`
- `src/storage/tauriSqlTaskRepository.ts`

## Domain Commands

Start with a small command set:

- `sidebar.toggle`
- `sidebar.open`
- `sidebar.close`
- `task.capture`
- `task.toggleComplete`
- `task.defer`
- `task.moveToToday`
- `suggestion.planToday`
- `suggestion.splitTask`
- `suggestion.approve`
- `suggestion.reject`

Every UI action should call a command. This makes shortcuts, command palette,
tray actions, and future AI approvals hit the same product behavior.

## Storage Interface

The UI should depend on a repository interface:

```ts
export interface TaskRepository {
  listTasks(): Promise<Task[]>;
  createTask(input: CreateTaskInput): Promise<Task>;
  updateTask(id: TaskId, patch: TaskPatch): Promise<Task>;
  recordEvent(event: TaskEvent): Promise<void>;
}
```

Milestone 0 implementation uses localStorage behind this interface. Milestone 2
swaps the implementation to SQLite.

## Runtime Interface

The runtime interface should hide platform details:

```ts
export interface DesktopRuntime {
  kind: 'web' | 'tauri' | 'electron';
  toggleSidebar(): Promise<void>;
  closeSidebar(): Promise<void>;
  registerShortcut(action: string, accelerator: string): Promise<ShortcutResult>;
  getMonitors(): Promise<MonitorInfo[]>;
  openSettings(): Promise<void>;
}
```

In web preview, unsupported methods return safe no-op results with explanatory
messages for development.

## UI Work

Keep the first screen as the actual sidebar experience:

- right-edge layout
- smooth open/close
- Inbox/Today/Month/Later tabs
- quick capture
- task rows with complete/defer controls
- suggestion queue surface
- small connection/status area for future MCP sources
- command palette skeleton opened by `Cmd/Ctrl+K`

Do not add a landing page.

## Test Plan

- Build must pass.
- Lint must pass.
- Browser smoke test:
  - quick capture creates a task
  - toggling completion persists after reload
  - suggestion draft appears and can be rejected
  - sidebar opens/closes by shortcut
  - mobile/narrow viewport does not overflow

## Exit Criteria

Milestone 0 is done when adding Tauri is mostly native-shell work, not a UI
rewrite.

