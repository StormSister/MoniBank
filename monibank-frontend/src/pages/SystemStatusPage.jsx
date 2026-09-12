import {
  Activity,
  Clock3,
  Cpu,
  HardDrive,
  Network,
  RefreshCw,
  Server,
  SquareTerminal,
} from 'lucide-react'
import { useMainframeStatus } from '../hooks/useMainframeStatus.js'
import Button from '../components/ui/Button.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'

export default function SystemStatusPage() {
  const statusQuery = useMainframeStatus()
  const mainframe = statusQuery.data
  const connections = mainframe?.connections
  const terminals = connections?.terminals || []

  return (
    <div className="w-full space-y-4">
      <section className="flex flex-col gap-4 rounded-xl border border-mb-gold/20 bg-[linear-gradient(120deg,rgba(215,162,59,.08),rgba(16,36,49,.7)_45%,rgba(45,212,191,.05))] px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-mb-text">Legacy core infrastructure</p>
          <p className="mt-1 text-xs text-mb-muted">
            Live health of Hercules, transport interfaces and the KICKS terminal pool.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge variant={overallVariant(mainframe?.status)}>
            {mainframe?.status || (statusQuery.isLoading ? 'CHECKING' : 'UNAVAILABLE')}
          </StatusBadge>
          <Button
            className="min-h-8 px-3 py-1"
            onClick={() => statusQuery.refetch()}
            disabled={statusQuery.isFetching}
          >
            <RefreshCw size={14} className={statusQuery.isFetching ? 'animate-spin' : ''} />
            Refresh
          </Button>
        </div>
      </section>

      {statusQuery.isError && (
        <div className="rounded-lg border border-mb-danger/25 bg-mb-danger/7 px-4 py-3 text-sm text-mb-danger">
          The backend status endpoint is unavailable. {statusQuery.error?.message}
        </div>
      )}

      <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,240px),1fr))] gap-3">
        <SummaryCard
          icon={SquareTerminal}
          label="Terminal pool"
          value={`${connections?.readyTerminals ?? 0} / ${connections?.configuredTerminals ?? 0} ready`}
          detail={`${connections?.busyTerminals ?? 0} busy · ${connections?.recoveringTerminals ?? 0} recovering`}
        />
        <SummaryCard
          icon={Activity}
          label="Request queue"
          value={String(connections?.queuedRequests ?? '—')}
          detail="Requests waiting for a free terminal"
        />
        <SummaryCard
          icon={Cpu}
          label="Hercules CPU"
          value={formatPercent(mainframe?.runtime?.cpuPercent)}
          detail={`Container: ${mainframe?.runtime?.containerState || 'CHECKING'}`}
        />
        <SummaryCard
          icon={Clock3}
          label="Hercules uptime"
          value={formatUptime(mainframe?.runtime?.uptimeSeconds)}
          detail={mainframe?.metricsStale ? 'Last runtime sample is stale' : 'Live container sample'}
        />
      </div>

      <Panel
        title="KICKS Terminal Pool"
        action={<span className="text-xs text-mb-muted">Shared request queue · isolated TSO sessions</span>}
      >
        {terminals.length === 0 ? (
          <div className="px-4 py-10 text-center text-sm text-mb-muted">
            Waiting for terminal status data.
          </div>
        ) : (
          <div className="grid gap-3 p-3 lg:grid-cols-3">
            {terminals.map((terminal) => (
              <TerminalCard key={terminal.id} terminal={terminal} />
            ))}
          </div>
        )}
      </Panel>

      <div className="grid gap-3 lg:grid-cols-2">
        <Panel title="Mainframe Interfaces · Live">
          <div className="grid gap-2 p-4 sm:grid-cols-2">
            <InterfaceState icon={Network} label="JES reader · 3505" value={connections?.reader} />
            <InterfaceState icon={HardDrive} label="Result printer · 5001" value={connections?.resultPrinter} />
            <InterfaceState icon={SquareTerminal} label="Terminal pool" value={connections?.terminal} />
            <InterfaceState icon={Server} label="System" value={`${mainframe?.system || 'MVS 3.8j'} / ${mainframe?.systemId || 'TK5R'}`} alwaysReady />
          </div>
        </Panel>

        <Panel title="Hercules Runtime">
          <div className="space-y-4 p-4 text-xs">
            <Metric
              label="CPU"
              value={formatPercent(mainframe?.runtime?.cpuPercent)}
              width={metricWidth(mainframe?.runtime?.cpuPercent)}
            />
            <Metric
              label="Container memory"
              value={formatMemory(mainframe?.runtime)}
              width={metricWidth(mainframe?.runtime?.memoryPercent)}
            />
            <p className="border-t border-mb-border pt-3 text-[10px] text-mb-muted">
              Backend snapshot: {formatDateTime(mainframe?.checkedAt)} · refreshed every 15 seconds.
            </p>
          </div>
        </Panel>
      </div>
    </div>
  )
}

function TerminalCard({ terminal }) {
  const healthy = ['READY', 'BUSY'].includes(terminal.state)

  return (
    <article className="rounded-xl border border-mb-border bg-[linear-gradient(145deg,rgba(20,43,57,.78),rgba(10,27,38,.96))] p-4">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className={`grid size-10 place-items-center rounded-lg border ${healthy ? 'border-mb-terminal/25 bg-mb-terminal/10 text-mb-terminal' : 'border-mb-gold/25 bg-mb-gold/10 text-mb-gold-light'}`}>
            <SquareTerminal size={21} />
          </span>
          <div>
            <p className="font-semibold text-mb-text">{terminal.id}</p>
            <p className="mt-0.5 font-mono text-[11px] text-mb-muted">TSO · {terminal.username}</p>
          </div>
        </div>
        <StatusBadge variant={terminalVariant(terminal.state)}>{terminal.state}</StatusBadge>
      </div>

      <dl className="mt-4 grid grid-cols-2 gap-2 text-xs">
        <TerminalDetail label="Current request" value={terminal.currentRequestId || 'None'} mono />
        <TerminalDetail label="Recoveries" value={String(terminal.recoveryCount ?? 0)} />
        <TerminalDetail label="Ready since" value={formatDateTime(terminal.readySince)} />
        <TerminalDetail label="Last failure" value={formatDateTime(terminal.lastFailureAt)} />
      </dl>

      {terminal.lastError && (
        <p className="mt-3 break-words rounded-lg border border-mb-danger/20 bg-mb-danger/7 px-3 py-2 text-[10px] text-mb-danger">
          {terminal.lastError}
        </p>
      )}
    </article>
  )
}

function TerminalDetail({ label, value, mono = false }) {
  return (
    <div className="rounded-lg border border-mb-border bg-white/[0.02] px-2.5 py-2">
      <dt className="text-[10px] text-mb-muted">{label}</dt>
      <dd className={`mt-1 truncate text-mb-text ${mono ? 'font-mono text-[11px]' : ''}`} title={value}>{value}</dd>
    </div>
  )
}

function SummaryCard({ icon: Icon, label, value, detail }) {
  return (
    <div className="mb-panel flex items-center gap-3 p-4">
      <span className="grid size-11 shrink-0 place-items-center rounded-xl border border-mb-teal/20 bg-mb-teal/10 text-mb-teal">
        <Icon size={23} />
      </span>
      <div className="min-w-0">
        <p className="text-[10px] uppercase tracking-wide text-mb-muted">{label}</p>
        <p className="mt-1 text-lg font-semibold text-mb-text">{value}</p>
        <p className="mt-0.5 truncate text-[10px] text-mb-muted" title={detail}>{detail}</p>
      </div>
    </div>
  )
}

function InterfaceState({ icon: Icon, label, value, alwaysReady = false }) {
  const ready = alwaysReady || ['CONNECTED', 'READY', 'BUSY'].includes(value)

  return (
    <div className="flex items-center gap-3 rounded-lg border border-mb-border bg-white/[0.02] px-3 py-3">
      <Icon size={18} className={ready ? 'text-mb-terminal' : 'text-mb-gold-light'} />
      <div>
        <p className="text-[10px] text-mb-muted">{label}</p>
        <p className={`mt-1 font-mono text-[10px] ${ready ? 'text-mb-terminal' : 'text-mb-gold-light'}`}>
          {value || 'CHECKING'}
        </p>
      </div>
    </div>
  )
}

function Metric({ label, value, width }) {
  return (
    <div>
      <div className="flex justify-between"><span className="text-mb-muted">{label}</span><span>{value}</span></div>
      <div className="mt-2 h-1.5 rounded bg-white/6"><div className="h-full rounded bg-mb-teal" style={{ width }} /></div>
    </div>
  )
}

function overallVariant(status) {
  if (status === 'ONLINE') return 'success'
  if (status === 'OFFLINE') return 'danger'
  return 'warning'
}

function terminalVariant(state) {
  if (['READY', 'BUSY'].includes(state)) return 'success'
  if (['FAILED', 'CLOSED'].includes(state)) return 'danger'
  return 'warning'
}

function formatPercent(value) {
  return Number.isFinite(value) ? `${value.toFixed(1)}%` : '—'
}

function metricWidth(value) {
  const percentage = Number.isFinite(value) ? Math.min(Math.max(value, 0), 100) : 0
  return `${percentage}%`
}

function formatMemory(runtime) {
  if (!runtime?.memoryUsed || !runtime?.memoryLimit) return '—'
  return `${runtime.memoryUsed} / ${runtime.memoryLimit}`
}

function formatUptime(totalSeconds) {
  if (!Number.isFinite(totalSeconds)) return '—'
  const days = Math.floor(totalSeconds / 86_400)
  const hours = Math.floor((totalSeconds % 86_400) / 3_600)
  const minutes = Math.floor((totalSeconds % 3_600) / 60)
  return `${days}d ${hours}h ${minutes}m`
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString([], { hour12: false })
}
