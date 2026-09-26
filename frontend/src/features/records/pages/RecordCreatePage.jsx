import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { PlantGate } from '../../../layouts/PlantGate';
import { ErrorMessage, FormField, PageHeader } from '../../../components/common';
import { PartNamesInput } from '../components/PartNamesInput';
import { createRecord, listFilterOptions } from '../services/recordService';
import { listFailureModes } from '../../config/services/configService';
import { todayIso } from '../../../utils/format';

/**
 * CreateRecordRequest { plantId*, machineId*, recordDate*, description* (≤2000), downtimeMinutes, actionTaken (≤2000),
 *                       technician (≤100), failureModeId, failureModeText (≤200), partNames[] }
 */
export function RecordCreatePage() {
  const { plantId } = usePlant();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const form = useContractForm('CreateRecordRequest', {
    machineId: params.get('machineId') ?? '',
    recordDate: todayIso(),
    partNames: [],
  });
  const options = useAsync(() => listFilterOptions(plantId), [plantId], { enabled: Boolean(plantId) });
  const failureModes = useAsync(listFailureModes, []);
  const mutation = useMutation(createRecord);

  const onSubmit = async (e) => {
    e.preventDefault();
    // Free-text failure mode only applies when no listed failure mode is chosen.
    const payload = form.validate({ plantId, ...(form.values.failureModeId ? { failureModeText: '' } : {}) });
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) navigate(`/records/${result.data.id}`, { replace: true });
    else form.applyServerErrors(result.error);
  };

  return (
    <>
      <PageHeader
        breadcrumb={[{ label: 'Records', to: '/records' }, { label: 'New' }]}
        title="New maintenance record"
        subtitle={
          <>
            Prefer to describe it in your own words? Try <Link to="/entry">guided entry</Link>.
          </>
        }
      />
      <PlantGate>
        <form className="card card-body form" onSubmit={onSubmit} noValidate>
          <ErrorMessage error={mutation.error} title="Could not save record" />
          <ErrorMessage error={options.error} title="Could not load machines" onRetry={options.reload} />
          <div className="form-grid">
            <FormField label="Machine" required error={form.errors.machineId}>
              <select className="input" {...form.bind('machineId')} disabled={options.loading}>
                <option value="">{options.loading ? 'Loading…' : 'Select a machine'}</option>
                {(options.data?.machines ?? []).map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Date" required error={form.errors.recordDate}>
              <input className="input" type="date" max={todayIso()} {...form.bind('recordDate')} />
            </FormField>
            <FormField label="Failure mode" error={form.errors.failureModeId}>
              <select className="input" {...form.bind('failureModeId')}>
                <option value="">Not specified / other</option>
                {(failureModes.data ?? []).map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name}
                    {f.category ? ` — ${f.category}` : ''}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField
              label="Failure mode (free text)"
              error={form.errors.failureModeText}
              hint="Optional, when no listed failure mode fits."
            >
              <input className="input" {...form.bind('failureModeText')} disabled={Boolean(form.values.failureModeId)} />
            </FormField>
            <FormField label="Downtime (minutes)" error={form.errors.downtimeMinutes}>
              <input className="input" type="number" min={0} inputMode="numeric" {...form.bind('downtimeMinutes')} />
            </FormField>
            <FormField label="Technician" error={form.errors.technician}>
              <input className="input" {...form.bind('technician')} />
            </FormField>
            <FormField label="Description" required error={form.errors.description} className="span-2" hint="What happened? Up to 2000 characters.">
              <textarea className="input" rows={4} {...form.bind('description')} />
            </FormField>
            <FormField label="Action taken" error={form.errors.actionTaken} className="span-2">
              <textarea className="input" rows={3} {...form.bind('actionTaken')} />
            </FormField>
            <FormField label="Spare parts used" error={form.errors.partNames} className="span-2" hint="Separate part names with commas.">
              <PartNamesInput value={form.values.partNames} onChange={(v) => form.set('partNames', v)} />
            </FormField>
          </div>
          <div className="form-actions">
            <Link className="btn" to="/records">
              Cancel
            </Link>
            <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
              {mutation.pending ? 'Saving…' : 'Save record'}
            </button>
          </div>
        </form>
      </PlantGate>
    </>
  );
}
