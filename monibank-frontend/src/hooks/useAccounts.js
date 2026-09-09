import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPatch, apiPost } from '../lib/api.js'

const ACCOUNTS_QUERY_KEY = ['accounts']

export function useAccounts() {
  return useQuery({
    queryKey: ACCOUNTS_QUERY_KEY,
    queryFn: ({ signal }) => apiGet('/api/accounts', { signal }),
  })
}

export function useCreateAccount() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (account) => apiPost('/api/accounts', account),
    onSuccess: (created) => {
      queryClient.setQueryData(ACCOUNTS_QUERY_KEY, (current = []) => [
        created,
        ...current.filter((account) => account.accountId !== created.accountId),
      ])
    },
  })
}

export function useChangeAccountStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ accountId, status }) => apiPatch(`/api/accounts/${accountId}/status`, { status }),
    onSuccess: (updated) => {
      queryClient.setQueryData(ACCOUNTS_QUERY_KEY, (current = []) =>
        current.map((account) => account.accountId === updated.accountId ? updated : account),
      )
    },
  })
}
