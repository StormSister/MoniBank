import { useEffect, useId } from 'react'
import { X } from 'lucide-react'

export default function Modal({ open, title, description, onClose, children, size = 'md' }) {
  const titleId = useId()
  const descriptionId = useId()

  useEffect(() => {
    if (!open) return undefined

    const closeOnEscape = (event) => {
      if (event.key === 'Escape') onClose()
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', closeOnEscape)

    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', closeOnEscape)
    }
  }, [open, onClose])

  if (!open) return null

  const widths = {
    sm: 'max-w-md',
    md: 'max-w-xl',
    lg: 'max-w-2xl',
  }

  return (
    <div className="fixed inset-0 z-[80] grid place-items-center overflow-y-auto bg-black/72 p-4 backdrop-blur-[2px]" onMouseDown={onClose}>
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descriptionId : undefined}
        className={`my-auto w-full ${widths[size]} overflow-hidden rounded-xl border border-mb-border bg-[#0b1b27] shadow-[0_28px_90px_rgba(0,0,0,.62)]`}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="flex items-start justify-between gap-5 border-b border-mb-border px-5 py-4">
          <div>
            <h2 id={titleId} className="text-lg font-semibold text-mb-text">{title}</h2>
            {description && <p id={descriptionId} className="mt-1 text-sm leading-6 text-mb-muted">{description}</p>}
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-2 text-mb-muted transition hover:bg-white/5 hover:text-mb-text" aria-label="Close dialog">
            <X size={18} />
          </button>
        </header>
        {children}
      </section>
    </div>
  )
}
