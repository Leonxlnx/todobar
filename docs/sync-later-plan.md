# Sync Later Plan

Todobar should be local-first before it is synced. Sync is valuable, but it is
not part of the first native milestone.

## Why Not Sync First

- The task model is not stable yet.
- Sync bugs can destroy trust faster than missing sync.
- AI/MCP audit history needs a clear local source of truth.
- Desktop sidebar value exists even offline.

## Local-First Foundation

Milestone 2 should store:

- tasks
- lists
- projects
- sources
- task events
- suggestions
- audit events

Every mutation should create an event. This makes future sync possible without
inventing a second history model later.

## Event Shape

```ts
type TaskEvent = {
  id: string;
  taskId: string;
  type: 'created' | 'updated' | 'completed' | 'deferred' | 'moved' | 'deleted';
  actor: 'user' | 'ai-approved' | 'connector-approved' | 'import';
  patch?: unknown;
  createdAt: string;
};
```

## First Export/Import

Before sync:

- JSON export
- Markdown export
- JSON import preview
- full local backup path

These features give users ownership immediately and help test the data model.

## Future Sync Options

Possible later paths:

- file-based sync folder
- CalDAV/VTODO
- self-hosted sync server
- hosted account sync
- Git-backed Markdown tasks

Recommended exploration order:

1. JSON/Markdown export.
2. File-based local backup.
3. CalDAV or Git-backed sync spike.
4. Hosted sync only after product-market proof.

## Sync Guardrails

- never block local capture on network
- queue remote writes
- show sync status
- preserve task event history
- support conflict review
- avoid silent destructive conflict resolution
- make sync optional

