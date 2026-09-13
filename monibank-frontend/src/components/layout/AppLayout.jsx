import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar.jsx'
import Topbar from './Topbar.jsx'
import LiveConsole from './LiveConsole.jsx'
import EducationalProjectNotice from './EducationalProjectNotice.jsx'

const EDUCATIONAL_NOTICE_KEY = 'monibank.educational-notice.v1'

export default function AppLayout() {
  const [navigationOpen, setNavigationOpen] = useState(false)
  const [consoleOpen, setConsoleOpen] = useState(true)
  const [projectNoticeOpen, setProjectNoticeOpen] = useState(shouldShowProjectNotice)

  const closeProjectNotice = () => {
    setProjectNoticeOpen(false)
    try {
      window.localStorage.setItem(EDUCATIONAL_NOTICE_KEY, 'acknowledged')
    } catch {
      // The notice still works when browser storage is unavailable.
    }
  }

  return (
    <div className="flex min-h-screen overflow-x-hidden bg-mb-ink text-mb-text">
      <Sidebar
        open={navigationOpen}
        onClose={() => setNavigationOpen(false)}
        onOpenProjectInfo={() => setProjectNoticeOpen(true)}
      />
      <div className={`app-workspace min-w-0 flex-1 transition-[margin] duration-200 ${consoleOpen ? 'console-open' : ''}`}>
        <Topbar onOpenNavigation={() => setNavigationOpen(true)} onToggleConsole={() => setConsoleOpen((value) => !value)} />
        <main className="min-w-0 p-3 sm:p-4 md:p-5"><Outlet /></main>
      </div>
      <LiveConsole open={consoleOpen} onClose={() => setConsoleOpen(false)} />
      <EducationalProjectNotice open={projectNoticeOpen} onClose={closeProjectNotice} />
    </div>
  )
}

function shouldShowProjectNotice() {
  try {
    return window.localStorage.getItem(EDUCATIONAL_NOTICE_KEY) !== 'acknowledged'
  } catch {
    return true
  }
}
