# Platform Notes: Windows And macOS

Todobar targets Windows and macOS first. The product should feel native on both,
but the implementation should avoid pretending both platforms behave the same.

## Shared Product Behavior

- global shortcut opens/closes sidebar
- quick capture works from any normal app
- sidebar appears on the right edge
- tray/menu bar access exists
- launch at login can be enabled
- data is local-first and exportable
- AI/MCP are optional

## Windows Notes

Primary risks:

- mixed-DPI monitor placement
- SmartScreen warnings for unsigned public builds
- installer choice and update artifacts
- global shortcut collisions
- frameless window shadows and resize edges

Recommended approach:

- test Windows 11 first
- use NSIS installer for early testers
- plan signing before public beta
- do not promise Microsoft Store early
- keep Windows Widget work separate from sidebar work

## macOS Notes

Primary risks:

- Gatekeeper signing/notarization
- global shortcut permission edge cases
- Spaces/full-screen app behavior
- menu bar utility expectations
- WidgetKit extension complexity

Recommended approach:

- ship a normal signed/notarized app outside App Store first
- add menu bar control early
- avoid relying on transparent windows
- document full-screen/Spaces behavior honestly
- postpone WidgetKit until the sidebar is proven

## Shortcut Defaults

Candidate defaults:

- toggle sidebar: `Alt+T`
- fallback toggle: `Alt+Shift+T`
- quick capture: `CommandOrControl+Shift+Space`
- command palette inside app: `CommandOrControl+K`

All global shortcuts must be editable.

## Installer Direction

Windows:

- dev: local unsigned build
- tester: GitHub release NSIS installer
- beta: signed installer
- later: WinGet

macOS:

- dev: local app bundle
- tester: DMG
- beta: signed/notarized DMG
- later: Homebrew cask

## Native Widgets

Windows Widgets and macOS WidgetKit are later platform extensions. They should
read a small snapshot and deep-link into Todobar instead of owning the full task
model.
