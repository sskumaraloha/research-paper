import { useParams } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { Badge, EmptyState, ErrorMessage, LoadingState, PageHeader } from '../../../components/common';
import { getOrganisation } from '../services/platformService';
import { formatMinutes, formatNumber } from '../../../utils/format';

export function OrganisationPage() {
  const { organisationId } = useParams();
  const org = useAsync(() => getOrganisation(organisationId), [organisationId]);
  if (org.loading && !org.data) return <LoadingState />;
  if (org.error) return <ErrorMessage error={org.error} onRetry={org.reload} />;
  const o = org.data;

  return (
    <>
      <PageHeader breadcrumb={[{ label: 'Platform', to: '/platform' }, { label: o.code }]} title={o.name} />
      <div className="card">
        <div className="card-header">
          <h2>Plants</h2>
          <span className="subtle">{o.plants?.length ?? 0}</span>
        </div>
        {o.plants?.length ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Plant</th>
                  <th className="num">Machines</th>
                  <th className="num">Records</th>
                  <th className="num">Downtime (30 d)</th>
                  <th className="num">Pending validations</th>
                </tr>
              </thead>
              <tbody>
                {o.plants.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <div style={{ fontWeight: 500 }}>{p.name}</div>
                      <div className="subtle mono">{p.code}</div>
                    </td>
                    <td className="num">{formatNumber(p.machineCount)}</td>
                    <td className="num">{formatNumber(p.recordCount)}</td>
                    <td className="num">{formatMinutes(p.downtimeLast30DaysMinutes)}</td>
                    <td className="num">{formatNumber(p.pendingValidations)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No plants" />
        )}
      </div>
      <div className="card">
        <div className="card-header">
          <h2>Users</h2>
          <span className="subtle">{o.users?.length ?? 0}</span>
        </div>
        {o.users?.length ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Role</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {o.users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div style={{ fontWeight: 500 }}>{u.fullName}</div>
                      <div className="subtle">{u.email}</div>
                    </td>
                    <td>
                      <Badge>{u.role}</Badge>
                    </td>
                    <td>{u.active ? <Badge tone="good">Active</Badge> : <Badge>Inactive</Badge>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="No users" />
        )}
      </div>
    </>
  );
}
