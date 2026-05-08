# Sidebar UX And Motion Plan

Todobar should feel like a native utility: fast, quiet, and predictable.

## Motion Principles

- Movement should explain state, not decorate.
- Open/close animation should be short.
- The sidebar should never bounce or overshoot like a marketing UI.
- Reduce motion should be respected.
- Keyboard interaction must not wait on long animation.

## Recommended Timings

| Interaction | Duration | Easing |
| --- | --- | --- |
| Sidebar open | 180-220ms | ease-out or custom cubic |
| Sidebar close | 140-180ms | ease-in |
| Task row enter | 120-160ms | ease-out |
| Suggestion card enter | 160-220ms | ease-out |
| Tab/content change | 100-140ms | subtle opacity/translate |
| Edge peek | 100-140ms | immediate feeling |

## Native Window Motion

Preferred architecture:

- native shell shows/hides the window
- React animates panel contents and inner transform
- avoid transparent-window tricks as a dependency
- keep the actual native window rectangle stable while animating

Why: moving/resizing native windows every frame can feel platform-specific and
can create flicker, DPI bugs, or focus oddities.

## Sidebar States

```ts
type SidebarState =
  | 'closed'
  | 'opening'
  | 'open'
  | 'capture'
  | 'command'
  | 'closing';
```

Important behavior:

- `capture` is an open state with input focused.
- `command` is an overlay inside the sidebar, not a separate window.
- Escape closes command first, then capture/sidebar.

## Layout Rules

- fixed width on desktop, around 380-440px
- max width should not exceed practical scanning width
- full width on small screens in preview/mobile
- stable toolbar height
- stable task row dimensions
- no nested cards
- no landing page
- no hero layout

## Keyboard Rules

- `CommandOrControl+Alt+T`: toggle sidebar
- `CommandOrControl+Shift+Space`: quick capture candidate
- `CommandOrControl+K`: command palette while sidebar is open
- `Escape`: close current overlay/state
- `Enter`: save capture or activate selected command
- `ArrowUp/ArrowDown`: navigate tasks/commands

Shortcuts must be editable because collisions are normal.

## Visual Direction

The sidebar should use:

- restrained contrast
- clear active states
- compact rows
- icons for repeated tools
- text labels only for important commands
- source chips for AI/MCP context
- clear approval cards for suggestions

Avoid:

- giant dashboard cards
- decorative gradients
- chat-first layout
- heavy illustrations
- multiple competing accent colors
- marketing copy inside the app surface

## Acceptance Checks

- Open/close feels instant on a low-end Windows laptop.
- No text shifts when tasks are added/completed.
- Focus state is always visible.
- Animation does not hide task loss or delayed persistence.
- Reduced-motion mode still feels clean.
