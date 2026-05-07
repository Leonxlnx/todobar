# Open Source Plan

Todobar should be designed as an open-source product from the start, not opened
after the architecture has already become hard to understand.

## License

Recommended default: Apache-2.0.

Why:

- permissive for personal and commercial use
- explicit patent grant
- familiar to serious infrastructure contributors
- compatible with a future plugin ecosystem

MIT is acceptable if maximum simplicity matters more than patent language.
AGPL should only be considered if hosted sync becomes the strategic boundary.

## Repository Shape

Short term:

- single app repo
- React/Vite frontend
- Tauri shell when native spike starts
- docs in `docs/`
- no monorepo until there are real packages to split

Possible later split:

- `apps/desktop`
- `packages/task-domain`
- `packages/runtime-adapters`
- `packages/mcp-client`
- `packages/ui`

Do not split early. It slows down product iteration before the boundaries are
proven.

## Contribution Surface

Good first contribution areas:

- small UI fixes
- keyboard shortcut polish
- import/export formats
- local parsing rules
- connector manifests
- documentation

Areas that should require maintainer review:

- native window behavior
- MCP permissions
- AI provider handling
- database migrations
- updater/signing changes
- telemetry/crash reporting

## Project Standards

- Local-first by default.
- No hidden network calls.
- AI changes are drafts until approved.
- External connector writes require explicit confirmation.
- Every connector needs a clear permission model.
- Every migration needs a rollback/backup story.
- No dependency with unclear license in core code.

## Public Beta Checklist

- license file
- contributor guide
- code of conduct
- security policy
- privacy policy
- issue templates
- architecture overview
- signed Windows build
- signed/notarized macOS build
- update channel
- export/delete-data controls

