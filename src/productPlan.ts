import type { LucideIcon } from 'lucide-react'
import {
  Bot,
  Cable,
  Command,
  Database,
  MonitorCog,
  Pin,
  ShieldCheck,
} from 'lucide-react'

export type ProductNote = {
  icon: LucideIcon
  title: string
  text: string
}

export const platformNotes: ProductNote[] = [
  {
    icon: MonitorCog,
    title: 'Native shell',
    text: 'Use Tauri first for a small open-source app; keep Electron as the fallback if overlay APIs become the blocker.',
  },
  {
    icon: Pin,
    title: 'Everywhere sidebar',
    text: 'A frameless always-on-top window can attach to the right edge of the active monitor and remember per-screen placement.',
  },
  {
    icon: Command,
    title: 'Global control',
    text: 'Desktop builds register Cmd/Ctrl + Shift + T natively, while the web preview keeps the same shortcut for development.',
  },
]

export const runtimeCards: ProductNote[] = [
  {
    icon: Database,
    title: 'Local-first tasks',
    text: 'Start with local persistence, then add sync only after the task model is stable.',
  },
  {
    icon: Cable,
    title: 'MCP connectors',
    text: 'Connect GitHub, calendar, notes, files, or project tools through permissioned MCP adapters.',
  },
  {
    icon: Bot,
    title: 'AI planning layer',
    text: 'Let AI summarize inboxes, split work into next actions, and suggest Today without owning the source of truth.',
  },
  {
    icon: ShieldCheck,
    title: 'Permission wall',
    text: 'Every external read, write, sync, or AI action needs scoped capabilities and visible user approval.',
  },
]
