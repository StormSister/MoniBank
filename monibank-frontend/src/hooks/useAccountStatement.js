import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../lib/api.js'

export function useAccountStatement(request) {
  const accountId = request?.accountId || ''
  const from = request?.from || ''
  const to = request?.to || ''

  return useQuery({
    queryKey: ['statement', accountId, from, to],
    queryFn: ({ signal }) => apiGet(
      `/api/statements/accounts/${encodeURIComponent(accountId)}`
        + `?from=${encodeURIComponent(from)}`
        + `&to=${encodeURIComponent(to)}`,
      { signal },
    ),
    enabled: Boolean(accountId && from && to),
    retry: false,
  })
}
