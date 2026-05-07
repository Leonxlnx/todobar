# Product Spec V0

Todobar is a native desktop sidebar for capturing, triaging, and planning tasks
without leaving the current app.

## One-Sentence Product

A fast right-edge task sidebar that opens from anywhere, keeps the day clear,
and later uses AI/MCP to turn scattered context into approved task suggestions.

## Target User

People who work across many apps and lose tasks between browser tabs, chats,
issues, calendars, notes, and code editors.

Todobar is not for teams first. It is a personal operating layer first.

## Core Promise

- Open instantly.
- Capture without context switching.
- Decide what matters today.
- Keep local data trustworthy.
- Let AI suggest, never silently act.

## Core Surfaces

### Right Sidebar

The main product surface.

- appears from the right edge
- keyboard-first
- compact task list
- quick capture
- Today/Inbox/Month/Later views
- suggestion queue
- connector status chips

### Quick Capture

Fastest path from thought to saved task.

- global shortcut opens capture state
- natural text entry
- optional due/date parsing later
- save and vanish

### Command Palette

Power surface for actions.

- capture
- plan today
- split task
- move/defer
- connect source
- open settings

### Settings

Normal window, not in the sidebar.

- shortcuts
- launch at login
- data export/delete
- AI provider settings
- connector permissions
- theme/window behavior

## V0 Task Views

### Inbox

Unprocessed tasks. Fast capture lands here unless explicitly assigned.

### Today

The daily working list. Should stay small.

### Month Plan

Rough larger commitments. Not a full calendar or roadmap.

### Later

Deferred tasks that should not distract today.

## Expected Daily Flow

1. User presses global shortcut from any app.
2. Sidebar appears on the right edge.
3. User captures a task.
4. Sidebar hides automatically or stays open based on setting.
5. Later, user opens Today and triages Inbox.
6. AI can propose a plan, but the user approves every change.

## AI Role

AI should reduce planning friction:

- split vague tasks
- group inbox items
- suggest Today order
- find stale tasks
- extract follow-ups from approved context

AI should not become a chat-first interface. Chat may exist later, but the main
interaction is task suggestions and commands.

## MCP Role

MCP should bring context into task planning:

- GitHub issues/PRs
- calendar events
- local Markdown notes
- Notion/docs
- Linear/Jira-style work items

MCP should not create a hidden background agent. Users choose sources and scopes.

## Non-Goals For V0

- team workspace
- full project management dashboard
- calendar replacement
- email client
- kanban board
- autonomous agent mode
- native OS widgets
- hosted sync
- mobile app

## Quality Bar

Todobar should feel like a system utility:

- open/close animation under user control
- no visual noise
- no startup lag
- no task loss
- no hidden network behavior
- clear shortcut failure handling
- small, readable settings

## V0 Success Criteria

- User can capture tasks from any app.
- User can keep Today separate from Inbox.
- App works offline.
- Data is exportable.
- Sidebar behavior is reliable on Windows and macOS.
- AI/MCP design is visible but permissioned and optional.

