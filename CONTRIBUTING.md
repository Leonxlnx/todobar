# Contributing

Thanks for helping improve Todobar.

## Local Setup

```bash
npm install
npm run tauri:dev
```

Before opening a pull request, run:

```bash
npm run build
npm run lint
cargo check --manifest-path src-tauri/Cargo.toml
```

## Product Principles

- Keep the sidebar fast and quiet.
- Prefer local-first behavior.
- Do not add external network access without an explicit permission model.
- Keep AI and MCP features optional and inspectable.
- Preserve Windows and macOS behavior when changing native window code.

## Pull Requests

Good pull requests include:

- a focused change
- screenshots or notes for visible UI changes
- the commands used to test the change
- platform notes when touching Tauri/window behavior

## Release Notes

User-visible changes should be written in plain English. Avoid internal jargon.
