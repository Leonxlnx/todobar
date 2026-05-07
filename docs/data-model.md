# Data Model Draft

This is a first draft for the local-first task model. It should be implemented
with TypeScript types first and SQLite migrations in the native milestone.

## Task

```ts
type Task = {
  id: string
  title: string
  notes?: string
  status: 'open' | 'done' | 'canceled'
  priority: 'focus' | 'normal' | 'later'
  listId: string
  projectId?: string
  dueAt?: string
  scheduledFor?: string
  completedAt?: string
  createdAt: string
  updatedAt: string
  sourceId?: string
}
```

## List

```ts
type List = {
  id: string
  name: 'Inbox' | 'Today' | 'Month Plan' | 'Later' | string
  sortOrder: number
  createdAt: string
  updatedAt: string
}
```

## Project

```ts
type Project = {
  id: string
  name: string
  color?: string
  archivedAt?: string
  createdAt: string
  updatedAt: string
}
```

## Source

```ts
type Source = {
  id: string
  type: 'manual' | 'ai' | 'mcp' | 'import'
  label: string
  uri?: string
  connectorId?: string
  createdAt: string
}
```

## Suggestion

```ts
type Suggestion = {
  id: string
  type: 'create_task' | 'update_task' | 'move_task' | 'split_task'
  status: 'pending' | 'approved' | 'rejected' | 'applied'
  title: string
  rationale?: string
  payload: unknown
  sourceId: string
  createdAt: string
  resolvedAt?: string
}
```

## AuditEvent

```ts
type AuditEvent = {
  id: string
  actor: 'user' | 'ai' | 'mcp' | 'system'
  action: string
  targetType: 'task' | 'suggestion' | 'connector' | 'setting'
  targetId?: string
  summary: string
  metadata?: unknown
  createdAt: string
}
```

## Initial SQLite Tables

- `tasks`
- `lists`
- `projects`
- `sources`
- `suggestions`
- `audit_events`
- `settings`
- `connectors`
- `connector_scopes`

## Sync Later

Do not add sync until local behavior is stable. When sync is added, use an
append-only event log and conflict resolution rules instead of overwriting whole
task records.
