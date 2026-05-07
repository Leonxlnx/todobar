# Privacy And Telemetry Plan

Todobar should earn trust by being useful without hidden collection.

## Default Privacy Position

- Local-first data.
- No telemetry in the first public prototype.
- No hidden network calls.
- No AI request unless the user explicitly triggers or enables it.
- No connector reads without visible scope.
- No external writes without confirmation.

## Data Classes

| Data | Default storage | Network allowed? |
| --- | --- | --- |
| Tasks/lists/projects | Local SQLite | Only future sync or explicit export/import |
| Audit events | Local SQLite | No by default |
| AI suggestions | Local SQLite | AI provider only after user action |
| API keys/tokens | OS credential store later | Only to chosen provider |
| Connector metadata | Local SQLite | Only connector-specific calls |
| Crash logs | None initially | Optional later, explicit setting |

## AI Requests

AI requests must show:

- which tasks/context are being sent
- which provider is being used
- whether the model is cloud or local
- what change will be proposed

AI responses become suggestion objects. They do not mutate tasks directly.

## Connector Requests

Connectors need visible scopes:

- source name
- read/write capability
- resource boundary, such as repository, folder, calendar, or workspace
- last access time
- disconnect control

High-risk connector actions should create an approval card before execution.

## Telemetry Later

If telemetry is added, it should be:

- off by default during early open-source development
- documented in plain language
- event-level, not content-level
- easy to inspect
- easy to disable
- separate from crash/error reporting

Allowed examples if users opt in:

- app version
- operating system
- startup failure count
- anonymized feature usage counters
- update success/failure state

Never collect by default:

- task text
- connector content
- AI prompt contents
- file names from private folders
- clipboard contents
- calendar/email content

## Open Source Requirement

Privacy behavior must be understandable from code and docs. The project should
avoid "trust us" architecture where sensitive behavior is hidden in a closed
backend.

