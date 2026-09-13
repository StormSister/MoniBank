import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import CustomersPage from './pages/CustomersPage.jsx'
import AccountsPage from './pages/AccountsPage.jsx'
import CardsPage from './pages/CardsPage.jsx'
import CashDeskPage from './pages/CashDeskPage.jsx'
import StatementsPage from './pages/StatementsPage.jsx'
import ReportsPage from './pages/ReportsPage.jsx'
import OperationsPage from './pages/OperationsPage.jsx'
import SystemStatusPage from './pages/SystemStatusPage.jsx'

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
        <Route path="operations" element={<OperationsPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="system-status" element={<SystemStatusPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  )
}
