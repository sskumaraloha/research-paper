import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { AsyncView, EmptyState, PageHeader, Tabs } from '../../../components/common';
import { globalSearch, searchMachines, searchRecords } from '../services/searchService';
import { formatDate, formatMinutes } from '../../../utils/format';

const Section = ({ title, count, children }) => (
  <div className="card">
    <div className="card-header">
      <h2>{title}</h2>
      <span className="subtle">{count}</span>
    </div>
    {children}
  </div>
);

/** RecordMatchResponse[] */
function RecordMatches({ records }) {
  return (
    <ul className="list">
      {records.map((r) => (
        <li key={r.id}>
          <div className="row between">
            <Link to={`/records/${r.id}`} style={{ fontWeight: 500 }}>
              {r.machineName} · {formatDate(r.recordDate)}
            </Link>
            <span className="subtle">
              {r.failureMode ?? ''} {r.downtimeMinutes ? `· ${formatMinutes(r.downtimeMinutes)}` : ''}
            </span>
          </div>
          {r.snippet && <div className="muted">{r.snippet}</div>}
        </li>
      ))}
    </ul>
  );
}

/** MachineMatch[] */
function MachineMatches({ machines }) {
  return (
    <ul className="list">
      {machines.map((m) => (
        <li key={m.id} className="row between">
          <Link to={`/machines/${m.id}`} style={{ fontWeight: 500 }}>
            {m.name}
          </Link>
          <span className="subtle">
            <span className="mono">{m.code}</span> {m.plantName && `· ${m.plantName}`}
          </span>
        </li>
      ))}
    </ul>
  );
}

function AllResults({ query }) {
  // GlobalSearchResponse { query, machines, records, parts, failureModes }
  const result = useAsync(() => globalSearch(query), [query]);
  const total = (d) => (d.machines?.length ?? 0) + (d.records?.length ?? 0) + (d.parts?.length ?? 0) + (d.failureModes?.length ?? 0);
  return (
    <AsyncView state={result} isEmpty={(d) => total(d) === 0} empty={<div className="card"><EmptyState title={`No results for “${query}”`} /></div>}>
      {(d) => (
        <div className="grid grid-2" style={{ alignItems: 'start' }}>
          {d.machines?.length > 0 && (
            <Section title="Machines" count={d.machines.length}>
              <MachineMatches machines={d.machines} />
            </Section>
          )}
          {d.parts?.length > 0 && (
            <Section title="Spare parts" count={d.parts.length}>
              <ul className="list">
                {d.parts.map((p) => (
                  <li key={p.id} className="row between">
                    <Link to={`/parts/${p.id}`} style={{ fontWeight: 500 }}>
                      {p.name}
                    </Link>
                    <span className="subtle mono">{p.partNumber}</span>
                  </li>
                ))}
              </ul>
            </Section>
          )}
          {d.records?.length > 0 && (
            <Section title="Maintenance records" count={d.records.length}>
              <RecordMatches records={d.records} />
            </Section>
          )}
          {d.failureModes?.length > 0 && (
            <Section title="Failure modes" count={d.failureModes.length}>
              <ul className="list">
                {d.failureModes.map((f) => (
                  <li key={f.id} className="row between">
                    <Link to={`/records?failureModeId=${f.id}`} style={{ fontWeight: 500 }}>
                      {f.name}
                    </Link>
                    <span className="subtle mono">{f.code}</span>
                  </li>
                ))}
              </ul>
            </Section>
          )}
        </div>
      )}
    </AsyncView>
  );
}

function ListResults({ query, loader, render, noun }) {
  const result = useAsync(() => loader(query), [query]);
  return (
    <div className="card">
      <AsyncView state={result} isEmpty={(d) => !d.length} empty={<EmptyState title={`No ${noun} match “${query}”`} />}>
        {render}
      </AsyncView>
    </div>
  );
}

export function SearchPage() {
  const [params, setParams] = useSearchParams();
  const query = (params.get('query') ?? '').trim();
  const [tab, setTab] = useState('all');
  const [input, setInput] = useState(query);

  const onSubmit = (e) => {
    e.preventDefault();
    if (input.trim()) setParams({ query: input.trim() });
  };

  return (
    <>
      <PageHeader title="Search" subtitle={query ? `Results for “${query}”` : 'Search across machines, records, parts and failure modes.'} />
      <form className="row" role="search" onSubmit={onSubmit}>
        <label className="sr-only" htmlFor="search-page-input">
          Search
        </label>
        <input
          id="search-page-input"
          className="input"
          style={{ flex: 1, maxWidth: 520 }}
          type="search"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Search…"
          autoFocus={!query}
        />
        <button type="submit" className="btn btn-primary">
          Search
        </button>
      </form>
      {query ? (
        <>
          <Tabs
            value={tab}
            onChange={setTab}
            tabs={[
              { value: 'all', label: 'All' },
              { value: 'records', label: 'Records' },
              { value: 'machines', label: 'Machines' },
            ]}
          />
          {tab === 'all' && <AllResults query={query} />}
          {tab === 'records' && (
            <ListResults query={query} loader={searchRecords} noun="records" render={(d) => <RecordMatches records={d} />} />
          )}
          {tab === 'machines' && (
            <ListResults query={query} loader={searchMachines} noun="machines" render={(d) => <MachineMatches machines={d} />} />
          )}
        </>
      ) : (
        <div className="card">
          <EmptyState title="Type something to search" />
        </div>
      )}
    </>
  );
}
