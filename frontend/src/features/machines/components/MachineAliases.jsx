import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { AsyncView, Badge, EmptyState, ErrorMessage, FormField } from '../../../components/common';
import { addAlias, listAliases } from '../services/machineService';

/** Aliases are alternative names used to match imported rows to this machine. */
export function MachineAliases({ machineId }) {
  const aliases = useAsync(() => listAliases(machineId), [machineId]);
  // AddAliasRequest { alias* (≤150) }
  const form = useContractForm('AddAliasRequest', { alias: '' });
  const mutation = useMutation((payload) => addAlias(machineId, payload.alias));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) {
      aliases.setData((list) => [...(list ?? []), result.data]);
      form.setValues({ alias: '' });
    } else form.applyServerErrors(result.error);
  };

  return (
    <div className="card-body stack">
      <form className="filters" onSubmit={onSubmit} noValidate>
        <FormField label="New alias" className="grow" error={form.errors.alias}>
          <input className="input" placeholder="e.g. Press #2, PRS-02" {...form.bind('alias')} />
        </FormField>
        <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
          {mutation.pending ? 'Adding…' : 'Add alias'}
        </button>
      </form>
      <ErrorMessage error={mutation.error} title="Could not add alias" />
      <AsyncView state={aliases} isEmpty={(d) => !d.length} empty={<EmptyState title="No aliases yet" />}>
        {(data) => (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Alias</th>
                  <th>Source</th>
                </tr>
              </thead>
              <tbody>
                {data.map((a) => (
                  <tr key={a.id}>
                    <td>{a.alias}</td>
                    <td>
                      <Badge>{a.source}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AsyncView>
    </div>
  );
}
