import { ArrowDownToLine, ArrowUpFromLine, CircleDollarSign, Users } from 'lucide-react'
import { useDailyCloseReport } from '../hooks/useDailyCloseReport.js'
import { useRecentTransactions } from '../hooks/useRecentTransactions.js'
import { previewReport } from '../data/dashboardPreview.js'
import StatCard from '../components/dashboard/StatCard.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import Button from '../components/ui/Button.jsx'

const money = new Intl.NumberFormat('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export default function DashboardPage() {
  const reportQuery = useDailyCloseReport('EUR')
  const transactionsQuery = useRecentTransactions(5)
  const report = reportQuery.data || previewReport
  const preview = reportQuery.isError

  return (
    <div className="mx-auto max-w-[1120px] space-y-3.5">
      {preview && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-mb-gold/25 bg-mb-gold/7 px-4 py-3 text-sm text-mb-gold-light">
          <span>Backend is unavailable. The dashboard is showing preview data.</span>
          <Button className="min-h-8 py-1" onClick={() => reportQuery.refetch()}>Try again</Button>
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Closed-day operations" value={report.transactions.operationCount} change={`${report.transactions.interestCount} interest posting`} icon={CircleDollarSign} tone="teal" loading={reportQuery.isLoading} />
        <StatCard label="Cash deposits" value={money.format(report.transactions.depositAmount)} suffix={report.currency} change={`${report.transactions.depositCount} completed`} icon={ArrowDownToLine} tone="green" loading={reportQuery.isLoading} />
        <StatCard label="Cash withdrawals" value={money.format(report.transactions.withdrawalAmount)} suffix={report.currency} change={`${report.transactions.withdrawalCount} completed`} icon={ArrowUpFromLine} tone="blue" loading={reportQuery.isLoading} />
        <StatCard label="Active customers" value={report.customers.activeCount} change={`${report.customers.newCount} new on ${report.businessDate}`} icon={Users} tone="gold" loading={reportQuery.isLoading} />
      </div>

      <Panel title="Recent Transactions" action={<button className="text-xs text-mb-muted hover:text-mb-gold-light">View all</button>}>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-xs">
            <thead className="bg-white/[0.025] text-[10px] uppercase tracking-wide text-mb-muted"><tr>{['Date & time', 'Type', 'Account', 'Detail', 'Amount', 'Balance after', 'Status'].map((heading) => <th key={heading} className="px-4 py-3 font-medium">{heading}</th>)}</tr></thead>
            <tbody>
              {transactionsQuery.isLoading && <LoadingRows />}
              {transactionsQuery.isError && (
                <tr><td colSpan="7" className="px-4 py-8 text-center text-mb-danger">Could not load transactions from the mainframe.</td></tr>
              )}
              {transactionsQuery.isSuccess && transactionsQuery.data.length === 0 && (
                <tr><td colSpan="7" className="px-4 py-8 text-center text-mb-muted">No transactions found.</td></tr>
              )}
              {transactionsQuery.data?.map((transaction) => {
                const debit = transaction.direction === 'D'
                return (
                  <tr key={transaction.transactionId} className="border-t border-mb-border/80 hover:bg-white/[0.02]">
                    <td className="whitespace-nowrap px-4 py-3 text-mb-muted">{formatTransactionDateTime(transaction.createdAt)}</td>
                    <td className={`px-4 py-3 font-medium ${debit ? 'text-mb-danger' : 'text-mb-terminal'}`}>{transactionType(transaction.type)}</td>
                    <td className="px-4 py-3 font-mono text-[11px] text-mb-muted">{transaction.accountId}</td>
                    <td className="max-w-52 truncate px-4 py-3" title={transaction.detail}>{transaction.detail}</td>
                    <td className={`px-4 py-3 font-medium ${debit ? 'text-mb-danger' : 'text-mb-terminal'}`}>{debit ? '-' : '+'}{money.format(transaction.amount)} {transaction.currency}</td>
                    <td className="px-4 py-3">{money.format(transaction.balanceAfter)} {transaction.currency}</td>
                    <td className="px-4 py-3"><StatusBadge variant={transaction.status === 'C' ? 'success' : 'warning'}>{transactionStatus(transaction.status)}</StatusBadge></td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </Panel>

      <div className="grid gap-3 lg:grid-cols-[1fr_1.15fr_.9fr]">
        <Panel title="System Status" className="min-h-58">
          <div className="space-y-4 p-4 text-xs">
            <div className="flex justify-between"><div><p className="text-mb-muted">Core System</p><p className="mt-1">MVS 3.8j / Hercules</p></div><StatusBadge>ONLINE</StatusBadge></div>
            <Metric label="CPU" value="2%" width="22%" />
            <Metric label="Memory" value="64 MB" width="48%" />
            <div><p className="text-mb-muted">Uptime</p><p className="mt-1">2d 14h 32m</p></div>
          </div>
        </Panel>
        <Panel title="Jobs Overview" action={<button className="text-xs text-mb-muted">View all</button>}>
          <div className="flex items-center justify-around gap-5 p-5">
            <div className="donut grid size-28 place-items-center rounded-full"><div className="grid size-17 place-items-center rounded-full bg-mb-panel text-center"><span><b className="block text-xl">16</b><small className="text-mb-muted">Total Jobs</small></span></div></div>
            <div className="space-y-3 text-xs"><JobLegend color="bg-mb-terminal" label="Completed" value="12" /><JobLegend color="bg-mb-gold" label="Running" value="2" /><JobLegend color="bg-blue-400" label="Queued" value="1" /><JobLegend color="bg-mb-danger" label="Failed" value="1" /></div>
          </div>
        </Panel>
        <Panel title="Last Job" action={<StatusBadge>{report.state}</StatusBadge>}>
          <div className="p-4"><div className="text-2xl font-semibold">{report.requestId || '—'}</div><div className="mt-1 text-xs text-mb-muted">DAYSTAT</div><div className="mt-5 font-mono text-sm text-mb-terminal">RC={report.resultCode === 'OK' ? '0000' : report.resultCode}</div><Button className="mt-7 w-full" variant="secondary">View Job Details</Button></div>
        </Panel>
      </div>

      <Panel title="Quick Actions">
        <div className="grid gap-2 p-4 sm:grid-cols-2 lg:grid-cols-6">
          {['New Customer', 'Open Account', 'Deposit', 'Withdraw', 'Issue Card', 'Statement'].map((label) => <Button key={label} className="min-h-18 flex-col">{label}</Button>)}
        </div>
      </Panel>
    </div>
  )
}

function Metric({ label, value, width }) {
  return <div><div className="flex justify-between"><span className="text-mb-muted">{label}</span><span>{value}</span></div><div className="mt-2 h-1.5 rounded bg-white/6"><div className="h-full rounded bg-mb-teal" style={{ width }} /></div></div>
}

function JobLegend({ color, label, value }) {
  return <div className="flex items-center gap-2"><span className={`size-2 rounded-full ${color}`} /><span className="w-18 text-mb-muted">{label}</span><b>{value}</b></div>
}

function LoadingRows() {
  return Array.from({ length: 4 }, (_, index) => (
    <tr key={index} className="border-t border-mb-border/80">
      <td colSpan="7" className="px-4 py-3"><div className="h-4 animate-pulse rounded bg-white/5" /></td>
    </tr>
  ))
}

function transactionType(type) {
  return ({ DP: 'Deposit', WD: 'Withdrawal', IN: 'Interest' })[type] || type
}

function transactionStatus(status) {
  return ({ C: 'Completed', P: 'Pending', F: 'Failed' })[status] || status
}

function formatTransactionDateTime(value) {
  if (!value || value.length !== 14) return '—'
  return `${value.slice(6, 8)}.${value.slice(4, 6)}.${value.slice(0, 4)} ${value.slice(8, 10)}:${value.slice(10, 12)}`
}
