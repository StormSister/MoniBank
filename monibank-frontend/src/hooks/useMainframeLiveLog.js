import { useEffect, useState } from 'react'
import { apiUrl } from '../lib/api.js'

export const MAX_LOG_LINES = 500

export function useMainframeLiveLog() {
  const [lines, setLines] = useState([])
  const [connectionState, setConnectionState] = useState('connecting')

  useEffect(() => {
    const source = new EventSource(
      apiUrl('/api/mainframe/logs/stream'),
      { withCredentials: true },
    )

    source.onopen = () => setConnectionState('connected')
    source.onerror = () => setConnectionState('reconnecting')
    source.onmessage = (event) => {
      setLines((current) => [...current, event.data].slice(-MAX_LOG_LINES))
    }

    return () => source.close()
  }, [])

  return {
    lines,
    connectionState,
    clear: () => setLines([]),
  }
}
