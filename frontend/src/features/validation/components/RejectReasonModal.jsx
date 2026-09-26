import { useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { ErrorMessage, FormField, Modal } from '../../../components/common';

/** Collects a RejectRequest { reason* (≤500) } and submits it via `onReject(reason)`. */
export function RejectReasonModal({ title, onReject, onClose, onDone }) {
  const form = useContractForm('RejectRequest', { reason: '' });
  const mutation = useMutation((payload) => onReject(payload.reason));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) onDone(result.data);
    else form.applyServerErrors(result.error);
  };

  return (
    <Modal title={title} onClose={onClose}>
      <form className="form" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={mutation.error} />
        <FormField label="Reason" required error={form.errors.reason} hint="Up to 500 characters.">
          <textarea className="input" autoFocus {...form.bind('reason')} />
        </FormField>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-danger solid" disabled={mutation.pending}>
            {mutation.pending ? 'Rejecting…' : 'Reject'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
