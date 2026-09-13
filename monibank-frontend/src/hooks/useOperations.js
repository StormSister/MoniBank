import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../lib/api.js'

export function useOperations({
  hours = 24,
  limit = 100,
  core = '',
  status = '',
  operation = '',
} = {}) {
  const params = new URLSearchParams({
    hours: String(hours),
    limit: String(limit),
  })

  if (core) params.set('core', core)
  if (status) params.set('status', status)
  if (operation.trim()) params.set('operation', operation.trim())

  return useQuery({
    queryKey: ['operations', hours, limit, core, status, operation],
    queryFn: ({ signal }) => apiGet(`/api/operations?${params}`, { signal }),
    refetchInterval: 15_000,
  })
}

export function useOperationSummary({ hours = 24, core = '' } = {}) {
  const params = new URLSearchParams({ hours: String(hours) })
  if (core) params.set('core', core)

  return useQuery({
    queryKey: ['operations', 'summary', hours, core],
    queryFn: ({ signal }) => apiGet(`/api/operations/summary?${params}`, { signal }),
    refetchInterval: 15_000,
  })
}