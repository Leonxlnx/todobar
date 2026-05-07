# Widget Strategy

Widgets are useful later, but they should not be the first version of Todobar.
The core product is a right-edge desktop utility that opens above any app. OS
widgets are separate extension surfaces with their own limitations.

## Recommendation

Milestone 1 should ship the native sidebar, tray/menu bar, global shortcuts, and
local database before native widgets.

Add widgets later for glanceable information:

- Today count
- next focus task
- overdue count
- quick open into Todobar
- maybe one-tap complete for a pinned focus item if the platform allows it

Do not try to make the widget the main task UI.

## Windows Widgets

Windows widgets are provided by a packaged Win32 app or PWA and rendered through
the Windows widget host. That means a Windows widget is not the same surface as
our always-on-top sidebar.

Useful later:

- small Today widget
- pinned task widget
- open Todobar from widget

Risk:

- packaging and Windows App SDK work are separate from the Tauri shell
- widget UI uses Adaptive Cards/provider JSON, not the React sidebar directly
- Store/distribution constraints may affect timing

## macOS Widgets

macOS widgets are WidgetKit extensions. They are good for glanceable state, but
they are not a replacement for a fast global-shortcut desktop sidebar.

Useful later:

- Today count
- next task
- focus task
- open Todobar via deep link

Risk:

- widget extension is a separate Apple target
- data sharing must be designed carefully
- interactivity and refresh behavior are platform-governed
- signing/notarization complexity increases

## Better First Native Surface

For the actual product feel, build these first:

- right-edge frameless sidebar
- tray/menu bar icon
- launch-at-login
- global quick capture
- command palette
- local database

These solve the user's daily workflow directly. Widgets are a distribution
expansion, not the foundation.

## Widget Data Boundary

When widgets are added, they should read a tiny derived state from local storage
or an app-group/shared file:

```ts
type WidgetSnapshot = {
  todayOpenCount: number;
  nextTaskTitle?: string;
  focusTaskTitle?: string;
  updatedAt: string;
};
```

Widgets should not have direct access to the full task database, AI prompts, or
connector tokens.

## Sources

- Windows widget providers: https://learn.microsoft.com/en-us/windows/apps/develop/widgets/widget-providers
- Apple WidgetKit: https://developer.apple.com/documentation/WidgetKit/
- Apple widget extension guide: https://developer.apple.com/documentation/WidgetKit/Creating-a-Widget-Extension

