import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../lib/api.js'

export function useMainframeStatus() {
  return useQuery({
    queryKey: ['mainframe-status'],
    queryFn: ({ signal }) => apiGet('/api/mainframe/status', { signal }),
    refetchInterval: 15_000,
    staleTime: 10_000,
  })
}
