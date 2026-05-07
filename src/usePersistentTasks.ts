import { useEffect, useState } from 'react'
import type { Task } from './tasks'

export function usePersistentTasks(seedTasks: Task[], storageKey: string) {
  const [tasks, setTasks] = useState<Task[]>(() => {
    try {
      const stored = window.localStorage.getItem(storageKey)

      if (!stored) {
        return seedTasks
      }

      const parsed = JSON.parse(stored) as Task[]
      return Array.isArray(parsed) ? parsed : seedTasks
    } catch {
      return seedTasks
    }
  })

  useEffect(() => {
    try {
      window.localStorage.setItem(storageKey, JSON.stringify(tasks))
    } catch {
      // Desktop builds can swap this for SQLite or an encrypted local store.
    }
  }, [storageKey, tasks])

  return [tasks, setTasks] as const
}
