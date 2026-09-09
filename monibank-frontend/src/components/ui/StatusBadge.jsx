const variants = {
  success: 'bg-mb-terminal/10 text-mb-terminal ring-mb-terminal/20',
  warning: 'bg-mb-gold/10 text-mb-gold-light ring-mb-gold/20',
  danger: 'bg-mb-danger/10 text-mb-danger ring-mb-danger/20',
  neutral: 'bg-white/5 text-mb-muted ring-white/10',
}

export default function StatusBadge({ children, variant = 'success' }) {
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset ${variants[variant]}`}>
      <span className="size-1.5 rounded-full bg-current" />
      {children}
    </span>
  )
}
