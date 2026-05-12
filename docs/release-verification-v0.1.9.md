# Release Verification v0.1.9

This page records the checks for the `v0.1.9` release.

## Release

- Repository: `Leonxlnx/todobar`
- Release: https://github.com/Leonxlnx/todobar/releases/tag/v0.1.9
- Release workflow:
  https://github.com/Leonxlnx/todobar/actions/runs/25755544886
- Workflow status: passed
- Release commit: `9063bb75907986fbba5099016d01813bb893bab7`

Published assets:

- `Todobar_0.1.9_x64-setup.exe`
  - Size: 3,085,646 bytes
  - SHA-256: `1473a0a399c93382ee57fdf899e7dcc87adbc735d1fa5f6bd7202c40ad93dfc5`
- `Todobar_0.1.9_x64_en-US.msi`
  - Size: 4,472,832 bytes
  - SHA-256: `84542e8da2f2b74eeda3b56323d07219deec6e6f02126f11cc8b5bd663e949aa`
- `Todobar_0.1.9_aarch64.dmg`
  - Size: 5,052,824 bytes
  - SHA-256: `104cfca4461ff36ba90f28ef2cb21984e775d5e8c1b7ee7f92b244ccba7d7c3f`
- `Todobar_0.1.9_x64.dmg`
  - Size: 5,209,188 bytes
  - SHA-256: `53fcd8817ced9f980f508060d164227b3108484e083e1d077402f36d9199fe11`
- `Todobar_aarch64.app.tar.gz`
  - Size: 4,786,103 bytes
  - SHA-256: `fc7789afd8f19daf0b19acd91a63b13f1d6269f0fd572cb798c34e95a41f9059`
- `Todobar_x64.app.tar.gz`
  - Size: 4,944,519 bytes
  - SHA-256: `92abd3739809ac93821aba84db8ad93276d691ea3323ae1c7fac207b73d419e2`

## Release Scope

`v0.1.9` keeps the Gmail OAuth foundation in code but hides the visible Gmail
connector UI until Google OAuth verification and a real public rollout are
ready.

Included changes:

- Gmail connector UI is hidden behind an internal feature flag.
- Inbox suggestions are not shown in the normal product UI.
- README clarifies that Gmail is implemented as a hidden foundation, not a
  visible feature.
- Tests assert that Gmail remains hidden while the code stays available for
  future activation.

## Local Windows Verification

Checked on Windows from this repository:

```bash
npm run verify
npm run lint
npm run build
npm run test:smoke
cargo check
npm run test:native
```

The local native no-bundle build produces:

- `src-tauri/target/release/todobar.exe`

## CI Verification

GitHub Actions CI is configured to run on:

- `windows-latest`
- `macos-latest`

The release workflow produces platform artifacts for:

- Windows
- macOS Apple Silicon
- macOS Intel

Release workflow result:

- Build Windows: passed
- Build macOS Apple Silicon: passed
- Build macOS Intel: passed

## Known Follow-Ups

- Gmail OAuth app registration and Google verification are not complete.
- Gmail remains hidden until the app identity, privacy policy, verification,
  and public connector UX are ready.
- macOS artifacts are unsigned and not notarized.
- Windows artifacts are unsigned.
