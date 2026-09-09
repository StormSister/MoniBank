import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.jsx'
import Topbar from './Topbar.jsx'
import LiveConsole from './LiveConsole.jsx'

export default function AppLayout() {
  const [navigationOpen, setNavigationOpen] = useState(false)
  const [consoleOpen, setConsoleOpen] = useState(true)

  return (
    <div className="flex min-h-screen overflow-x-hidden bg-mb-ink text-mb-text">
      <Sidebar open={navigationOpen} onClose={() => setNavigationOpen(false)} />
      <div className={`app-workspace min-w-0 flex-1 transition-[margin] duration-200 ${consoleOpen ? 'console-open' : ''}`}>
        <Topbar onOpenNavigation={() => setNavigationOpen(true)} onToggleConsole={() => setConsoleOpen((value) => !value)} />
        <main className="min-w-0 p-3 sm:p-4 md:p-5"><Outlet /></main>
      </div>
      <LiveConsole open={consoleOpen} onClose={() => setConsoleOpen(false)} />
    </div>
  )
}
