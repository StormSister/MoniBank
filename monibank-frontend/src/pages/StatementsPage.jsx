import { useMemo, useState } from 'react'
import { ArrowDownToLine, ArrowUpFromLine, CalendarDays, RefreshCw, Search, X } from 'lucide-react'
import Button from '../components/ui/Button.jsx'
import { Input, Select } from '../components/ui/FormField.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import { useAccounts } from '../hooks/useAccounts.js'
import { useTransactions } from '../hooks/useTransactions.js'

export default function StatementsPage() {
  const [accountId, setAccountId] = useState('')
  const [search, setSearch] = useState('')
  const [type, setType] = useState('ALL')
  const transactionsQuery = useTransactions(accountId)
  const accountsQuery = useAccounts()
  const transactions = transactionsQuery.data || []
  const account = (accountsQuery.data || []).find((item) => item.accountId === accountId)

  const visible = useMemo(() => {
    const phrase = normalize(search)
    return transactions.filter((transaction) => type === 'ALL' || transaction.type === type)
      .filter((transaction) => !phrase || normalize(`${transaction.transactionId} ${transaction.accountId} ${transaction.detail} ${transaction.sourceId}`).includes(phrase))
  }, [search, transactions, type])

  return <div className="mx-auto max-w-[1220px] space-y-4">
    <header><p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">Transaction history</p><h1 className="mt-1 text-2xl font-semibold">Statements</h1><p className="mt-1 text-sm text-mb-muted">Review all transactions or limit the statement to a single account.</p></header>

    <Panel>
      <div className="grid gap-3 border-b border-mb-border p-4 xl:grid-cols-[minmax(250px,1fr)_minmax(250px,1fr)_180px_auto]">
        <Select aria-label="Select statement account" value={accountId} onChange={(event) => setAccountId(event.target.value)} disabled={accountsQuery.isLoading}>
          <option value="">All accounts</option>{(accountsQuery.data || []).map((item) => <option key={item.accountId} value={item.accountId}>{item.accountId} · {item.iban}</option>)}
        </Select>
        <label className="relative"><Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={16} /><Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search ID, detail or cash desk" className="pl-10 pr-10" />{search && <button type="button" onClick={() => setSearch('')} className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-mb-muted hover:text-mb-text"><X size={15} /></button>}</label>
        <Select aria-label="Filter transaction type" value={type} onChange={(event) => setType(event.target.value)}><option value="ALL">All types</option><option value="DP">Deposits</option><option value="WD">Withdrawals</option><option value="IN">Interest</option><option value="OP">Opening entries</option></Select>
        <Button variant="ghost" disabled={!accountId && !search && type === 'ALL'} onClick={() => { setAccountId(''); setSearch(''); setType('ALL') }}>Clear filters</Button>
      </div>

      {account && <div className="flex flex-wrap items-center justify-between gap-3 border-b border-mb-border bg-mb-teal/5 px-4 py-3 text-sm"><span><b className="font-mono text-mb-teal">{account.accountId}</b><span className="ml-3 text-mb-muted">{account.iban}</span></span><span>Current balance: <b>{money(account.balance, account.currency)}</b></span></div>}
      {transactionsQuery.isError ? <div className="p-6"><Notice><div className="flex flex-wrap items-center justify-between gap-3"><span>Transactions could not be loaded. {transactionsQuery.error?.message}</span><Button variant="danger" onClick={() => transactionsQuery.refetch()} disabled={transactionsQuery.isFetching}><RefreshCw className={transactionsQuery.isFetching ? 'animate-spin' : ''} size={15} /> Retry</Button></div></Notice></div> : <TransactionTable transactions={visible} loading={transactionsQuery.isLoading} />}
      {!transactionsQuery.isLoading && !transactionsQuery.isError && <footer className="flex flex-wrap justify-between gap-3 border-t border-mb-border px-4 py-3 text-sm text-mb-muted"><span>Showing <b className="text-mb-text">{visible.length}</b> of {transactions.length}</span><span>Newest transactions first · LISTTXN / VSAM</span></footer>}
    </Panel>
  </div>
}

function TransactionTable({ transactions, loading }) {
  return <div className="overflow-x-auto"><table className="w-full min-w-[1050px] text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase tracking-wide text-mb-muted"><tr>{['Date & time', 'Type', 'Transaction ID', 'Account', 'Detail', 'Amount', 'Balance after', 'Source', 'Status'].map((heading) => <th key={heading} className="px-4 py-3 font-medium">{heading}</th>)}</tr></thead><tbody>
    {loading && Array.from({ length: 5 }, (_, row) => <tr key={row} className="border-t border-mb-border/80">{Array.from({ length: 9 }, (_, cell) => <td key={cell} className="px-4 py-4"><span className="block h-4 animate-pulse rounded bg-white/6" /></td>)}</tr>)}
    {!loading && transactions.length === 0 && <tr><td colSpan="9" className="px-4 py-14 text-center text-mb-muted"><CalendarDays className="mx-auto mb-3" size={25} />No transactions match the selected filters.</td></tr>}
    {transactions.map((transaction) => <tr key={transaction.transactionId} className="border-t border-mb-border/80 hover:bg-white/[0.025]"><td className="whitespace-nowrap px-4 py-3.5 text-mb-muted">{formatDateTime(transaction.createdAt)}</td><td className="px-4 py-3.5"><TransactionType type={transaction.type} /></td><td className="px-4 py-3.5 font-mono text-xs text-mb-teal">{transaction.transactionId}</td><td className="px-4 py-3.5 font-mono text-xs">{transaction.accountId}</td><td className="max-w-56 truncate px-4 py-3.5" title={transaction.detail}>{transaction.detail}</td><td className={`whitespace-nowrap px-4 py-3.5 font-semibold ${transaction.direction === 'D' ? 'text-mb-danger' : 'text-mb-terminal'}`}>{transaction.direction === 'D' ? '−' : '+'}{money(transaction.amount, transaction.currency)}</td><td className="whitespace-nowrap px-4 py-3.5">{money(transaction.balanceAfter, transaction.currency)}</td><td className="px-4 py-3.5 font-mono text-xs text-mb-muted">{transaction.sourceId}</td><td className="px-4 py-3.5"><StatusBadge variant={transaction.status === 'C' ? 'success' : 'warning'}>{transaction.status === 'C' ? 'Completed' : transaction.status}</StatusBadge></td></tr>)}
  </tbody></table></div>
}

function TransactionType({ type }) { const config = { DP: ['Deposit', ArrowDownToLine, 'text-mb-terminal'], WD: ['Withdrawal', ArrowUpFromLine, 'text-mb-danger'], IN: ['Interest', ArrowDownToLine, 'text-mb-teal'], OP: ['Opening', CalendarDays, 'text-mb-gold-light'] }; const [label, Icon, color] = config[type] || [type || 'Unknown', CalendarDays, 'text-mb-muted']; return <span className={`inline-flex items-center gap-1.5 font-medium ${color}`}><Icon size={14} />{label}</span> }
function money(value, currency) { const number = Number(value); return Number.isFinite(number) ? `${number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency || ''}`.trim() : '—' }
function formatDateTime(value) { if (!/^\d{14}$/.test(value || '')) return value || '—'; return `${value.slice(6, 8)}.${value.slice(4, 6)}.${value.slice(0, 4)} ${value.slice(8, 10)}:${value.slice(10, 12)}` }
function normalize(value) { return String(value || '').trim().toLocaleLowerCase() }
