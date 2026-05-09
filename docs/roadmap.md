# Todobar Roadmap

This roadmap keeps the project focused on becoming a real desktop tool, not a
browser-only app.

## Milestone 0 - Product Spine

Goal: make the current prototype feel like the future product.

- Keep React + Vite as the dev preview.
- Move task logic into domain modules.
- Add a runtime adapter interface for web/Tauri/Electron.
- Replace `localStorage` with an interface that can later point to SQLite.
- Define task/list/project/source/suggestion types.
- Add command concepts: capture, show/hide, suggest, approve, defer.
- Keep UI fast, narrow, keyboard-first, and responsive.

Exit criteria:

- Sidebar opens/closes smoothly.
- Tasks persist locally.
- Quick capture works.
- AI/MCP section is clearly shown as suggestion infrastructure.
- No external data is accessed.

## Milestone 1 - Tauri Desktop Spike

Goal: prove that Tauri can deliver the required native UX.

- Add Tauri v2.
- Create a frameless dockable sidebar window.
- Add a settings window.
- Register global shortcuts.
- Add tray/menu-bar item.
- Add launch-at-login setting.
- Position sidebar on the active monitor.
- Test on Windows and macOS.
- Document every platform quirk.

Exit criteria:

- `Alt + T` opens Todobar from another app.
- Window appears on the correct monitor.
- Sidebar hides without leaving focus weirdness.
- App can run from tray/menu bar.
- No severe rendering mismatch between macOS and Windows.

Decision gate:

- If Tauri overlay behavior is smooth, continue with Tauri.
- If window behavior is unreliable after focused fixes, run an Electron spike
  before building more product features.

## Milestone 2 - Local Database

Goal: make the task core production-shaped.

- Add SQLite in the desktop shell.
- Add migrations.
- Add task event log.
- Add source metadata.
- Add import/export JSON.
- Add Markdown export.
- Add basic backup path.

Exit criteria:

- Data survives app updates and restarts.
- Migration tests pass.
- User can export all data.
- AI/imported tasks can be traced back to their source.

## Milestone 3 - Core UX

Goal: make Todobar useful every day without AI.

- Inbox
- Today
- Calendar
- Later
- Quick Capture
- Command Mode
- Keyboard navigation
- Editable tasks
- Local notifications and reminders
- Recurring tasks
- Natural-language date parsing
- Priority markers
- Defer/snooze
- Focus task
- Empty states and undo

Exit criteria:

- A user can capture, triage, plan today, complete, defer, and recover from
  mistakes using mostly keyboard actions.

## Milestone 4 - AI Suggestions

Goal: add intelligence without losing trust.

- Add provider adapter.
- Keep API keys out of browser/client UI.
- Use structured output for proposed task changes.
- Add suggestion queue.
- Add approve/reject/apply controls.
- Add audit log.
- Add red-team tests for prompt injection and accidental data exposure.

Exit criteria:

- AI can suggest a Today plan.
- AI can split a task.
- AI can summarize a local inbox.
- AI cannot silently edit tasks.
- Every suggestion shows source and confidence.

## Milestone 5 - MCP Connectors

Goal: connect useful context safely.

- Add MCP client adapter.
- Support local stdio MCP servers first.
- Add connector permission UI.
- Add tool/resource/prompt discovery view.
- Add scoped reads.
- Add write confirmation.
- Add audit log for every tool call.

First connector targets:

- local Markdown/files
- GitHub issues and PRs
- Gmail inbox summaries
- calendar day context
- Notion/docs
- Linear/Jira style issue trackers

Exit criteria:

- The user can connect one local MCP server.
- Todobar can read selected resources.
- Todobar can propose tasks from that context.
- No external write happens without explicit approval.

## Milestone 6 - Distribution

Goal: make it installable and trustworthy.

- Windows installer path.
- macOS app bundle path.
- Code signing plan.
- Notarization plan for macOS.
- Update channel.
- Crash/error reporting decision.
- Privacy policy.
- Contributor guide.
- Public license.

## Milestone 7 - Companion Manager

Goal: keep the sidebar compact while giving power users a larger planning
surface.

- Add a second Tauri window for project/list management.
- Move connector setup and AI provider configuration out of the sidebar.
- Add bulk task editing.
- Add notification rule management.
- Add import/export and backup controls.
- Add assistant audit log review.

Exit criteria:

- Sidebar stays fast and focused.
- Larger planning and settings workflows have room to breathe.
- Users can manage connectors, notifications, and data without cluttering the
  edge panel.

Open-source default:

- Apache-2.0 is the better default than MIT if we want an explicit patent grant
  for a serious open-source desktop app.
- Consider AGPL only if the project later depends on hosted sync as a core
  business boundary.

## Design Guardrails

- No marketing landing page as the product screen.
- No giant dashboard.
- No bloated AI chat surface.
- No silent sync.
- No hidden external reads.
- No task writes from AI without approval.
- Keep the sidebar calm and compact.
- Prefer icons for tools, text only for clear commands.
- Keep cards shallow; avoid nested card-heavy UI.
- Motion should explain open/close and state changes.

## Next Build Step

Implement Milestone 0 fully before adding Tauri:

1. Add `src/domain/` task model.
2. Add `src/runtime/` adapter shape.
3. Add `src/storage/` web storage implementation.
4. Add command palette skeleton.
5. Add suggestion queue data model.
6. Keep current UI working while making the architecture real.
