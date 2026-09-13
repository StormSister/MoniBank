import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  CalendarDays,
  FileSearch,
  RefreshCw,
  Search,
  X,
} from 'lucide-react'
import Button from '../components/ui/Button.jsx'
import { Input, Select } from '../components/ui/FormField.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import { useAccounts } from '../hooks/useAccounts.js'
import { useAccountStatement } from '../hooks/useAccountStatement.js'

const INITIAL_PERIOD = currentMonthPeriod()

export default function StatementsPage() {
  const [searchParams] = useSearchParams()
  const [form, setForm] = useState({
    accountId: searchParams.get('accountId') || '',
    from: INITIAL_PERIOD.from,
    to: INITIAL_PERIOD.to,
  })
  const [request, setRequest] = useState(null)
  const [formError, setFormError] = useState('')
  const [search, setSearch] = useState('')
  const [type, setType] = useState('ALL')

  const accountsQuery = useAccounts()
  const statementQuery = useAccountStatement(request)
  const statement = statementQuery.data

  const visibleTransactions = useMemo(() => {
    const phrase = normalize(search)
    const transactions = statement?.transactions || []

    return transactions
      .filter((transaction) => type === 'ALL' || transaction.type === type)
      .filter((transaction) => !phrase || normalize(
        `${transaction.transactionId} ${transaction.detail} ${transaction.sourceId}`,
      ).includes(phrase))
  }, [search, statement, type])

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
    setFormError('')
  }

  function generateStatement(event) {
    event.preventDefault()

    if (!form.accountId) {
      setFormError('Select an account before generating the statement.')
      return
    }
    if (!form.from || !form.to) {
      setFormError('Select both the start and end date.')
      return
    }
    if (form.from > form.to) {
      setFormError('The start date cannot be after the end date.')
      return
    }

    setSearch('')
    setType('ALL')

    if (request
      && request.accountId === form.accountId
      && request.from === form.from
      && request.to === form.to) {
      statementQuery.refetch()
      return
    }

    setRequest({ ...form })
  }

  return (
    <div className="mx-auto max-w-[1220px] space-y-4">
      <header>
        <p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">Account history</p>
        <h1 className="mt-1 text-2xl font-semibold">Statements</h1>
        <p className="mt-1 text-sm text-mb-muted">Select an account and date range to generate a statement from transactions stored in VSAM.</p>
      </header>

      <Panel title="Generate account statement">
        <form onSubmit={generateStatement} className="grid gap-3 p-4 lg:grid-cols-[minmax(300px,1fr)_190px_190px_auto] lg:items-end">
          <label className="grid gap-1.5 text-xs text-mb-muted">
            Account
            <Select
              aria-label="Select statement account"
              value={form.accountId}
              onChange={(event) => update('accountId', event.target.value)}
              disabled={accountsQuery.isLoading || accountsQuery.isError}
            >
              <option value="">Select an account</option>
              {(accountsQuery.data || []).map((account) => (
                <option key={account.accountId} value={account.accountId}>
                  {account.accountId} · {account.iban}
                </option>
              ))}
            </Select>
          </label>

          <label className="grid gap-1.5 text-xs text-mb-muted">
            From
            <Input
              type="date"
              value={form.from}
              max={form.to}
              onChange={(event) => update('from', event.target.value)}
            />
          </label>

          <label className="grid gap-1.5 text-xs text-mb-muted">
            To
            <Input
              type="date"
              value={form.to}
              min={form.from}
              onChange={(event) => update('to', event.target.value)}
            />
          </label>

          <Button type="submit" disabled={accountsQuery.isLoading || statementQuery.isFetching}>
            <FileSearch size={16} />
            {statementQuery.isFetching ? 'Generating…' : 'Generate statement'}
          </Button>
        </form>

        {(formError || accountsQuery.isError) && (
          <div className="border-t border-mb-border p-4">
            <Notice>
              {formError || `Accounts could not be loaded. ${accountsQuery.error?.message || ''}`}
            </Notice>
          </div>
        )}
      </Panel>

      {!request && !statementQuery.isFetching && <StatementEmptyState />}
      {statementQuery.isFetching && <StatementLoading />}

      {statementQuery.isError && (
        <Panel>
          <div className="p-6">
            <Notice>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span>Statement could not be generated. {statementQuery.error?.message}</span>
                <Button variant="danger" onClick={() => statementQuery.refetch()} disabled={statementQuery.isFetching}>
                  <RefreshCw size={15} /> Retry
                </Button>
              </div>
            </Notice>
          </div>
        </Panel>
      )}

      {statement && !statementQuery.isError && (
        <>
          <StatementHeader statement={statement} />
          <StatementSummary statement={statement} />

          <Panel title="Transactions">
            <div className="grid gap-3 border-b border-mb-border p-4 md:grid-cols-[minmax(280px,1fr)_200px_auto]">
              <label className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={16} />
                <Input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search transaction, detail or source"
                  className="pl-10 pr-10"
                />
                {search && (
                  <button type="button" onClick={() => setSearch('')} className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-mb-muted hover:text-mb-text">
                    <X size={15} />
                  </button>
                )}
              </label>

              <Select aria-label="Filter transaction type" value={type} onChange={(event) => setType(event.target.value)}>
                <option value="ALL">All transaction types</option>
                <option value="DP">Deposits</option>
                <option value="WD">Withdrawals</option>
                <option value="IN">Interest</option>
                <option value="OP">Opening entries</option>
              </Select>

              <Button variant="ghost" disabled={!search && type === 'ALL'} onClick={() => { setSearch(''); setType('ALL') }}>
                Clear filters
              </Button>
            </div>

            <TransactionTable transactions={visibleTransactions} />

            <footer className="flex flex-wrap justify-between gap-3 border-t border-mb-border px-4 py-3 text-sm text-mb-muted">
              <span>Showing <b className="text-mb-text">{visibleTransactions.length}</b> of {statement.transactions.length}</span>
              <span>Chronological order · LISTTXN / MBANK.TXN</span>
            </footer>
          </Panel>
        </>
      )}
    </div>
  )
}

function StatementHeader({ statement }) {
  const { account, period } = statement

  return (
    <Panel>
      <div className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-sm font-semibold text-mb-teal">{account.accountId}</span>
            <StatusBadge variant={account.status === 'A' ? 'success' : 'warning'}>
              {account.status === 'A' ? 'Active' : 'Inactive'}
            </StatusBadge>
          </div>
          <p className="mt-1 font-mono text-sm text-mb-muted">{account.iban}</p>
          <p className="mt-1 text-xs text-mb-muted">Customer {account.customerId} · {account.type}</p>
        </div>
        <div className="sm:text-right">
          <p className="text-xs uppercase tracking-wide text-mb-muted">Statement period</p>
          <p className="mt-1 font-medium">{formatDate(period.from)} – {formatDate(period.to)}</p>
        </div>
      </div>
    </Panel>
  )
}

function StatementSummary({ statement }) {
  const { summary } = statement
  const currency = statement.account.currency
  const items = [
    ['Opening balance', money(summary.openingBalance, currency), 'text-mb-text'],
    ['Credits', `+${money(summary.totalCredits, currency)}`, 'text-mb-terminal', `${summary.creditCount} transaction${summary.creditCount === 1 ? '' : 's'}`],
    ['Debits', `−${money(summary.totalDebits, currency)}`, 'text-mb-danger', `${summary.debitCount} transaction${summary.debitCount === 1 ? '' : 's'}`],
    ['Closing balance', money(summary.closingBalance, currency), 'text-mb-gold-light'],
  ]

  return (
    <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {items.map(([label, value, color, hint]) => (
        <div key={label} className="rounded-xl border border-mb-border bg-mb-panel/70 p-4">
          <p className="text-xs uppercase tracking-wide text-mb-muted">{label}</p>
          <p className={`mt-2 text-xl font-semibold ${color}`}>{value}</p>
          {hint && <p className="mt-1 text-xs text-mb-muted">{hint}</p>}
        </div>
      ))}
    </section>
  )
}

function StatementEmptyState() {
  return (
    <Panel>
      <div className="px-6 py-16 text-center text-mb-muted">
        <FileSearch className="mx-auto mb-3 text-mb-gold-light" size={30} />
        <p className="font-medium text-mb-text">Choose an account and statement period</p>
        <p className="mt-1 text-sm">The mainframe will return transactions recorded for that account.</p>
      </div>
    </Panel>
  )
}

function StatementLoading() {
  return (
    <Panel>
      <div className="space-y-3 p-6">
        <div className="h-5 w-64 animate-pulse rounded bg-white/6" />
        <div className="h-16 animate-pulse rounded bg-white/5" />
        <div className="h-16 animate-pulse rounded bg-white/5" />
      </div>
    </Panel>
  )
}

function TransactionTable({ transactions }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[960px] text-left text-sm">
        <thead className="bg-white/[0.025] text-xs uppercase tracking-wide text-mb-muted">
          <tr>
            {['Date & time', 'Type', 'Transaction ID', 'Detail', 'Amount', 'Balance after', 'Source', 'Status'].map((heading) => (
              <th key={heading} className="px-4 py-3 font-medium">{heading}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {transactions.length === 0 && (
            <tr>
              <td colSpan="8" className="px-4 py-14 text-center text-mb-muted">
                <CalendarDays className="mx-auto mb-3" size={25} />
                No transactions match this statement and its local filters.
              </td>
            </tr>
          )}
          {transactions.map((transaction) => (
            <tr key={transaction.transactionId} className="border-t border-mb-border/80 hover:bg-white/[0.025]">
              <td className="whitespace-nowrap px-4 py-3.5 text-mb-muted">{formatDateTime(transaction.createdAt)}</td>
              <td className="px-4 py-3.5"><TransactionType type={transaction.type} /></td>
              <td className="px-4 py-3.5 font-mono text-xs text-mb-teal">{transaction.transactionId}</td>
              <td className="max-w-64 truncate px-4 py-3.5" title={transaction.detail}>{transaction.detail}</td>
              <td className={`whitespace-nowrap px-4 py-3.5 font-semibold ${transaction.direction === 'D' ? 'text-mb-danger' : 'text-mb-terminal'}`}>
                {transaction.direction === 'D' ? '−' : '+'}{money(transaction.amount, transaction.currency)}
              </td>
              <td className="whitespace-nowrap px-4 py-3.5">{money(transaction.balanceAfter, transaction.currency)}</td>
              <td className="px-4 py-3.5 font-mono text-xs text-mb-muted">{transaction.sourceId || '—'}</td>
              <td className="px-4 py-3.5">
                <StatusBadge variant={transaction.status === 'C' ? 'success' : 'warning'}>
                  {transaction.status === 'C' ? 'Completed' : transaction.status}
                </StatusBadge>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function TransactionType({ type }) {
  const config = {
    DP: ['Deposit', ArrowDownToLine, 'text-mb-terminal'],
    WD: ['Withdrawal', ArrowUpFromLine, 'text-mb-danger'],
    IN: ['Interest', ArrowDownToLine, 'text-mb-teal'],
    OP: ['Opening', CalendarDays, 'text-mb-gold-light'],
  }
  const [label, Icon, color] = config[type] || [type || 'Unknown', CalendarDays, 'text-mb-muted']

  return <span className={`inline-flex items-center gap-1.5 font-medium ${color}`}><Icon size={14} />{label}</span>
}

function currentMonthPeriod() {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')

  return {
    from: `${year}-${month}-01`,
    to: `${year}-${month}-${day}`,
  }
}

function money(value, currency) {
  const number = Number(value)
  return Number.isFinite(number)
    ? `${number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency || ''}`.trim()
    : '—'
}

function formatDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return value || '—'
  return `${value.slice(8, 10)}.${value.slice(5, 7)}.${value.slice(0, 4)}`
}

function formatDateTime(value) {
  if (!/^\d{14}$/.test(value || '')) return value || '—'
  return `${value.slice(6, 8)}.${value.slice(4, 6)}.${value.slice(0, 4)} ${value.slice(8, 10)}:${value.slice(10, 12)}`
}

function normalize(value) {
  return String(value || '').trim().toLocaleLowerCase()
}
