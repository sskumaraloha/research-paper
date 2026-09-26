import { useState } from 'react';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, EmptyState, ErrorMessage, FormField, PageHeader, SuccessMessage } from '../../../components/common';
import { InsightCard } from '../components/InsightCard';
import { insightCount, listInsights, recomputeAll } from '../services/insightService';

export function InsightsPage() {
  const { plantId, refreshBadges } = usePlant();
  const insights = useAsync(() => listInsights(plantId), [plantId], { enabled: Boolean(plantId) });
  const count = useAsync(() => insightCount(plantId), [plantId], { enabled: Boolean(plantId) });
  const recompute = useMutation(() => recomputeAll(plantId));
  const [notice, setNotice] = useState(null);
  const [severity, setSeverity] = useState('');
  const [type, setType] = useState('');

  const onRecompute = async () => {
    const result = await recompute.run();
    if (result.ok) {
      insights.setData(result.data);
      setNotice(`Recomputed: ${result.data.length} insights.`);
      count.reload();
      refreshBadges();
    }
  };

  // Filter options come from values present in the response (the contract defines no enums).
  const list = insights.data ?? [];
  const severities = [...new Set(list.map((i) => i.severity).filter(Boolean))];
  const types = [...new Set(list.map((i) => i.type).filter(Boolean))];

  return (
    <>
      <PageHeader
        title="Insights"
        subtitle={
          count.data?.count !== undefined
            ? `${count.data.count} active insights detected from maintenance history.`
            : 'Patterns detected from maintenance history.'
        }
        actions={
          <button type="button" className="btn btn-primary" onClick={onRecompute} disabled={recompute.pending || !plantId}>
            {recompute.pending ? 'Recomputing…' : 'Recompute all'}
          </button>
        }
      />
      <SuccessMessage>{notice}</SuccessMessage>
      <ErrorMessage error={recompute.error} title="Recompute failed" />
      <PlantGate>
        {list.length > 0 && (severities.length > 1 || types.length > 1) && (
          <div className="filters">
            <FormField label="Severity">
              <select className="input" value={severity} onChange={(e) => setSeverity(e.target.value)}>
                <option value="">All</option>
                {severities.map((s) => (
                  <option key={s}>{s}</option>
                ))}
              </select>
            </FormField>
            <FormField label="Type">
              <select className="input" value={type} onChange={(e) => setType(e.target.value)}>
                <option value="">All</option>
                {types.map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </select>
            </FormField>
          </div>
        )}
        <AsyncView
          state={insights}
          isEmpty={(d) => !d.length}
          empty={
            <div className="card">
              <EmptyState title="No insights yet">Recompute to analyse the latest maintenance records.</EmptyState>
            </div>
          }
        >
          {(data) => {
            const filtered = data.filter((i) => (!severity || i.severity === severity) && (!type || i.type === type));
            return filtered.length ? (
              <div className="grid grid-2">
                {filtered.map((i) => (
                  <InsightCard key={i.id} insight={i} />
                ))}
              </div>
            ) : (
              <div className="card">
                <EmptyState title="No insights match these filters" />
              </div>
            );
          }}
        </AsyncView>
      </PlantGate>
    </>
  );
}
