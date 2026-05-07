# Release Checklist

Use this before any public beta release.

## Code

- build passes
- lint passes
- native smoke tests pass on Windows
- native smoke tests pass on macOS
- migrations tested from previous version
- export/import tested
- update check tested

## Product

- default shortcuts documented
- shortcut collision handling works
- launch-at-login setting works
- settings window works
- offline startup works
- no hidden network calls
- AI disabled unless configured
- connectors disabled unless configured

## Security And Privacy

- no API keys in repo
- no secrets in logs
- data export works
- delete-all-data works
- connector scopes visible
- external writes require confirmation
- audit log records AI/MCP actions
- privacy policy updated
- security policy updated

## Distribution

- Windows installer generated
- Windows installer signed for beta
- macOS DMG generated
- macOS app signed and notarized for beta
- updater artifacts generated and signed
- release notes written
- checksums published

## Open Source

- license present
- contributor guide present
- issue templates present
- known limitations documented
- roadmap current
- source bibliography current

