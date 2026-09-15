import { BookOpen, ExternalLink } from 'lucide-react'
import { documentationUrl } from '../../config/documentation.js'

export default function DocumentationLink({
  page,
  section,
  locale = 'en',
  children = 'How it works',
  className = '',
}) {
  return (
    <a
      href={documentationUrl(page, { locale, section })}
      target="_blank"
      rel="noreferrer"
      aria-label={`${children} — opens documentation in a new tab`}
      title={`${children} — opens documentation in a new tab`}
      className={`group inline-flex min-h-8 shrink-0 items-center gap-1.5 rounded-lg border border-mb-teal/20 bg-mb-teal/[0.055] px-2 py-1 text-[11px] font-medium text-mb-muted outline-none transition hover:border-mb-teal/45 hover:bg-mb-teal/10 hover:text-mb-teal focus-visible:ring-2 focus-visible:ring-mb-gold/60 sm:px-2.5 ${className}`}
    >
      <BookOpen size={13} aria-hidden="true" />
      <span className="hidden sm:inline">{children}</span>
      <ExternalLink
        size={11}
        aria-hidden="true"
        className="opacity-55 transition group-hover:opacity-100"
      />
    </a>
  )
}
