import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { PlantGate } from '../../../layouts/PlantGate';
import { ErrorMessage, FormField, LoadingState, PageHeader } from '../../../components/common';
import { createMachine, getMachine, updateMachine } from '../services/machineService';
import { listLines } from '../../plants/services/plantService';

/**
 * Create: CreateMachineRequest { plantId*, lineId, code* (≤30), name* (≤150), manufacturer, model, criticality, commissionedOn }
 * Edit:   UpdateMachineRequest { name, lineId, manufacturer, model, criticality, commissionedOn, active }
 */
export function MachineFormPage() {
  const { machineId } = useParams();
  const isEdit = Boolean(machineId);
  const { plantId } = usePlant();
  const navigate = useNavigate();
  const schema = isEdit ? 'UpdateMachineRequest' : 'CreateMachineRequest';
  const form = useContractForm(schema, { active: true });

  const existing = useAsync(() => getMachine(machineId), [machineId], { enabled: isEdit });
  const linePlantId = existing.data?.plantId ?? plantId;
  const lines = useAsync(() => listLines(linePlantId), [linePlantId], { enabled: Boolean(linePlantId) });

  useEffect(() => {
    const m = existing.data;
    if (!m) return;
    form.setValues({
      name: m.name ?? '',
      lineId: m.lineId ?? '',
      manufacturer: m.manufacturer ?? '',
      model: m.model ?? '',
      criticality: m.criticality ?? '',
      commissionedOn: m.commissionedOn ?? '',
      active: m.active ?? true,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [existing.data]);

  const mutation = useMutation((payload) => (isEdit ? updateMachine(machineId, payload) : createMachine(payload)));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate(isEdit ? { active: Boolean(form.values.active) } : { plantId });
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) navigate(`/machines/${result.data.id}`, { replace: true });
    else form.applyServerErrors(result.error);
  };

  if (isEdit && existing.loading) return <LoadingState />;
  if (isEdit && existing.error) return <ErrorMessage error={existing.error} onRetry={existing.reload} />;

  const title = isEdit ? `Edit ${existing.data?.name ?? 'machine'}` : 'Add machine';
  return (
    <>
      <PageHeader
        title={title}
        breadcrumb={[
          { label: 'Machines', to: '/machines' },
          ...(isEdit ? [{ label: existing.data?.code ?? machineId, to: `/machines/${machineId}` }] : []),
          { label: isEdit ? 'Edit' : 'New' },
        ]}
      />
      <PlantGate>
        <form className="card card-body form" onSubmit={onSubmit} noValidate>
          <ErrorMessage error={mutation.error} title="Could not save machine" />
          <div className="form-grid">
            {isEdit ? (
              <FormField label="Code" hint="Machine codes cannot be changed.">
                <input className="input mono" value={existing.data?.code ?? ''} disabled />
              </FormField>
            ) : (
              <FormField label="Code" required error={form.errors.code} hint="Up to 30 characters.">
                <input className="input mono" autoFocus {...form.bind('code')} />
              </FormField>
            )}
            <FormField label="Name" required={!isEdit} error={form.errors.name}>
              <input className="input" autoFocus={isEdit} {...form.bind('name')} />
            </FormField>
            <FormField label="Line" error={form.errors.lineId}>
              <select className="input" {...form.bind('lineId')}>
                <option value="">No line</option>
                {(lines.data ?? []).map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.name} ({l.code})
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Criticality" error={form.errors.criticality}>
              <input className="input" {...form.bind('criticality')} />
            </FormField>
            <FormField label="Manufacturer" error={form.errors.manufacturer}>
              <input className="input" {...form.bind('manufacturer')} />
            </FormField>
            <FormField label="Model" error={form.errors.model}>
              <input className="input" {...form.bind('model')} />
            </FormField>
            <FormField label="Commissioned on" error={form.errors.commissionedOn}>
              <input className="input" type="date" {...form.bind('commissionedOn')} />
            </FormField>
            {isEdit && (
              <div className="field" style={{ justifyContent: 'flex-end' }}>
                <label className="checkbox">
                  <input type="checkbox" {...form.bindCheckbox('active')} />
                  Active
                </label>
              </div>
            )}
          </div>
          <div className="form-actions">
            <Link className="btn" to={isEdit ? `/machines/${machineId}` : '/machines'}>
              Cancel
            </Link>
            <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
              {mutation.pending ? 'Saving…' : isEdit ? 'Save changes' : 'Create machine'}
            </button>
          </div>
        </form>
      </PlantGate>
    </>
  );
}
