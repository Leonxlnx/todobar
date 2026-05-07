# Command System Plan

Todobar should be command-driven internally. Buttons, shortcuts, tray actions,
AI approvals, and command palette items should all call the same command layer.

## Why

This keeps the app coherent as it grows:

- global shortcuts can trigger the same behavior as UI buttons
- command palette does not duplicate logic
- tray/menu bar actions stay simple
- AI/MCP suggestions can propose commands instead of mutating data directly
- audit logging can happen in one place

## Command Shape

```ts
type CommandId =
  | 'sidebar.toggle'
  | 'sidebar.open'
  | 'sidebar.close'
  | 'capture.open'
  | 'task.create'
  | 'task.complete'
  | 'task.defer'
  | 'task.moveToToday'
  | 'suggestion.planToday'
  | 'suggestion.splitTask'
  | 'suggestion.approve'
  | 'suggestion.reject'
  | 'settings.open';

type CommandContext = {
  source: 'keyboard' | 'button' | 'tray' | 'command-palette' | 'ai' | 'mcp';
  selectedTaskIds?: string[];
};

type CommandResult = {
  ok: boolean;
  message?: string;
  auditEventId?: string;
};
```

## Command Categories

### Local UI Commands

- toggle sidebar
- open command palette
- switch views
- focus capture input

These do not need audit events unless they expose data externally.

### Task Commands

- create task
- complete task
- edit title
- defer
- move list
- restore from undo

These should produce task events.

### Suggestion Commands

- generate suggestion
- approve suggestion
- reject suggestion
- apply suggestion

These should always produce audit events.

### Connector Commands

- connect source
- refresh source
- read selected resource
- propose import
- execute external write

Reads and writes require source/scope metadata. Writes require confirmation.

## Shortcut Binding

Shortcut settings should bind accelerators to command IDs:

```ts
type ShortcutBinding = {
  commandId: CommandId;
  accelerator: string;
  enabled: boolean;
  editable: boolean;
};
```

The native runtime registers only enabled global shortcuts. The web preview can
simulate the subset that works inside the browser tab.

## Command Palette

Palette entries should be generated from command metadata:

```ts
type CommandDefinition = {
  id: CommandId;
  title: string;
  section: 'Tasks' | 'Suggestions' | 'Navigation' | 'Settings';
  enabled(context: CommandContext): boolean;
  run(context: CommandContext): Promise<CommandResult>;
};
```

This makes the palette extensible without hardcoding every action in the UI.

## AI/MCP Boundary

AI and MCP should not execute arbitrary commands. They can produce proposed
command batches that are validated before the user sees them:

```ts
type ProposedCommand = {
  commandId: CommandId;
  arguments: Record<string, unknown>;
  reason: string;
  sourceIds: string[];
};
```

The app validates:

- command exists
- arguments match schema
- source scope allows it
- risk level is acceptable
- user approved when required

