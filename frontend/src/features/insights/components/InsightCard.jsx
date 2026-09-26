import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../../../components/common';
import { formatDate, formatDateTime, formatNumber } from '../../../utils/format';

/** InsightResponse { type, severity, title, detail, machineId, machineName, metricValue, windowDays, computedAt, evidence[] } */
export function InsightCard({ insight, showMachine = true }) {
  const [open, setOpen] = useState(false);
  const evidence = insight.evidence ?? [];
  return (
    <article className="card card-body stack" style={{ gap: 8 }}>
      <div className="row between">
        <div className="row">
          <Badge>{insight.severity}</Badge>
          <Badge>{insight.type}</Badge>
        </div>
        <span className="subtle">{formatDateTime(insight.computedAt)}</span>
      </div>
      <h3>{insight.title}</h3>
      {insight.detail && <p className="muted prose">{insight.detail}</p>}
      <div className="row subtle">
        {showMachine && insight.machineId && (
          <Link to={`/machines/${insight.machineId}`}>{insight.machineName ?? `Machine #${insight.machineId}`}</Link>
        )}
        {insight.metricValue !== undefined && insight.metricValue !== null && (
          <span>Metric: {formatNumber(insight.metricValue, 2)}</span>
        )}
        {insight.windowDays && <span>Window: {insight.windowDays} days</span>}
      </div>
      {evidence.length > 0 && (
        <div>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setOpen(!open)} aria-expanded={open}>
            {open ? 'Hide' : 'Show'} evidence ({evidence.length})
          </button>
          {open && (
            <ul className="list">
              {evidence.map((ev, i) => (
                <li key={`${ev.recordId}-${i}`}>
                  <div className="row between">
                    <Link to={`/records/${ev.recordId}`}>Record #{ev.recordId}</Link>
                    <span className="subtle">{formatDate(ev.recordDate)}</span>
                  </div>
                  {ev.description && <div className="prose">{ev.description}</div>}
                  {ev.note && <div className="subtle">{ev.note}</div>}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </article>
  );
}
