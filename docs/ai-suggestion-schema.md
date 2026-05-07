# AI Suggestion Schema

AI output should be structured and validated before it touches the task store.

## Suggestion Batch

```ts
type SuggestionBatch = {
  summary: string
  suggestions: TaskSuggestion[]
}
```

## Task Suggestion

```ts
type TaskSuggestion =
  | CreateTaskSuggestion
  | UpdateTaskSuggestion
  | MoveTaskSuggestion
  | SplitTaskSuggestion
```

## Create Task

```ts
type CreateTaskSuggestion = {
  type: 'create_task'
  title: string
  notes?: string
  priority?: 'focus' | 'normal' | 'later'
  targetList: 'Inbox' | 'Today' | 'Month Plan' | 'Later'
  dueAt?: string
  rationale: string
  sourceRefs: string[]
  confidence: number
}
```

## Update Task

```ts
type UpdateTaskSuggestion = {
  type: 'update_task'
  taskId: string
  patch: {
    title?: string
    notes?: string
    priority?: 'focus' | 'normal' | 'later'
    dueAt?: string | null
  }
  rationale: string
  sourceRefs: string[]
  confidence: number
}
```

## Move Task

```ts
type MoveTaskSuggestion = {
  type: 'move_task'
  taskId: string
  targetList: 'Inbox' | 'Today' | 'Month Plan' | 'Later'
  rationale: string
  sourceRefs: string[]
  confidence: number
}
```

## Split Task

```ts
type SplitTaskSuggestion = {
  type: 'split_task'
  taskId: string
  subtasks: Array<{
    title: string
    notes?: string
    priority?: 'focus' | 'normal' | 'later'
  }>
  rationale: string
  sourceRefs: string[]
  confidence: number
}
```

## Validation Rules

- title is required and must be short enough for the sidebar
- confidence must be between 0 and 1
- sourceRefs must point to visible selected context
- write suggestions must enter the review queue
- invalid suggestions are discarded and logged
- the user can edit before applying

## UI States

- pending
- approved
- rejected
- applied
- failed validation
- failed apply
