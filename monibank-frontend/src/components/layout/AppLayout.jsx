import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.jsx'
import Topbar from './Topbar.jsx'
import LiveConsole from './LiveConsole.jsx'

export default function AppLayout() {
  const [navigationOpen, setNavigationOpen] = useState(false)
  const [consoleOpen, setConsoleOpen] = useState(true)

  return (
    <div className="flex min-h-screen bg-mb-ink text-mb-text">
      <Sidebar open={navigationOpen} onClose={() => setNavigationOpen(false)} />
      <div className="min-w-0 flex-1">
        <Topbar onOpenNavigation={() => setNavigationOpen(true)} onToggleConsole={() => setConsoleOpen((value) => !value)} />
        <main className="p-4 md:p-5"><Outlet /></main>
      </div>
      <LiveConsole open={consoleOpen} onClose={() => setConsoleOpen(false)} />
    </div>
  )
}
