import { useState } from 'react';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, Badge, EmptyState, ErrorMessage, FormField, Modal, PageHeader, SuccessMessage } from '../../../components/common';
import { dismissAlias, listAliasSuggestions, mapAlias } from '../services/validationService';
import { listFilterOptions } from '../../records/services/recordService';
import { formatConfidence, formatNumber } from '../../../utils/format';

/** MapAliasRequest { machineId* } */
function MapModal({ suggestion, machines, onClose, onMapped }) {
  const [machineId, setMachineId] = useState(suggestion.suggestedMachineId ?? '');
  const [fieldError, setFieldError] = useState(null);
  const mutation = useMutation(() => mapAlias(suggestion.id, Number(machineId)));

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!machineId) return setFieldError('Choose a machine.');
    const result = await mutation.run();
    if (result.ok) onMapped(result.data);
  };

  return (
    <Modal title="Map alias to machine" onClose={onClose}>
      <form className="form" onSubmit={onSubmit} noValidate>
        <p>
          Treat <strong>“{suggestion.rawText}”</strong> as a name for:
        </p>
        <ErrorMessage error={mutation.error} />
        <FormField label="Machine" required error={fieldError}>
          <select
            className="input"
            value={machineId}
            onChange={(e) => {
              setMachineId(e.target.value);
              setFieldError(null);
            }}
          >
            <option value="">Select a machine</option>
            {machines.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
                {m.id === suggestion.suggestedMachineId ? ' (suggested)' : ''}
              </option>
            ))}
          </select>
        </FormField>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
            {mutation.pending ? 'Mapping…' : 'Map alias'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export function AliasSuggestionsPage() {
  const { plantId, refreshBadges } = usePlant();
  const suggestions = useAsync(() => listAliasSuggestions(plantId), [plantId], { enabled: Boolean(plantId) });
  const options = useAsync(() => listFilterOptions(plantId), [plantId], { enabled: Boolean(plantId) });
  const [mapping, setMapping] = useState(null);
  const [notice, setNotice] = useState(null);
  const [busyId, setBusyId] = useState(null);
  const dismiss = useMutation(dismissAlias);

  const onDismiss = async (s) => {
    setBusyId(s.id);
    const result = await dismiss.run(s.id);
    setBusyId(null);
    if (result.ok) {
      suggestions.setData((list) => list.map((x) => (x.id === s.id ? result.data : x)));
      setNotice(`Dismissed “${s.rawText}”.`);
    }
  };

  const onMapped = (res) => {
    // AliasMappingResponse { suggestionId, machineId, alias, revalidatedItemCount }
    setMapping(null);
    setNotice(`Mapped “${res.alias}”. ${formatNumber(res.revalidatedItemCount)} queued rows were re-validated.`);
    suggestions.reload();
    refreshBadges();
  };

  return (
    <>
      <PageHeader
        breadcrumb={[{ label: 'Validation queue', to: '/validation' }, { label: 'Alias suggestions' }]}
        title="Alias suggestions"
        subtitle="Unrecognized machine names found in imports. Map them once and matching rows re-validate automatically."
      />
      <SuccessMessage>{notice}</SuccessMessage>
      <ErrorMessage error={dismiss.error} title="Could not dismiss" />
      <PlantGate>
        <div className="card">
          <AsyncView
            state={suggestions}
            isEmpty={(d) => !d.length}
            empty={<EmptyState title="No alias suggestions">Every imported machine name was recognized.</EmptyState>}
          >
            {(data) => (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Text in source</th>
                      <th className="num">Occurrences</th>
                      <th>Suggested machine</th>
                      <th className="num">Confidence</th>
                      <th>Status</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((s) => (
                      <tr key={s.id}>
                        <td style={{ fontWeight: 500 }}>{s.rawText}</td>
                        <td className="num">{formatNumber(s.occurrences)}</td>
                        <td>{s.suggestedMachineName ?? '—'}</td>
                        <td className="num">{formatConfidence(s.confidence)}</td>
                        <td>
                          <Badge>{s.status}</Badge>
                        </td>
                        <td>
                          <div className="actions" style={{ justifyContent: 'flex-end' }}>
                            <button type="button" className="btn btn-sm btn-primary" onClick={() => setMapping(s)} disabled={busyId === s.id}>
                              Map
                            </button>
                            <button type="button" className="btn btn-sm" onClick={() => onDismiss(s)} disabled={busyId === s.id}>
                              Dismiss
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </AsyncView>
        </div>
      </PlantGate>
      {mapping && (
        <MapModal suggestion={mapping} machines={options.data?.machines ?? []} onClose={() => setMapping(null)} onMapped={onMapped} />
      )}
    </>
  );
}
