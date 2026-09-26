import { Link, useParams } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { EmptyState, ErrorMessage, LoadingState, PageHeader, StatTile } from '../../../components/common';
import { RecordsTable } from '../../records/components/RecordsTable';
import { getPartDetail } from '../services/partService';
import { formatDate, formatNumber } from '../../../utils/format';

export function PartDetailPage() {
  const { partId } = useParams();
  const part = useAsync(() => getPartDetail(partId), [partId]);
  if (part.loading && !part.data) return <LoadingState />;
  if (part.error) return <ErrorMessage error={part.error} onRetry={part.reload} />;
  const p = part.data;

  return (
    <>
      <PageHeader
        breadcrumb={[{ label: 'Spare parts', to: '/parts' }, { label: p.partNumber ?? `#${p.id}` }]}
        title={p.name}
        subtitle={[p.partNumber, p.category].filter(Boolean).join(' · ')}
      />
      <div className="tiles">
        <StatTile label="Times used" value={formatNumber(p.usageCount)} />
        <StatTile label="Last used" value={formatDate(p.lastUsedDate)} />
        <StatTile
          label="Avg replacement interval"
          value={p.avgReplacementIntervalDays ? formatNumber(p.avgReplacementIntervalDays, 1) : '—'}
          unit={p.avgReplacementIntervalDays ? 'days' : undefined}
        />
        <StatTile label="Machines" value={formatNumber(p.machinesUsedOn?.length ?? 0)} />
      </div>
      <div className="card">
        <div className="card-header">
          <h2>Used on machines</h2>
        </div>
        <div className="card-body">
          {p.machinesUsedOn?.length ? (
            <div className="chips">
              {p.machinesUsedOn.map((m) => (
                <Link key={m.id} className="chip" to={`/machines/${m.id}`}>
                  {m.name}
                </Link>
              ))}
            </div>
          ) : (
            <span className="muted">Not linked to any machine yet.</span>
          )}
        </div>
      </div>
      <div className="card">
        <div className="card-header">
          <h2>Recent records</h2>
        </div>
        {p.recentRecords?.length ? <RecordsTable records={p.recentRecords} /> : <EmptyState title="No records reference this part" />}
      </div>
    </>
  );
}
