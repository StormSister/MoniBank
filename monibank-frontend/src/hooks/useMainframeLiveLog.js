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
    const receive = (sourceName) => (event) => {
      const nextLine = {
        id: event.lastEventId || `${sourceName}-${Date.now()}-${Math.random()}`,
        source: sourceName,
        text: event.data,
      }

      setLines((current) => [...current, nextLine].slice(-MAX_LOG_LINES))
    }

    const receiveJes = receive('jes')
    const receiveKicks = receive('kicks')
    const receiveLegacyJes = receive('jes')

    source.onmessage = receiveLegacyJes
    source.addEventListener('jes', receiveJes)
    source.addEventListener('kicks', receiveKicks)

    return () => {
      source.removeEventListener('jes', receiveJes)
      source.removeEventListener('kicks', receiveKicks)
      source.onmessage = null
      source.close()
    }
  }, [])

  return {
    lines,
    connectionState,
    clear: () => setLines([]),
  }
}
