import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../lib/api.js'

function previousDay() {
  const date = new Date()
  date.setDate(date.getDate() - 1)
  return date.toISOString().slice(0, 10)
}

export function useDailyCloseReport(currency = 'EUR', date, options = {}) {
  const businessDate = date || import.meta.env.VITE_DAILY_CLOSE_DATE || previousDay()

  return useQuery({
    queryKey: ['daily-close', businessDate, currency],
    queryFn: ({ signal }) =>
      apiGet(
        `/api/dashboard/daily-close?date=${businessDate}&currency=${currency}`,
        { signal },
      ),
    ...options,
  })
}
