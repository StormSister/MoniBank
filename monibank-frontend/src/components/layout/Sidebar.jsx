import { NavLink } from 'react-router-dom'
import {
  BadgeDollarSign, BarChart3, BriefcaseBusiness, CreditCard, FileClock,
  FileText, Gauge, Landmark, ReceiptText, Settings, ShieldCheck, Users,
} from 'lucide-react'
import Brand from './Brand.jsx'

const items = [
  ['Dashboard', '/dashboard', Gauge],
  ['Customers', '/customers', Users],
  ['Accounts', '/accounts', BriefcaseBusiness],
  ['Cards', '/cards', CreditCard],
  ['Cash Desk', '/cash-desk', Landmark],
  ['Statements', '/statements', ReceiptText],
  ['Jobs', '/jobs', FileClock],
  ['Audit Log', '/audit-log', ShieldCheck],
  ['Reports', '/reports', BarChart3],
  ['Settings', '/settings', Settings],
]

export default function Sidebar({ open, onClose }) {
  return (
    <>
      {open && <button aria-label="Close navigation" onClick={onClose} className="fixed inset-0 z-30 bg-black/60 lg:hidden" />}
      <aside className={`fixed inset-y-0 left-0 z-40 flex w-62 flex-col border-r border-mb-border bg-[#061722]/97 px-4 py-6 transition-transform lg:static lg:translate-x-0 ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="px-1 pb-6"><Brand /></div>
        <nav className="flex-1 space-y-1 overflow-y-auto">
          {items.map(([label, path, Icon]) => (
            <NavLink
              key={path}
              to={path}
              onClick={onClose}
              className={({ isActive }) => `flex items-center gap-3 rounded-lg border px-3 py-2.5 text-sm transition ${isActive ? 'border-mb-teal/25 bg-mb-teal/12 text-mb-text shadow-[inset_3px_0_0_#2dd4bf]' : 'border-transparent text-mb-muted hover:bg-white/4 hover:text-mb-text'}`}
            >
              <Icon size={18} />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="space-y-3 border-t border-mb-border pt-4">
          <div className="flex items-center gap-3 rounded-xl border border-mb-border bg-mb-panel/55 p-3">
            <div className="grid size-10 place-items-center rounded-full bg-gradient-to-br from-mb-gold to-amber-700 font-bold text-mb-ink">M</div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-semibold">Monika</div>
              <div className="text-[11px] uppercase tracking-wider text-mb-teal">Operator</div>
            </div>
          </div>
          <button className="flex w-full items-center gap-3 rounded-lg border border-mb-border px-3 py-2.5 text-sm text-mb-gold-light transition hover:bg-mb-gold/8">
            <BadgeDollarSign size={18} /> Switch bank
          </button>
        </div>
      </aside>
    </>
  )
}
