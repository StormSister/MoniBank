import {
  Activity,
  ArrowDownToLine,
  ArrowUpFromLine,
  Clock3,
  CircleDollarSign,
  CreditCard,
  FileText,
  Landmark,
  Server,
  SquareTerminal,
  UserRoundPlus,
  Users,
} from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useDailyCloseReport } from '../hooks/useDailyCloseReport.js'
import { useMainframeStatus } from '../hooks/useMainframeStatus.js'
import { useOperationSummary } from '../hooks/useOperations.js'
import { useRecentTransactions } from '../hooks/useRecentTransactions.js'
import StatCard from '../components/dashboard/StatCard.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import Button from '../components/ui/Button.jsx'
import DocumentationLink from '../components/ui/DocumentationLink.jsx'

const money = new Intl.NumberFormat('en-GB', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export default function DashboardPage() {
  const navigate = useNavigate()
  const statusQuery = useMainframeStatus()
  const operationSummaryQuery = useOperationSummary({ hours: 24 })
  const reportQuery = useDailyCloseReport('EUR')
  const transactionsQuery = useRecentTransactions(5)
  const report = reportQuery.data
  const reportUnavailable = reportQuery.isError
  const reportHint = reportUnavailable
    ? 'Previous-day report unavailable'
    : 'Loading previous-day close'

  return (
    <div className="w-full space-y-4">
      <SystemOverview
        mainframe={statusQuery.data}
        loading={statusQuery.isLoading}
        error={statusQuery.isError}
        operationSummary={operationSummaryQuery.data}
        operationLoading={operationSummaryQuery.isLoading}
        onOpen={() => navigate('/system-status')}
        onOpenOperations={() => navigate('/operations')}
      />

      {reportUnavailable && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-mb-gold/25 bg-mb-gold/7 px-4 py-3 text-sm text-mb-gold-light">
          <span>{reportErrorMessage(reportQuery.error)}</span>
          <Button className="min-h-8 py-1" onClick={() => reportQuery.refetch()}>Try again</Button>
        </div>
      )}

      <section className="overflow-hidden rounded-xl border border-mb-gold/20 bg-[linear-gradient(120deg,rgba(215,162,59,.08),rgba(16,36,49,.7)_45%,rgba(45,212,191,.05))]">
        <div className="flex flex-col items-start gap-3 border-b border-mb-border px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-mb-text">Previous-day close summary</p>
            <p className="mt-1 text-xs text-mb-muted">
              Figures produced by MVS end-of-day processing
              <span className="hidden sm:inline"><span className="mx-2 text-mb-muted/40">·</span>Business date: <span className="font-mono text-mb-gold-light">{formatReportDate(report?.businessDate)}</span></span>
            </p>
            <p className="mt-1 text-xs text-mb-muted sm:hidden">Business date: <span className="font-mono text-mb-gold-light">{formatReportDate(report?.businessDate)}</span></p>
          </div>
          <div className="flex items-center gap-2">
            <DocumentationLink page="previousDayClose" section="production" />
            <StatusBadge variant={report?.state === 'CLOSED' ? 'success' : 'warning'}>
              {report?.state || (reportQuery.isLoading ? 'LOADING' : 'UNAVAILABLE')}
            </StatusBadge>
          </div>
        </div>

        <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,210px),1fr))] gap-3 p-3">
          <StatCard label="Total closed-day operations" value={report?.transactions.operationCount ?? '—'} change={report ? `${report.transactions.interestCount} interest posting${report.transactions.interestCount === 1 ? '' : 's'} · +${money.format(report.transactions.interestAmount)} ${report.currency}` : reportHint} icon={CircleDollarSign} tone="teal" loading={reportQuery.isLoading} />
          <StatCard label="Cash deposits during the day" value={report ? money.format(report.transactions.depositAmount) : '—'} suffix={report?.currency} change={report ? `${report.transactions.depositCount} completed` : reportHint} icon={ArrowDownToLine} tone="green" loading={reportQuery.isLoading} />
          <StatCard label="Cash withdrawals during the day" value={report ? money.format(report.transactions.withdrawalAmount) : '—'} suffix={report?.currency} change={report ? `${report.transactions.withdrawalCount} completed` : reportHint} icon={ArrowUpFromLine} tone="blue" loading={reportQuery.isLoading} />
          <StatCard label="Active customers at day close" value={report?.customers.activeCount ?? '—'} change={report ? `${report.customers.newCount} new customer${report.customers.newCount === 1 ? '' : 's'} during the day` : reportHint} icon={Users} tone="gold" loading={reportQuery.isLoading} />
        </div>
      </section>

      <Panel
        title="Recent Transactions · Live"
        action={(
          <div className="flex items-center gap-2">
            <DocumentationLink page="recentTransactions" section="refresh" />
            <button type="button" onClick={() => navigate('/statements')} className="text-xs text-mb-muted hover:text-mb-gold-light">Open statements</button>
          </div>
        )}
      >
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

      <Panel
        title="Quick Actions"
        action={<DocumentationLink page="quickActions" section="routes">About these shortcuts</DocumentationLink>}
      >
        <div className="grid grid-cols-2 gap-2.5 p-3 sm:grid-cols-3 sm:p-4 xl:grid-cols-6">
          {QUICK_ACTIONS.map((action) => (
            <QuickAction
              key={action.label}
              {...action}
              onClick={() => navigate(action.to)}
            />
          ))}
        </div>
      </Panel>
    </div>
  )
}

function SystemOverview({
  mainframe,
  loading,
  error,
  operationSummary,
  operationLoading,
  onOpen,
  onOpenOperations,
}) {
  const connections = mainframe?.connections
  const status = mainframe?.status || (loading ? 'CHECKING' : 'UNAVAILABLE')
  const healthy = status === 'ONLINE'

  return (
    <section className="overflow-hidden rounded-xl border border-mb-border bg-[linear-gradient(110deg,rgba(16,36,49,.94),rgba(9,27,39,.94)_60%,rgba(45,212,191,.055))]">
      <div className="flex flex-col gap-3 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex min-w-0 items-center gap-3">
          <span className={`grid size-10 shrink-0 place-items-center rounded-xl border ${healthy ? 'border-mb-terminal/25 bg-mb-terminal/10 text-mb-terminal' : 'border-mb-gold/25 bg-mb-gold/10 text-mb-gold-light'}`}>
            <Server size={20} />
          </span>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="font-semibold text-mb-text">
                {mainframe?.system || 'MVS 3.8j'} · {mainframe?.systemId || 'TK5R'}
              </p>
              <StatusBadge variant={healthy ? 'success' : error ? 'danger' : 'warning'}>{status}</StatusBadge>
            </div>
            <p className="mt-1 text-xs text-mb-muted">
              Legacy core system overview · detailed infrastructure health is available in System Status.
            </p>
            <DocumentationLink
              page="systemOverview"
              section="refresh"
              className="mt-2"
            />
          </div>
        </div>

        <div className="flex w-full flex-wrap items-center justify-between gap-4 sm:w-auto sm:justify-end">
          <OperationHealthDonut
            summary={operationSummary}
            loading={operationLoading}
            onOpen={onOpenOperations}
          />
          <button
            type="button"
            onClick={onOpen}
            className="shrink-0 text-left text-xs font-medium text-mb-muted transition hover:text-mb-gold-light sm:text-right"
          >
            Open System Status →
          </button>
        </div>
      </div>

      <dl className="grid gap-px border-t border-mb-border bg-mb-border sm:grid-cols-2 xl:grid-cols-4">
        <SystemMetric
          icon={SquareTerminal}
          label="Terminal pool"
          value={`${connections?.readyTerminals ?? 0} / ${connections?.configuredTerminals ?? 0} ready`}
        />
        <SystemMetric
          icon={Activity}
          label="Request queue"
          value={`${connections?.queuedRequests ?? 0} waiting`}
        />
        <SystemMetric
          icon={Server}
          label="Interfaces"
          value={`${shortState(connections?.reader)} · ${shortState(connections?.resultPrinter)}`}
        />
        <SystemMetric
          icon={Clock3}
          label="Last health check"
          value={formatHealthCheck(mainframe?.checkedAt)}
        />
      </dl>
    </section>
  )
}

function OperationHealthDonut({ summary, loading, onOpen }) {
  const success = summary?.successCount ?? 0
  const businessErrors = summary?.businessErrorCount ?? 0
  const technicalErrors = summary?.technicalErrorCount ?? 0
  const failed = businessErrors + technicalErrors
  const total = summary?.totalCount ?? success + failed
  const successPercent = total > 0 ? (success / total) * 100 : 0
  const businessPercent = total > 0 ? (businessErrors / total) * 100 : 0
  const businessEnd = successPercent + businessPercent
  const ring = total > 0
    ? `conic-gradient(#55e36a 0 ${successPercent}%, #d7a23b ${successPercent}% ${businessEnd}%, #ff5b57 ${businessEnd}% 100%)`
    : 'conic-gradient(rgba(141,161,175,.2) 0 100%)'

  return (
    <button
      type="button"
      onClick={onOpen}
      className="group flex items-center gap-3 rounded-xl border border-mb-border/80 bg-black/10 px-3 py-2 text-left outline-none transition hover:border-mb-teal/35 hover:bg-mb-teal/[0.04] focus-visible:ring-2 focus-visible:ring-mb-gold/60"
      aria-label={total > 0
        ? `Operations in the last 24 hours: ${success} successful and ${failed} failed. Open operations.`
        : 'No operations recorded in the last 24 hours. Open operations.'}
    >
      <span
        role="img"
        aria-hidden="true"
        className="relative grid size-16 shrink-0 place-items-center rounded-full shadow-[0_0_22px_rgba(85,227,106,.08)]"
        style={{ background: ring }}
      >
        <span className="grid size-12 place-items-center rounded-full border border-white/[0.04] bg-mb-panel">
          <span className="text-center">
            <span className="block font-mono text-sm font-semibold leading-none text-mb-text">
              {loading ? '…' : total}
            </span>
            <span className="mt-1 block text-[8px] uppercase tracking-wider text-mb-muted">24h</span>
          </span>
        </span>
      </span>

      <span className="min-w-28">
        <span className="block text-[10px] font-medium uppercase tracking-wide text-mb-muted">Operations</span>
        <span className="mt-1.5 flex items-center gap-1.5 text-xs">
          <span className="size-1.5 rounded-full bg-mb-terminal" />
          <span className="text-mb-terminal">{success} successful</span>
        </span>
        <span className="mt-1 flex items-center gap-1.5 text-xs">
          <span className="size-1.5 rounded-full bg-mb-danger" />
          <span className={failed > 0 ? 'text-mb-danger' : 'text-mb-muted'}>{failed} failed</span>
        </span>
        <span className="mt-1.5 block text-[9px] text-mb-muted transition group-hover:text-mb-gold-light">Open details →</span>
      </span>
    </button>
  )
}

function SystemMetric({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center gap-3 bg-mb-panel/95 px-4 py-3">
      <Icon size={17} className="shrink-0 text-mb-teal" />
      <div className="min-w-0">
        <dt className="text-[10px] uppercase tracking-wide text-mb-muted">{label}</dt>
        <dd className="mt-0.5 truncate text-xs font-medium text-mb-text" title={value}>{value}</dd>
      </div>
    </div>
  )
}

function shortState(value) {
  if (value === 'CONNECTED') return 'Connected'
  if (!value) return 'Checking'
  return value.charAt(0) + value.slice(1).toLowerCase()
}

function formatHealthCheck(value) {
  if (!value) return 'Waiting for data'
  return new Date(value).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

const QUICK_ACTIONS = [
  { label: 'New Customer', to: '/customers?action=new', icon: UserRoundPlus, tone: 'teal' },
  { label: 'Open Account', to: '/accounts?action=new', icon: Landmark, tone: 'indigo' },
  { label: 'Deposit', to: '/cash-desk?operation=deposit', icon: ArrowDownToLine, tone: 'green' },
  { label: 'Withdraw', to: '/cash-desk?operation=withdrawal', icon: ArrowUpFromLine, tone: 'red' },
  { label: 'Issue Card', to: '/cards?action=new', icon: CreditCard, tone: 'blue' },
  { label: 'Statement', to: '/statements', icon: FileText, tone: 'violet' },
]

const QUICK_ACTION_TONES = {
  teal: {
    icon: 'text-[#55d7d0]',
    iconSurface: 'border-[#55d7d0]/20 bg-[#55d7d0]/10 shadow-[0_0_24px_rgba(85,215,208,.08)]',
    hover: 'hover:border-[#55d7d0]/35 hover:shadow-[0_12px_32px_rgba(85,215,208,.08)]',
  },
  indigo: {
    icon: 'text-[#719cff]',
    iconSurface: 'border-[#719cff]/20 bg-[#719cff]/10 shadow-[0_0_24px_rgba(113,156,255,.08)]',
    hover: 'hover:border-[#719cff]/35 hover:shadow-[0_12px_32px_rgba(113,156,255,.08)]',
  },
  green: {
    icon: 'text-mb-terminal',
    iconSurface: 'border-mb-terminal/20 bg-mb-terminal/10 shadow-[0_0_24px_rgba(85,227,106,.08)]',
    hover: 'hover:border-mb-terminal/35 hover:shadow-[0_12px_32px_rgba(85,227,106,.08)]',
  },
  red: {
    icon: 'text-mb-danger',
    iconSurface: 'border-mb-danger/20 bg-mb-danger/10 shadow-[0_0_24px_rgba(255,91,87,.08)]',
    hover: 'hover:border-mb-danger/35 hover:shadow-[0_12px_32px_rgba(255,91,87,.08)]',
  },
  blue: {
    icon: 'text-[#61a0ff]',
    iconSurface: 'border-[#61a0ff]/20 bg-[#61a0ff]/10 shadow-[0_0_24px_rgba(97,160,255,.08)]',
    hover: 'hover:border-[#61a0ff]/35 hover:shadow-[0_12px_32px_rgba(97,160,255,.08)]',
  },
  violet: {
    icon: 'text-[#9a8cff]',
    iconSurface: 'border-[#9a8cff]/20 bg-[#9a8cff]/10 shadow-[0_0_24px_rgba(154,140,255,.08)]',
    hover: 'hover:border-[#9a8cff]/35 hover:shadow-[0_12px_32px_rgba(154,140,255,.08)]',
  },
}

function QuickAction({ label, icon: Icon, tone, onClick }) {
  const colors = QUICK_ACTION_TONES[tone]

  return (
    <button
      type="button"
      onClick={onClick}
      className={`group relative min-h-28 overflow-hidden rounded-xl border border-mb-border bg-[linear-gradient(145deg,rgba(20,43,57,.78),rgba(10,27,38,.96))] px-3 py-4 text-center outline-none transition duration-200 hover:-translate-y-0.5 focus-visible:ring-2 focus-visible:ring-mb-gold/60 ${colors.hover}`}
    >
      <span className="pointer-events-none absolute inset-x-5 top-0 h-px bg-gradient-to-r from-transparent via-white/15 to-transparent opacity-0 transition group-hover:opacity-100" />
      <span className={`mx-auto grid size-11 place-items-center rounded-xl border transition duration-200 group-hover:scale-105 ${colors.iconSurface} ${colors.icon}`}>
        <Icon size={25} strokeWidth={2} />
      </span>
      <span className="mt-3 block text-sm font-medium text-mb-text transition group-hover:text-white">{label}</span>
    </button>
  )
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

function formatReportDate(value) {
  if (!value) return '—'
  const [year, month, day] = value.split('-')
  return year && month && day ? `${day}.${month}.${year}` : value
}

function reportErrorMessage(error) {
  if (error?.status === 404) {
    return 'The previous-day close report has not been produced yet. Live transactions remain available below.'
  }
  return 'The previous-day close report could not be loaded from the mainframe. Live transactions remain available below.'
}

function getGreeting() {
  const hour = new Date().getHours()

  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}
