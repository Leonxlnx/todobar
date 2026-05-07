# Security and Permission Model

Todobar will eventually sit close to private work: tasks, files, calendars,
issues, notes, email, and AI context. The security model must be part of the
product, not an afterthought.

## Core Rules

- Local tasks are private by default.
- No external connector is enabled by default.
- No AI action reads context unless the user selects or authorizes that context.
- No AI or MCP action writes to external systems without confirmation.
- Imported tasks keep source metadata.
- Every connector action creates an audit event.
- Tool descriptions, remote resources, documents, and web content are
  untrusted.

## Permission Scopes

Examples:

- `tasks:read`
- `tasks:write`
- `files:read:selected-folder`
- `github:read:issues`
- `github:write:issues`
- `calendar:read:today`
- `calendar:write:events`
- `mcp:tool:list`
- `mcp:tool:call:read`
- `mcp:tool:call:write`

The UI should show scopes in plain language:

- "Calendar: read today's events"
- "GitHub: read assigned issues"
- "Files: read selected folder"
- "MCP: call read-only tools"

## AI Suggestion Flow

```text
User action
  -> select allowed context
  -> AI creates structured suggestion
  -> suggestion appears in review queue
  -> user approves, edits, or rejects
  -> approved change writes to local task database
  -> audit event is stored
```

## MCP Tool Flow

```text
Discover server
  -> list tools/resources/prompts
  -> display capabilities
  -> request permission scope
  -> call tool only inside scope
  -> validate output
  -> create suggestion or local event
  -> require confirmation for writes
```

## Threats To Design Against

- prompt injection inside notes, web pages, issues, or docs
- malicious MCP tool descriptions
- lookalike tools with trusted-sounding names
- accidental data exfiltration through tool input
- overbroad resource access
- hidden background sync
- API key leakage
- untrusted update artifacts
- unsigned desktop builds

## Product UI Requirements

- Permission chips in the sidebar.
- Connector settings with exact scopes.
- Review queue for AI-created changes.
- Audit log for reads/writes/tool calls.
- Clear external-write confirmation dialogs.
- Local-only mode.
- Export and delete local data.
- Shortcut conflict warnings.

## Implementation Requirements

- Keep API keys in native secure storage or backend, not client code.
- Validate structured AI outputs before applying them.
- Validate MCP tool results before passing them back to the model.
- Time out tool calls.
- Rate-limit connector actions.
- Store minimal audit metadata.
- Never log secrets or full private document bodies by default.
