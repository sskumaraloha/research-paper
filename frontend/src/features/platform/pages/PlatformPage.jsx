import { useNavigate } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { AsyncView, EmptyState, PageHeader, StatTile } from '../../../components/common';
import { listOrganisations, overview } from '../services/platformService';
import { formatDateTime, formatNumber } from '../../../utils/format';

export function PlatformPage() {
  const navigate = useNavigate();
  const stats = useAsync(overview, []);
  const orgs = useAsync(listOrganisations, []);

  return (
    <>
      <PageHeader title="Platform" subtitle="Usage across all organisations." />
      <AsyncView state={stats}>
        {(s) => (
          <div className="tiles">
            <StatTile label="Organisations" value={formatNumber(s.organisations)} />
            <StatTile label="Plants" value={formatNumber(s.plants)} />
            <StatTile label="Users" value={formatNumber(s.users)} />
            <StatTile label="Machines" value={formatNumber(s.machines)} />
            <StatTile label="Maintenance records" value={formatNumber(s.maintenanceRecords)} foot={`${formatNumber(s.recordsLast30Days)} in last 30 days`} />
            <StatTile label="Import jobs" value={formatNumber(s.importJobs)} />
            <StatTile label="Pending validations" value={formatNumber(s.pendingValidations)} />
          </div>
        )}
      </AsyncView>
      <div className="card">
        <div className="card-header">
          <h2>Organisations</h2>
        </div>
        <AsyncView state={orgs} isEmpty={(d) => !d.length} empty={<EmptyState title="No organisations" />}>
          {(list) => (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Organisation</th>
                    <th className="num">Plants</th>
                    <th className="num">Users</th>
                    <th className="num hide-sm">Machines</th>
                    <th className="num">Records</th>
                    <th className="num hide-sm">Imports</th>
                    <th className="hide-sm">Last activity</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map((o) => (
                    <tr
                      key={o.id}
                      className="clickable"
                      tabIndex={0}
                      onClick={() => navigate(`/platform/organisations/${o.id}`)}
                      onKeyDown={(e) => e.key === 'Enter' && navigate(`/platform/organisations/${o.id}`)}
                    >
                      <td>
                        <div style={{ fontWeight: 500 }}>{o.name}</div>
                        <div className="subtle mono">{o.code}</div>
                      </td>
                      <td className="num">{formatNumber(o.plantCount)}</td>
                      <td className="num">{formatNumber(o.userCount)}</td>
                      <td className="num hide-sm">{formatNumber(o.machineCount)}</td>
                      <td className="num">{formatNumber(o.recordCount)}</td>
                      <td className="num hide-sm">{formatNumber(o.importJobCount)}</td>
                      <td className="hide-sm">{formatDateTime(o.lastActivityAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </AsyncView>
      </div>
    </>
  );
}
