# Todobar Product Ideas

These are creative directions to explore after the core desktop shell is stable.
They are grouped by product value, not implementation order.

## Fast Capture

- Universal Quick Capture: one shortcut opens a tiny input over any app.
- Capture With Source: capture the active app/window title when explicitly
  enabled.
- Capture Stack: enter several tasks quickly, one per line.
- Voice Capture: push-to-talk task capture for rough thoughts.
- Clipboard Capture: turn copied text into tasks, only when invoked.
- Screenshot Capture: attach a screenshot snippet to a task with explicit user
  action.
- Natural Language Capture: `send invoice tomorrow 9am #admin p1`.

## Sidebar Modes

- Today Mode: the default narrow plan.
- Inbox Mode: raw captures waiting for cleanup.
- Month Plan: slow tasks and bigger outcomes.
- Later: parked work that should not pollute Today.
- Focus Mode: one task, minimal controls, optional timer.
- Review Mode: approve/reject AI and MCP suggestions.
- Source Mode: see tasks grouped by where they came from.

## AI Features

- Plan Today: draft a realistic Today list from tasks and allowed context.
- Split Task: break a vague item into concrete next actions.
- Defer Smartly: propose what to move to Later or Month Plan.
- Find Blockers: detect tasks waiting on someone or missing context.
- Summarize Inbox: cluster messy captures into projects.
- Meeting Follow-ups: after a meeting, propose follow-up tasks from notes.
- End-of-Day Sweep: suggest what was completed, deferred, or still important.
- Weekly Reset: convert loose tasks into a practical week plan.
- Local Tone: user-defined planning style, such as calm, strict, or GTD.

## MCP Connectors

- GitHub: issues, PR review requests, assigned items.
- Calendar: today's meetings and follow-up windows.
- Markdown Vault: parse local notes and todo markers.
- Notion/Docs: selected pages only, never whole workspace by default.
- Linear/Jira: assigned issues, cycles, stale blockers.
- Gmail/Email: later, only with explicit mailbox scope.
- Local Files: selected folders only, with a visible resource picker.
- Browser Reading: not automatic history; only user-selected pages.

## Privacy and Trust

- Permission Chips: show active scopes in the sidebar.
- Source Labels: every imported task shows origin.
- Audit Ledger: list every AI/MCP read, suggestion, and write.
- Approval Queue: external changes must be accepted before entering tasks.
- One-Click Disconnect: disable a connector instantly.
- Local-Only Mode: permanently hide all AI/cloud features.
- Data Export: JSON and Markdown from day one.
- Red Team Mode: test prompt injection and unsafe connector behavior.

## Desktop-Native UX

- Edge Peek: hover/click handle to show a tiny preview.
- Snap Zones: right, left, or active-monitor edge.
- Per-Monitor Memory: remember preferred width/side per display.
- Tray/Menu Bar: capture, show/hide, settings, quit.
- Launch at Login: explicit setting with clear state.
- Compact Always-On-Top: pinned mini task strip.
- Window Rules: don't steal focus unless capture needs typing.
- Shortcut Repair: detect conflicts and offer alternatives.

## Open-Source Differentiators

- Local-first by default.
- No account required for core use.
- Transparent data model.
- Pluggable connectors.
- Public roadmap and issue templates.
- Themable, but restrained.
- Import/export instead of lock-in.
- Build docs for Windows and macOS from the first native release.

## Feature Ideas To Avoid Early

- Full calendar replacement.
- Kanban/project-management dashboard.
- Chat-first interface.
- Silent auto-sync.
- Background reading of browser history or files.
- Native widgets before the main desktop overlay is excellent.
- Complex team collaboration before local personal workflow is sharp.
