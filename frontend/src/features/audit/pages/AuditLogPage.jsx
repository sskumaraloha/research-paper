import { useEffect, useState } from 'react';
import { usePlant } from '../../../context/PlantContext';
import { useAsync } from '../../../hooks/useAsync';
import { useSearchState } from '../../../hooks/useSearchState';
import { AsyncView, Badge, EmptyState, FormField, PageHeader, Pagination } from '../../../components/common';
import { listAuditLogs } from '../services/auditService';
import { formatDateTime } from '../../../utils/format';

export function AuditLogPage() {
  const { plants } = usePlant();
  // plantId is optional for GET /api/audit; empty = all plants.
  const [filters, setFilters] = useSearchState({ plantId: '', action: '', page: 0 });
  const [actionInput, setActionInput] = useState(filters.action);

  useEffect(() => {
    const t = setTimeout(() => actionInput !== filters.action && setFilters({ action: actionInput }), 350);
    return () => clearTimeout(t);
  }, [actionInput, filters.action, setFilters]);

  const logs = useAsync(() => listAuditLogs({ ...filters, size: 50 }), [filters.plantId, filters.action, filters.page]);

  return (
    <>
      <PageHeader title="Audit log" subtitle="Who changed what, and when." />
      <div className="card">
        <div className="card-body filters">
          <FormField label="Plant">
            <select className="input" value={filters.plantId} onChange={(e) => setFilters({ plantId: e.target.value })}>
              <option value="">All plants</option>
              {plants.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Action">
            <input className="input" placeholder="Any action" value={actionInput} onChange={(e) => setActionInput(e.target.value)} />
          </FormField>
        </div>
        <AsyncView state={logs} isEmpty={(d) => !d.content?.length} empty={<EmptyState title="No audit entries" />}>
          {(data) => (
            <>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>When</th>
                      <th>Actor</th>
                      <th>Action</th>
                      <th>Entity</th>
                      <th className="hide-sm">Detail</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((a) => (
                      <tr key={a.id}>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(a.occurredAt)}</td>
                        <td>{a.actorName ?? (a.actorId ? `#${a.actorId}` : 'System')}</td>
                        <td>
                          <Badge>{a.action}</Badge>
                        </td>
                        <td>
                          {a.entityType ?? '—'}
                          {a.entityId ? <span className="subtle"> #{a.entityId}</span> : null}
                        </td>
                        <td className="hide-sm truncate" title={a.detail}>
                          {a.detail ?? '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination pageData={data} onPageChange={(page) => setFilters({ page })} />
            </>
          )}
        </AsyncView>
      </div>
    </>
  );
}
