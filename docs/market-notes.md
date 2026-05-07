# Market and Inspiration Notes

Todobar should not copy existing task apps, but several products validate the
core interaction pattern.

## What Existing Tools Prove

### Todoist

Todoist validates global desktop shortcuts for both show/hide and Quick Add.
Their docs also note platform limitations, such as Linux support gaps for some
global shortcuts. Lesson: shortcuts must be editable and failure-tolerant.

### Things

Things validates Quick Entry from any app. Their Quick Entry with Autofill also
shows that contextual capture is useful, but it must be intentional and
user-controlled. Lesson: source-aware capture is powerful, but should never
silently read private context.

### Raycast-style command tools

Command palettes work because they are fast, keyboard-first, and extensible.
Lesson: Todobar should eventually have command mode, but the main sidebar
should remain a todo surface, not a generic launcher.

### Potasko

Potasko is a task app built with Tauri 2.0, Rust, SvelteKit, SQLite, and CalDAV.
It validates the local/offline-first + sync-later direction. Its public feature
set includes due dates, priorities, recurring tasks, multiple lists, local
SQLite storage, offline work, sync queues, and conflict handling.

Lesson: Todobar should keep a clean local model first, then add sync through a
change/event queue instead of direct remote ownership of the task list.

### Tabularis

Tabularis is an open-source Tauri + React desktop app with an MCP-native
direction, audit/approval messaging, and local-first positioning. Lesson:
Todobar can be MCP-native without making AI the whole product.

Tabularis also shows a useful open-source shape for Todobar:

- Apache-2.0 license
- Tauri backend + React frontend
- local secrets in the system keychain
- plugins as isolated processes over JSON-RPC/stdin/stdout
- MCP support positioned as a controlled bridge, not raw credentials in chat
- public release channels such as GitHub releases, Homebrew, WinGet, Snap/AUR

Lesson: a serious open-source desktop utility needs product docs, security
docs, release paths, and a contribution surface, not just app code.

## Differentiation

Todobar's opportunity:

- right-edge overlay instead of full task manager window
- fast capture plus lightweight planning
- local-first task database
- AI suggestions with approval queue
- MCP connectors with visible permission scopes
- no account required for core use
- open-source and transparent data model

## Product Risks

- becoming a generic dashboard
- adding AI chat before task workflow is good
- making connector setup too heavy
- losing trust through hidden context reads
- shipping a desktop app without proper signing/update path
- letting shortcut or focus bugs break the core promise

## Research Links

- Todoist shortcuts: https://www.todoist.com/help/articles/205063212-Kurzbefehle
- Todoist Quick Add: https://get.todoist.help/hc/en-us/articles/115001745265-Task-Quick-Add
- Things Quick Entry: https://culturedcode.com/things/help/quickentry/
- Potasko: https://potasko.app/
- Tabularis: https://tabularis.dev/
