import { useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { ErrorMessage, FormField, Modal } from '../../../components/common';
import { PartNamesInput } from '../../records/components/PartNamesInput';
import { editAndApprove } from '../services/validationService';

/**
 * EditValidationItemRequest { machineId, failureModeId, recordDate, downtimeMinutes, description (≤2000),
 *                             actionTaken (≤2000), technician (≤100), partNames[] }
 * Prefilled from the ValidationItemResponse being reviewed.
 */
export function EditApproveModal({ item, machines, failureModes, onClose, onDone }) {
  const form = useContractForm('EditValidationItemRequest', {
    machineId: item.machineId ?? '',
    failureModeId: item.failureModeId ?? '',
    recordDate: item.recordDate ?? '',
    downtimeMinutes: item.downtimeMinutes ?? '',
    description: item.description ?? '',
    actionTaken: item.actionTaken ?? '',
    technician: item.technician ?? '',
    // partsText is free text in the response; split on commas/semicolons as a starting point for review.
    partNames: item.partsText ? item.partsText.split(/[,;]/).map((s) => s.trim()).filter(Boolean) : [],
  });
  const mutation = useMutation((payload) => editAndApprove(item.id, payload));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) onDone(result.data);
    else form.applyServerErrors(result.error);
  };

  return (
    <Modal title={`Edit & approve row ${item.rowNumber ?? ''}`} onClose={onClose} wide>
      <form className="form" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={mutation.error} title="Could not approve" />
        {item.machineText && (
          <div className="alert alert-info">
            <div className="alert-body">
              Machine text in source: <strong>{item.machineText}</strong>
            </div>
          </div>
        )}
        <div className="form-grid">
          <FormField label="Machine" error={form.errors.machineId}>
            <select className="input" {...form.bind('machineId')}>
              <option value="">Select a machine</option>
              {machines.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Failure mode" error={form.errors.failureModeId}>
            <select className="input" {...form.bind('failureModeId')}>
              <option value="">Not specified</option>
              {failureModes.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Date" error={form.errors.recordDate}>
            <input className="input" type="date" {...form.bind('recordDate')} />
          </FormField>
          <FormField label="Downtime (minutes)" error={form.errors.downtimeMinutes}>
            <input className="input" type="number" min={0} {...form.bind('downtimeMinutes')} />
          </FormField>
          <FormField label="Technician" error={form.errors.technician} className="span-2">
            <input className="input" {...form.bind('technician')} />
          </FormField>
          <FormField label="Description" error={form.errors.description} className="span-2">
            <textarea className="input" rows={3} {...form.bind('description')} />
          </FormField>
          <FormField label="Action taken" error={form.errors.actionTaken} className="span-2">
            <textarea className="input" rows={2} {...form.bind('actionTaken')} />
          </FormField>
          <FormField
            label="Spare parts"
            error={form.errors.partNames}
            className="span-2"
            hint={item.partsText ? `Source text: “${item.partsText}”` : 'Separate part names with commas.'}
          >
            <PartNamesInput value={form.values.partNames} onChange={(v) => form.set('partNames', v)} />
          </FormField>
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
            {mutation.pending ? 'Approving…' : 'Save & approve'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
