import { useNavigate } from 'react-router-dom';
import { Badge } from '../../../components/common';
import { formatConfidence, formatDate, formatMinutes } from '../../../utils/format';

/** Table of RecordRowResponse rows (listRecords, getTimeline, part recentRecords, assistant records). */
export function RecordsTable({ records, showMachine = true }) {
  const navigate = useNavigate();
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Date</th>
            {showMachine && <th>Machine</th>}
            <th>Failure mode</th>
            <th>Description</th>
            <th className="num">Downtime</th>
            <th className="hide-sm">Technician</th>
            <th className="hide-sm">Source</th>
            <th>Status</th>
            <th className="num hide-sm">Confidence</th>
          </tr>
        </thead>
        <tbody>
          {records.map((r) => (
            <tr
              key={r.id}
              className="clickable"
              tabIndex={0}
              onClick={() => navigate(`/records/${r.id}`)}
              onKeyDown={(e) => e.key === 'Enter' && navigate(`/records/${r.id}`)}
            >
              <td style={{ whiteSpace: 'nowrap' }}>{formatDate(r.recordDate)}</td>
              {showMachine && (
                <td>
                  <div>{r.machineName ?? '—'}</div>
                  {r.machineCode && <div className="subtle mono">{r.machineCode}</div>}
                </td>
              )}
              <td>
                <div>{r.failureMode ?? '—'}</div>
                {r.failureModeCategory && <div className="subtle">{r.failureModeCategory}</div>}
              </td>
              <td className="truncate" title={r.description}>
                {r.description ?? '—'}
              </td>
              <td className="num">{formatMinutes(r.downtimeMinutes)}</td>
              <td className="hide-sm">{r.technician ?? '—'}</td>
              <td className="hide-sm">
                <Badge>{r.source}</Badge>
              </td>
              <td>
                <Badge>{r.status}</Badge>
              </td>
              <td className="num hide-sm">{formatConfidence(r.confidence)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
