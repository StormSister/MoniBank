import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPatch, apiPost } from '../lib/api.js'

const CUSTOMERS_QUERY_KEY = ['customers']

export function useCustomers() {
  return useQuery({
    queryKey: CUSTOMERS_QUERY_KEY,
    queryFn: ({ signal }) => apiGet('/api/customers', { signal }),
  })
}

export function useCreateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (customer) => apiPost('/api/customers', customer),
    onSuccess: (createdCustomer) => {
      queryClient.setQueryData(CUSTOMERS_QUERY_KEY, (current = []) => [
        createdCustomer,
        ...current.filter((customer) => customer.customerId !== createdCustomer.customerId),
      ])
    },
  })
}

export function useChangeCustomerStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ customerId, status }) =>
      apiPatch(`/api/customers/${customerId}/status`, { status }),
    onSuccess: (updatedCustomer) => {
      queryClient.setQueryData(CUSTOMERS_QUERY_KEY, (current = []) =>
        current.map((customer) =>
          customer.customerId === updatedCustomer.customerId ? updatedCustomer : customer,
        ),
      )
    },
  })
}
