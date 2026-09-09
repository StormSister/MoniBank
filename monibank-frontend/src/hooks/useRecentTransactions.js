import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../lib/api.js'

export function useRecentTransactions(limit = 5) {
  return useQuery({
    queryKey: ['transactions', 'recent', limit],
    queryFn: ({ signal }) => apiGet(`/api/transactions/recent?limit=${limit}`, { signal }),
  })
}
