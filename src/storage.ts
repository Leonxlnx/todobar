type IdleWindow = Window &
  typeof globalThis & {
    cancelIdleCallback?: (handle: number) => void
    requestIdleCallback?: (
      callback: () => void,
      options?: { timeout?: number },
    ) => number
  }

export function scheduleLocalStorageWrite(
  key: string,
  value: string,
  timeout = 500,
) {
  let cancelled = false
  const idleWindow = window as IdleWindow
  const write = () => {
    if (cancelled) {
      return
    }

    try {
      window.localStorage.setItem(key, value)
    } catch {
      // Native builds can later swap localStorage for a dedicated store.
    }
  }

  if (idleWindow.requestIdleCallback) {
    const handle = idleWindow.requestIdleCallback(write, { timeout })

    return () => {
      cancelled = true
      idleWindow.cancelIdleCallback?.(handle)
    }
  }

  const handle = window.setTimeout(write, Math.min(timeout, 160))

  return () => {
    cancelled = true
    window.clearTimeout(handle)
  }
}
