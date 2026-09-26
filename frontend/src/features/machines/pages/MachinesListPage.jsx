import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync } from '../../../hooks/useAsync';
import { useSearchState } from '../../../hooks/useSearchState';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, Badge, EmptyState, FormField, PageHeader, Pagination } from '../../../components/common';
import { listMachines } from '../services/machineService';
import { listLines } from '../../plants/services/plantService';
import { formatDate, formatMinutes, formatNumber } from '../../../utils/format';

const PAGE_SIZE = 20;

export function MachinesListPage() {
  const { plantId } = usePlant();
  const navigate = useNavigate();
  const [filters, setFilters] = useSearchState({ query: '', lineId: '', criticality: '', page: 0 });
  const [queryInput, setQueryInput] = useState(filters.query);

  // Debounce free-text search.
  useEffect(() => {
    const t = setTimeout(() => queryInput !== filters.query && setFilters({ query: queryInput }), 350);
    return () => clearTimeout(t);
  }, [queryInput, filters.query, setFilters]);

  const lines = useAsync(() => listLines(plantId), [plantId], { enabled: Boolean(plantId) });
  const machines = useAsync(
    () => listMachines({ plantId, ...filters, size: PAGE_SIZE }),
    [plantId, filters.query, filters.lineId, filters.criticality, filters.page],
    { enabled: Boolean(plantId) },
  );
  const criticalities = [...new Set((machines.data?.content ?? []).map((m) => m.criticality).filter(Boolean))];

  return (
    <>
      <PageHeader
        title="Machines"
        subtitle="Equipment registered for this plant."
        actions={
          <Link className="btn btn-primary" to="/machines/new">
            Add machine
          </Link>
        }
      />
      <PlantGate>
        <div className="card">
          <div className="card-body filters">
            <FormField label="Search" className="grow">
              <input
                className="input"
                type="search"
                placeholder="Name or code"
                value={queryInput}
                onChange={(e) => setQueryInput(e.target.value)}
              />
            </FormField>
            <FormField label="Line">
              <select className="input" value={filters.lineId} onChange={(e) => setFilters({ lineId: e.target.value })}>
                <option value="">All lines</option>
                {(lines.data ?? []).map((l) => (
                  <option key={l.id} value={l.id}>
                    {l.name}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Criticality">
              <input
                className="input"
                list="criticality-options"
                placeholder="Any"
                value={filters.criticality}
                onChange={(e) => setFilters({ criticality: e.target.value })}
              />
            </FormField>
            <datalist id="criticality-options">
              {criticalities.map((c) => (
                <option key={c} value={c} />
              ))}
            </datalist>
          </div>
          <AsyncView
            state={machines}
            isEmpty={(d) => !d.content?.length}
            empty={
              <EmptyState title="No machines found">
                {filters.query || filters.lineId || filters.criticality ? 'Try clearing the filters.' : 'Add the first machine for this plant.'}
              </EmptyState>
            }
          >
            {(data) => (
              <>
                <div className="table-wrap">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Code</th>
                        <th>Name</th>
                        <th className="hide-sm">Line</th>
                        <th>Criticality</th>
                        <th>Status</th>
                        <th className="num">Records</th>
                        <th className="num">Downtime</th>
                        <th className="hide-sm">Last maintenance</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.content.map((m) => (
                        <tr
                          key={m.id}
                          className="clickable"
                          tabIndex={0}
                          onClick={() => navigate(`/machines/${m.id}`)}
                          onKeyDown={(e) => e.key === 'Enter' && navigate(`/machines/${m.id}`)}
                        >
                          <td className="mono">{m.code}</td>
                          <td style={{ fontWeight: 500 }}>{m.name}</td>
                          <td className="hide-sm">{m.lineName ?? '—'}</td>
                          <td>
                            <Badge>{m.criticality}</Badge>
                          </td>
                          <td>
                            <Badge>{m.status}</Badge>
                          </td>
                          <td className="num">{formatNumber(m.recordCount)}</td>
                          <td className="num">{formatMinutes(m.totalDowntimeMinutes)}</td>
                          <td className="hide-sm">{formatDate(m.lastMaintenanceDate)}</td>
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
      </PlantGate>
    </>
  );
}
