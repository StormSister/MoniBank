export default function Panel({ title, action, className = '', children }) {
  return (
    <section className={`mb-panel overflow-hidden ${className}`}>
      {(title || action) && (
        <header className="flex min-h-13 items-center justify-between gap-4 border-b border-mb-border px-4 py-3">
          {title && <h2 className="text-base font-semibold text-mb-text">{title}</h2>}
          {action}
        </header>
      )}
      {children}
    </section>
  )
}
