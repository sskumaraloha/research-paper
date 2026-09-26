import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useSearchState } from '../../../hooks/useSearchState';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, EmptyState, ErrorMessage, FormField, PageHeader, Pagination } from '../../../components/common';
import { RecordsTable } from '../components/RecordsTable';
import { exportRecords, listFilterOptions, listRecords } from '../services/recordService';
import { downloadText } from '../../../utils/download';

const PAGE_SIZE = 20;
const FILTER_DEFAULTS = { machineId: '', lineId: '', failureModeId: '', from: '', to: '', text: '', page: 0 };

export function RecordsListPage() {
  const { plantId } = usePlant();
  const [filters, setFilters] = useSearchState(FILTER_DEFAULTS);
  const [textInput, setTextInput] = useState(filters.text);

  useEffect(() => {
    const t = setTimeout(() => textInput !== filters.text && setFilters({ text: textInput }), 350);
    return () => clearTimeout(t);
  }, [textInput, filters.text, setFilters]);

  // RecordFilterOptionsResponse { machines: NamedRef[], lines: NamedRef[], failureModes: NamedRef[] }
  const options = useAsync(() => listFilterOptions(plantId), [plantId], { enabled: Boolean(plantId) });
  const { page, ...criteria } = filters;
  const records = useAsync(
    () => listRecords({ plantId, ...criteria, page, size: PAGE_SIZE }),
    [plantId, ...Object.values(filters)],
    { enabled: Boolean(plantId) },
  );
  const exporter = useMutation(async () => {
    const file = await exportRecords({ plantId, ...criteria });
    downloadText(file.content, file.filename, file.contentType);
  });
  const hasFilters = Object.values(criteria).some(Boolean);
  const rangeError = criteria.from && criteria.to && criteria.from > criteria.to ? '"From" must be before "To".' : null;

  const select = (key, label, items) => (
    <FormField label={label}>
      <select className="input" value={filters[key]} onChange={(e) => setFilters({ [key]: e.target.value })}>
        <option value="">All</option>
        {(items ?? []).map((o) => (
          <option key={o.id} value={o.id}>
            {o.name}
          </option>
        ))}
      </select>
    </FormField>
  );

  return (
    <>
      <PageHeader
        title="Maintenance records"
        subtitle="Every logged repair, inspection and breakdown for this plant."
        actions={
          <>
            <button type="button" className="btn" onClick={() => exporter.run()} disabled={exporter.pending || !plantId}>
              {exporter.pending ? 'Exporting…' : 'Export CSV'}
            </button>
            <Link className="btn btn-primary" to="/records/new">
              New record
            </Link>
          </>
        }
      />
      <ErrorMessage error={exporter.error} title="Export failed" />
      <PlantGate>
        <div className="card">
          <div className="card-body filters">
            <FormField label="Search text" className="grow">
              <input
                className="input"
                type="search"
                placeholder="Description, action, technician…"
                value={textInput}
                onChange={(e) => setTextInput(e.target.value)}
              />
            </FormField>
            {select('machineId', 'Machine', options.data?.machines)}
            {select('lineId', 'Line', options.data?.lines)}
            {select('failureModeId', 'Failure mode', options.data?.failureModes)}
            <FormField label="From" error={rangeError}>
              <input className="input" type="date" value={filters.from} onChange={(e) => setFilters({ from: e.target.value })} />
            </FormField>
            <FormField label="To">
              <input className="input" type="date" value={filters.to} onChange={(e) => setFilters({ to: e.target.value })} />
            </FormField>
            {hasFilters && (
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setTextInput('');
                  setFilters({ ...FILTER_DEFAULTS, page: undefined });
                }}
              >
                Clear filters
              </button>
            )}
          </div>
          <AsyncView
            state={records}
            isEmpty={(d) => !d.content?.length}
            empty={<EmptyState title="No records found">{hasFilters ? 'Try widening the filters.' : 'Log the first maintenance record.'}</EmptyState>}
          >
            {(data) => (
              <>
                <RecordsTable records={data.content} />
                <Pagination pageData={data} onPageChange={(p) => setFilters({ page: p })} />
              </>
            )}
          </AsyncView>
        </div>
      </PlantGate>
    </>
  );
}
