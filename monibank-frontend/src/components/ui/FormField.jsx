export default function FormField({ label, error, hint, id, children }) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="block text-sm font-medium text-mb-text">{label}</label>
      {children}
      {error && <p className="text-xs text-mb-danger">{error}</p>}
      {!error && hint && <p className="text-xs text-mb-muted">{hint}</p>}
    </div>
  )
}

export function Input({ className = '', ...props }) {
  return <input className={`mb-input ${className}`} {...props} />
}

export function Select({ className = '', children, ...props }) {
  return <select className={`mb-input ${className}`} {...props}>{children}</select>
}
