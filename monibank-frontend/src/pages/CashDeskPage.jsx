import { useMemo, useState } from 'react'
import { ArrowDownToLine, ArrowUpFromLine, CheckCircle2, Landmark, RefreshCw, Search } from 'lucide-react'
import Button from '../components/ui/Button.jsx'
import FormField, { Input, Select } from '../components/ui/FormField.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import { useAccounts } from '../hooks/useAccounts.js'
import { useDeposit, useWithdrawal } from '../hooks/useTransactions.js'

const INITIAL_FORM = Object.freeze({ accountId: '', amount: '', detail: 'CASH DESK DEPOSIT', sourceId: 'CASHDESK00001' })

export default function CashDeskPage() {
  const accountsQuery = useAccounts()
  const deposit = useDeposit()
  const withdrawal = useWithdrawal()
  const [operation, setOperation] = useState('deposit')
  const [form, setForm] = useState({ ...INITIAL_FORM })
  const [errors, setErrors] = useState({})
  const [result, setResult] = useState(null)
  const [accountSearch, setAccountSearch] = useState('')

  const mutation = operation === 'deposit' ? deposit : withdrawal
  const activeAccounts = (accountsQuery.data || []).filter((account) => account.status === 'A')
  const matchingAccounts = useMemo(() => {
    const phrase = normalize(accountSearch)
    return activeAccounts.filter((account) => !phrase || normalize(`${account.accountId} ${account.iban} ${account.customerId}`).includes(phrase))
  }, [activeAccounts, accountSearch])
  const selectedAccount = activeAccounts.find((account) => account.accountId === form.accountId)
  const availableFunds = selectedAccount
    ? Number(selectedAccount.balance || 0) + Number(selectedAccount.overdraftLimit || 0) - Number(selectedAccount.blockedAmount || 0)
    : null

  const chooseOperation = (nextOperation) => {
    if (mutation.isPending) return
    setOperation(nextOperation)
    setForm((current) => ({ ...current, detail: nextOperation === 'deposit' ? 'CASH DESK DEPOSIT' : 'CASH DESK WITHDRAWAL' }))
    setErrors({})
    setResult(null)
    deposit.reset()
    withdrawal.reset()
  }

  const update = (field, value) => {
    const sanitized = field === 'sourceId' ? value.toUpperCase().replace(/[^A-Z0-9#_-]/g, '').slice(0, 13) : value
    setForm((current) => ({ ...current, [field]: sanitized }))
    setErrors((current) => ({ ...current, [field]: undefined }))
    mutation.reset()
  }

  const submit = (event) => {
    event.preventDefault()
    const nextErrors = validate(form, operation, availableFunds)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return

    mutation.mutate({
      accountId: form.accountId,
      amount: Number(form.amount),
      detail: form.detail.trim(),
      sourceId: form.sourceId.trim(),
    }, {
      onSuccess: (transaction) => {
        setResult(transaction)
        setForm((current) => ({ ...current, amount: '' }))
      },
    })
  }

  return (
    <div className="mx-auto max-w-[1120px] space-y-4">
      <header><p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">Branch operations</p><h1 className="mt-1 text-2xl font-semibold">Cash Desk</h1><p className="mt-1 text-sm text-mb-muted">Post a cash deposit or withdrawal directly to an active account.</p></header>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_340px]">
        <Panel>
          <div className="grid grid-cols-2 border-b border-mb-border p-4">
            <OperationButton active={operation === 'deposit'} onClick={() => chooseOperation('deposit')} icon={ArrowDownToLine} label="Cash deposit" tone="green" />
            <OperationButton active={operation === 'withdrawal'} onClick={() => chooseOperation('withdrawal')} icon={ArrowUpFromLine} label="Cash withdrawal" tone="blue" />
          </div>

          <form onSubmit={submit} noValidate>
            <div className="space-y-5 p-5">
              {mutation.isError && <Notice>{operation === 'deposit' ? 'Deposit' : 'Withdrawal'} failed. {mutation.error?.message}</Notice>}
              {accountsQuery.isError && <Notice>Accounts could not be loaded. {accountsQuery.error?.message}</Notice>}

              <FormField id="cash-account-search" label="Find account" hint="Search by account ID, IBAN or customer ID.">
                <label className="relative block"><Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={16} /><Input id="cash-account-search" value={accountSearch} onChange={(event) => setAccountSearch(event.target.value)} placeholder="A000000000002 or IBAN" className="pl-10" /></label>
              </FormField>

              <FormField id="cash-account" label="Active account" error={errors.accountId}>
                <Select id="cash-account" value={form.accountId} onChange={(event) => update('accountId', event.target.value)} disabled={accountsQuery.isLoading}>
                  <option value="">Select account…</option>
                  {matchingAccounts.map((account) => <option key={account.accountId} value={account.accountId}>{account.accountId} · {account.iban} · {account.customerId}</option>)}
                </Select>
              </FormField>

              {selectedAccount && <AccountSummary account={selectedAccount} availableFunds={availableFunds} />}

              <div className="grid gap-5 sm:grid-cols-2">
                <FormField id="cash-amount" label="Amount" error={errors.amount} hint={operation === 'withdrawal' && availableFunds !== null ? `Available funds: ${money(availableFunds, selectedAccount?.currency)}` : 'Positive amount with up to two decimals.'}>
                  <Input id="cash-amount" type="number" min="0.01" step="0.01" value={form.amount} onChange={(event) => update('amount', event.target.value)} placeholder="0.00" autoFocus />
                </FormField>
                <FormField id="cash-source" label="Cash desk ID" error={errors.sourceId} hint="Up to 13 letters, digits, #, _ or -." >
                  <Input id="cash-source" value={form.sourceId} onChange={(event) => update('sourceId', event.target.value)} maxLength="13" />
                </FormField>
              </div>

              <FormField id="cash-detail" label="Transaction detail" error={errors.detail} hint={`${form.detail.length}/34 characters`}>
                <Input id="cash-detail" value={form.detail} onChange={(event) => update('detail', event.target.value)} maxLength="34" placeholder={operation === 'deposit' ? 'CASH DESK DEPOSIT' : 'CASH DESK WITHDRAWAL'} />
              </FormField>
            </div>

            <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-mb-border px-5 py-4">
              <span className="text-xs text-mb-muted">The mainframe performs the final balance validation.</span>
              <Button type="submit" variant="primary" disabled={mutation.isPending || accountsQuery.isLoading}>
                {mutation.isPending ? <RefreshCw className="animate-spin" size={16} /> : operation === 'deposit' ? <ArrowDownToLine size={16} /> : <ArrowUpFromLine size={16} />}
                {mutation.isPending ? 'Posting in MVS…' : operation === 'deposit' ? 'Post deposit' : 'Post withdrawal'}
              </Button>
            </footer>
          </form>
        </Panel>

        <Receipt transaction={result} operation={operation} />
      </div>
    </div>
  )
}

function OperationButton({ active, onClick, icon: Icon, label, tone }) {
  const activeClass = tone === 'green' ? 'border-mb-terminal/45 bg-mb-terminal/8 text-mb-terminal' : 'border-blue-400/40 bg-blue-500/8 text-blue-300'
  return <button type="button" onClick={onClick} className={`flex items-center justify-center gap-2 border-b-2 px-4 py-3 text-sm font-semibold transition ${active ? activeClass : 'border-transparent text-mb-muted hover:bg-white/[0.025] hover:text-mb-text'}`}><Icon size={18} />{label}</button>
}

function AccountSummary({ account, availableFunds }) {
  return <div className="grid gap-3 rounded-xl border border-mb-teal/20 bg-mb-teal/5 p-4 sm:grid-cols-3"><Small label="Current balance" value={money(account.balance, account.currency)} /><Small label="Available funds" value={money(availableFunds, account.currency)} /><Small label="Blocked" value={money(account.blockedAmount, account.currency)} /></div>
}

function Receipt({ transaction, operation }) {
  return <Panel title="Transaction receipt" className="h-fit"><div className="p-5">{!transaction ? <div className="grid min-h-72 place-items-center text-center"><div><span className="mx-auto grid size-14 place-items-center rounded-full border border-mb-border bg-white/[0.02] text-mb-muted"><Landmark size={24} /></span><p className="mt-4 font-semibold text-mb-text">Waiting for an operation</p><p className="mt-2 text-sm leading-6 text-mb-muted">The mainframe confirmation and resulting balance will appear here.</p></div></div> : <div className="space-y-5"><div className="text-center"><CheckCircle2 className="mx-auto text-mb-terminal" size={38} /><p className="mt-3 text-lg font-semibold text-mb-terminal">Completed</p><p className="mt-1 text-xs text-mb-muted">{operation === 'deposit' ? 'Cash deposit' : 'Cash withdrawal'}</p></div><dl className="space-y-3 border-y border-dashed border-mb-border py-4"><ReceiptRow label="Transaction ID" value={transaction.transactionId} mono /><ReceiptRow label="Account" value={transaction.accountId} mono /><ReceiptRow label="Amount" value={money(transaction.amount, transaction.currency)} /><ReceiptRow label="Balance after" value={money(transaction.balanceAfter, transaction.currency)} /><ReceiptRow label="Detail" value={transaction.detail} /><ReceiptRow label="Date and time" value={formatDateTime(transaction.createdAt)} /></dl><p className="text-center font-mono text-[10px] uppercase tracking-[0.14em] text-mb-muted">POSTTXN · RC=0000</p></div>}</div></Panel>
}

function Small({ label, value }) { return <div><p className="text-[11px] uppercase tracking-wide text-mb-muted">{label}</p><p className="mt-1 font-semibold text-mb-text">{value}</p></div> }
function ReceiptRow({ label, value, mono }) { return <div className="flex justify-between gap-4 text-sm"><dt className="text-mb-muted">{label}</dt><dd className={`text-right text-mb-text ${mono ? 'font-mono text-xs' : 'font-semibold'}`}>{value || '—'}</dd></div> }
function validate(form, operation, availableFunds) { const errors = {}; if (!/^A\d{12}$/.test(form.accountId)) errors.accountId = 'Select an active account.'; const amount = Number(form.amount); if (!form.amount || !Number.isFinite(amount) || amount <= 0 || !/^\d+(\.\d{1,2})?$/.test(form.amount)) errors.amount = 'Enter an amount greater than zero with up to two decimals.'; else if (operation === 'withdrawal' && availableFunds !== null && amount > availableFunds) errors.amount = 'The amount exceeds the funds currently available.'; if (!form.detail.trim() || form.detail.length > 34 || !/^[A-Za-z0-9 .#/_'-]+$/.test(form.detail)) errors.detail = 'Use 1–34 supported letters, digits and punctuation characters.'; if (!/^[A-Za-z0-9#_-]{1,13}$/.test(form.sourceId)) errors.sourceId = 'Use 1–13 letters, digits, #, _ or -.'; return errors }
function money(value, currency) { const number = Number(value); return Number.isFinite(number) ? `${number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency || ''}`.trim() : '—' }
function formatDateTime(value) { if (!/^\d{14}$/.test(value || '')) return value || '—'; return `${value.slice(6, 8)}.${value.slice(4, 6)}.${value.slice(0, 4)} ${value.slice(8, 10)}:${value.slice(10, 12)}:${value.slice(12, 14)}` }
function normalize(value) { return String(value || '').trim().toLocaleLowerCase() }
