import { Link } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync } from '../../../hooks/useAsync';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, Badge, EmptyState, PageHeader, StatTile } from '../../../components/common';
import { BarList } from '../../../components/charts/BarList';
import { ColumnChart } from '../../../components/charts/ColumnChart';
import { ChartFrame } from '../../../components/charts/ChartFrame';
import { InsightCard } from '../../insights/components/InsightCard';
import { plantKpis } from '../services/dashboardService';
import { downtimeTrend, topMachinesByDowntime } from '../../analytics/services/analyticsService';
import { listDue } from '../../schedules/services/scheduleService';
import { listInsights } from '../../insights/services/insightService';
import { pendingCount } from '../../validation/services/validationService';
import { formatDate, formatMinutes, formatNumber, formatPct } from '../../../utils/format';

function KpiTiles({ plantId }) {
  const kpis = useAsync(() => plantKpis(plantId), [plantId]);
  return (
    <AsyncView state={kpis} isEmpty={(d) => !d.kpis?.length} empty={<EmptyState title="No KPIs available" />}>
      {(data) => (
        <div className="stack" style={{ gap: 8 }}>
          <div className="subtle">
            {formatDate(data.windowFrom)} – {formatDate(data.windowTo)}
          </div>
          <div className="tiles">
            {data.kpis.map((k) => (
              <StatTile
                key={k.key}
                label={k.label}
                value={formatNumber(k.value, 1)}
                foot={
                  k.changePct !== null && k.changePct !== undefined
                    ? `${k.changePct > 0 ? '▲' : k.changePct < 0 ? '▼' : '■'} ${formatPct(Math.abs(k.changePct))} vs previous (${formatNumber(k.previousValue, 1)})`
                    : null
                }
              />
            ))}
          </div>
        </div>
      )}
    </AsyncView>
  );
}

function TrendCard({ plantId }) {
  const trend = useAsync(() => downtimeTrend({ plantId, months: 6 }), [plantId]);
  const points = trend.data?.points ?? [];
  return (
    <ChartFrame
      title="Downtime trend (6 months)"
      rows={points}
      columns={[
        { label: 'Month', render: (p) => p.yearMonth },
        { label: 'Records', numeric: true, render: (p) => formatNumber(p.recordCount) },
        { label: 'Downtime', numeric: true, render: (p) => formatMinutes(p.downtimeMinutes) },
      ]}
    >
      <AsyncView state={trend} isEmpty={(d) => !d.points?.length} empty={<EmptyState title="No trend data" />}>
        {(data) => (
          <ColumnChart
            ariaLabel="Downtime minutes per month"
            formatValue={(v) => formatNumber(Math.round(v))}
            points={data.points.map((p) => ({
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

function TopMachinesCard({ plantId }) {
  const top = useAsync(() => topMachinesByDowntime({ plantId, limit: 5 }), [plantId]);
  return (
    <ChartFrame
      title="Top machines by downtime"
      rows={top.data ?? []}
      columns={[
        { label: 'Machine', render: (m) => m.machineName },
        { label: 'Records', numeric: true, render: (m) => formatNumber(m.recordCount) },
        { label: 'Downtime', numeric: true, render: (m) => formatMinutes(m.downtimeMinutes) },
      ]}
    >
      <AsyncView state={top} isEmpty={(d) => !d.length} empty={<EmptyState title="No downtime recorded" />}>
        {(data) => (
          <BarList
            ariaLabel="Top machines by downtime"
            items={data.map((m) => ({
              key: m.machineId,
              label: m.machineName ?? m.machineCode,
              value: m.downtimeMinutes,
              display: formatMinutes(m.downtimeMinutes),
              tooltip: `${m.machineName}: ${formatMinutes(m.downtimeMinutes)} over ${formatNumber(m.recordCount)} records`,
            }))}
          />
        )}
      </AsyncView>
    </ChartFrame>
  );
}

function DueSchedulesCard({ plantId }) {
  const due = useAsync(() => listDue(plantId), [plantId]);
  return (
    <div className="card">
      <div className="card-header">
        <h2>Due maintenance</h2>
        <Link to="/schedules">All schedules</Link>
      </div>
      <AsyncView state={due} isEmpty={(d) => !d.length} empty={<EmptyState title="Nothing due">All schedules are up to date.</EmptyState>}>
        {(data) => (
          <ul className="list">
            {data.slice(0, 6).map((s) => (
              <li key={s.id} className="row between">
                <div>
                  <div style={{ fontWeight: 500 }}>{s.title}</div>
                  <Link className="subtle" to={`/machines/${s.machineId}`}>
                    {s.machineName}
                  </Link>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <Badge>{s.status}</Badge>
                  <div className="subtle">
                    {formatDate(s.nextDueOn)}
                    {s.daysUntilDue !== null && s.daysUntilDue !== undefined && ` · ${s.daysUntilDue} days`}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </AsyncView>
    </div>
  );
}

function InsightsCard({ plantId }) {
  const insights = useAsync(() => listInsights(plantId), [plantId]);
  return (
    <div className="card">
      <div className="card-header">
        <h2>Latest insights</h2>
        <Link to="/insights">All insights</Link>
      </div>
      <div className="card-body">
        <AsyncView state={insights} isEmpty={(d) => !d.length} empty={<EmptyState title="No insights yet" />}>
          {(data) => (
            <div className="stack">
              {data.slice(0, 3).map((i) => (
                <InsightCard key={i.id} insight={i} />
              ))}
            </div>
          )}
        </AsyncView>
      </div>
    </div>
  );
}

/** CountResponse from GET /api/validation/pending-count — shown only when rows await review. */
function PendingValidationBanner({ plantId }) {
  const pending = useAsync(() => pendingCount(plantId), [plantId]);
  const count = pending.data?.count ?? 0;
  if (!count) return null;
  return (
    <div className="alert alert-info">
      <div className="alert-body">
        <strong>{formatNumber(count)}</strong> imported {count === 1 ? 'row is' : 'rows are'} waiting for review.
      </div>
      <Link className="btn btn-sm" to="/validation">
        Review now
      </Link>
    </div>
  );
}

export function DashboardPage() {
  const { plant, plantId } = usePlant();
  return (
    <>
      <PageHeader
        title="Dashboard"
        subtitle={plant ? `${plant.name}${plant.location ? ` · ${plant.location}` : ''}` : undefined}
        actions={
          <>
            <Link className="btn" to="/imports">
              Import file
            </Link>
            <Link className="btn btn-primary" to="/records/new">
              New record
            </Link>
          </>
        }
      />
      <PlantGate>
        <PendingValidationBanner plantId={plantId} />
        <KpiTiles plantId={plantId} />
        <div className="grid grid-2">
          <TrendCard plantId={plantId} />
          <TopMachinesCard plantId={plantId} />
        </div>
        <div className="grid grid-2" style={{ alignItems: 'start' }}>
          <DueSchedulesCard plantId={plantId} />
          <InsightsCard plantId={plantId} />
        </div>
      </PlantGate>
    </>
  );
}
