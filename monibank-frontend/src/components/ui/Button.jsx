const variants = {
  primary: 'border-mb-gold bg-mb-gold text-mb-ink hover:bg-mb-gold-light hover:border-mb-gold-light',
  secondary: 'border-mb-border bg-mb-panel-soft text-mb-text hover:border-mb-gold/50 hover:text-mb-gold-light',
  ghost: 'border-transparent bg-transparent text-mb-muted hover:bg-white/5 hover:text-mb-text',
  danger: 'border-mb-danger/40 bg-mb-danger/10 text-mb-danger hover:bg-mb-danger/20',
}

export default function Button({ variant = 'secondary', className = '', children, ...props }) {
  return (
    <button
      className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
