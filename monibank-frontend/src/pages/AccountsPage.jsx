import { useEffect, useMemo, useState } from 'react'
import { ArrowDownAZ, Building2, CircleDollarSign, Eye, Plus, RefreshCw, Search, ShieldCheck, ShieldX, Users, X } from 'lucide-react'
import CustomerFormFields from '../components/customer/CustomerFormFields.jsx'
import Button from '../components/ui/Button.jsx'
import FormField, { Input, Select } from '../components/ui/FormField.jsx'
import Modal from '../components/ui/Modal.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import StatusChangeModal from '../components/ui/StatusChangeModal.jsx'
import { customerRequest, newCustomerForm, sanitizeCustomerField, validateCustomerForm } from '../features/customers/CustomerForm.js'
import { useAccounts, useChangeAccountStatus, useCreateAccount } from '../hooks/useAccounts.js'
import { useCreateCustomer, useCustomers } from '../hooks/useCustomers.js'

export default function AccountsPage() {
  const accountsQuery = useAccounts()
  const changeStatus = useChangeAccountStatus()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [type, setType] = useState('ALL')
  const [sort, setSort] = useState('ID_ASC')
  const [createOpen, setCreateOpen] = useState(false)
  const [detailsId, setDetailsId] = useState(null)
  const [statusId, setStatusId] = useState(null)
  const [success, setSuccess] = useState('')

  const accounts = accountsQuery.data || []
  const details = accounts.find((account) => account.accountId === detailsId)
  const statusAccount = accounts.find((account) => account.accountId === statusId)
  const visibleAccounts = useMemo(() => {
    const phrase = normalize(search)
    return accounts
      .filter((account) => status === 'ALL' || account.status === status)
      .filter((account) => type === 'ALL' || account.type === type)
      .filter((account) => !phrase || normalize(`${account.accountId} ${account.customerId} ${account.iban}`).includes(phrase))
      .toSorted(accountComparator(sort))
  }, [accounts, search, sort, status, type])

  const confirmStatus = () => {
    if (!statusAccount) return
    const nextStatus = statusAccount.status === 'A' ? 'I' : 'A'
    changeStatus.mutate({ accountId: statusAccount.accountId, status: nextStatus }, {
      onSuccess: (updated) => {
        setStatusId(null)
        setSuccess(`Account ${updated.accountId} is now ${updated.status === 'A' ? 'active' : 'inactive'}.`)
      },
    })
  }

  return (
    <div className="mx-auto max-w-[1220px] space-y-4">
      <PageHeader onCreate={() => setCreateOpen(true)} />
      {success && <Notice tone="success" onClose={() => setSuccess('')}>{success}</Notice>}

      <div className="grid gap-3 sm:grid-cols-3">
        <Summary label="All accounts" value={accounts.length} icon={Building2} tone="teal" loading={accountsQuery.isLoading} />
        <Summary label="Active" value={accounts.filter((item) => item.status === 'A').length} icon={ShieldCheck} tone="green" loading={accountsQuery.isLoading} />
        <Summary label="Overdraft accounts" value={accounts.filter((item) => item.type === 'OD').length} icon={CircleDollarSign} tone="gold" loading={accountsQuery.isLoading} />
      </div>

      <Panel>
        <div className="grid gap-3 border-b border-mb-border p-4 xl:grid-cols-[minmax(240px,1fr)_170px_170px_205px_auto]">
          <SearchBox value={search} onChange={setSearch} />
          <Select aria-label="Filter account status" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="ALL">All statuses</option><option value="A">Active</option><option value="I">Inactive</option>
          </Select>
          <Select aria-label="Filter account type" value={type} onChange={(event) => setType(event.target.value)}>
            <option value="ALL">All types</option><option value="ST">Standard</option><option value="OD">Overdraft</option>
          </Select>
          <label className="relative block">
            <ArrowDownAZ className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={17} />
            <Select aria-label="Sort accounts" value={sort} onChange={(event) => setSort(event.target.value)} className="pl-10">
              <option value="ID_ASC">Account ID ascending</option><option value="ID_DESC">Account ID descending</option>
              <option value="BALANCE_DESC">Highest balance</option><option value="BALANCE_ASC">Lowest balance</option>
            </Select>
          </label>
          <Button variant="ghost" disabled={!search && status === 'ALL' && type === 'ALL' && sort === 'ID_ASC'} onClick={() => { setSearch(''); setStatus('ALL'); setType('ALL'); setSort('ID_ASC') }}>Clear</Button>
        </div>

        {accountsQuery.isError ? <LoadError noun="accounts" query={accountsQuery} /> : (
          <AccountsTable accounts={visibleAccounts} loading={accountsQuery.isLoading} onView={(account) => setDetailsId(account.accountId)} onStatus={(account) => { changeStatus.reset(); setStatusId(account.accountId) }} />
        )}
        {!accountsQuery.isLoading && !accountsQuery.isError && <footer className="flex justify-between gap-3 border-t border-mb-border px-4 py-3 text-sm text-mb-muted"><span>Showing <b className="text-mb-text">{visibleAccounts.length}</b> of {accounts.length}</span><span>Source: LISTACCT / VSAM</span></footer>}
      </Panel>

      <CreateAccountWizard open={createOpen} onClose={() => setCreateOpen(false)} onCreated={(account) => { setCreateOpen(false); setSuccess(`Account ${account.accountId} was created successfully.`); setDetailsId(account.accountId) }} />
      <AccountDetailsModal account={details} onClose={() => setDetailsId(null)} onStatus={() => { changeStatus.reset(); setDetailsId(null); setStatusId(details?.accountId || null) }} />
      <StatusChangeModal open={Boolean(statusAccount)} active={statusAccount?.status === 'A'} entityName="account" entityId={statusAccount?.accountId || ''} pending={changeStatus.isPending} error={changeStatus.error} onClose={() => !changeStatus.isPending && setStatusId(null)} onConfirm={confirmStatus} inactiveExplanation="Posting operations should be rejected for an inactive account." />
    </div>
  )
}

function PageHeader({ onCreate }) {
  return <header className="flex flex-wrap items-end justify-between gap-4"><div><p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">Core accounts</p><h1 className="mt-1 text-2xl font-semibold">Accounts</h1><p className="mt-1 text-sm text-mb-muted">Balances and account records maintained by the MVS core system.</p></div><Button variant="primary" onClick={onCreate}><Plus size={17} /> Open account</Button></header>
}

function Summary({ label, value, icon: Icon, tone, loading }) {
  const tones = { teal: 'bg-mb-teal/12 text-mb-teal', green: 'bg-mb-terminal/10 text-mb-terminal', gold: 'bg-mb-gold/12 text-mb-gold-light' }
  return <div className="mb-panel flex items-center justify-between p-4"><div><p className="text-sm text-mb-muted">{label}</p><p className="mt-1 text-2xl font-semibold">{loading ? '—' : value}</p></div><span className={`grid size-10 place-items-center rounded-full ${tones[tone]}`}><Icon size={19} /></span></div>
}

function SearchBox({ value, onChange }) {
  return <label className="relative block"><Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={17} /><Input value={value} onChange={(event) => onChange(event.target.value)} placeholder="Search account, IBAN or customer" className="pl-10 pr-10" />{value && <button type="button" onClick={() => onChange('')} className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-mb-muted hover:text-mb-text"><X size={15} /></button>}</label>
}

function AccountsTable({ accounts, loading, onView, onStatus }) {
  return <div className="overflow-x-auto"><table className="w-full min-w-[1040px] text-left text-sm"><thead className="bg-white/[0.025] text-xs uppercase tracking-wide text-mb-muted"><tr>{['Account / IBAN', 'Customer', 'Type', 'Balance', 'Blocked', 'Overdraft', 'Status', 'Actions'].map((item) => <th key={item} className="px-4 py-3 font-medium">{item}</th>)}</tr></thead><tbody>
    {loading && Array.from({ length: 4 }, (_, index) => <tr key={index} className="border-t border-mb-border/80">{Array.from({ length: 8 }, (_, cell) => <td key={cell} className="px-4 py-4"><span className="block h-4 animate-pulse rounded bg-white/6" /></td>)}</tr>)}
    {!loading && accounts.length === 0 && <tr><td colSpan="8" className="px-4 py-14 text-center text-mb-muted">No accounts match the selected filters.</td></tr>}
    {accounts.map((account) => <tr key={account.accountId} className="border-t border-mb-border/80 hover:bg-white/[0.025]"><td className="px-4 py-3.5"><button onClick={() => onView(account)} className="font-mono text-xs font-semibold text-mb-teal hover:text-mb-gold-light">{account.accountId}</button><div className="mt-1 font-mono text-[11px] text-mb-muted">{account.iban}</div></td><td className="px-4 py-3.5 font-mono text-xs">{account.customerId}</td><td className="px-4 py-3.5">{accountType(account.type)}</td><td className="whitespace-nowrap px-4 py-3.5 font-semibold">{money(account.balance, account.currency)}</td><td className="whitespace-nowrap px-4 py-3.5 text-mb-muted">{money(account.blockedAmount, account.currency)}</td><td className="whitespace-nowrap px-4 py-3.5 text-mb-muted">{money(account.overdraftLimit, account.currency)}</td><td className="px-4 py-3.5"><EntityStatus status={account.status} /></td><td className="px-4 py-3.5"><div className="flex"><IconButton label="View account" onClick={() => onView(account)}><Eye size={16} /></IconButton><IconButton label={account.status === 'A' ? 'Deactivate account' : 'Activate account'} danger={account.status === 'A'} onClick={() => onStatus(account)}>{account.status === 'A' ? <ShieldX size={16} /> : <ShieldCheck size={16} />}</IconButton></div></td></tr>)}
  </tbody></table></div>
}

function CreateAccountWizard({ open, onClose, onCreated }) {
  const customersQuery = useCustomers()
  const createCustomer = useCreateCustomer()
  const createAccount = useCreateAccount()
  const [mode, setMode] = useState('existing')
  const [customerId, setCustomerId] = useState('')
  const [customerForm, setCustomerForm] = useState(newCustomerForm)
  const [customerErrors, setCustomerErrors] = useState({})
  const [accountForm, setAccountForm] = useState({ type: 'ST', overdraftLimit: '0.00' })
  const [accountErrors, setAccountErrors] = useState({})
  const [createdCustomer, setCreatedCustomer] = useState(null)
  const [flowError, setFlowError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const activeCustomers = (customersQuery.data || []).filter((customer) => customer.status === 'A')
  useEffect(() => {
    if (!open) return
    setMode('existing'); setCustomerId(''); setCustomerForm(newCustomerForm()); setCustomerErrors({}); setAccountForm({ type: 'ST', overdraftLimit: '0.00' }); setAccountErrors({}); setCreatedCustomer(null); setFlowError(''); setSubmitting(false); createCustomer.reset(); createAccount.reset()
  }, [open])

  const updateCustomer = (field, value) => { setCustomerForm((current) => ({ ...current, [field]: sanitizeCustomerField(field, value) })); setCustomerErrors((current) => ({ ...current, [field]: undefined })) }
  const updateAccount = (field, value) => { setAccountForm((current) => ({ ...current, [field]: value, ...(field === 'type' && value === 'ST' ? { overdraftLimit: '0.00' } : {}) })); setAccountErrors((current) => ({ ...current, [field]: undefined })); setFlowError('') }

  const submit = async (event) => {
    event.preventDefault()
    const errors = validateAccount(
      accountForm,
      mode === 'new' ? (createdCustomer?.customerId || 'CUSTOMER-WILL-BE-CREATED') : customerId,
    )
    setAccountErrors(errors)
    if (mode === 'new' && !createdCustomer) {
      const nextCustomerErrors = validateCustomerForm(customerForm)
      setCustomerErrors(nextCustomerErrors)
      if (Object.keys(nextCustomerErrors).length) return
    }
    if (Object.keys(errors).length) return

    setSubmitting(true); setFlowError('')
    let targetCustomer = createdCustomer
    try {
      if (mode === 'new' && !targetCustomer) {
        targetCustomer = await createCustomer.mutateAsync(customerRequest(customerForm))
        setCreatedCustomer(targetCustomer)
      }
      const account = await createAccount.mutateAsync({ customerId: mode === 'new' ? targetCustomer.customerId : customerId, type: accountForm.type, overdraftLimit: Number(accountForm.type === 'ST' ? 0 : accountForm.overdraftLimit) })
      onCreated(account)
    } catch (error) {
      setFlowError(targetCustomer ? `Customer ${targetCustomer.customerId} exists, but the account could not be created. ${error.message} You can retry the account without creating the customer again.` : `The operation could not be completed. ${error.message}`)
    } finally { setSubmitting(false) }
  }

  return <Modal open={open} onClose={() => !submitting && onClose()} title="Open account" description="Choose an existing customer or register a new customer first." size="lg"><form onSubmit={submit} noValidate>
    <div className="space-y-5 p-5">
      {flowError && <Notice>{flowError}</Notice>}
      {createdCustomer && <Notice tone="success">Customer <b>{createdCustomer.customerId}</b> was created. Only account creation will be retried.</Notice>}
      <div className="grid grid-cols-2 gap-3"><ModeButton selected={mode === 'existing'} onClick={() => { setMode('existing'); setCreatedCustomer(null); setFlowError('') }} icon={Users} title="Existing customer" subtitle="Select an active record" /><ModeButton selected={mode === 'new'} onClick={() => { setMode('new'); setFlowError('') }} icon={Plus} title="New customer" subtitle="Create customer first" /></div>
      {mode === 'existing' ? <div className="space-y-3"><FormField id="account-customer" label="Active customer" error={accountErrors.customerId} hint="Only active customers can be selected."><Select id="account-customer" value={customerId} onChange={(event) => { setCustomerId(event.target.value); setAccountErrors((current) => ({ ...current, customerId: undefined })) }} disabled={customersQuery.isLoading}><option value="">Select customer…</option>{activeCustomers.map((customer) => <option key={customer.customerId} value={customer.customerId}>{customer.lastName}, {customer.firstName} · {customer.customerId}</option>)}</Select></FormField>{customersQuery.isError && <Notice>Customers could not be loaded. {customersQuery.error?.message}</Notice>}{!customersQuery.isLoading && !customersQuery.isError && activeCustomers.length === 0 && <Notice>No active customer is available. Choose “New customer”.</Notice>}</div> : !createdCustomer && <div className="grid gap-5 rounded-xl border border-mb-border bg-white/[0.015] p-4 sm:grid-cols-2"><CustomerFormFields form={customerForm} errors={customerErrors} onChange={updateCustomer} idPrefix="account-new-customer" /></div>}
      <div className="grid gap-5 border-t border-mb-border pt-5 sm:grid-cols-2"><FormField id="account-type" label="Account type"><Select id="account-type" value={accountForm.type} onChange={(event) => updateAccount('type', event.target.value)}><option value="ST">Standard</option><option value="OD">Overdraft</option></Select></FormField><FormField id="account-overdraft" label="Overdraft limit" error={accountErrors.overdraftLimit} hint={accountForm.type === 'ST' ? 'Standard accounts use a zero overdraft limit.' : 'Non-negative amount, maximum two decimal places.'}><Input id="account-overdraft" type="number" min="0" step="0.01" value={accountForm.overdraftLimit} onChange={(event) => updateAccount('overdraftLimit', event.target.value)} disabled={accountForm.type === 'ST'} /></FormField></div>
      <p className="text-xs text-mb-muted">Account ID, IBAN and currency are assigned by the mainframe.</p>
    </div><footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4"><Button type="button" variant="ghost" onClick={onClose} disabled={submitting}>Cancel</Button><Button type="submit" variant="primary" disabled={submitting || (mode === 'existing' && customersQuery.isLoading)}>{submitting && <RefreshCw className="animate-spin" size={16} />}{submitting ? 'Processing in MVS…' : createdCustomer ? 'Retry account creation' : 'Open account'}</Button></footer>
  </form></Modal>
}

function ModeButton({ selected, onClick, icon: Icon, title, subtitle }) { return <button type="button" onClick={onClick} className={`flex items-center gap-3 rounded-xl border p-4 text-left transition ${selected ? 'border-mb-gold/60 bg-mb-gold/8' : 'border-mb-border bg-white/[0.015] hover:border-mb-teal/35'}`}><span className={`grid size-10 place-items-center rounded-lg ${selected ? 'bg-mb-gold/15 text-mb-gold-light' : 'bg-mb-teal/8 text-mb-teal'}`}><Icon size={19} /></span><span><b className="block text-sm">{title}</b><small className="text-mb-muted">{subtitle}</small></span></button> }

function AccountDetailsModal({ account, onClose, onStatus }) { return <Modal open={Boolean(account)} onClose={onClose} title="Account details" description={account?.accountId}>{account && <><div className="space-y-5 p-5"><div className="flex items-center justify-between gap-3 rounded-lg border border-mb-border bg-white/[0.02] p-4"><div><p className="font-mono text-xs text-mb-teal">{account.iban}</p><p className="mt-1 text-xl font-semibold">{money(account.balance, account.currency)}</p></div><EntityStatus status={account.status} /></div><dl className="grid gap-4 sm:grid-cols-2"><Detail label="Customer ID" value={account.customerId} mono /><Detail label="Account type" value={accountType(account.type)} /><Detail label="Blocked amount" value={money(account.blockedAmount, account.currency)} /><Detail label="Overdraft limit" value={money(account.overdraftLimit, account.currency)} /><Detail label="Available funds" value={money(Number(account.balance || 0) + Number(account.overdraftLimit || 0) - Number(account.blockedAmount || 0), account.currency)} /><Detail label="Record source" value="MBANK.ACCT / VSAM" /></dl></div><footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4"><Button variant="ghost" onClick={onClose}>Close</Button><Button variant={account.status === 'A' ? 'danger' : 'secondary'} onClick={onStatus}>{account.status === 'A' ? <ShieldX size={16} /> : <ShieldCheck size={16} />}{account.status === 'A' ? 'Deactivate' : 'Activate'}</Button></footer></>}</Modal> }

function EntityStatus({ status }) { return <StatusBadge variant={status === 'A' ? 'success' : 'neutral'}>{status === 'A' ? 'Active' : 'Inactive'}</StatusBadge> }
function Detail({ label, value, mono }) { return <div><dt className="text-xs uppercase tracking-wide text-mb-muted">{label}</dt><dd className={`mt-1.5 text-sm ${mono ? 'font-mono' : ''}`}>{value ?? '—'}</dd></div> }
function IconButton({ label, onClick, danger, children }) { return <button type="button" aria-label={label} title={label} onClick={onClick} className={`rounded-lg p-2 text-mb-muted transition hover:bg-white/5 ${danger ? 'hover:text-mb-danger' : 'hover:text-mb-teal'}`}>{children}</button> }
function LoadError({ noun, query }) { return <div className="p-6"><Notice><div className="flex flex-wrap items-center justify-between gap-3"><span>The {noun} could not be loaded. {query.error?.message}</span><Button variant="danger" onClick={() => query.refetch()} disabled={query.isFetching}><RefreshCw className={query.isFetching ? 'animate-spin' : ''} size={15} /> Retry</Button></div></Notice></div> }
function validateAccount(form, customerId) { const errors = {}; if (!customerId) errors.customerId = 'Select or create a customer.'; if (!['ST', 'OD'].includes(form.type)) errors.type = 'Choose a valid account type.'; const value = Number(form.overdraftLimit); if (form.type === 'OD' && (!form.overdraftLimit || !Number.isFinite(value) || value < 0 || !/^\d+(\.\d{1,2})?$/.test(form.overdraftLimit))) errors.overdraftLimit = 'Enter a non-negative amount with up to two decimals.'; return errors }
function accountComparator(sort) { if (sort === 'ID_DESC') return (a, b) => b.accountId.localeCompare(a.accountId); if (sort === 'BALANCE_DESC') return (a, b) => Number(b.balance) - Number(a.balance); if (sort === 'BALANCE_ASC') return (a, b) => Number(a.balance) - Number(b.balance); return (a, b) => a.accountId.localeCompare(b.accountId) }
function accountType(type) { return type === 'OD' ? 'Overdraft' : type === 'ST' ? 'Standard' : type || '—' }
function money(value, currency) { const number = Number(value); return Number.isFinite(number) ? `${number.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency || ''}`.trim() : '—' }
function normalize(value) { return String(value || '').trim().toLocaleLowerCase() }
