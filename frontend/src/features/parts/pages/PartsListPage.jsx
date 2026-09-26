import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { useSearchState } from '../../../hooks/useSearchState';
import { AsyncView, EmptyState, FormField, PageHeader, Pagination } from '../../../components/common';
import { listParts } from '../services/partService';
import { formatDate, formatNumber } from '../../../utils/format';

export function PartsListPage() {
  const navigate = useNavigate();
  const [filters, setFilters] = useSearchState({ query: '', page: 0 });
  const [queryInput, setQueryInput] = useState(filters.query);

  useEffect(() => {
    const t = setTimeout(() => queryInput !== filters.query && setFilters({ query: queryInput }), 350);
    return () => clearTimeout(t);
  }, [queryInput, filters.query, setFilters]);

  // listParts is not plant-scoped in the contract (API_ANALYSIS Q9).
  const parts = useAsync(() => listParts({ query: filters.query, page: filters.page, size: 20 }), [filters.query, filters.page]);

  return (
    <>
      <PageHeader title="Spare parts" subtitle="Parts referenced in maintenance records, with usage history." />
      <div className="card">
        <div className="card-body filters">
          <FormField label="Search" className="grow">
            <input
              className="input"
              type="search"
              placeholder="Part number or name"
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
            />
          </FormField>
        </div>
        <AsyncView state={parts} isEmpty={(d) => !d.content?.length} empty={<EmptyState title="No parts found" />}>
          {(data) => (
            <>
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Part number</th>
                      <th>Name</th>
                      <th className="hide-sm">Category</th>
                      <th className="num">Times used</th>
                      <th>Last used</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((p) => (
                      <tr
                        key={p.id}
                        className="clickable"
                        tabIndex={0}
                        onClick={() => navigate(`/parts/${p.id}`)}
                        onKeyDown={(e) => e.key === 'Enter' && navigate(`/parts/${p.id}`)}
                      >
                        <td className="mono">{p.partNumber ?? '—'}</td>
                        <td style={{ fontWeight: 500 }}>{p.name}</td>
                        <td className="hide-sm">{p.category ?? '—'}</td>
                        <td className="num">{formatNumber(p.usageCount)}</td>
                        <td>{formatDate(p.lastUsedDate)}</td>
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
