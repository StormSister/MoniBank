import { ChevronDown, Menu, Monitor, PanelRightOpen } from 'lucide-react'
import Button from '../ui/Button.jsx'
import { useMainframeStatus } from '../../hooks/useMainframeStatus.js'

export default function Topbar({ onOpenNavigation, onToggleConsole }) {
  const statusQuery = useMainframeStatus()
  const status = statusQuery.data?.status || (statusQuery.isLoading ? 'CHECKING' : 'UNAVAILABLE')
  const healthy = status === 'ONLINE'

  return (
    <header className="flex flex-wrap items-center justify-between gap-4 border-b border-mb-border px-4 py-4 md:px-6">
      <div className="flex items-center gap-3">
        <button onClick={onOpenNavigation} className="rounded-lg border border-mb-border p-2 text-mb-muted lg:hidden" aria-label="Open navigation"><Menu size={20} /></button>
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-mb-text md:text-2xl">{getGreeting()}, Monika <span aria-hidden>👋</span></h1>
          <p className="mt-1 text-sm text-mb-muted">Here’s what’s happening in your core banking today.</p>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <button className="hidden min-h-10 items-center gap-2 rounded-lg border border-mb-border bg-mb-surface px-3 text-sm text-mb-text sm:flex">
          <Monitor size={17} className="text-blue-400" /> Legacy Bank (MVS 3.8j) <ChevronDown size={15} />
        </button>
        <span className={`hidden min-h-10 items-center gap-2 rounded-lg border border-mb-border bg-mb-surface px-3 text-xs font-semibold md:flex ${healthy ? 'text-mb-terminal' : 'text-mb-gold-light'}`}>
          <span className={`size-2 rounded-full bg-current ${healthy ? 'shadow-[0_0_10px_#55e36a]' : ''}`} /> {status}
        </span>
        <Button variant="ghost" className="px-2.5" onClick={onToggleConsole} aria-label="Toggle live mainframe log"><PanelRightOpen size={19} /></Button>
      </div>
    </header>
  )
}

function getGreeting() {
  const hour = new Date().getHours()

  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

export function useGreeting() {
  const [greeting, setGreeting] = useState(getGreeting)

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setGreeting(getGreeting())
    }, 60_000)

    return () => window.clearInterval(intervalId)
  }, [])

  return greeting
}
