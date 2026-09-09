import { RefreshCw } from 'lucide-react'
import Button from './Button.jsx'
import Modal from './Modal.jsx'
import Notice from './Notice.jsx'

export default function StatusChangeModal({
  open,
  active,
  entityName,
  entityId,
  pending,
  error,
  onClose,
  onConfirm,
  inactiveExplanation = 'The record will remain available for audit and lookup.',
}) {
  if (!open) return null
  const activating = !active

  return (
    <Modal open onClose={onClose} title={activating ? `Activate ${entityName}?` : `Deactivate ${entityName}?`} description={entityId} size="sm">
      <div className="space-y-4 p-5">
        {error && <Notice>Status could not be changed. {error.message}</Notice>}
        <p className="text-sm leading-6 text-mb-muted">
          {activating
            ? `The ${entityName} will be marked as active in the mainframe.`
            : `The ${entityName} will be marked as inactive. ${inactiveExplanation}`}
        </p>
      </div>
      <footer className="flex justify-end gap-3 border-t border-mb-border px-5 py-4">
        <Button variant="ghost" onClick={onClose} disabled={pending}>Cancel</Button>
        <Button variant={activating ? 'primary' : 'danger'} onClick={onConfirm} disabled={pending}>
          {pending && <RefreshCw className="animate-spin" size={16} />}
          {pending ? 'Updating MVS…' : activating ? `Activate ${entityName}` : `Deactivate ${entityName}`}
        </Button>
      </footer>
    </Modal>
  )
}
