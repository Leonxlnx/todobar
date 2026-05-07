# Product Vision

Todobar starts as a small right-edge todo utility, but the long-term product is
a local-first planning assistant that can sit next to any work app without
becoming a giant dashboard.

## Version 0 - Edge Planner

Version 0 is the current product foundation.

Goals:

- stay available from the screen edge
- open and close instantly through the handle, shortcut, and tray/menu bar
- capture Today, Month Plan, and custom list tasks
- keep data local
- launch at login
- feel native on Windows and macOS
- remain understandable for open-source contributors

Non-goals:

- no mandatory account
- no cloud sync
- no AI writes
- no hidden external reads

## Version 1 - Better Planner

Version 1 should make Todobar useful as a daily planning tool without depending
on AI.

Planned capabilities:

- editable tasks
- undo after delete and complete
- reminders and local notifications
- due dates and natural date parsing
- recurring tasks
- keyboard-first capture and triage
- list templates for Today, Week, Month, Backlog, and Projects
- import/export for user-owned data
- SQLite storage with migrations

## Version 2 - Assistant Layer

The assistant layer should help the user plan, not replace the task system.

Planned capabilities:

- suggestion queue for proposed task changes
- explainable AI planning cards
- split a rough task into next actions
- summarize selected context into a proposed Today plan
- confidence/source display for every suggestion
- approve, reject, or edit before applying changes
- audit log for every assistant action

Guardrails:

- AI never edits tasks silently
- AI is optional
- provider keys and secrets never live in the browser UI
- every external context source is permissioned

## Version 3 - MCP and Context Connectors

MCP connectors should make Todobar context-aware while keeping trust explicit.

First connector targets:

- local files and Markdown notes
- GitHub issues and pull requests
- Gmail inbox summaries
- calendar day context
- Notion or docs
- Linear/Jira style issue trackers

Connector rules:

- scoped reads
- visible permission prompts
- write confirmation
- connector status screen
- source links on imported/suggested tasks
- no background sync before the user enables it

## Companion Manager App

The sidebar should stay compact. Larger organization belongs in a separate
companion surface.

Possible manager app responsibilities:

- manage all lists, projects, templates, and connector settings
- configure AI providers and MCP servers
- review assistant audit logs
- batch-edit tasks
- export and backup data
- manage notification rules

This can be a second window inside the same Tauri app first. A separate app only
makes sense if the settings and planning surface outgrow the sidebar.

## Open Source Direction

The repository should make the current state and future direction obvious:

- README explains what works now
- roadmap explains what comes later
- issues track release gates and feature milestones
- release verification records CI and artifacts
- architecture docs keep future AI/MCP work from leaking into the simple task UI
