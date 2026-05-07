# Distribution Plan

Todobar should be easy to install, but we should not pretend distribution is
free. Signing, notarization, and update artifacts are real work.

## Release Targets

Milestone 1:

- GitHub release artifacts for testers
- unsigned local builds for development
- Windows NSIS installer for testing
- macOS DMG for testing

Milestone 2:

- signed Windows installer
- signed and notarized macOS build
- automatic update channel
- Homebrew cask consideration
- WinGet consideration

## Windows

Tauri can produce Windows installers as MSI or NSIS setup executables. For
public distribution, signing matters because unsigned downloaded apps can hit
SmartScreen trust warnings.

Recommended first path:

- build NSIS installer first
- use GitHub Actions for artifacts
- decide on Microsoft Artifact Signing / Trusted Signing or certificate later
- do not promise Microsoft Store until the product is stable

## macOS

DMG is the normal outside-App-Store distribution shape. Public distribution
needs Developer ID signing and notarization for a clean Gatekeeper experience.

Recommended first path:

- build `.app` and `.dmg`
- test local unsigned app internally
- add Developer ID signing/notarization when approaching public beta

## Updates

The updater should be added before public beta, not after. Users should not
manually reinstall a utility app that sits in their daily workflow.

Requirements:

- signed updater artifacts
- clear update check state
- manual "check for updates"
- no forced restart while the user is capturing a task

## CI

Use official Tauri GitHub Action or a simple matrix workflow:

- Windows runner builds Windows installer
- macOS runner builds Intel and Apple Silicon targets
- Linux can wait unless Linux becomes a target

## Sources

- Tauri GitHub Action: https://github.com/tauri-apps/tauri-action
- Tauri DMG: https://v2.tauri.app/distribute/dmg/
- Tauri Windows installer: https://tauri.app/distribute/windows-installer/
- Tauri updater: https://v2.tauri.app/plugin/updater/
- Tauri Windows signing: https://tauri.app/distribute/sign/windows/
- Apple macOS code signing: https://support.apple.com/guide/security/app-code-signing-process-sec3ad8e6e53/web
- Windows code signing options: https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/code-signing-options
