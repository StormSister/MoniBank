import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  ArrowDownAZ,
  Eye,
  Plus,
  RefreshCw,
  Search,
  UserCheck,
  Users,
  UserX,
  X,
} from 'lucide-react'
import { useChangeCustomerStatus, useCreateCustomer, useCustomers } from '../hooks/useCustomers.js'
import Button from '../components/ui/Button.jsx'
import { Input, Select } from '../components/ui/FormField.jsx'
import FormField from '../components/ui/FormField.jsx'
import Modal from '../components/ui/Modal.jsx'
import Notice from '../components/ui/Notice.jsx'
import Panel from '../components/ui/Panel.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'

const INITIAL_FORM = {
  countryCode: 'PL',
  nationalId: '',
  firstName: '',
  lastName: '',
  dateOfBirth: '',
}

const NAME_PATTERN = /^[A-Za-z][A-Za-z .'-]*$/

export default function CustomersPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const customersQuery = useCustomers()
  const createCustomer = useCreateCustomer()
  const changeStatus = useChangeCustomerStatus()
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [sort, setSort] = useState('LAST_NAME_ASC')
  const [createOpen, setCreateOpen] = useState(false)
  const [selectedCustomerId, setSelectedCustomerId] = useState(null)
  const [statusCustomerId, setStatusCustomerId] = useState(null)
  const [successMessage, setSuccessMessage] = useState('')

  const customers = customersQuery.data || []
  const selectedCustomer = customers.find((customer) => customer.customerId === selectedCustomerId)
  const statusCustomer = customers.find((customer) => customer.customerId === statusCustomerId)

  const visibleCustomers = useMemo(() => {
    const phrase = normalize(search)

    return customers
      .filter((customer) => status === 'ALL' || customer.status === status)
      .filter((customer) => {
        if (!phrase) return true
        return normalize([
          customer.firstName,
          customer.lastName,
          customer.customerId,
          customer.nationalId,
        ].join(' ')).includes(phrase)
      })
      .toSorted(customerComparator(sort))
  }, [customers, search, sort, status])

  const activeCount = customers.filter((customer) => customer.status === 'A').length
  const inactiveCount = customers.filter((customer) => customer.status === 'I').length
  const filtersActive = search.trim() || status !== 'ALL' || sort !== 'LAST_NAME_ASC'

  useEffect(() => {
    if (searchParams.get('action') !== 'new') return

    createCustomer.reset()
    setCreateOpen(true)
    const nextParams = new URLSearchParams(searchParams)
    nextParams.delete('action')
    setSearchParams(nextParams, { replace: true })
  }, [searchParams, setSearchParams])

  const openCreate = () => {
    createCustomer.reset()
    setCreateOpen(true)
  }

  const clearFilters = () => {
    setSearch('')
    setStatus('ALL')
    setSort('LAST_NAME_ASC')
  }

  const confirmStatusChange = () => {
    if (!statusCustomer) return

    const nextStatus = statusCustomer.status === 'A' ? 'I' : 'A'
    changeStatus.mutate(
      { customerId: statusCustomer.customerId, status: nextStatus },
      {
        onSuccess: (updated) => {
          setStatusCustomerId(null)
          setSuccessMessage(
            `${updated.firstName} ${updated.lastName} is now ${updated.status === 'A' ? 'active' : 'inactive'}.`,
          )
        },
      },
    )
  }

  return (
    <div className="mx-auto max-w-[1220px] space-y-4">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-mb-gold-light">Customer registry</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight text-mb-text">Customers</h1>
          <p className="mt-1 text-sm text-mb-muted">Customer records maintained by the MVS core system.</p>
        </div>
        <Button variant="primary" onClick={openCreate}><Plus size={17} /> New customer</Button>
      </header>

      {successMessage && <Notice tone="success" onClose={() => setSuccessMessage('')}>{successMessage}</Notice>}

      <div className="grid gap-3 sm:grid-cols-3">
        <SummaryCard label="All customers" value={customers.length} icon={Users} tone="teal" loading={customersQuery.isLoading} />
        <SummaryCard label="Active" value={activeCount} icon={UserCheck} tone="green" loading={customersQuery.isLoading} />
        <SummaryCard label="Inactive" value={inactiveCount} icon={UserX} tone="gold" loading={customersQuery.isLoading} />
      </div>

      <Panel>
        <div className="grid gap-3 border-b border-mb-border p-4 lg:grid-cols-[minmax(260px,1fr)_190px_210px_auto]">
          <label className="relative block">
            <span className="sr-only">Search customers</span>
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={17} />
            <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, customer ID or national ID" className="pl-10 pr-10" />
            {search && (
              <button type="button" onClick={() => setSearch('')} className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-mb-muted hover:bg-white/5 hover:text-mb-text" aria-label="Clear search">
                <X size={15} />
              </button>
            )}
          </label>

          <Select aria-label="Filter by status" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="A">Active</option>
            <option value="I">Inactive</option>
          </Select>

          <label className="relative block">
            <ArrowDownAZ className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-mb-muted" size={17} />
            <Select aria-label="Sort customers" value={sort} onChange={(event) => setSort(event.target.value)} className="pl-10">
              <option value="LAST_NAME_ASC">Last name A–Z</option>
              <option value="LAST_NAME_DESC">Last name Z–A</option>
              <option value="ID_ASC">Customer ID ascending</option>
              <option value="ID_DESC">Customer ID descending</option>
            </Select>
          </label>

          <Button variant="ghost" onClick={clearFilters} disabled={!filtersActive}>Clear filters</Button>
        </div>

        {customersQuery.isError ? (
          <div className="p-6">
            <Notice>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span>Customers could not be loaded. {customersQuery.error?.message}</span>
                <Button variant="danger" onClick={() => customersQuery.refetch()} disabled={customersQuery.isFetching}>
                  <RefreshCw size={15} className={customersQuery.isFetching ? 'animate-spin' : ''} /> Retry
                </Button>
              </div>
            </Notice>
          </div>
        ) : (
          <CustomersTable
            customers={visibleCustomers}
            loading={customersQuery.isLoading}
            onView={(customer) => setSelectedCustomerId(customer.customerId)}
            onChangeStatus={(customer) => {
              changeStatus.reset()
              setStatusCustomerId(customer.customerId)
            }}
          />
        )}

        {!customersQuery.isLoading && !customersQuery.isError && (
          <footer className="flex flex-wrap items-center justify-between gap-2 border-t border-mb-border px-4 py-3 text-sm text-mb-muted">
            <span>Showing <b className="font-semibold text-mb-text">{visibleCustomers.length}</b> of {customers.length}</span>
            <span>Source: LISTCUST / VSAM</span>
          </footer>
        )}
      </Panel>

      <CreateCustomerModal
        open={createOpen}
        mutation={createCustomer}
        onClose={() => !createCustomer.isPending && setCreateOpen(false)}
        onCreated={(customer) => {
          setCreateOpen(false)
          setSuccessMessage(`Customer ${customer.customerId} was created successfully.`)
          setSelectedCustomerId(customer.customerId)
        }}
      />

      <CustomerDetailsModal customer={selectedCustomer} onClose={() => setSelectedCustomerId(null)} onChangeStatus={() => {
        changeStatus.reset()
        setSelectedCustomerId(null)
        setStatusCustomerId(selectedCustomer?.customerId || null)
      }} />

      <ChangeStatusModal
        customer={statusCustomer}
        mutation={changeStatus}
        onClose={() => !changeStatus.isPending && setStatusCustomerId(null)}
        onConfirm={confirmStatusChange}
      />
    </div>
  )
}

function SummaryCard({ label, value, icon: Icon, tone, loading }) {
  const tones = {
    teal: 'bg-mb-teal/12 text-mb-teal',
    green: 'bg-mb-terminal/10 text-mb-terminal',
    gold: 'bg-mb-gold/12 text-mb-gold-light',
  }

  return (
    <div className="mb-panel flex items-center justify-between gap-4 p-4">
      <div><p className="text-sm text-mb-muted">{label}</p><p className="mt-1 text-2xl font-semibold">{loading ? '—' : value}</p></div>
      <span className={`grid size-10 place-items-center rounded-full ${tones[tone]}`}><Icon size={19} /></span>
    </div>
  )
}

function CustomersTable({ customers, loading, onView, onChangeStatus }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[920px] text-left text-sm">
        <thead className="bg-white/[0.025] text-xs uppercase tracking-wide text-mb-muted">
          <tr>
            {['Customer', 'Customer ID', 'National ID', 'Date of birth', 'Created', 'Status', 'Actions'].map((heading) => (
              <th key={heading} className="px-4 py-3 font-medium">{heading}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading && Array.from({ length: 5 }, (_, index) => <CustomerSkeleton key={index} />)}
          {!loading && customers.length === 0 && (
            <tr><td colSpan="7" className="px-4 py-14 text-center text-mb-muted">No customers match the selected filters.</td></tr>
          )}
          {customers.map((customer) => (
            <tr key={customer.customerId} className="border-t border-mb-border/80 transition hover:bg-white/[0.025]">
              <td className="px-4 py-3.5">
                <button type="button" onClick={() => onView(customer)} className="text-left font-semibold text-mb-text hover:text-mb-gold-light">
                  {customer.firstName} {customer.lastName}
                </button>
                <div className="mt-1 text-xs text-mb-muted">{customer.countryCode}</div>
              </td>
              <td className="px-4 py-3.5 font-mono text-xs text-mb-teal">{customer.customerId}</td>
              <td className="px-4 py-3.5 font-mono text-xs text-mb-muted">{customer.nationalId}</td>
              <td className="whitespace-nowrap px-4 py-3.5 text-mb-muted">{formatDate(customer.dateOfBirth)}</td>
              <td className="whitespace-nowrap px-4 py-3.5 text-mb-muted">{formatDateTime(customer.createdAt)}</td>
              <td className="px-4 py-3.5"><CustomerStatus status={customer.status} /></td>
              <td className="px-4 py-3.5">
                <div className="flex items-center gap-1">
                  <IconButton label={`View ${customer.firstName} ${customer.lastName}`} onClick={() => onView(customer)}><Eye size={16} /></IconButton>
                  <IconButton label={customer.status === 'A' ? 'Deactivate customer' : 'Activate customer'} onClick={() => onChangeStatus(customer)} danger={customer.status === 'A'}>
                    {customer.status === 'A' ? <UserX size={16} /> : <UserCheck size={16} />}
                  </IconButton>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function CustomerSkeleton() {
  return (
    <tr className="border-t border-mb-border/80">
      {Array.from({ length: 7 }, (_, index) => <td key={index} className="px-4 py-4"><span className="block h-4 animate-pulse rounded bg-white/6" /></td>)}
    </tr>
  )
}

function IconButton({ label, onClick, danger = false, children }) {
  return (
    <button type="button" onClick={onClick} aria-label={label} title={label} className={`rounded-lg border p-2 transition ${danger ? 'border-transparent text-mb-muted hover:border-mb-danger/25 hover:bg-mb-danger/8 hover:text-mb-danger' : 'border-transparent text-mb-muted hover:border-mb-teal/20 hover:bg-mb-teal/8 hover:text-mb-teal'}`}>
      {children}
    </button>
  )
}

function CreateCustomerModal({ open, mutation, onClose, onCreated }) {
  const [form, setForm] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState({})

  const update = (field) => (event) => {
    let value = event.target.value
    if (field === 'countryCode') value = value.toUpperCase().replace(/[^A-Z]/g, '').slice(0, 2)
    if (field === 'nationalId') value = value.replace(/\D/g, '').slice(0, 11)
    setForm((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: undefined }))
  }

  const submit = (event) => {
    event.preventDefault()
    const nextErrors = validateCustomer(form)
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length > 0) return

    mutation.mutate(
      { ...form, dateOfBirth: form.dateOfBirth.replaceAll('-', '') },
      {
        onSuccess: (customer) => {
          setForm(INITIAL_FORM)
          setErrors({})
          onCreated(customer)
        },
      },
    )
  }

  return (
    <Modal open={open} onClose={onClose} title="Create customer" description="The customer ID will be generated by the mainframe." size="lg">
      <form onSubmit={submit} noValidate>
        <div className="grid gap-5 p-5 sm:grid-cols-2">
          {mutation.isError && <div className="sm:col-span-2"><Notice>Customer could not be created. {mutation.error?.message}</Notice></div>}

          <FormField id="countryCode" label="Country code" error={errors.countryCode} hint="Two-letter ISO code, for example PL.">
            <Input id="countryCode" value={form.countryCode} onChange={update('countryCode')} maxLength="2" autoComplete="country" />
          </FormField>
          <FormField id="nationalId" label="National ID" error={errors.nationalId} hint="Exactly 11 digits.">
            <Input id="nationalId" value={form.nationalId} onChange={update('nationalId')} inputMode="numeric" maxLength="11" autoComplete="off" />
          </FormField>
          <FormField id="firstName" label="First name" error={errors.firstName}>
            <Input id="firstName" value={form.firstName} onChange={update('firstName')} maxLength="30" autoComplete="given-name" />
          </FormField>
          <FormField id="lastName" label="Last name" error={errors.lastName}>
            <Input id="lastName" value={form.lastName} onChange={update('lastName')} maxLength="40" autoComplete="family-name" />
          </FormField>
          <FormField id="dateOfBirth" label="Date of birth" error={errors.dateOfBirth}>
            <Input id="dateOfBirth" type="date" value={form.dateOfBirth} onChange={update('dateOfBirth')} max={today()} autoComplete="bday" />
          </FormField>
        </div>
        <footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4">
          <Button type="button" variant="ghost" onClick={onClose} disabled={mutation.isPending}>Cancel</Button>
          <Button type="submit" variant="primary" disabled={mutation.isPending}>
            {mutation.isPending && <RefreshCw className="animate-spin" size={16} />}
            {mutation.isPending ? 'Creating in MVS…' : 'Create customer'}
          </Button>
        </footer>
      </form>
    </Modal>
  )
}

function CustomerDetailsModal({ customer, onClose, onChangeStatus }) {
  return (
    <Modal open={Boolean(customer)} onClose={onClose} title="Customer details" description={customer?.customerId}>
      {customer && (
        <>
          <div className="space-y-5 p-5">
            <div className="flex items-center justify-between gap-4 rounded-lg border border-mb-border bg-white/[0.02] p-4">
              <div><p className="text-xs uppercase tracking-wide text-mb-muted">Customer</p><p className="mt-1 text-lg font-semibold">{customer.firstName} {customer.lastName}</p></div>
              <CustomerStatus status={customer.status} />
            </div>
            <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
              <Detail label="Customer ID" value={customer.customerId} mono />
              <Detail label="National ID" value={customer.nationalId} mono />
              <Detail label="Country" value={customer.countryCode} />
              <Detail label="Date of birth" value={formatDate(customer.dateOfBirth)} />
              <Detail label="Created at" value={formatDateTime(customer.createdAt)} />
              <Detail label="Record source" value="MBANK.CUST / VSAM" />
            </dl>
          </div>
          <footer className="flex flex-wrap justify-end gap-3 border-t border-mb-border px-5 py-4">
            <Button variant="ghost" onClick={onClose}>Close</Button>
            <Button variant={customer.status === 'A' ? 'danger' : 'secondary'} onClick={onChangeStatus}>
              {customer.status === 'A' ? <UserX size={16} /> : <UserCheck size={16} />}
              {customer.status === 'A' ? 'Deactivate' : 'Activate'}
            </Button>
          </footer>
        </>
      )}
    </Modal>
  )
}

function ChangeStatusModal({ customer, mutation, onClose, onConfirm }) {
  if (!customer) return null
  const activating = customer.status !== 'A'

  return (
    <Modal open onClose={onClose} title={activating ? 'Activate customer?' : 'Deactivate customer?'} description={`${customer.firstName} ${customer.lastName} · ${customer.customerId}`} size="sm">
      <div className="space-y-4 p-5">
        {mutation.isError && <Notice>Status could not be changed. {mutation.error?.message}</Notice>}
        <p className="text-sm leading-6 text-mb-muted">
          {activating
            ? 'The customer will be marked as active in the mainframe customer file.'
            : 'The customer will be marked as inactive. The record will remain available for audit and lookup.'}
        </p>
      </div>
      <footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4">
        <Button variant="ghost" onClick={onClose} disabled={mutation.isPending}>Cancel</Button>
        <Button variant={activating ? 'primary' : 'danger'} onClick={onConfirm} disabled={mutation.isPending}>
          {mutation.isPending && <RefreshCw className="animate-spin" size={16} />}
          {mutation.isPending ? 'Updating MVS…' : activating ? 'Activate customer' : 'Deactivate customer'}
        </Button>
      </footer>
    </Modal>
  )
}

function CustomerStatus({ status }) {
  return <StatusBadge variant={status === 'A' ? 'success' : 'neutral'}>{status === 'A' ? 'Active' : 'Inactive'}</StatusBadge>
}

function Detail({ label, value, mono = false }) {
  return <div><dt className="text-xs uppercase tracking-wide text-mb-muted">{label}</dt><dd className={`mt-1.5 text-sm text-mb-text ${mono ? 'font-mono' : ''}`}>{value || '—'}</dd></div>
}

function validateCustomer(form) {
  const errors = {}
  if (!/^[A-Z]{2}$/.test(form.countryCode)) errors.countryCode = 'Enter exactly two capital letters.'
  if (!/^\d{11}$/.test(form.nationalId)) errors.nationalId = 'National ID must contain exactly 11 digits.'
  if (!form.firstName.trim() || form.firstName.length > 30 || !NAME_PATTERN.test(form.firstName)) errors.firstName = 'Use 1–30 letters and the characters space, apostrophe, hyphen or dot.'
  if (!form.lastName.trim() || form.lastName.length > 40 || !NAME_PATTERN.test(form.lastName)) errors.lastName = 'Use 1–40 letters and the characters space, apostrophe, hyphen or dot.'
  if (!form.dateOfBirth) errors.dateOfBirth = 'Date of birth is required.'
  else if (form.dateOfBirth > today()) errors.dateOfBirth = 'Date of birth cannot be in the future.'
  return errors
}

function customerComparator(sort) {
  if (sort === 'ID_ASC') return (a, b) => a.customerId.localeCompare(b.customerId)
  if (sort === 'ID_DESC') return (a, b) => b.customerId.localeCompare(a.customerId)
  const direction = sort === 'LAST_NAME_DESC' ? -1 : 1
  return (a, b) => direction * (`${a.lastName} ${a.firstName}`).localeCompare(`${b.lastName} ${b.firstName}`, 'en', { sensitivity: 'base' })
}

function normalize(value) {
  return String(value || '').trim().toLocaleLowerCase()
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

function formatDate(value) {
  if (!/^\d{8}$/.test(value || '')) return value || '—'
  return `${value.slice(6, 8)}.${value.slice(4, 6)}.${value.slice(0, 4)}`
}

function formatDateTime(value) {
  if (!/^\d{14}$/.test(value || '')) return value || '—'
  return `${formatDate(value.slice(0, 8))} ${value.slice(8, 10)}:${value.slice(10, 12)}`
}
