import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import CustomersPage from './pages/CustomersPage.jsx'
import AccountsPage from './pages/AccountsPage.jsx'
import CardsPage from './pages/CardsPage.jsx'
import CashDeskPage from './pages/CashDeskPage.jsx'
import StatementsPage from './pages/StatementsPage.jsx'
import PlaceholderPage from './pages/PlaceholderPage.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="customers" element={<CustomersPage />} />
        <Route path="accounts" element={<AccountsPage />} />
        <Route path="cards" element={<CardsPage />} />
        <Route path="cash-desk" element={<CashDeskPage />} />
        <Route path="statements" element={<StatementsPage />} />
        <Route path="jobs" element={<PlaceholderPage title="Jobs" />} />
        <Route path="audit-log" element={<PlaceholderPage title="Audit Log" />} />
        <Route path="reports" element={<PlaceholderPage title="Reports" />} />
        <Route path="settings" element={<PlaceholderPage title="Settings" />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
