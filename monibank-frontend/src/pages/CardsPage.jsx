import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { CreditCard, Eye, Plus, RefreshCw, Search, ShieldCheck, ShieldX, WalletCards, X } from 'lucide-react'
import Button from '../components/ui/Button.jsx'
import FormField, { Input, Select } from '../components/ui/FormField.jsx'
import Modal from '../components/ui/Modal.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import StatusChangeModal from '../components/ui/StatusChangeModal.jsx'
import { useAccounts } from '../hooks/useAccounts.js'
import { useCards, useChangeCardStatus, useCreateCard } from '../hooks/useCards.js'

export default function CardsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const cardsQuery = useCards()
  const accountsQuery = useAccounts()
  const createCard = useCreateCard()
  const changeStatus = useChangeCardStatus()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [sort, setSort] = useState('ID_ASC')
  const [createOpen, setCreateOpen] = useState(false)
  const [detailsId, setDetailsId] = useState(null)
  const [statusId, setStatusId] = useState(null)
  const [success, setSuccess] = useState('')

  useEffect(() => {
    if (searchParams.get('action') !== 'new') return

    createCard.reset()
    setCreateOpen(true)
    const nextParams = new URLSearchParams(searchParams)
    nextParams.delete('action')
    setSearchParams(nextParams, { replace: true })
  }, [searchParams, setSearchParams])

  const cards = cardsQuery.data || []
  const accounts = accountsQuery.data || []
  const accountById = new Map(accounts.map((account) => [account.accountId, account]))
  const details = cards.find((card) => card.cardId === detailsId)
  const statusCard = cards.find((card) => card.cardId === statusId)
  const visibleCards = useMemo(() => {
    const phrase = normalize(search)
    return cards
      .filter((card) => status === 'ALL' || card.status === status)
      .filter((card) => !phrase || normalize(`${card.cardId} ${card.accountId} ${card.customerId} ${card.cardNumber} ${lastFour(card.cardNumber)}`).includes(phrase))
      .toSorted(cardComparator(sort))
  }, [cards, search, sort, status])

  const confirmStatus = () => {
    if (!statusCard) return
    const nextStatus = statusCard.status === 'A' ? 'I' : 'A'
    changeStatus.mutate({ cardId: statusCard.cardId, status: nextStatus }, {
      onSuccess: (updated) => {
        setStatusId(null)
        setSuccess(`Card ending ${lastFour(updated.cardNumber)} is now ${updated.status === 'A' ? 'active' : 'inactive'}.`)
      },
    })
  }

  return (
    <div className="mx-auto max-w-[1220px] space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div><p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">Card registry</p><h1 className="mt-1 text-2xl font-semibold">Cards</h1><p className="mt-1 text-sm text-mb-muted">Payment cards linked to existing core accounts.</p></div>
        <Button variant="primary" onClick={() => setCreateOpen(true)}><Plus size={17} /> Issue card</Button>
      </header>
      {success && <Notice tone="success" onClose={() => setSuccess('')}>{success}</Notice>}

      <div className="grid gap-3 sm:grid-cols-3">
        <Summary label="All cards" value={cards.length} icon={WalletCards} tone="teal" loading={cardsQuery.isLoading} />
        <Summary label="Active" value={cards.filter((card) => card.status === 'A').length} icon={ShieldCheck} tone="green" loading={cardsQuery.isLoading} />
        <Summary label="Inactive" value={cards.filter((card) => card.status !== 'A').length} icon={ShieldX} tone="gold" loading={cardsQuery.isLoading} />
      </div>

      <Panel>
        <div className="grid gap-3 border-b border-mb-border p-4 lg:grid-cols-[minmax(260px,1fr)_190px_210px_auto]">
          <label className="relative block"><Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={17} /><Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search card, account or customer" className="pl-10 pr-10" />{search && <button type="button" onClick={() => setSearch('')} className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-mb-muted hover:text-mb-text"><X size={15} /></button>}</label>
          <Select aria-label="Filter card status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="A">Active</option><option value="I">Inactive</option></Select>
          <Select aria-label="Sort cards" value={sort} onChange={(event) => setSort(event.target.value)}><option value="ID_ASC">Card ID ascending</option><option value="ID_DESC">Card ID descending</option><option value="EXPIRY_ASC">Expiry date ascending</option><option value="LIMIT_DESC">Highest daily limit</option></Select>
          <Button variant="ghost" disabled={!search && status === 'ALL' && sort === 'ID_ASC'} onClick={() => { setSearch(''); setStatus('ALL'); setSort('ID_ASC') }}>Clear filters</Button>
        </div>

        {cardsQuery.isError ? <LoadError query={cardsQuery} /> : <CardsTable cards={visibleCards} accounts={accountById} loading={cardsQuery.isLoading} onView={(card) => setDetailsId(card.cardId)} onStatus={(card) => { changeStatus.reset(); setStatusId(card.cardId) }} />}
        {!cardsQuery.isLoading && !cardsQuery.isError && <footer className="flex justify-between gap-3 border-t border-mb-border px-4 py-3 text-sm text-mb-muted"><span>Showing <b className="text-mb-text">{visibleCards.length}</b> of {cards.length}</span><span>Source: LISTCARD / VSAM</span></footer>}
      </Panel>

      <CreateCardModal open={createOpen} accountsQuery={accountsQuery} mutation={createCard} onClose={() => setCreateOpen(false)} onCreated={(card) => { setCreateOpen(false); setSuccess(`Card ${card.cardId} was issued successfully.`); setDetailsId(card.cardId) }} />
      <CardDetailsModal card={details} account={details ? accountById.get(details.accountId) : null} onClose={() => setDetailsId(null)} onStatus={() => { changeStatus.reset(); setDetailsId(null); setStatusId(details?.cardId || null) }} />
      <StatusChangeModal open={Boolean(statusCard)} active={statusCard?.status === 'A'} entityName="card" entityId={statusCard ? `${maskCard(statusCard.cardNumber)} · ${statusCard.cardId}` : ''} pending={changeStatus.isPending} error={changeStatus.error} onClose={() => !changeStatus.isPending && setStatusId(null)} onConfirm={confirmStatus} inactiveExplanation="The card remains linked to its account and available for audit." />
    </div>
  )
}

function CardsTable({ cards, accounts, loading, onView, onStatus }) {
  return <div className="overflow-x-auto"><table className="w-full min-w-[1060px] text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase tracking-wide text-mb-muted"><tr>{['Card', 'Account', 'Customer', 'Type / network', 'Expiry', 'Daily usage', 'Status', 'Actions'].map((heading) => <th key={heading} className="px-4 py-3 font-medium">{heading}</th>)}</tr></thead><tbody>
    {loading && Array.from({ length: 4 }, (_, row) => <tr key={row} className="border-t border-mb-border/80">{Array.from({ length: 8 }, (_, cell) => <td key={cell} className="px-4 py-4"><span className="block h-4 animate-pulse rounded bg-white/6" /></td>)}</tr>)}
    {!loading && cards.length === 0 && <tr><td colSpan="8" className="px-4 py-14 text-center text-mb-muted">No cards match the selected filters.</td></tr>}
    {cards.map((card) => { const account = accounts.get(card.accountId); return <tr key={card.cardId} className="border-t border-mb-border/80 hover:bg-white/[0.025]"><td className="px-4 py-3.5"><button onClick={() => onView(card)} className="font-mono text-sm font-semibold text-mb-text hover:text-mb-gold-light">{maskCard(card.cardNumber)}</button><div className="mt-1 font-mono text-[11px] text-mb-teal">{card.cardId}</div></td><td className="px-4 py-3.5"><span className="font-mono text-xs">{card.accountId}</span>{account?.iban && <div className="mt-1 max-w-40 truncate font-mono text-[10px] text-mb-muted" title={account.iban}>{account.iban}</div>}</td><td className="px-4 py-3.5 font-mono text-xs">{card.customerId}</td><td className="px-4 py-3.5"><b>{card.type || '—'}</b><div className="mt-1 text-xs text-mb-muted">{card.network || '—'}</div></td><td className="whitespace-nowrap px-4 py-3.5">{formatExpiry(card.expiry)}</td><td className="whitespace-nowrap px-4 py-3.5"><b>{money(card.dailySpent)}</b><span className="text-mb-muted"> / {money(card.dailyLimit)}</span></td><td className="px-4 py-3.5"><EntityStatus status={card.status} /></td><td className="px-4 py-3.5"><div className="flex"><IconButton label="View card" onClick={() => onView(card)}><Eye size={16} /></IconButton><IconButton label={card.status === 'A' ? 'Deactivate card' : 'Activate card'} danger={card.status === 'A'} onClick={() => onStatus(card)}>{card.status === 'A' ? <ShieldX size={16} /> : <ShieldCheck size={16} />}</IconButton></div></td></tr> })}
  </tbody></table></div>
}

function CreateCardModal({ open, accountsQuery, mutation, onClose, onCreated }) {
  const [form, setForm] = useState({ accountId: '', dailyLimit: '1000.00', expiry: defaultExpiry() })
  const [errors, setErrors] = useState({})
  const activeAccounts = (accountsQuery.data || []).filter((account) => account.status === 'A')
  const selected = activeAccounts.find((account) => account.accountId === form.accountId)

  useEffect(() => { if (open) { setForm({ accountId: '', dailyLimit: '1000.00', expiry: defaultExpiry() }); setErrors({}); mutation.reset() } }, [open])

  const update = (field, value) => { setForm((current) => ({ ...current, [field]: value })); setErrors((current) => ({ ...current, [field]: undefined })); mutation.reset() }
  const submit = (event) => {
    event.preventDefault()
    const nextErrors = validateCard(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return
    mutation.mutate({ accountId: form.accountId, dailyLimit: Number(form.dailyLimit), expiry: form.expiry.replace('-', '') }, { onSuccess: onCreated })
  }

  return <Modal open={open} onClose={() => !mutation.isPending && onClose()} title="Issue card" description="A card can only be issued for an existing active account." size="lg"><form onSubmit={submit} noValidate><div className="space-y-5 p-5">
    {mutation.isError && <Notice>Card could not be issued. {mutation.error?.message}</Notice>}
    {accountsQuery.isError && <Notice>Accounts could not be loaded. {accountsQuery.error?.message}</Notice>}
    <FormField id="card-account" label="Active account" error={errors.accountId} hint="The card will inherit the account customer and currency."><Select id="card-account" value={form.accountId} onChange={(event) => update('accountId', event.target.value)} disabled={accountsQuery.isLoading}><option value="">Select account…</option>{activeAccounts.map((account) => <option key={account.accountId} value={account.accountId}>{account.accountId} · {account.iban} · {account.customerId}</option>)}</Select></FormField>
    {!accountsQuery.isLoading && !accountsQuery.isError && activeAccounts.length === 0 && <Notice>No active account is available. Open or activate an account first.</Notice>}
    {selected && <div className="grid gap-3 rounded-lg border border-mb-teal/20 bg-mb-teal/5 p-4 text-sm sm:grid-cols-3"><Mini label="Customer" value={selected.customerId} /><Mini label="Balance" value={`${money(selected.balance)} ${selected.currency}`} /><Mini label="Account type" value={selected.type === 'OD' ? 'Overdraft' : 'Standard'} /></div>}
    <div className="grid gap-5 sm:grid-cols-2"><FormField id="card-limit" label="Daily limit" error={errors.dailyLimit} hint="Positive amount with up to two decimal places."><Input id="card-limit" type="number" min="0.01" step="0.01" value={form.dailyLimit} onChange={(event) => update('dailyLimit', event.target.value)} /></FormField><FormField id="card-expiry" label="Expiry month" error={errors.expiry} hint="The current month or a future month."><Input id="card-expiry" type="month" min={currentMonth()} value={form.expiry} onChange={(event) => update('expiry', event.target.value)} /></FormField></div>
    <p className="text-xs text-mb-muted">Card ID, number, type and network are assigned by the mainframe. Card numbers stay masked in the interface.</p>
  </div><footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4"><Button type="button" variant="ghost" onClick={onClose} disabled={mutation.isPending}>Cancel</Button><Button type="submit" variant="primary" disabled={mutation.isPending || accountsQuery.isLoading}>{mutation.isPending && <RefreshCw className="animate-spin" size={16} />}{mutation.isPending ? 'Issuing in MVS…' : 'Issue card'}</Button></footer></form></Modal>
}

function CardDetailsModal({ card, account, onClose, onStatus }) { return <Modal open={Boolean(card)} onClose={onClose} title="Card details" description={card?.cardId}>{card && <><div className="space-y-5 p-5"><div className="relative overflow-hidden rounded-xl border border-mb-gold/25 bg-[linear-gradient(135deg,#132b38,#091820)] p-5"><CreditCard className="absolute -bottom-4 -right-2 text-mb-gold/8" size={110} /><p className="text-xs uppercase tracking-[0.18em] text-mb-gold-light">{card.network || 'Payment card'}</p><p className="mt-5 font-mono text-xl tracking-[0.16em]">{maskCard(card.cardNumber)}</p><div className="mt-5 flex justify-between text-xs text-mb-muted"><span>{card.type || '—'}</span><span>EXPIRES {formatExpiry(card.expiry)}</span></div></div><div className="flex justify-end"><EntityStatus status={card.status} /></div><dl className="grid gap-4 sm:grid-cols-2"><Detail label="Account ID" value={card.accountId} mono /><Detail label="Customer ID" value={card.customerId} mono /><Detail label="IBAN" value={account?.iban || 'Account data unavailable'} mono /><Detail label="Daily limit" value={money(card.dailyLimit)} /><Detail label="Spent today" value={money(card.dailySpent)} /><Detail label="Spent date" value={formatCompactDate(card.spentDate)} /><Detail label="Record source" value="MBANK.CARD / VSAM" /></dl></div><footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4"><Button variant="ghost" onClick={onClose}>Close</Button><Button variant={card.status === 'A' ? 'danger' : 'secondary'} onClick={onStatus}>{card.status === 'A' ? <ShieldX size={16} /> : <ShieldCheck size={16} />}{card.status === 'A' ? 'Deactivate' : 'Activate'}</Button></footer></>}</Modal> }

function Summary({ label, value, icon: Icon, tone, loading }) { const tones = { teal: 'bg-mb-teal/12 text-mb-teal', green: 'bg-mb-terminal/10 text-mb-terminal', gold: 'bg-mb-gold/12 text-mb-gold-light' }; return <div className="mb-panel flex items-center justify-between p-4"><div><p className="text-sm text-mb-muted">{label}</p><p className="mt-1 text-2xl font-semibold">{loading ? '—' : value}</p></div><span className={`grid size-10 place-items-center rounded-full ${tones[tone]}`}><Icon size={19} /></span></div> }
function LoadError({ query }) { return <div className="p-6"><Notice><div className="flex flex-wrap items-center justify-between gap-3"><span>Cards could not be loaded. {query.error?.message}</span><Button variant="danger" onClick={() => query.refetch()} disabled={query.isFetching}><RefreshCw className={query.isFetching ? 'animate-spin' : ''} size={15} /> Retry</Button></div></Notice></div> }
function EntityStatus({ status }) { return <StatusBadge variant={status === 'A' ? 'success' : 'neutral'}>{status === 'A' ? 'Active' : 'Inactive'}</StatusBadge> }
function IconButton({ label, onClick, danger, children }) { return <button type="button" onClick={onClick} aria-label={label} title={label} className={`rounded-lg p-2 text-mb-muted transition hover:bg-white/5 ${danger ? 'hover:text-mb-danger' : 'hover:text-mb-teal'}`}>{children}</button> }
function Mini({ label, value }) { return <div><p className="text-[10px] uppercase tracking-wide text-mb-muted">{label}</p><p className="mt-1 truncate font-mono text-xs text-mb-text">{value}</p></div> }
function Detail({ label, value, mono }) { return <div><dt className="text-xs uppercase tracking-wide text-mb-muted">{label}</dt><dd className={`mt-1.5 break-all text-sm ${mono ? 'font-mono' : ''}`}>{value ?? '—'}</dd></div> }
function validateCard(form) { const errors = {}; if (!/^A\d{12}$/.test(form.accountId)) errors.accountId = 'Select an active account.'; const amount = Number(form.dailyLimit); if (!form.dailyLimit || !Number.isFinite(amount) || amount <= 0 || !/^\d+(\.\d{1,2})?$/.test(form.dailyLimit)) errors.dailyLimit = 'Enter an amount greater than zero with up to two decimals.'; if (!/^\d{4}-\d{2}$/.test(form.expiry) || form.expiry < currentMonth()) errors.expiry = 'Choose the current month or a future month.'; return errors }
function cardComparator(sort) { if (sort === 'ID_DESC') return (a, b) => b.cardId.localeCompare(a.cardId); if (sort === 'EXPIRY_ASC') return (a, b) => String(a.expiry).localeCompare(String(b.expiry)); if (sort === 'LIMIT_DESC') return (a, b) => Number(b.dailyLimit) - Number(a.dailyLimit); return (a, b) => a.cardId.localeCompare(b.cardId) }
function currentMonth() { return new Date().toISOString().slice(0, 7) }
function defaultExpiry() { const date = new Date(); date.setFullYear(date.getFullYear() + 3); return date.toISOString().slice(0, 7) }
function maskCard(value) { const digits = String(value || '').replace(/\s/g, ''); return digits ? `•••• •••• •••• ${digits.slice(-4)}` : '•••• •••• •••• ••••' }
function lastFour(value) { return String(value || '').replace(/\s/g, '').slice(-4) || '—' }
function formatExpiry(value) { const compact = String(value || '').replace('-', ''); return /^\d{6}$/.test(compact) ? `${compact.slice(4, 6)}/${compact.slice(2, 4)}` : value || '—' }
function formatCompactDate(value) { const compact = String(value || '').replaceAll('-', ''); return /^\d{8}$/.test(compact) ? `${compact.slice(6, 8)}.${compact.slice(4, 6)}.${compact.slice(0, 4)}` : value || '—' }
function money(value) { const number = Number(value); return Number.isFinite(number) ? number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '—' }
function normalize(value) { return String(value || '').trim().toLocaleLowerCase() }
