import { useEffect, useState } from 'react'
import type { Task } from './tasks'
import { scheduleLocalStorageWrite } from './storage'

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
    return scheduleLocalStorageWrite(storageKey, JSON.stringify(tasks))
  }, [storageKey, tasks])

  return [tasks, setTasks] as const
}
