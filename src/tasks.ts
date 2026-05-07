export type Task = {
  id: number
  title: string
  meta: string
  priority: 'focus' | 'normal' | 'later'
  done?: boolean
}

export const initialToday: Task[] = [
  {
    id: 1,
    title: 'Design sidebar shell',
    meta: 'Today · 40 min',
    priority: 'focus',
  },
  {
    id: 2,
    title: 'Prototype desktop shortcut',
    meta: 'Today · Native hook',
    priority: 'normal',
  },
  {
    id: 3,
    title: 'Capture inbox',
    meta: 'Quick add',
    priority: 'normal',
    done: true,
  },
]

export const monthPlan: Task[] = [
  {
    id: 4,
    title: 'Open source roadmap',
    meta: 'May · Milestone 0.1',
    priority: 'focus',
  },
  {
    id: 5,
    title: 'Later',
    meta: 'Park ideas without pressure',
    priority: 'later',
  },
]
