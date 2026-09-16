import { useEffect, useRef, useState } from 'react'
import { ChevronsLeftRight, Maximize2, Minimize2, Minus, Trash2, X } from 'lucide-react'
import { MAX_LOG_LINES, useMainframeLiveLog } from '../../hooks/useMainframeLiveLog.js'
import DocumentationLink from '../ui/DocumentationLink.jsx'

const WIDTH_STORAGE_KEY = 'monibank.live-console.width.v4'
const DEFAULT_WIDTH = 520
const MIN_WIDTH = 420
const MAX_WIDTH = 760

export default function LiveConsole({ open, onClose }) {
  const { lines, connectionState, clear } = useMainframeLiveLog()
  const [sourceFilter, setSourceFilter] = useState('activity')
  const [hideBanners, setHideBanners] = useState(true)
  const [autoScroll, setAutoScroll] = useState(true)
  const [width, setWidth] = useState(readStoredWidth)
  const [resizing, setResizing] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)
  const outputRef = useRef(null)
  const resizeStartRef = useRef({ pointerX: 0, width: DEFAULT_WIDTH })

  useEffect(() => {
    if (autoScroll && outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight
    }
  }, [autoScroll, lines])

  useEffect(() => {
    if (!resizing) return undefined

    const resize = (event) => {
      const movement = resizeStartRef.current.pointerX - event.clientX
      setWidth(clampWidth(resizeStartRef.current.width + movement))
    }

    const stopResizing = () => setResizing(false)

    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'
    window.addEventListener('pointermove', resize)
    window.addEventListener('pointerup', stopResizing, { once: true })

    return () => {
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
      window.removeEventListener('pointermove', resize)
      window.removeEventListener('pointerup', stopResizing)
    }
  }, [resizing])

  useEffect(() => {
  window.localStorage.setItem(WIDTH_STORAGE_KEY, String(width))
  document.documentElement.style.setProperty(
    '--live-console-width',
    `${width}px`,
  )

  return () => {
    document.documentElement.style.removeProperty(
      '--live-console-width',
    )
  }
}, [width])

  const connected = connectionState === 'connected'
  const sourceLines = sourceFilter === 'activity'
    ? lines.map(toActivityLine).filter(Boolean)
    : lines.filter((line) => line.source === sourceFilter)
  const visibleLines = sourceFilter === 'activity' || !hideBanners
    ? sourceLines
    : sourceLines.filter((line) => !isJesSeparatorLine(line.text))

  const close = () => {
    setFullscreen(false)
    onClose()
  }

  const startResizing = (event) => {
    event.preventDefault()
    resizeStartRef.current = { pointerX: event.clientX, width }
    setResizing(true)
  }

  const resizeWithKeyboard = (event) => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
    event.preventDefault()
    setWidth((current) => clampWidth(current + (event.key === 'ArrowLeft' ? 24 : -24)))
  }

  const updateAutoScroll = () => {
    const output = outputRef.current
    if (!output) return

    const distanceFromBottom = output.scrollHeight - output.scrollTop - output.clientHeight
    setAutoScroll(distanceFromBottom < 36)
  }

  const positioning = fullscreen
    ? 'fixed inset-3 z-[70] w-auto rounded-xl border border-mb-terminal/25 shadow-[0_24px_90px_rgba(0,0,0,.68)]'
    : 'fixed inset-y-0 right-0 z-50 w-full border-l border-mb-border shadow-[-24px_0_60px_rgba(0,0,0,.48)] sm:w-[var(--live-console-width)]'

  return (
    <aside
      style={{ '--live-console-width': `${width}px` }}
      className={`live-console flex flex-col overflow-visible bg-[#050f16] transition-[transform,width] duration-200 ${positioning} ${resizing ? '!transition-none' : ''} ${open ? 'translate-x-0' : 'translate-x-full xl:hidden'}`}
      aria-label="Hercules live system log"
    >
      {!fullscreen && (
        <div
          role="separator"
          aria-label="Resize live log panel"
          aria-orientation="vertical"
          tabIndex={0}
          title="Drag to resize · double-click to reset"
          onPointerDown={startResizing}
          onDoubleClick={() => setWidth(DEFAULT_WIDTH)}
          onKeyDown={resizeWithKeyboard}
          className="group absolute -left-5 top-0 z-10 hidden h-full w-10 cursor-col-resize items-center justify-end outline-none sm:flex"
        >
          <span
            className={`relative flex h-28 w-8 items-center justify-center overflow-hidden rounded-l-xl border border-r-0 transition-all duration-200
              ${resizing
                ? 'border-mb-terminal/80 bg-mb-terminal/18 text-mb-terminal shadow-[-8px_0_26px_rgba(85,227,106,.35)]'
                : 'border-mb-border bg-[linear-gradient(180deg,rgba(20,43,57,.98),rgba(8,23,32,.98))] text-mb-muted shadow-[-7px_0_20px_rgba(0,0,0,.32)] group-hover:h-32 group-hover:border-mb-terminal/55 group-hover:bg-mb-terminal/10 group-hover:text-mb-terminal group-hover:shadow-[-8px_0_24px_rgba(85,227,106,.22)] group-focus-visible:ring-2 group-focus-visible:ring-mb-terminal/50'
              }`}
          >
            <span className="absolute inset-y-3 right-0 w-px bg-gradient-to-b from-transparent via-mb-terminal/45 to-transparent" />
            <ChevronsLeftRight size={16} strokeWidth={1.8} />
          </span>
        </div>
      )}

      <header className="relative overflow-hidden border-b border-mb-border bg-[linear-gradient(135deg,rgba(16,36,49,.98),rgba(5,15,22,.98))] px-5 py-4">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-mb-terminal/60 to-transparent" />
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex items-center gap-3 font-mono text-sm font-bold tracking-[0.08em] text-mb-terminal">
              <span className={`size-3 shrink-0 rounded-full ${connected ? 'bg-mb-terminal shadow-[0_0_14px_#55e36a]' : 'animate-pulse bg-mb-gold'}`} />
              MAINFRAME LIVE
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 font-mono text-xs text-mb-terminal/70">
              <span>✦ Hercules / MVS + KICKS</span>
              <span className="text-mb-muted/60">·</span>
              <span className="text-mb-muted">MVS hardcopy + device 5001</span>
              <span className="rounded border border-mb-terminal/15 bg-mb-terminal/5 px-1.5 py-0.5 text-[10px] text-mb-terminal/65">{lines.length}/{MAX_LOG_LINES}</span>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-0.5 text-mb-muted">
            <DocumentationLink page="liveConsole">How it works</DocumentationLink>
            <HeaderButton label="Minimize" onClick={close}><Minus size={16} /></HeaderButton>
            <HeaderButton label={fullscreen ? 'Restore panel' : 'Full screen'} onClick={() => setFullscreen((current) => !current)}>
              {fullscreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            </HeaderButton>
            <HeaderButton label="Close" onClick={close}><X size={17} /></HeaderButton>
          </div>
        </div>
      </header>

      <div className="flex items-center gap-1 border-b border-mb-border bg-[#07131b] px-5 py-2 font-mono text-[10px] tracking-[0.1em]">
        {[
          ['activity', 'Activity'],
          ['kicks', 'KICKS raw'],
          ['jes', 'System raw'],
        ].map(([sourceName, label]) => (
          <button
            key={sourceName}
            type="button"
            onClick={() => setSourceFilter(sourceName)}
            className={`rounded border px-2.5 py-1.5 uppercase transition-colors ${sourceFilter === sourceName
              ? 'border-mb-terminal/35 bg-mb-terminal/10 text-mb-terminal'
              : 'border-transparent text-mb-muted hover:border-mb-border hover:text-mb-text'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <div ref={outputRef} onScroll={updateAutoScroll} className="terminal-grid terminal-scrollbar min-h-0 flex-1 overflow-auto px-5 py-5 font-mono text-xs leading-6 text-mb-terminal">
        <div className="min-w-max pr-5">
          {visibleLines.length === 0 && (
            <div className="text-mb-terminal/55">{connected ? 'Waiting for mainframe output...' : 'Connecting to mainframe streams...'}</div>
          )}
          {visibleLines.map((line) => (
            <div key={line.id} className={`group flex min-h-6 whitespace-pre ${lineTone(line.text)}`}>
              <span className={`mr-3 select-none text-[9px] tracking-[0.08em] ${line.source === 'kicks' ? 'text-mb-teal' : 'text-mb-muted/55'}`}>
                {(line.origin || line.source).toUpperCase().padEnd(5)}
              </span>
              <span>{line.text || ' '}</span>
            </div>
          ))}
          <div className="mt-4 h-4 w-2 animate-pulse bg-mb-terminal shadow-[0_0_9px_rgba(85,227,106,.7)]" />
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-mb-border bg-[#081720] px-5 py-3 text-xs text-mb-muted">
        <div className="flex flex-wrap items-center gap-4">
          <label className="flex cursor-pointer items-center gap-2 hover:text-mb-text">
            <input type="checkbox" checked={autoScroll} onChange={(event) => setAutoScroll(event.target.checked)} className="size-3.5 accent-[#55e36a]" />
            Auto-scroll
          </label>
          {sourceFilter !== 'activity' && <label className="flex cursor-pointer items-center gap-2 hover:text-mb-text">
            <input type="checkbox" checked={hideBanners} onChange={(event) => setHideBanners(event.target.checked)} className="size-3.5 accent-[#55e36a]" />
            Hide JES banners
          </label>}
        </div>
        <button onClick={clear} className="flex items-center gap-2 rounded-md border border-mb-border bg-white/[0.02] px-3 py-2 hover:border-mb-terminal/25 hover:text-mb-text">
          <Trash2 size={13} /> Clear
        </button>
      </div>

      <footer className={`flex items-center justify-between gap-3 border-t border-mb-border bg-[#07131b] px-5 py-3 font-mono text-[11px] ${connected ? 'text-mb-terminal/75' : 'text-mb-gold-light'}`}>
        <span className="min-w-0 truncate"><span className={`mr-2 inline-block size-2 rounded-full ${connected ? 'bg-mb-terminal' : 'animate-pulse bg-mb-gold'}`} />{connectionLabel(connectionState)}</span>
        <span className="shrink-0 rounded border border-mb-border px-2 py-1 text-[9px] tracking-[0.12em] text-mb-muted">{sourceFilter === 'activity' ? 'MVS ACTIVITY' : 'RAW STREAM'}</span>
      </footer>
    </aside>
  )
}

function HeaderButton({ label, onClick, children }) {
  return <button onClick={onClick} className="rounded-md p-2 outline-none hover:bg-white/5 hover:text-mb-text focus-visible:ring-2 focus-visible:ring-mb-terminal/40" aria-label={label} title={label}>{children}</button>
}

function connectionLabel(state) {
  if (state === 'connected') return 'Connected to JES + KICKS streams'
  if (state === 'reconnecting') return 'Connection lost — reconnecting...'
  return 'Connecting to mainframe streams...'
}

function lineTone(line) {
  if (/^MBR;E;|ABEND|ERROR|FAILED|FAILURE|SEVERE| failed\b/i.test(line)) return 'text-mb-danger'
  if (/WARN|WARNING|RC=0*[1-9]/.test(line)) return 'text-mb-gold-light'
  return 'text-mb-terminal'
}

function toActivityLine(line) {
  if (line.source === 'kicks') return kicksActivity(line)
  if (line.source === 'jes') return systemActivity(line)
  return null
}

function kicksActivity(line) {
  const text = line.text.trim()

  if (text.startsWith('MBS;')) {
    return dailyReportActivity(line, text)
  }

  if (text.startsWith('MBR;D;')) {
    const fields = text.split(';').map((field) => field.trim())
    const recordType = fields[2] || 'DATA'
    const requestId = fields[3] || 'NO-REQUEST'

    return {
      ...line,
      source: 'activity',
      origin: 'kicks',
      text: `[${requestId}] ${recordType} record returned by mainframe`,
    }
  }

  if (!text.startsWith('MBR;S;') && !text.startsWith('MBR;E;')) {
    return null
  }

  const fields = text.split(';').map((field) => field.trim())
  const failed = fields[1] === 'E'
  const operation = fields[2] || 'PROGRAM'
  const program = KICKS_PROGRAMS[operation] || operation
  const requestId = fields[3] || 'NO-REQUEST'
  const entityId = fields[4] && fields[4] !== requestId ? fields[4] : ''
  const result = fields.slice(5).filter((field) => field && field !== 'C').join(' · ')
  const details = [entityId, result].filter(Boolean).join(' · ')

  return {
    ...line,
    source: 'activity',
    origin: 'kicks',
    text: `[${requestId}] MBGATE → ${program} ${failed ? 'failed' : 'completed'}${details ? ` · ${details}` : ''}`,
  }
}

const KICKS_PROGRAMS = {
  ADDCUST: 'ADDCUSG',
}

function dailyReportActivity(line, text) {
  const fields = text.split(';').map((field) => field.trim())
  const recordType = fields[1]

  if (recordType !== 'H' && recordType !== 'E') return null

  const date = fields[2] || 'unknown date'
  const currency = fields[3] || ''
  const result = fields[4] || ''
  const label = recordType === 'H' ? 'daily report started' : 'daily report completed'

  return {
    ...line,
    source: 'activity',
    origin: 'kicks',
    text: `[${date}] ${label} · ${currency}${result ? ` · ${result}` : ''}`,
  }
}

function systemActivity(line) {
  const text = normalizeHardcopyLine(line.text)

  if (!text || /\bMF1\b|IRB101I|\$HASP160|\$HASP250|\$HASP395|IEF403I|IEFACTRT - Stepname/i.test(text)) {
    return null
  }

  const prefix = activityPrefix(text)
  const body = stripActivityPrefix(text)

  const queued = body.match(/\$HASP100\s+([A-Z0-9@$#]{1,8})\s+ON\s+(\S+)/i)
  if (queued) return activityEvent(line, `${prefix}${queued[1]} queued on ${queued[2]}`)

  const started = body.match(/\$HASP373\s+([A-Z0-9@$#]{1,8})\s+STARTED(?:\s+-\s+INIT\s+(\d+))?/i)
  if (started) {
    const initiator = started[2] ? ` · initiator ${started[2]}` : ''
    return activityEvent(line, `${prefix}${started[1]} started${initiator}`)
  }

  const step = body.match(/(?:JOB|TSU|STC)\s+\d+\s+([A-Z0-9@$#]{1,8})\s+([A-Z0-9@$#]{1,8})\s+(?:[A-Z0-9@$#]{1,8}\s+)?([A-Z0-9@$#]{1,8})\s+RC=\s*(\d+)/i)
  if (step) return activityEvent(line, `${prefix}${step[1]} · ${step[2]} / ${step[3]} · RC=${step[4]}`)

  const ended = body.match(/IEF404I\s+([A-Z0-9@$#]{1,8})\s+-\s+ENDED/i)
  if (ended) return activityEvent(line, `${prefix}${ended[1]} completed`)

  const abend = body.match(/IEF450I\s+([A-Z0-9@$#]{1,8}).*?ABEND\s+([A-Z0-9]+)/i)
  if (abend) return activityEvent(line, `${prefix}${abend[1]} failed · ABEND ${abend[2]}`)

  const jclError = body.match(/IEF452I\s+([A-Z0-9@$#]{1,8})\s+JOB NOT RUN - JCL ERROR/i)
  if (jclError) return activityEvent(line, `${prefix}${jclError[1]} rejected · JCL error`)

  const loggedOn = body.match(/IEF125I\s+([A-Z0-9@$#]{1,8})\s+-\s+LOGGED ON/i)
  if (loggedOn) return activityEvent(line, `${prefix}${loggedOn[1]} TSO session logged on`)

  const loggedOff = body.match(/IEF126I\s+([A-Z0-9@$#]{1,8})\s+-\s+LOGGED OFF/i)
  if (loggedOff) return activityEvent(line, `${prefix}${loggedOff[1]} TSO session logged off`)

  const waiting = body.match(/IEF099I\s+JOB\s+([A-Z0-9@$#]{1,8})\s+WAITING FOR DATA SETS/i)
  if (waiting) return activityEvent(line, `${prefix}${waiting[1]} waiting for a data set`)

  const unavailable = body.match(/IEF863I\s+DSN=(\S+)/i)
  if (unavailable) return activityEvent(line, `${prefix}data set unavailable · ${unavailable[1]}`)

  if (/JCL ERROR|ABEND|NOT EXECUTED|RC=\s*\d+/i.test(body)) {
    return activityEvent(line, `${prefix}${body}`)
  }

  return null
}

function normalizeHardcopyLine(line) {
  return line.replace(/^[0-9A-F]{4}\s+/, '').trim()
}

function activityPrefix(line) {
  const match = line.match(/^(\d{2}\.\d{2}\.\d{2})\s+/)
  return match ? `${match[1]} · ` : ''
}

function stripActivityPrefix(line) {
  return line.replace(/^\d{2}\.\d{2}\.\d{2}\s+/, '')
}

function activityEvent(line, text) {
  return { ...line, source: 'activity', origin: 'system', text }
}

function isJesSeparatorLine(line) {
  const trimmed = line.trim()

  if (/^\*{4}Z\s+(START|END)\s+(JOB|STC|TSU)\b.*Z\*{4}$/.test(trimmed)) {
    return true
  }

  const tokens = trimmed.split(/\s+/).filter(Boolean)
  if (tokens.length < 3) return false

  const repeatedGlyphTokens = tokens.filter((token) => /^([A-Z0-9])\1+$/.test(token))
  return repeatedGlyphTokens.length / tokens.length >= 0.75
}

function readStoredWidth() {
  const stored = Number(window.localStorage.getItem(WIDTH_STORAGE_KEY))
  return Number.isFinite(stored) && stored > 0
    ? Math.min(Math.max(stored, MIN_WIDTH), MAX_WIDTH)
    : DEFAULT_WIDTH
}

function clampWidth(value) {
  const viewportMaximum = typeof window === 'undefined' ? MAX_WIDTH : Math.max(MIN_WIDTH, window.innerWidth - 520)
  return Math.min(Math.max(value, MIN_WIDTH), Math.min(MAX_WIDTH, viewportMaximum))
}
