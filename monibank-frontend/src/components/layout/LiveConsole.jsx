import { useEffect, useRef, useState } from 'react'
import { ChevronsLeftRight, Maximize2, Minimize2, Minus, Trash2, X } from 'lucide-react'
import { MAX_LOG_LINES, useMainframeLiveLog } from '../../hooks/useMainframeLiveLog.js'

const WIDTH_STORAGE_KEY = 'monibank.live-console.width.v3'
const DEFAULT_WIDTH = 760
const MIN_WIDTH = 480
const MAX_WIDTH = 1100

export default function LiveConsole({ open, onClose }) {
  const { lines, connectionState, clear } = useMainframeLiveLog()
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
  }, [width])

  const connected = connectionState === 'connected'

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
              <span>✦ JES printer / Hercules</span>
              <span className="text-mb-muted/60">·</span>
              <span className="text-mb-muted">prt00e.txt</span>
              <span className="rounded border border-mb-terminal/15 bg-mb-terminal/5 px-1.5 py-0.5 text-[10px] text-mb-terminal/65">{lines.length}/{MAX_LOG_LINES}</span>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-0.5 text-mb-muted">
            <HeaderButton label="Minimize" onClick={close}><Minus size={16} /></HeaderButton>
            <HeaderButton label={fullscreen ? 'Restore panel' : 'Full screen'} onClick={() => setFullscreen((current) => !current)}>
              {fullscreen ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            </HeaderButton>
            <HeaderButton label="Close" onClick={close}><X size={17} /></HeaderButton>
          </div>
        </div>
      </header>

      <div ref={outputRef} onScroll={updateAutoScroll} className="terminal-grid terminal-scrollbar min-h-0 flex-1 overflow-auto px-5 py-5 font-mono text-xs leading-6 text-mb-terminal">
        <div className="min-w-max pr-5">
          {lines.length === 0 && (
            <div className="text-mb-terminal/55">{connected ? 'Waiting for JES printer output...' : 'Connecting to JES printer stream...'}</div>
          )}
          {lines.map((line, index) => (
            <div key={`${index}-${line}`} className={`min-h-6 whitespace-pre ${lineTone(line)}`}>{line || ' '}</div>
          ))}
          <div className="mt-4 h-4 w-2 animate-pulse bg-mb-terminal shadow-[0_0_9px_rgba(85,227,106,.7)]" />
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 border-t border-mb-border bg-[#081720] px-5 py-3 text-xs text-mb-muted">
        <label className="flex cursor-pointer items-center gap-2 hover:text-mb-text">
          <input type="checkbox" checked={autoScroll} onChange={(event) => setAutoScroll(event.target.checked)} className="size-3.5 accent-[#55e36a]" />
          Auto-scroll
        </label>
        <button onClick={clear} className="flex items-center gap-2 rounded-md border border-mb-border bg-white/[0.02] px-3 py-2 hover:border-mb-terminal/25 hover:text-mb-text">
          <Trash2 size={13} /> Clear
        </button>
      </div>

      <footer className={`flex items-center justify-between gap-3 border-t border-mb-border bg-[#07131b] px-5 py-3 font-mono text-[11px] ${connected ? 'text-mb-terminal/75' : 'text-mb-gold-light'}`}>
        <span className="min-w-0 truncate"><span className={`mr-2 inline-block size-2 rounded-full ${connected ? 'bg-mb-terminal' : 'animate-pulse bg-mb-gold'}`} />{connectionLabel(connectionState)}</span>
        <span className="shrink-0 rounded border border-mb-border px-2 py-1 text-[9px] tracking-[0.12em] text-mb-muted">RAW STREAM</span>
      </footer>
    </aside>
  )
}

function HeaderButton({ label, onClick, children }) {
  return <button onClick={onClick} className="rounded-md p-2 outline-none hover:bg-white/5 hover:text-mb-text focus-visible:ring-2 focus-visible:ring-mb-terminal/40" aria-label={label} title={label}>{children}</button>
}

function connectionLabel(state) {
  if (state === 'connected') return 'Connected to prt00e.txt'
  if (state === 'reconnecting') return 'Connection lost — reconnecting...'
  return 'Connecting to prt00e.txt...'
}

function lineTone(line) {
  if (/ABEND|ERROR|FAILED|FAILURE|SEVERE/.test(line)) return 'text-mb-danger'
  if (/WARN|WARNING/.test(line)) return 'text-mb-gold-light'
  return 'text-mb-terminal'
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
