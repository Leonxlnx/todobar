# UI Direction

Todobar should feel like a native desktop utility, not a website squeezed into a
panel. The sidebar stays compact, but the surface should still feel deliberate
and premium.

## Current Direction

- One visible edge handle opens the right-side utility panel.
- The open panel uses a compact command rail for navigation instead of a large
  top navigation bar.
- Today, Month Plan, and Lists are the visible planning zones; reminders stay
  inline on tasks instead of taking over a panel section.
- Task rows should behave like command rows, not floating cards.
- Hover states should clarify affordance without shifting layout.
- Motion should explain open, close, collapse, completion, and task insertion.
- Light mode should feel crisp and quiet.
- Dark mode should keep contrast high without turning hover states white.

## Customization

The product should let users tune the sidebar to their workspace:

- panel width
- visible tab size
- handle height and vertical position
- task row height, spacing, and text size
- motion speed
- corner radius
- surface opacity
- completed-task visibility
- section order
- light/dark mode
- Codex, Quartz Glass, Frost, Paper, Graphite, Midnight, Clay, and Blueprint
  theme presets

## Guardrails

- Avoid nested cards and decorative dashboards.
- Do not add a full AI chat surface into the sidebar.
- Keep destructive actions visible but visually quiet.
- Keep the sidebar useful without AI or cloud accounts.
- AI, MCP, Gmail, calendar, and file context must be optional,
  permissioned, and auditable.
- A larger companion window can handle setup-heavy workflows later.
