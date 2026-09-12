import { NavLink } from 'react-router-dom'
import {
  BadgeDollarSign, BarChart3, BriefcaseBusiness, CreditCard, FileClock,
  Gauge, Landmark, ReceiptText, ServerCog, Users, X,
} from 'lucide-react'
import Brand from './Brand.jsx'

const sections = [
  {
    label: 'Core banking',
    items: [
      ['Dashboard', '/dashboard', Gauge],
      ['Customers', '/customers', Users],
      ['Accounts', '/accounts', BriefcaseBusiness],
      ['Cards', '/cards', CreditCard],
      ['Cash Desk', '/cash-desk', Landmark],
      ['Statements', '/statements', ReceiptText],
    ],
  },
  {
    label: 'Operations',
    items: [
      ['Jobs', '/jobs', FileClock],
      ['Reports', '/reports', BarChart3],
    ],
  },
  {
    label: 'System',
    items: [
      ['System Status', '/system-status', ServerCog],
    ],
  },
]

export default function Sidebar({ open, onClose }) {
  return (
    <>
      {open && <button aria-label="Close navigation" onClick={onClose} className="fixed inset-0 z-30 bg-black/60 lg:hidden" />}
      <aside className={`fixed inset-y-0 left-0 z-40 flex w-[min(19rem,86vw)] flex-col overflow-hidden border-r border-mb-gold/10 bg-[linear-gradient(180deg,rgba(6,23,34,.995),rgba(5,18,27,.995))] px-4 py-5 shadow-[18px_0_45px_rgba(0,0,0,.3)] transition-transform lg:static lg:w-64 lg:translate-x-0 lg:shadow-none ${open ? 'translate-x-0' : '-translate-x-full'}`}>
        <div className="pointer-events-none absolute inset-x-0 top-0 h-56 bg-[radial-gradient(circle_at_18%_4%,rgba(215,162,59,.09),transparent_68%)]" />
        <div className="pointer-events-none absolute inset-y-0 right-0 w-px bg-gradient-to-b from-mb-gold/25 via-mb-teal/12 to-transparent" />

        <button
          type="button"
          aria-label="Close navigation"
          onClick={onClose}
          className="absolute right-3 top-3 z-10 grid size-9 place-items-center rounded-lg border border-mb-border bg-mb-ink/70 text-mb-muted outline-none transition hover:border-mb-gold/30 hover:text-mb-gold-light focus-visible:ring-2 focus-visible:ring-mb-gold/50 lg:hidden"
        >
          <X size={18} />
        </button>

        <div className="relative border-b border-mb-gold/10 px-1 pb-5 pt-1"><Brand /></div>

        <nav className="relative flex-1 space-y-5 overflow-y-auto pb-2 pr-1 pt-5">
          {sections.map((section) => (
            <div key={section.label}>
              <p className="mb-2 px-3 text-[9px] font-semibold uppercase tracking-[0.2em] text-mb-muted/55">
                {section.label}
              </p>
              <div className="space-y-1">
                {section.items.map(([label, path, Icon]) => (
                  <NavLink
                    key={path}
                    to={path}
                    onClick={onClose}
                    className={({ isActive }) => `group relative flex min-h-11 items-center gap-3 overflow-hidden rounded-xl border px-2.5 py-2 text-sm outline-none transition duration-200 focus-visible:ring-2 focus-visible:ring-mb-gold/50 ${isActive ? 'border-mb-teal/30 bg-[linear-gradient(90deg,rgba(45,212,191,.14),rgba(215,162,59,.06))] text-mb-text shadow-[inset_3px_0_0_#2dd4bf,0_8px_24px_rgba(0,0,0,.12)]' : 'border-transparent text-mb-muted hover:border-mb-gold/20 hover:bg-mb-gold/[.055] hover:text-mb-text'}`}
                  >
                    {({ isActive }) => (
                      <>
                        <span className={`grid size-9 shrink-0 place-items-center rounded-lg border transition duration-200 ${isActive ? 'border-mb-gold/35 bg-mb-gold/14 text-mb-gold-light shadow-[0_0_20px_rgba(215,162,59,.14)]' : 'border-mb-gold/12 bg-mb-gold/[.055] text-mb-gold/70 group-hover:scale-105 group-hover:border-mb-gold/30 group-hover:bg-mb-gold/12 group-hover:text-mb-gold-light'}`}>
                          <Icon size={20} strokeWidth={1.8} />
                        </span>
                        <span className={`transition ${isActive ? 'font-semibold' : 'font-medium'}`}>{label}</span>
                        {isActive && <span className="ml-auto size-1.5 rounded-full bg-mb-teal shadow-[0_0_9px_#2dd4bf]" />}
                      </>
                    )}
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>

        <div className="relative mt-4 space-y-2.5 border-t border-mb-border pt-4">
          <div className="flex items-center gap-3 rounded-xl border border-mb-border bg-[linear-gradient(135deg,rgba(16,36,49,.85),rgba(8,25,35,.9))] p-3 shadow-[inset_0_1px_rgba(255,255,255,.025)]">
            <div className="grid size-10 place-items-center rounded-full border border-mb-gold/35 bg-gradient-to-br from-mb-gold to-amber-700 font-bold text-mb-ink shadow-[0_0_22px_rgba(215,162,59,.13)]">M</div>
            <div className="min-w-0 flex-1">
              <div className="truncate text-sm font-semibold">Monika</div>
              <div className="text-[11px] uppercase tracking-wider text-mb-teal">Operator</div>
            </div>
          </div>
          <button className="group flex min-h-11 w-full items-center gap-3 rounded-xl border border-mb-border px-2.5 py-2 text-sm font-medium text-mb-gold-light outline-none transition hover:border-mb-gold/25 hover:bg-mb-gold/[.055] focus-visible:ring-2 focus-visible:ring-mb-gold/50">
            <span className="grid size-8.5 place-items-center rounded-lg border border-mb-gold/15 bg-mb-gold/[.055] text-mb-gold transition group-hover:border-mb-gold/30 group-hover:bg-mb-gold/10 group-hover:text-mb-gold-light">
              <BadgeDollarSign size={20} strokeWidth={1.8} />
            </span>
            Switch bank
          </button>
        </div>
      </aside>
    </>
  )
}
