# Native Test Matrix

This matrix exists because a right-edge utility can look correct in a browser
and still fail as a desktop app. The native spike must be judged on real OS
behavior.

## Platforms

Minimum manual test set:

- Windows 11, single monitor, 100% scale
- Windows 11, laptop + external monitor, mixed scaling if possible
- macOS Apple Silicon, single display
- macOS with external display

Optional later:

- Windows 10 if we decide to support it
- Intel macOS if community testers need it
- Linux only if the project scope expands

## Sidebar Window Tests

| Test | Expected result |
| --- | --- |
| Open from another app with global shortcut | Sidebar appears at right edge without requiring focus first. |
| Close with shortcut | Sidebar disappears and previous app remains usable. |
| Close with Escape | Sidebar closes only when Todobar has focus. |
| Open on external monitor | Sidebar appears on the active or configured monitor. |
| Move between monitors | Sidebar remembers the chosen monitor after restart. |
| Windows scaling 125/150% | Sidebar width and edge position are correct. |
| macOS Spaces | Sidebar behavior is documented; no surprise duplicate windows. |
| Full-screen app active | Behavior is documented even if OS restrictions apply. |
| Taskbar/dock visibility | Sidebar does not clutter taskbar/dock when configured as utility. |
| Always-on-top | Sidebar stays above normal windows while open. |
| Focus return | Hiding the sidebar returns focus to the previous app when possible. |

## Shortcut Tests

| Test | Expected result |
| --- | --- |
| Default shortcut registers | `Alt+T` works. |
| Fallback shortcut registers | `CommandOrControl+Alt+T` works if the primary shortcut is unavailable. |
| Shortcut collision | App shows failure and offers edit. |
| Shortcut changed in settings | New shortcut persists after restart. |
| Quick capture shortcut | Opens capture state directly. |
| Disabled shortcut | Does not register on launch. |

## Tray/Menu Bar Tests

| Test | Expected result |
| --- | --- |
| Tray/menu icon appears | User can open sidebar and settings. |
| Quit from tray/menu | App exits cleanly. |
| Launch at login toggle | State persists and can be disabled. |
| Background mode | Closing settings does not quit background utility. |

## Data Tests

| Test | Expected result |
| --- | --- |
| Create task | Task persists after app restart. |
| Toggle complete | Event is recorded. |
| Export data | User gets full readable JSON. |
| Broken migration | App preserves old data and reports the problem. |
| Delete all data | User can wipe local database deliberately. |

## MCP/AI Safety Tests

| Test | Expected result |
| --- | --- |
| AI proposes Today plan | Suggestions are drafts, not silent changes. |
| Reject suggestion | No task mutation happens. |
| Approve suggestion | Audit event records what changed. |
| Connector read | UI shows source and scope. |
| Connector write | Confirmation is required. |
| Tool prompt injection attempt | External text cannot silently change permissions. |
| Local MCP server crash | Todobar reports connector failure without losing tasks. |

## Release Tests

| Test | Expected result |
| --- | --- |
| Windows installer | Installs, launches, uninstalls cleanly. |
| macOS DMG | App launches after install. |
| Code signing | Public beta builds avoid avoidable trust warnings. |
| Update check | Reports no update or installs signed update. |
| Offline startup | App works without network. |
