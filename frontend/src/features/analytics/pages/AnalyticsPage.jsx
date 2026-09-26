import { Link } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync } from '../../../hooks/useAsync';
import { useSearchState } from '../../../hooks/useSearchState';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, EmptyState, FormField, PageHeader } from '../../../components/common';
import { BarList } from '../../../components/charts/BarList';
import { ColumnChart } from '../../../components/charts/ColumnChart';
import { ChartFrame } from '../../../components/charts/ChartFrame';
import {
  downtimeTrend,
  failureModeStats,
  lineDowntimeShare,
  pareto,
  partReplacementIntervals,
  topMachinesByDowntime,
} from '../services/analyticsService';
import { formatMinutes, formatNumber, formatPct } from '../../../utils/format';

const none = <EmptyState title="No data for this period" />;

function TrendChart({ plantId, months }) {
  const trend = useAsync(() => downtimeTrend({ plantId, months }), [plantId, months]);
  const points = trend.data?.points ?? [];
  return (
    <ChartFrame
      title={`Downtime by month (last ${months} months)`}
      rows={points}
      columns={[
        { label: 'Month', render: (p) => p.yearMonth },
        { label: 'Records', numeric: true, render: (p) => formatNumber(p.recordCount) },
        { label: 'Downtime', numeric: true, render: (p) => formatMinutes(p.downtimeMinutes) },
      ]}
    >
      <AsyncView state={trend} isEmpty={(d) => !d.points?.length} empty={none}>
        {(d) => (
          <ColumnChart
            ariaLabel="Downtime minutes per month"
            formatValue={(v) => formatNumber(Math.round(v))}
            points={d.points.map((p) => ({
              key: p.yearMonth,
              label: p.yearMonth,
              value: p.downtimeMinutes,
              tooltip: `${p.yearMonth}: ${formatMinutes(p.downtimeMinutes)} · ${formatNumber(p.recordCount)} records`,
            }))}
          />
        )}
      </AsyncView>
    </ChartFrame>
  );
}

function ParetoChart({ plantId, from, to }) {
  const data = useAsync(() => pareto({ plantId, from, to }), [plantId, from, to]);
  return (
    <ChartFrame
      title="Downtime Pareto by failure mode"
      rows={data.data ?? []}
      columns={[
        { label: 'Failure mode', render: (b) => b.failureMode },
        { label: 'Category', render: (b) => b.category ?? '—' },
        { label: 'Records', numeric: true, render: (b) => formatNumber(b.recordCount) },
        { label: 'Downtime', numeric: true, render: (b) => formatMinutes(b.downtimeMinutes) },
        { label: 'Share', numeric: true, render: (b) => formatPct(b.downtimeSharePct) },
        { label: 'Cumulative', numeric: true, render: (b) => formatPct(b.cumulativeSharePct) },
      ]}
    >
      <AsyncView state={data} isEmpty={(d) => !d.length} empty={none}>
        {(rows) => (
          <BarList
            ariaLabel="Downtime share by failure mode"
            items={rows.map((b) => ({
              key: b.failureModeId ?? b.failureMode,
              label: b.failureMode,
              value: b.downtimeSharePct,
              display: `${formatPct(b.downtimeSharePct)} · cum. ${formatPct(b.cumulativeSharePct, 0)}`,
              tooltip: `${b.failureMode}: ${formatMinutes(b.downtimeMinutes)} (${formatPct(b.downtimeSharePct)}), cumulative ${formatPct(b.cumulativeSharePct)}`,
            }))}
          />
        )}
      </AsyncView>
    </ChartFrame>
  );
}

function LineShareChart({ plantId, from, to }) {
  const data = useAsync(() => lineDowntimeShare({ plantId, from, to }), [plantId, from, to]);
  return (
    <ChartFrame
      title="Downtime share by line"
      rows={data.data ?? []}
      columns={[
        { label: 'Line', render: (l) => l.lineName },
        { label: 'Records', numeric: true, render: (l) => formatNumber(l.recordCount) },
        { label: 'Downtime', numeric: true, render: (l) => formatMinutes(l.downtimeMinutes) },
        { label: 'Share', numeric: true, render: (l) => formatPct(l.sharePct) },
      ]}
    >
      <AsyncView state={data} isEmpty={(d) => !d.length} empty={none}>
        {(rows) => (
          <BarList
            ariaLabel="Downtime share by line"
            items={rows.map((l) => ({
              key: l.lineId ?? l.lineName,
              label: l.lineName ?? 'No line',
              value: l.sharePct,
              display: formatPct(l.sharePct),
              tooltip: `${l.lineName}: ${formatMinutes(l.downtimeMinutes)} over ${formatNumber(l.recordCount)} records`,
            }))}
          />
        )}
      </AsyncView>
    </ChartFrame>
  );
}

function TopMachinesChart({ plantId, from, to }) {
  const data = useAsync(() => topMachinesByDowntime({ plantId, from, to, limit: 10 }), [plantId, from, to]);
  return (
    <ChartFrame
      title="Top 10 machines by downtime"
      rows={data.data ?? []}
      columns={[
        { label: 'Machine', render: (m) => <Link to={`/machines/${m.machineId}`}>{m.machineName}</Link> },
        { label: 'Code', render: (m) => m.machineCode },
        { label: 'Records', numeric: true, render: (m) => formatNumber(m.recordCount) },
        { label: 'Downtime', numeric: true, render: (m) => formatMinutes(m.downtimeMinutes) },
      ]}
    >
      <AsyncView state={data} isEmpty={(d) => !d.length} empty={none}>
        {(rows) => (
          <BarList
            ariaLabel="Top machines by downtime"
            items={rows.map((m) => ({
              key: m.machineId,
              label: m.machineName ?? m.machineCode,
              value: m.downtimeMinutes,
              display: formatMinutes(m.downtimeMinutes),
              tooltip: `${m.machineName}: ${formatMinutes(m.downtimeMinutes)} · ${formatNumber(m.recordCount)} records`,
            }))}
          />
        )}
      </AsyncView>
    </ChartFrame>
  );
}

function FailureModeTable({ plantId, from, to }) {
  const data = useAsync(() => failureModeStats({ plantId, from, to }), [plantId, from, to]);
  return (
    <div className="card">
      <div className="card-header">
        <h2>Failure mode statistics</h2>
      </div>
      <AsyncView state={data} isEmpty={(d) => !d.length} empty={none}>
        {(rows) => (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Failure mode</th>
                  <th className="hide-sm">Category</th>
                  <th className="num">Records</th>
                  <th className="num">Total downtime</th>
                  <th className="num">Avg / event</th>
                  <th className="num">Machines</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((f) => (
                  <tr key={f.failureModeId ?? f.name}>
                    <td>{f.name}</td>
                    <td className="hide-sm">{f.category ?? '—'}</td>
                    <td className="num">{formatNumber(f.recordCount)}</td>
                    <td className="num">{formatMinutes(f.totalDowntimeMinutes)}</td>
                    <td className="num">{formatMinutes(f.avgDowntimeMinutes && Math.round(f.avgDowntimeMinutes))}</td>
                    <td className="num">{formatNumber(f.machinesAffected)}</td>
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

function PartIntervalsTable({ plantId }) {
  const data = useAsync(() => partReplacementIntervals(plantId), [plantId]);
  return (
    <div className="card">
      <div className="card-header">
        <h2>Part replacement intervals</h2>
        <span className="subtle">All time</span>
      </div>
      <AsyncView state={data} isEmpty={(d) => !d.length} empty={<EmptyState title="No part usage recorded" />}>
        {(rows) => (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Part</th>
                  <th className="num">Times used</th>
                  <th className="num">Avg interval</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((p) => (
                  <tr key={p.partId}>
                    <td>
                      <Link to={`/parts/${p.partId}`}>{p.partName}</Link>
                    </td>
                    <td className="num">{formatNumber(p.usageCount)}</td>
                    <td className="num">{p.avgIntervalDays ? `${formatNumber(p.avgIntervalDays, 1)} days` : '—'}</td>
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

export function AnalyticsPage() {
  const { plantId } = usePlant();
  const [filters, setFilters] = useSearchState({ from: '', to: '', months: 6 });
  const { from, to, months } = filters;
  const rangeError = from && to && from > to ? '"From" must be before "To".' : null;
  const range = rangeError ? { from: '', to: '' } : { from, to };

  return (
    <>
      <PageHeader title="Analytics" subtitle="Where downtime comes from, and how it is trending." />
      <PlantGate>
        <div className="card card-body filters">
          <FormField label="From" error={rangeError}>
            <input className="input" type="date" value={from} onChange={(e) => setFilters({ from: e.target.value })} />
          </FormField>
          <FormField label="To">
            <input className="input" type="date" value={to} onChange={(e) => setFilters({ to: e.target.value })} />
          </FormField>
          <FormField label="Trend window" hint="Applies to the monthly trend only.">
            <select className="input" value={months} onChange={(e) => setFilters({ months: e.target.value })}>
              {[3, 6, 12, 24].map((m) => (
                <option key={m} value={m}>
                  {m} months
                </option>
              ))}
            </select>
          </FormField>
          {(from || to) && (
            <button type="button" className="btn btn-ghost" onClick={() => setFilters({ from: '', to: '' })}>
              All time
            </button>
          )}
        </div>
        <TrendChart plantId={plantId} months={months} />
        <div className="grid grid-2">
          <ParetoChart plantId={plantId} {...range} />
          <LineShareChart plantId={plantId} {...range} />
        </div>
        <div className="grid grid-2">
          <TopMachinesChart plantId={plantId} {...range} />
          <PartIntervalsTable plantId={plantId} />
        </div>
        <FailureModeTable plantId={plantId} {...range} />
      </PlantGate>
    </>
  );
}
