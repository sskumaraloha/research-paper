import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { AsyncView, Badge, EmptyState, ErrorMessage, LoadingState, PageHeader, StatTile, Tabs } from '../../../components/common';
import { BarList } from '../../../components/charts/BarList';
import { ChartFrame } from '../../../components/charts/ChartFrame';
import { getMachine, getStats } from '../services/machineService';
import { MachineTimeline } from '../components/MachineTimeline';
import { MachineInsights } from '../components/MachineInsights';
import { MachineAliases } from '../components/MachineAliases';
import { formatDate, formatMinutes, formatNumber } from '../../../utils/format';

function StatsSection({ machineId }) {
  const stats = useAsync(() => getStats(machineId), [machineId]);
  return (
    <AsyncView state={stats}>
      {(s) => (
        <>
          <div className="tiles">
            <StatTile label="Records" value={formatNumber(s.recordCount)} />
            <StatTile label="Total downtime" value={formatMinutes(s.totalDowntimeMinutes)} />
            <StatTile label="Avg downtime / event" value={formatMinutes(s.avgDowntimeMinutes && Math.round(s.avgDowntimeMinutes))} />
            <StatTile label="MTBF" value={formatNumber(s.mtbfDays, 1)} unit="days" />
            <StatTile label="Last maintenance" value={formatDate(s.lastMaintenanceDate)} />
          </div>
          {s.topFailureModes?.length > 0 && (
            <ChartFrame
              title="Top failure modes"
              rows={s.topFailureModes}
              columns={[
                { label: 'Failure mode', render: (f) => f.name },
                { label: 'Category', render: (f) => f.category ?? '—' },
                { label: 'Records', numeric: true, render: (f) => formatNumber(f.recordCount) },
                { label: 'Downtime', numeric: true, render: (f) => formatMinutes(f.totalDowntimeMinutes) },
              ]}
            >
              <BarList
                ariaLabel="Downtime by failure mode"
                items={s.topFailureModes.map((f) => ({
                  key: f.failureModeId ?? f.name,
                  label: f.name,
                  value: f.totalDowntimeMinutes,
                  display: formatMinutes(f.totalDowntimeMinutes),
                  tooltip: `${f.name}: ${formatMinutes(f.totalDowntimeMinutes)} · ${formatNumber(f.recordCount)} records`,
                }))}
              />
            </ChartFrame>
          )}
        </>
      )}
    </AsyncView>
  );
}

export function MachineDetailPage() {
  const { machineId } = useParams();
  const [tab, setTab] = useState('timeline');
  const machine = useAsync(() => getMachine(machineId), [machineId]);

  if (machine.loading && !machine.data) return <LoadingState />;
  if (machine.error) return <ErrorMessage error={machine.error} onRetry={machine.reload} />;
  const m = machine.data;
  if (!m) return <EmptyState title="Machine not found" />;

  return (
    <>
      <PageHeader
        breadcrumb={[{ label: 'Machines', to: '/machines' }, { label: m.code }]}
        title={m.name}
        subtitle={[m.plantName, m.lineName].filter(Boolean).join(' · ')}
        actions={
          <>
            <Link className="btn" to={`/records/new?machineId=${m.id}`}>
              Log maintenance
            </Link>
            <Link className="btn btn-primary" to={`/machines/${m.id}/edit`}>
              Edit
            </Link>
          </>
        }
      />
      <div className="card card-body">
        <dl className="dl">
          <div>
            <dt>Code</dt>
            <dd className="mono">{m.code}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>
              <Badge>{m.status}</Badge>
            </dd>
          </div>
          <div>
            <dt>Active</dt>
            <dd>{m.active ? 'Yes' : 'No'}</dd>
          </div>
          <div>
            <dt>Criticality</dt>
            <dd>
              <Badge>{m.criticality}</Badge>
            </dd>
          </div>
          <div>
            <dt>Manufacturer</dt>
            <dd>{m.manufacturer ?? '—'}</dd>
          </div>
          <div>
            <dt>Model</dt>
            <dd>{m.model ?? '—'}</dd>
          </div>
          <div>
            <dt>Commissioned</dt>
            <dd>{formatDate(m.commissionedOn)}</dd>
          </div>
          <div>
            <dt>Aliases</dt>
            <dd>{m.aliases?.length ? m.aliases.map((a) => a.alias).join(', ') : '—'}</dd>
          </div>
        </dl>
      </div>
      <StatsSection machineId={machineId} />
      <div className="card">
        <Tabs
          value={tab}
          onChange={setTab}
          tabs={[
            { value: 'timeline', label: 'Maintenance timeline' },
            { value: 'insights', label: 'Insights' },
            { value: 'aliases', label: 'Aliases' },
          ]}
        />
        {tab === 'timeline' && <MachineTimeline machineId={machineId} />}
        {tab === 'insights' && <MachineInsights machineId={machineId} />}
        {tab === 'aliases' && <MachineAliases machineId={machineId} />}
      </div>
    </>
  );
}
