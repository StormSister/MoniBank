import { useState } from 'react'
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock3,
  RefreshCw,
  ServerCog,
  TerminalSquare,
  XCircle,
} from 'lucide-react'
import { useOperations, useOperationSummary } from '../hooks/useOperations.js'
import StatCard from '../components/dashboard/StatCard.jsx'
import Button from '../components/ui/Button.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'

const PERIODS = [
  [1, 'Last hour'],
  [24, 'Last 24 hours'],
  [168, 'Last 7 days'],
  [720, 'Last 30 days'],
]

export default function OperationsPage() {
  const [hours, setHours] = useState(24)
  const [status, setStatus] = useState('')
  const summaryQuery = useOperationSummary({ hours })
  const operationsQuery = useOperations({ hours, status, limit: 100 })
  const summary = summaryQuery.data
  const operations = operationsQuery.data || []
  const refreshing = summaryQuery.isFetching || operationsQuery.isFetching

  const refresh = () => {
    summaryQuery.refetch()
    operationsQuery.refetch()
  }

  return (
    <div className="mx-auto max-w-[1320px] space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">
            Core execution journal
          </p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight text-mb-text">Operations</h1>
          <p className="mt-1 text-sm text-mb-muted">
            MVS result records correlated with Java request and terminal worker telemetry.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <select
            aria-label="Operation period"
            className="mb-input min-h-9 w-auto py-1.5 text-sm"
            value={hours}
            onChange={(event) => setHours(Number(event.target.value))}
          >
            {PERIODS.map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <Button className="min-h-9 px-3 py-1.5" onClick={refresh} disabled={refreshing}>
            <RefreshCw size={15} className={refreshing ? 'animate-spin' : ''} />
            Refresh
          </Button>
        </div>
      </header>

      {(summaryQuery.isError || operationsQuery.isError) && (
        <div className="rounded-lg border border-mb-danger/25 bg-mb-danger/7 px-4 py-3 text-sm text-mb-danger">
          Operation data is unavailable. {(summaryQuery.error || operationsQuery.error)?.message}
        </div>
      )}

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="All operations"
          value={summary?.totalCount ?? '—'}
          change={`${summary?.successRatePercent ?? 0}% success rate`}
          icon={Activity}
          tone="teal"
          loading={summaryQuery.isLoading}
        />
        <StatCard
          label="Successful"
          value={summary?.successCount ?? '—'}
          change="Confirmed by an MBR;S record"
          icon={CheckCircle2}
          tone="green"
          loading={summaryQuery.isLoading}
        />
        <StatCard
          label="Business errors"
          value={summary?.businessErrorCount ?? '—'}
          change="Valid MVS rejection · MBR;E"
          icon={AlertTriangle}
          tone="gold"
          loading={summaryQuery.isLoading}
        />
        <StatCard
          label="Technical errors"
          value={summary?.technicalErrorCount ?? '—'}
          change="Transport, timeout or terminal failure"
          icon={XCircle}
          tone="danger"
          loading={summaryQuery.isLoading}
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[1fr_1.25fr]">
        <WorkerPanel summary={summary} loading={summaryQuery.isLoading} />
        <OperationBreakdown summary={summary} loading={summaryQuery.isLoading} />
      </div>

      <Panel
        title="Recent operations"
        action={(
          <div className="flex items-center gap-2">
            <select
              aria-label="Operation status"
              className="mb-input min-h-8 w-auto py-1 text-xs"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="">All statuses</option>
              <option value="SUCCESS">Success</option>
              <option value="BUSINESS_ERROR">Business error</option>
              <option value="TECHNICAL_ERROR">Technical error</option>
            </select>
            <span className="hidden text-xs text-mb-muted sm:inline">Auto-refresh · 15s</span>
          </div>
        )}
      >
        <OperationsTable operations={operations} loading={operationsQuery.isLoading} />
      </Panel>

      <p className="px-1 text-[10px] leading-relaxed text-mb-muted/75">
        Source: append-only MoniBank operation journal. Business outcomes originate in standardized
        MVS result records; worker and timing metadata is added by the Java integration layer.
      </p>
    </div>
  )
}

function WorkerPanel({ summary, loading }) {
  const workers = summary?.executors || []
  const maxTotal = Math.max(1, ...workers.map((worker) => worker.totalCount))

  return (
    <Panel
      title="Terminal workers"
      action={<TerminalSquare size={18} className="text-mb-terminal" />}
    >
      {loading ? (
        <LoadingRows count={2} />
      ) : workers.length === 0 ? (
        <EmptyState text="No worker activity in this period." />
      ) : (
        <div className="space-y-3 p-4">
          {workers.map((worker) => (
            <article key={worker.executorId} className="rounded-lg border border-mb-border bg-white/[0.02] p-3">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-semibold text-mb-text">{worker.executorId}</p>
                  <p className="mt-0.5 text-[10px] text-mb-muted">
                    {worker.successCount} successful · avg {formatDuration(worker.averageDurationMs)}
                  </p>
                </div>
                <span className="font-mono text-sm text-mb-terminal">{worker.totalCount}</span>
              </div>
              <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-white/6">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-mb-teal to-mb-terminal"
                  style={{ width: `${(worker.totalCount / maxTotal) * 100}%` }}
                />
              </div>
              {(worker.businessErrorCount > 0 || worker.technicalErrorCount > 0) && (
                <p className="mt-2 text-[10px] text-mb-gold-light">
                  {worker.businessErrorCount} business · {worker.technicalErrorCount} technical errors
                </p>
              )}
            </article>
          ))}
        </div>
      )}
    </Panel>
  )
}

function OperationBreakdown({ summary, loading }) {
  const operations = summary?.operations || []

  return (
    <Panel
      title="Operation breakdown"
      action={summary && (
        <span className="text-xs text-mb-muted">
          Avg queue {formatDuration(summary.averageQueueMs)} · execution {formatDuration(summary.averageDurationMs)}
        </span>
      )}
    >
      {loading ? (
        <LoadingRows count={3} />
      ) : operations.length === 0 ? (
        <EmptyState text="No operations in this period." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[520px] text-left text-sm">
            <thead className="bg-white/[0.025] text-[10px] uppercase tracking-wide text-mb-muted">
              <tr>
                <th className="px-4 py-3 font-medium">Program operation</th>
                <th className="px-4 py-3 text-right font-medium">Total</th>
                <th className="px-4 py-3 text-right font-medium">Success</th>
                <th className="px-4 py-3 text-right font-medium">Business</th>
                <th className="px-4 py-3 text-right font-medium">Technical</th>
              </tr>
            </thead>
            <tbody>
              {operations.map((operation) => (
                <tr key={operation.operation} className="border-t border-mb-border/80">
                  <td className="px-4 py-3 font-mono text-xs text-mb-teal">{operation.operation}</td>
                  <td className="px-4 py-3 text-right font-mono">{operation.totalCount}</td>
                  <td className="px-4 py-3 text-right font-mono text-mb-terminal">{operation.successCount}</td>
                  <td className="px-4 py-3 text-right font-mono text-mb-gold-light">{operation.businessErrorCount}</td>
                  <td className="px-4 py-3 text-right font-mono text-mb-danger">{operation.technicalErrorCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  )
}

function OperationsTable({ operations, loading }) {
  if (loading) return <LoadingRows count={5} />
  if (operations.length === 0) return <EmptyState text="No operations match the selected filters." />

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[920px] text-left text-sm">
        <thead className="bg-white/[0.025] text-[10px] uppercase tracking-wide text-mb-muted">
          <tr>
            <th className="px-4 py-3 font-medium">Completed</th>
            <th className="px-4 py-3 font-medium">Request</th>
            <th className="px-4 py-3 font-medium">Operation</th>
            <th className="px-4 py-3 font-medium">Worker</th>
            <th className="px-4 py-3 text-right font-medium">Queue</th>
            <th className="px-4 py-3 text-right font-medium">Duration</th>
            <th className="px-4 py-3 font-medium">Result</th>
          </tr>
        </thead>
        <tbody>
          {operations.map((operation) => (
            <tr key={operation.requestId} className="border-t border-mb-border/80 transition hover:bg-white/[0.025]">
              <td className="whitespace-nowrap px-4 py-3 text-xs text-mb-muted">{formatDateTime(operation.completedAt)}</td>
              <td className="px-4 py-3 font-mono text-xs text-mb-text">{operation.requestId}</td>
              <td className="px-4 py-3">
                <p className="font-mono text-xs text-mb-teal">{operation.operation}</p>
                <p className="mt-0.5 text-[10px] text-mb-muted">{operation.core} · {operation.channel}</p>
              </td>
              <td className="px-4 py-3">
                <p className="text-xs font-medium text-mb-text">{operation.executorId || 'Unassigned'}</p>
                <p className="mt-0.5 font-mono text-[10px] text-mb-muted">{operation.username || operation.executorType}</p>
              </td>
              <td className="px-4 py-3 text-right font-mono text-xs text-mb-muted">{formatDuration(operation.queueMs)}</td>
              <td className="px-4 py-3 text-right font-mono text-xs text-mb-text">{formatDuration(operation.durationMs)}</td>
              <td className="px-4 py-3">
                <StatusBadge variant={statusVariant(operation.status)}>
                  {statusLabel(operation.status)}
                </StatusBadge>
                <p className="mt-1 font-mono text-[10px] text-mb-muted">{operation.resultCode || '—'}</p>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function LoadingRows({ count }) {
  return (
    <div className="space-y-2 p-4" aria-label="Loading operations">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="h-12 animate-pulse rounded-lg bg-white/5" />
      ))}
    </div>
  )
}

function EmptyState({ text }) {
  return (
    <div className="grid min-h-36 place-items-center px-4 py-8 text-center">
      <div>
        <ServerCog size={24} className="mx-auto text-mb-muted/60" />
        <p className="mt-2 text-sm text-mb-muted">{text}</p>
      </div>
    </div>
  )
}

function statusVariant(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'BUSINESS_ERROR') return 'warning'
  return 'danger'
}

function statusLabel(status) {
  if (status === 'SUCCESS') return 'SUCCESS'
  if (status === 'BUSINESS_ERROR') return 'BUSINESS'
  return 'TECHNICAL'
}

function formatDuration(value) {
  const milliseconds = Number(value)
  if (!Number.isFinite(milliseconds)) return '—'
  if (milliseconds < 1_000) return `${milliseconds} ms`
  return `${(milliseconds / 1_000).toFixed(milliseconds < 10_000 ? 2 : 1)} s`
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString([], {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}
