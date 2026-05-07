# MCP Connector Plan

MCP should be treated as a connector system with explicit user control.

## Supported Phases

### Phase 1: Read-Only Local MCP

- support stdio MCP servers
- user manually adds a local command
- Todobar lists tools/resources/prompts
- user grants read-only scope
- tool output can create suggestions only

### Phase 2: Trusted Connector Presets

- GitHub
- local Markdown folder
- calendar
- Notion/docs
- Linear/Jira

Each preset should define:

- default read scopes
- optional write scopes
- connector icon
- setup instructions
- danger labels for write tools

### Phase 3: Remote MCP

- support Streamable HTTP MCP servers
- require authentication
- validate origin/session behavior
- show remote endpoint clearly
- default to read-only

## Connector Object

```ts
type Connector = {
  id: string
  type: 'mcp-stdio' | 'mcp-http' | 'native'
  label: string
  enabled: boolean
  trustLevel: 'local' | 'verified' | 'custom'
  scopes: string[]
  createdAt: string
  updatedAt: string
}
```

## Tool Registry

```ts
type ConnectorTool = {
  connectorId: string
  name: string
  title?: string
  description?: string
  inputSchema: unknown
  outputSchema?: unknown
  risk: 'read' | 'write' | 'external' | 'unknown'
}
```

## UX Requirements

- connector list in settings
- active connector chips in sidebar
- tool/resource inspection panel
- approve dialog for first use
- approval queue for suggested task writes
- audit trail for every tool call

## Safety Defaults

- custom MCP servers are untrusted
- tool annotations are untrusted unless the connector is trusted
- write tools disabled by default
- resource reads are scoped
- no background polling until explicitly enabled
- all failures are visible and recoverable
