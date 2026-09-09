import { useMemo, useState } from 'react'
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  CalendarDays,
  CircleDollarSign,
  Coins,
  RefreshCw,
  Users,
} from 'lucide-react'
import { useDailyCloseReport } from '../hooks/useDailyCloseReport.js'
import StatCard from '../components/dashboard/StatCard.jsx'
import Button from '../components/ui/Button.jsx'
import FormField, { Input, Select } from '../components/ui/FormField.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'

const money = new Intl.NumberFormat('en-GB', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export default function ReportsPage() {
  const initialDate = reportDate()
  const [formDate, setFormDate] = useState(initialDate)
  const [formCurrency, setFormCurrency] = useState('EUR')
  const [businessDate, setBusinessDate] = useState(initialDate)
  const [currency, setCurrency] = useState('EUR')

  const reportQuery = useDailyCloseReport(currency, businessDate)
  const comparisonDate = useMemo(() => shiftDate(businessDate, -1), [businessDate])
  const comparisonQuery = useDailyCloseReport(currency, comparisonDate, {
    enabled: reportQuery.isSuccess,
    retry: false,
  })

  const report = reportQuery.data
  const comparison = comparisonQuery.data

  const loadReport = (event) => {
    event.preventDefault()
    if (formDate === businessDate && formCurrency === currency) {
      reportQuery.refetch()
      return
    }
    setBusinessDate(formDate)
    setCurrency(formCurrency)
  }

  return (
    <div className="mx-auto max-w-[1220px] space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">End-of-day reporting</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight text-mb-text">Reports</h1>
          <p className="mt-1 text-sm text-mb-muted">Closed-day figures calculated by the MVS core system.</p>
        </div>
        {report && <StatusBadge variant={report.state === 'CLOSED' ? 'success' : 'warning'}>{report.state}</StatusBadge>}
      </header>

      <Panel>
        <form onSubmit={loadReport} className="grid items-end gap-4 p-4 sm:grid-cols-[minmax(220px,1fr)_160px_auto]">
          <FormField id="report-date" label="Business date" hint="Reports are produced after the close of business.">
            <Input
              id="report-date"
              type="date"
              value={formDate}
              max={reportDate()}
              required
              onChange={(event) => setFormDate(event.target.value)}
            />
          </FormField>
          <FormField id="report-currency" label="Currency">
            <Select id="report-currency" value={formCurrency} onChange={(event) => setFormCurrency(event.target.value)}>
              <option value="EUR">EUR</option>
            </Select>
          </FormField>
          <Button type="submit" variant="primary" disabled={reportQuery.isFetching || !formDate}>
            <CalendarDays size={17} /> {reportQuery.isFetching ? 'Loading…' : 'Load report'}
          </Button>
        </form>
      </Panel>

      {reportQuery.isError && (
        <Notice>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span>No closed-day report is available for {formatDate(businessDate)}. {reportQuery.error?.message}</span>
            <Button variant="danger" onClick={() => reportQuery.refetch()} disabled={reportQuery.isFetching}>
              <RefreshCw size={15} className={reportQuery.isFetching ? 'animate-spin' : ''} /> Retry
            </Button>
          </div>
        </Notice>
      )}

      {reportQuery.isLoading && <ReportSkeleton />}

      {report && (
        <>
          <CloseSummary report={report} comparison={comparison} comparisonLoading={comparisonQuery.isLoading} comparisonDate={comparisonDate} />

          <div className="grid gap-4 xl:grid-cols-[1.35fr_.85fr]">
            <TransactionBreakdown report={report} />
            <CustomerSnapshot report={report} />
          </div>

          <ReportMetadata report={report} comparisonDate={comparisonDate} comparisonAvailable={Boolean(comparison)} />
        </>
      )}
    </div>
  )
}

function CloseSummary({ report, comparison, comparisonLoading, comparisonDate }) {
  const change = (current, previous) => comparisonText(current, previous, comparison, comparisonLoading, comparisonDate)

  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        label="Closed-day operations"
        value={report.transactions.operationCount}
        change={change(report.transactions.operationCount, comparison?.transactions.operationCount)}
        icon={CircleDollarSign}
        tone="teal"
      />
      <StatCard
        label="Cash deposits"
        value={money.format(report.transactions.depositAmount)}
        suffix={report.currency}
        change={change(report.transactions.depositAmount, comparison?.transactions.depositAmount)}
        icon={ArrowDownToLine}
        tone="green"
      />
      <StatCard
        label="Cash withdrawals"
        value={money.format(report.transactions.withdrawalAmount)}
        suffix={report.currency}
        change={change(report.transactions.withdrawalAmount, comparison?.transactions.withdrawalAmount)}
        icon={ArrowUpFromLine}
        tone="blue"
      />
      <StatCard
        label="Interest posted"
        value={money.format(report.transactions.interestAmount)}
        suffix={report.currency}
        change={`${report.transactions.interestCount} posting${report.transactions.interestCount === 1 ? '' : 's'}`}
        icon={Coins}
        tone="gold"
      />
    </div>
  )
}

function TransactionBreakdown({ report }) {
  const rows = [
    { label: 'Cash deposits', count: report.transactions.depositCount, amount: report.transactions.depositAmount, tone: 'text-mb-terminal' },
    { label: 'Cash withdrawals', count: report.transactions.withdrawalCount, amount: report.transactions.withdrawalAmount, tone: 'text-mb-danger' },
    { label: 'Daily interest', count: report.transactions.interestCount, amount: report.transactions.interestAmount, tone: 'text-mb-gold-light' },
  ]

  return (
    <Panel title="Transaction breakdown" action={<span className="text-xs text-mb-muted">Close {formatDate(report.businessDate)}</span>}>
      <div className="overflow-x-auto">
        <table className="w-full min-w-[560px] text-left text-sm">
          <thead className="bg-white/[0.025] text-xs uppercase tracking-wide text-mb-muted">
            <tr>
              <th className="px-4 py-3 font-medium">Operation</th>
              <th className="px-4 py-3 text-right font-medium">Count</th>
              <th className="px-4 py-3 text-right font-medium">Amount</th>
              <th className="px-4 py-3 text-right font-medium">Share</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.label} className="border-t border-mb-border/80">
                <td className={`px-4 py-4 font-medium ${row.tone}`}>{row.label}</td>
                <td className="px-4 py-4 text-right font-mono">{row.count}</td>
                <td className="px-4 py-4 text-right font-mono">{money.format(row.amount)} {report.currency}</td>
                <td className="px-4 py-4 text-right text-mb-muted">{share(row.count, report.transactions.operationCount)}</td>
              </tr>
            ))}
          </tbody>
          <tfoot className="border-t border-mb-border bg-white/[0.02]">
            <tr>
              <td className="px-4 py-4 font-semibold">All operations</td>
              <td className="px-4 py-4 text-right font-mono font-semibold">{report.transactions.operationCount}</td>
              <td className="px-4 py-4 text-right text-mb-muted">—</td>
              <td className="px-4 py-4 text-right text-mb-muted">100%</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </Panel>
  )
}

function CustomerSnapshot({ report }) {
  const customers = report.customers
  const activeShare = customers.totalCount === 0 ? 0 : Math.round((customers.activeCount / customers.totalCount) * 100)

  return (
    <Panel title="Customer snapshot" action={<Users size={18} className="text-mb-gold-light" />}>
      <div className="space-y-5 p-5">
        <div>
          <div className="flex items-end justify-between gap-4">
            <div><p className="text-sm text-mb-muted">Total customers</p><p className="mt-1 text-3xl font-semibold">{customers.totalCount}</p></div>
            <span className="font-mono text-sm text-mb-terminal">{activeShare}% active</span>
          </div>
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/6">
            <div className="h-full rounded-full bg-gradient-to-r from-mb-teal to-mb-terminal" style={{ width: `${activeShare}%` }} />
          </div>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <CustomerMetric label="Active" value={customers.activeCount} tone="text-mb-terminal" />
          <CustomerMetric label="Inactive" value={customers.inactiveCount} tone="text-mb-gold-light" />
          <CustomerMetric label="New" value={customers.newCount} tone="text-mb-teal" />
        </div>
      </div>
    </Panel>
  )
}

function CustomerMetric({ label, value, tone }) {
  return <div className="rounded-lg border border-mb-border bg-white/[0.02] p-3"><p className="text-xs text-mb-muted">{label}</p><p className={`mt-1 text-xl font-semibold ${tone}`}>{value}</p></div>
}

function ReportMetadata({ report, comparisonDate, comparisonAvailable }) {
  return (
    <Panel title="Mainframe report">
      <dl className="grid gap-px bg-mb-border sm:grid-cols-2 xl:grid-cols-6">
        <Metadata label="Business date" value={formatDate(report.businessDate)} />
        <Metadata label="Currency" value={report.currency} mono />
        <Metadata label="Request ID" value={report.requestId || '—'} mono />
        <Metadata label="Result" value={report.resultCode} success={report.resultCode === 'OK'} mono />
        <Metadata label="Loaded at" value={formatDateTime(report.loadedAt)} />
        <Metadata label="Comparison" value={comparisonAvailable ? formatDate(comparisonDate) : 'Unavailable'} />
      </dl>
    </Panel>
  )
}

function Metadata({ label, value, mono = false, success = false }) {
  return (
    <div className="min-w-0 bg-mb-panel/95 p-4">
      <dt className="text-xs text-mb-muted">{label}</dt>
      <dd className={`mt-1 truncate text-sm ${mono ? 'font-mono' : ''} ${success ? 'text-mb-terminal' : 'text-mb-text'}`} title={value}>{value}</dd>
    </div>
  )
}

function ReportSkeleton() {
  return (
    <div className="space-y-4" aria-label="Loading report">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => <div key={index} className="mb-panel h-32 animate-pulse bg-white/5" />)}
      </div>
      <div className="grid gap-4 xl:grid-cols-[1.35fr_.85fr]">
        <div className="mb-panel h-72 animate-pulse bg-white/5" />
        <div className="mb-panel h-72 animate-pulse bg-white/5" />
      </div>
    </div>
  )
}

function comparisonText(current, previous, comparison, loading, date) {
  if (loading) return 'Loading previous close…'
  if (!comparison || previous === undefined || previous === null) return `Comparison with ${formatDate(date)} unavailable`
  if (Number(previous) === 0) return Number(current) === 0 ? `No change from ${formatDate(date)}` : `No previous-day baseline`

  const difference = ((Number(current) - Number(previous)) / Math.abs(Number(previous))) * 100
  const sign = difference > 0 ? '+' : ''
  return `${sign}${difference.toFixed(1)}% vs ${formatDate(date)}`
}

function share(count, total) {
  if (!total) return '0%'
  return `${Math.round((count / total) * 100)}%`
}

function reportDate() {
  if (import.meta.env.VITE_DAILY_CLOSE_DATE) return import.meta.env.VITE_DAILY_CLOSE_DATE
  return shiftDate(localDateValue(new Date()), -1)
}

function shiftDate(value, days) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day + days))
  return date.toISOString().slice(0, 10)
}

function localDateValue(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDate(value) {
  if (!value) return '—'
  const [year, month, day] = value.split('-').map(Number)
  return new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
    .format(new Date(Date.UTC(year, month - 1, day)))
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
