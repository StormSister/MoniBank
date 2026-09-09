export default function StatCard({ label, value, suffix, change, icon: Icon, tone = 'teal', loading }) {
  const tones = {
    teal: 'bg-mb-teal/16 text-mb-teal',
    blue: 'bg-blue-500/16 text-blue-400',
    gold: 'bg-mb-gold/16 text-mb-gold-light',
    green: 'bg-mb-terminal/14 text-mb-terminal',
  }

  return (
    <article className="mb-panel min-h-31 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs text-mb-muted">{label}</p>
          {loading ? <div className="mt-3 h-8 w-28 animate-pulse rounded bg-white/7" /> : (
            <p className="mt-2 text-2xl font-semibold tracking-tight text-mb-text">{value} {suffix && <span className="text-sm font-normal">{suffix}</span>}</p>
          )}
        </div>
        <div className={`grid size-10 place-items-center rounded-full ${tones[tone]}`}><Icon size={20} /></div>
      </div>
      <p className="mt-3 text-xs text-mb-terminal">{change}</p>
    </article>
  )
}
