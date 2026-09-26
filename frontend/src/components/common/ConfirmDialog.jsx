import { Modal } from './Modal';
import { ErrorMessage } from './ErrorMessage';

export function ConfirmDialog({ title, children, confirmLabel = 'Confirm', danger, pending, error, onConfirm, onCancel }) {
  return (
    <Modal
      title={title}
      onClose={onCancel}
      footer={
        <div className="form-actions">
          <button type="button" className="btn" onClick={onCancel} disabled={pending}>
            Cancel
          </button>
          <button
            type="button"
            className={`btn ${danger ? 'btn-danger solid' : 'btn-primary'}`}
            onClick={onConfirm}
            disabled={pending}
          >
            {pending ? 'Working…' : confirmLabel}
          </button>
        </div>
      }
    >
      <div className="stack">
        <ErrorMessage error={error} />
        {children}
      </div>
    </Modal>
  );
}
