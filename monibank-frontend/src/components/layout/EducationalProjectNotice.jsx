import { BookOpen, GraduationCap, ShieldAlert, SquareTerminal } from 'lucide-react'
import Button from '../ui/Button.jsx'
import Modal from '../ui/Modal.jsx'

export default function EducationalProjectNotice({ open, onClose }) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Welcome to the MoniBank lab"
      description="An educational core-banking project built to demonstrate integration between modern applications and legacy mainframe technology."
      size="lg"
    >
      <div className="space-y-5 p-5">
        <div className="overflow-hidden rounded-xl border border-mb-gold/25 bg-[radial-gradient(circle_at_top_right,rgba(215,162,59,.13),transparent_42%),linear-gradient(135deg,rgba(20,43,57,.95),rgba(7,23,33,.98))] p-5">
          <div className="flex items-start gap-4">
            <span className="grid size-12 shrink-0 place-items-center rounded-xl border border-mb-gold/30 bg-mb-gold/10 text-mb-gold-light shadow-[0_0_28px_rgba(215,162,59,.1)]">
              <GraduationCap size={25} />
            </span>
            <div>
              <p className="font-semibold text-mb-text">This is not a real bank</p>
              <p className="mt-1.5 text-sm leading-6 text-mb-muted">
                MoniBank does not provide financial services and does not process real money.
                Use fictional data only—never enter real personal, account or payment information.
              </p>
            </div>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <NoticeItem
            icon={SquareTerminal}
            title="A working legacy core"
            text="Demo operations are executed by MVS 3.8j through KICKS, COBOL and VSAM—not simulated by the frontend."
          />
          <NoticeItem 
            icon={ShieldAlert} 
            title="Fair-use limits" 
            text="Per IP address: 240 read requests per minute and 12 data-changing operations, replenished gradually over one hour. If a limit is reached, the API returns 429 with the retry time." 
          /> 
        </div>

        <div className="flex items-start gap-3 rounded-lg border border-mb-teal/15 bg-mb-teal/[0.045] px-4 py-3 text-xs leading-5 text-mb-muted">
          <BookOpen size={17} className="mt-0.5 shrink-0 text-mb-teal" />
          <p>
            The interface exposes selected banking workflows for learning and portfolio demonstration.
            Technical implementation notes will be linked from the relevant screens.
          </p>
        </div>
      </div>

      <footer className="flex justify-end border-t border-mb-border px-5 py-4">
        <Button variant="primary" onClick={onClose}>Enter educational demo</Button>
      </footer>
    </Modal>
  )
}

function NoticeItem({ icon: Icon, title, text }) {
  return (
    <div className="rounded-xl border border-mb-border bg-white/[0.018] p-4">
      <Icon size={19} className="text-mb-teal" />
      <p className="mt-3 text-sm font-semibold text-mb-text">{title}</p>
      <p className="mt-1.5 text-xs leading-5 text-mb-muted">{text}</p>
    </div>
  )
}
