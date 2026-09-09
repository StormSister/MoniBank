import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPatch, apiPost } from '../lib/api.js'

const CARDS_QUERY_KEY = ['cards']

export function useCards() {
  return useQuery({
    queryKey: CARDS_QUERY_KEY,
    queryFn: ({ signal }) => apiGet('/api/cards', { signal }),
  })
}

export function useCreateCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (card) => apiPost('/api/cards', card),
    onSuccess: (created) => {
      queryClient.setQueryData(CARDS_QUERY_KEY, (current = []) => [
        created,
        ...current.filter((card) => card.cardId !== created.cardId),
      ])
    },
  })
}

export function useChangeCardStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ cardId, status }) => apiPatch(`/api/cards/${cardId}/status`, { status }),
    onSuccess: (updated) => {
      queryClient.setQueryData(CARDS_QUERY_KEY, (current = []) =>
        current.map((card) => card.cardId === updated.cardId ? updated : card),
      )
    },
  })
}
