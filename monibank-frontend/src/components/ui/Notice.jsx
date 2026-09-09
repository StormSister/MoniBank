import { AlertCircle, CheckCircle2, X } from 'lucide-react'

const tones = {
  error: {
    wrapper: 'border-mb-danger/30 bg-mb-danger/8 text-mb-danger',
    icon: AlertCircle,
  },
  success: {
    wrapper: 'border-mb-terminal/25 bg-mb-terminal/8 text-mb-terminal',
    icon: CheckCircle2,
  },
}

export default function Notice({ tone = 'error', children, onClose }) {
  const { wrapper, icon: Icon } = tones[tone]

  return (
    <div className={`flex items-start gap-3 rounded-lg border px-4 py-3 text-sm ${wrapper}`} role={tone === 'error' ? 'alert' : 'status'}>
      <Icon className="mt-0.5 shrink-0" size={17} />
      <div className="min-w-0 flex-1 leading-6">{children}</div>
      {onClose && (
        <button type="button" onClick={onClose} className="shrink-0 rounded p-1 opacity-70 hover:bg-white/5 hover:opacity-100" aria-label="Dismiss message">
          <X size={15} />
        </button>
      )}
    </div>
  )
}
