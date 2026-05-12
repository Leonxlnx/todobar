import { useCallback, useEffect, useMemo, useState } from 'react'

const GMAIL_ACTIVITY_STORAGE_KEY = 'todobar.gmail.activity.v1'
const GMAIL_IGNORED_STORAGE_KEY = 'todobar.gmail.ignored.v1'
const GMAIL_MOCK_STORAGE_KEY = 'todobar.gmail.mock.v1'
const GMAIL_SCOPE = 'https://www.googleapis.com/auth/gmail.readonly'

export type GmailConnectionState =
  | 'connected'
  | 'disconnected'
  | 'error'
  | 'needs_reconnect'
  | 'unconfigured'

export type GmailSyncState =
  | 'idle'
  | 'refresh-ready'
  | 'syncing'
  | 'error'

export type GmailConnectionStatus = {
  accountEmail?: string | null
  message: string
  scope: string
  state: GmailConnectionState
  syncState: GmailSyncState
}

export type GmailThreadSuggestion = {
  date: string
  from: string
  gmailUrl: string
  snippet: string
  subject: string
  threadId: string
}

export type GmailSuggestionsResponse = {
  accountEmail: string
  fetchedAt: string
  suggestions: GmailThreadSuggestion[]
}

export type GmailActivity = {
  action: 'connect' | 'convert' | 'disconnect' | 'error' | 'ignore' | 'read'
  at: string
  detail: string
  id: string
}

export type GmailConnectorController = {
  activities: GmailActivity[]
  clearError: () => void
  connect: () => Promise<void>
  disconnect: () => Promise<void>
  error: string
  ignoredThreadIds: string[]
  ignoreSuggestion: (threadId: string, subject: string) => void
  isLoading: boolean
  loadSuggestions: () => Promise<void>
  recordActivity: (action: GmailActivity['action'], detail: string) => void
  refreshStatus: () => Promise<void>
  status: GmailConnectionStatus
  suggestions: GmailThreadSuggestion[]
}

const isTauriRuntime = () =>
  new URLSearchParams(window.location.search).get('runtime') === 'tauri' ||
  window.location.protocol === 'tauri:' ||
  navigator.userAgent.includes('Tauri') ||
  '__TAURI_INTERNALS__' in window ||
  '__TAURI__' in window

const defaultDisconnectedStatus: GmailConnectionStatus = {
  accountEmail: null,
  message: 'Connect Gmail to review unread inbox suggestions.',
  scope: GMAIL_SCOPE,
  state: 'disconnected',
  syncState: 'idle',
}

const defaultMockSuggestions: GmailThreadSuggestion[] = [
  {
    date: 'Today',
    from: 'Maya Chen',
    gmailUrl: 'https://mail.google.com/mail/u/0/#inbox/mock-thread-1',
    snippet: 'Can you turn the desktop sidebar review into a short action list?',
    subject: 'Review Todobar demo notes',
    threadId: 'mock-thread-1',
  },
  {
    date: 'Today',
    from: 'GitHub',
    gmailUrl: 'https://mail.google.com/mail/u/0/#inbox/mock-thread-2',
    snippet: 'One release checklist comment still needs a follow-up.',
    subject: 'Open-source release follow-up',
    threadId: 'mock-thread-2',
  },
]

function loadJson<T>(key: string, fallback: T): T {
  try {
    const stored = window.localStorage.getItem(key)

    if (!stored) {
      return fallback
    }

    const parsed = JSON.parse(stored) as T
    return parsed ?? fallback
  } catch {
    return fallback
  }
}

function saveJson<T>(key: string, value: T) {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // Connector audit state is helpful, but it must not block the UI.
  }
}

async function invokeNative<T>(command: string, args?: Record<string, unknown>) {
  const { invoke } = await import('@tauri-apps/api/core')

  return invoke<T>(command, args)
}

function loadBrowserMockStatus(): GmailConnectionStatus {
  const params = new URLSearchParams(window.location.search)
  const forcedState = params.get('gmail')
  const stored = loadJson<Partial<GmailConnectionStatus> & {
    suggestions?: GmailThreadSuggestion[]
  }>(GMAIL_MOCK_STORAGE_KEY, {})

  if (forcedState === 'connected' || stored.state === 'connected') {
    return {
      accountEmail: stored.accountEmail ?? 'alex@example.com',
      message: 'Gmail mock is connected for browser QA.',
      scope: GMAIL_SCOPE,
      state: 'connected',
      syncState: 'idle',
    }
  }

  if (forcedState === 'revoked' || stored.state === 'needs_reconnect') {
    return {
      accountEmail: stored.accountEmail ?? 'alex@example.com',
      message: 'Gmail authorization expired. Reconnect Gmail.',
      scope: GMAIL_SCOPE,
      state: 'needs_reconnect',
      syncState: 'error',
    }
  }

  return defaultDisconnectedStatus
}

function loadBrowserMockSuggestions() {
  const stored = loadJson<{
    suggestions?: GmailThreadSuggestion[]
  }>(GMAIL_MOCK_STORAGE_KEY, {})

  return stored.suggestions?.length
    ? stored.suggestions
    : defaultMockSuggestions
}

function filterIgnoredSuggestions(
  suggestions: GmailThreadSuggestion[],
  ignoredThreadIds: string[],
) {
  const ignored = new Set(ignoredThreadIds)

  return suggestions.filter((suggestion) => !ignored.has(suggestion.threadId))
}

export function useGmailConnector(): GmailConnectorController {
  const [status, setStatus] = useState<GmailConnectionStatus>(
    defaultDisconnectedStatus,
  )
  const [suggestions, setSuggestions] = useState<GmailThreadSuggestion[]>([])
  const [activities, setActivities] = useState<GmailActivity[]>(() =>
    loadJson<GmailActivity[]>(GMAIL_ACTIVITY_STORAGE_KEY, []),
  )
  const [ignoredThreadIds, setIgnoredThreadIds] = useState<string[]>(() =>
    loadJson<string[]>(GMAIL_IGNORED_STORAGE_KEY, []),
  )
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const recordActivity = useCallback(
    (action: GmailActivity['action'], detail: string) => {
      setActivities((current) => {
        const next = [
          {
            action,
            at: new Date().toISOString(),
            detail,
            id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
          },
          ...current,
        ].slice(0, 8)

        saveJson(GMAIL_ACTIVITY_STORAGE_KEY, next)
        return next
      })
    },
    [],
  )

  const refreshStatus = useCallback(async () => {
    if (!isTauriRuntime()) {
      setStatus(loadBrowserMockStatus())
      return
    }

    try {
      const next = await invokeNative<GmailConnectionStatus>('gmail_status')

      setStatus(next)
      setError('')
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : String(reason)

      setError(message)
      setStatus({
        accountEmail: null,
        message,
        scope: GMAIL_SCOPE,
        state: 'error',
        syncState: 'error',
      })
      recordActivity('error', message)
    }
  }, [recordActivity])

  const loadSuggestions = useCallback(async () => {
    setIsLoading(true)
    setError('')

    try {
      if (!isTauriRuntime()) {
        const browserStatus = loadBrowserMockStatus()

        setStatus(browserStatus)

        if (browserStatus.state !== 'connected') {
          setSuggestions([])
          return
        }

        const filtered = filterIgnoredSuggestions(
          loadBrowserMockSuggestions(),
          ignoredThreadIds,
        )

        setSuggestions(filtered)
        recordActivity('read', `Read ${filtered.length} mocked unread Gmail threads.`)
        return
      }

      const response = await invokeNative<GmailSuggestionsResponse>(
        'gmail_fetch_unread',
        { limit: 8 },
      )
      const filtered = filterIgnoredSuggestions(
        response.suggestions,
        ignoredThreadIds,
      )

      setStatus({
        accountEmail: response.accountEmail,
        message: `Last sync found ${filtered.length} unread suggestions.`,
        scope: GMAIL_SCOPE,
        state: 'connected',
        syncState: 'idle',
      })
      setSuggestions(filtered)
      recordActivity(
        'read',
        `Read ${response.suggestions.length} unread Gmail threads.`,
      )
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : String(reason)
      const needsReconnect =
        /reconnect|invalid_grant|401|403|authorization/i.test(message)

      setError(message)
      setStatus((current) => ({
        ...current,
        message: needsReconnect
          ? 'Gmail authorization expired. Reconnect Gmail.'
          : message,
        state: needsReconnect ? 'needs_reconnect' : 'error',
        syncState: 'error',
      }))
      recordActivity('error', message)
    } finally {
      setIsLoading(false)
    }
  }, [ignoredThreadIds, recordActivity])

  const connect = useCallback(async () => {
    setIsLoading(true)
    setError('')

    try {
      if (!isTauriRuntime()) {
        const next = {
          accountEmail: 'alex@example.com',
          message: 'Gmail mock is connected for browser QA.',
          scope: GMAIL_SCOPE,
          state: 'connected' as const,
          syncState: 'idle' as const,
        }

        saveJson(GMAIL_MOCK_STORAGE_KEY, next)
        setStatus(next)
        recordActivity('connect', 'Connected Gmail mock for browser QA.')
        return
      }

      const next = await invokeNative<GmailConnectionStatus>('gmail_connect')

      setStatus(next)
      recordActivity(
        'connect',
        `Connected Gmail read-only${next.accountEmail ? ` for ${next.accountEmail}` : ''}.`,
      )
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : String(reason)

      setError(message)
      setStatus((current) => ({
        ...current,
        message,
        state: message.includes('not configured') ? 'unconfigured' : 'error',
        syncState: 'error',
      }))
      recordActivity('error', message)
    } finally {
      setIsLoading(false)
    }
  }, [recordActivity])

  const disconnect = useCallback(async () => {
    setIsLoading(true)
    setError('')

    try {
      if (isTauriRuntime()) {
        await invokeNative<GmailConnectionStatus>('gmail_disconnect')
      } else {
        window.localStorage.removeItem(GMAIL_MOCK_STORAGE_KEY)
      }

      setStatus(defaultDisconnectedStatus)
      setSuggestions([])
      recordActivity('disconnect', 'Disconnected Gmail and removed stored OAuth tokens.')
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : String(reason)

      setError(message)
      recordActivity('error', message)
    } finally {
      setIsLoading(false)
    }
  }, [recordActivity])

  const ignoreSuggestion = useCallback(
    (threadId: string, subject: string) => {
      setIgnoredThreadIds((current) => {
        const next = [...new Set([...current, threadId])]

        saveJson(GMAIL_IGNORED_STORAGE_KEY, next)
        return next
      })
      setSuggestions((current) =>
        current.filter((suggestion) => suggestion.threadId !== threadId),
      )
      recordActivity('ignore', `Ignored Gmail suggestion: ${subject}`)
    },
    [recordActivity],
  )

  useEffect(() => {
    const handle = window.setTimeout(() => {
      void refreshStatus()
    }, 0)

    return () => window.clearTimeout(handle)
  }, [refreshStatus])

  useEffect(() => {
    if (status.state === 'connected') {
      const handle = window.setTimeout(() => {
        void loadSuggestions()
      }, 0)

      return () => window.clearTimeout(handle)
    }

    return undefined
  }, [loadSuggestions, status.state])

  return useMemo(
    () => ({
      activities,
      clearError: () => setError(''),
      connect,
      disconnect,
      error,
      ignoredThreadIds,
      ignoreSuggestion,
      isLoading,
      loadSuggestions,
      recordActivity,
      refreshStatus,
      status,
      suggestions,
    }),
    [
      activities,
      connect,
      disconnect,
      error,
      ignoredThreadIds,
      ignoreSuggestion,
      isLoading,
      loadSuggestions,
      recordActivity,
      refreshStatus,
      status,
      suggestions,
    ],
  )
}
