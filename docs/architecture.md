# Todobar Architecture

Todobar should be designed as a desktop tool first, with the web build acting as
a fast development surface.

## Runtime layers

```text
React UI
  -> task domain model
  -> runtime adapter
    -> web preview
    -> Tauri desktop shell
    -> optional Electron shell
```

The UI should not know whether the app is running in a browser, Tauri, or
Electron. It should ask the runtime adapter for capabilities:

- open or close the sidebar window
- register global shortcuts
- position the sidebar on the active monitor
- read and write local storage
- request integration permissions
- invoke MCP or AI actions

## Native shell

The first serious desktop build should target Tauri v2. It keeps the app small,
uses the system webview, and has plugins for desktop capabilities such as global
shortcuts. Electron remains a valid fallback if windowing, overlays, or updater
behavior need its more mature APIs.

The expected native windows are:

- `sidebar`: frameless, right-edge, always-on-top when open
- `settings`: normal window for preferences, integrations, and permissions
- `command`: optional compact capture window later

## Data model

Start local-first:

- `Task`
- `List`
- `Project`
- `Source`
- `Suggestion`
- `AuditEvent`

The source of truth should be local storage in the web preview and SQLite or an
equivalent embedded store in the desktop build. Sync can come later after the
model stabilizes.

## AI and MCP

AI should be a planning layer, not the database. It can propose task edits,
summaries, and next actions. The user should approve writes that came from
external context.

MCP should enter through adapters:

- `listTools()`
- `requestPermission(scope)`
- `readContext(query)`
- `proposeTasks(context)`
- `applyApprovedChanges(changes)`

That shape keeps GitHub, calendar, notes, filesystem, and future services from
leaking into the task UI.

## Permission model

Todobar should treat every external connection as sensitive:

- no silent sync
- no hidden AI reads
- no automatic third-party writes
- visible data scopes
- clear audit trail for imported or AI-created tasks

This matters because the app will sit close to the user's work and eventually
connect to private tools.
