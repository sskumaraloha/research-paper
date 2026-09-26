import { useAsync, useMutation } from '../../../hooks/useAsync';
import { AsyncView, EmptyState, ErrorMessage } from '../../../components/common';
import { InsightCard } from '../../insights/components/InsightCard';
import { getInsights, recomputeInsights } from '../services/machineService';

export function MachineInsights({ machineId }) {
  const insights = useAsync(() => getInsights(machineId), [machineId]);
  const recompute = useMutation(() => recomputeInsights(machineId));

  const onRecompute = async () => {
    const result = await recompute.run();
    if (result.ok) insights.setData(result.data);
  };

  return (
    <div className="card-body stack">
      <div className="row between">
        <span className="muted">Automatically detected patterns for this machine.</span>
        <button type="button" className="btn btn-sm" onClick={onRecompute} disabled={recompute.pending}>
          {recompute.pending ? 'Recomputing…' : 'Recompute insights'}
        </button>
      </div>
      <ErrorMessage error={recompute.error} title="Recompute failed" />
      <AsyncView state={insights} isEmpty={(d) => !d.length} empty={<EmptyState title="No insights for this machine" />}>
        {(data) => (
          <div className="grid grid-2">
            {data.map((i) => (
              <InsightCard key={i.id} insight={i} showMachine={false} />
            ))}
          </div>
        )}
      </AsyncView>
    </div>
  );
}
