import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../lib/api.js'

export function useTransactions(accountId = '') {
  return useQuery({
    queryKey: ['transactions', accountId || 'all'],
    queryFn: ({ signal }) => apiGet(
      accountId
        ? `/api/transactions/accounts/${encodeURIComponent(accountId)}`
        : '/api/transactions',
      { signal },
    ),
  })
}

export function useDeposit() {
  return useCashTransaction('deposits')
}

export function useWithdrawal() {
  return useCashTransaction('withdrawals')
}

function useCashTransaction(operation) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request) => apiPost(`/api/transactions/${operation}`, request),
    onSuccess: (transaction) => {
      queryClient.setQueryData(['accounts'], (current = []) =>
        current.map((account) => account.accountId === transaction.accountId
          ? { ...account, balance: transaction.balanceAfter }
          : account),
      )

      const prependTransaction = (current) => {
        if (!Array.isArray(current)) return current
        return [
          transaction,
          ...current.filter((item) => item.transactionId !== transaction.transactionId),
        ]
      }

      queryClient.setQueryData(['transactions', 'all'], prependTransaction)
      queryClient.setQueryData(['transactions', transaction.accountId], prependTransaction)
      queryClient.setQueriesData({ queryKey: ['transactions', 'recent'] }, (current) => {
        const updated = prependTransaction(current)
        return Array.isArray(current) ? updated.slice(0, current.length || 5) : updated
      })

      queryClient.invalidateQueries({ queryKey: ['transactions'], refetchType: 'active' })
    },
  })
}
