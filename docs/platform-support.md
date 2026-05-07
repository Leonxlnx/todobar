# Platform Support

Todobar targets Windows and macOS first.

## Windows

Status: working.

Tested locally on Windows with:

- `npm run build`
- `npm run lint`
- `cargo check --manifest-path src-tauri/Cargo.toml`
- `npm run tauri:build`

The Windows release includes:

- NSIS setup executable
- MSI installer

Desktop behavior:

- frameless always-on-top window
- hidden taskbar entry
- right-edge dock handle
- click-through transparent regions
- global shortcut
- launch at login
- single-instance process behavior

## macOS

Status: CI-built.

GitHub Actions builds:

- Apple Silicon `.dmg`
- Intel `.dmg`
- `.app.tar.gz` archives

The macOS build is currently unsigned. That means macOS Gatekeeper can warn on
first open. Code signing and notarization are required before the app feels
fully production-ready for non-technical users.

Expected desktop behavior:

- frameless right-edge window
- global shortcut
- launch at login
- single-instance process behavior
- responsive panel sizing based on monitor work area

## Known Gaps

- No signed Windows certificate yet.
- No Apple Developer signing or notarization yet.
- No auto-update channel yet.
- No tray/menu bar controls yet.
- No multi-monitor position memory yet.

## Release Confidence

Windows has been built locally and in CI. macOS is built in CI on GitHub-hosted
runners, which gives packaging confidence, but real-device UX testing is still
needed for final polish.
