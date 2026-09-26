import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAsync } from '../../../hooks/useAsync';
import { Badge, ErrorMessage, LoadingState, PageHeader } from '../../../components/common';
import { RejectReasonModal } from '../../validation/components/RejectReasonModal';
import { getRecord, rejectRecord } from '../services/recordService';
import { formatConfidence, formatDate, formatMinutes } from '../../../utils/format';

const Item = ({ label, children }) => (
  <div>
    <dt>{label}</dt>
    <dd>{children ?? '—'}</dd>
  </div>
);

export function RecordDetailPage() {
  const { recordId } = useParams();
  const record = useAsync(() => getRecord(recordId), [recordId]);
  const [rejecting, setRejecting] = useState(false);

  if (record.loading && !record.data) return <LoadingState />;
  if (record.error) return <ErrorMessage error={record.error} onRetry={record.reload} />;
  const r = record.data;

  return (
    <>
      <PageHeader
        breadcrumb={[{ label: 'Records', to: '/records' }, { label: `#${r.id}` }]}
        title={`${r.machineName ?? 'Record'} · ${formatDate(r.recordDate)}`}
        subtitle={r.failureMode ? `${r.failureMode}${r.failureModeCategory ? ` (${r.failureModeCategory})` : ''}` : undefined}
        actions={
          !r.rejectedReason && (
            <button type="button" className="btn btn-danger" onClick={() => setRejecting(true)}>
              Reject
            </button>
          )
        }
      />
      {r.rejectedReason && (
        <div className="alert alert-error">
          <div className="alert-body">
            <strong>Rejected</strong>
            <div>{r.rejectedReason}</div>
          </div>
        </div>
      )}
      <div className="card card-body">
        <dl className="dl">
          <Item label="Machine">
            <Link to={`/machines/${r.machineId}`}>{r.machineName}</Link> <span className="subtle mono">{r.machineCode}</span>
          </Item>
          <Item label="Line">{r.lineName}</Item>
          <Item label="Date">{formatDate(r.recordDate)}</Item>
          <Item label="Downtime">{formatMinutes(r.downtimeMinutes)}</Item>
          <Item label="Technician">{r.technician}</Item>
          <Item label="Status">
            <Badge>{r.status}</Badge>
          </Item>
          <Item label="Source">
            <Badge>{r.source}</Badge>
          </Item>
          <Item label="Confidence">{formatConfidence(r.confidence)}</Item>
          <Item label="Created by">{r.createdByName}</Item>
          <Item label="Source document">{r.sourceDocumentName}</Item>
        </dl>
      </div>
      <div className="grid grid-2">
        <div className="card">
          <div className="card-header">
            <h2>Description</h2>
          </div>
          <div className="card-body prose">{r.description ?? '—'}</div>
        </div>
        <div className="card">
          <div className="card-header">
            <h2>Action taken</h2>
          </div>
          <div className="card-body prose">{r.actionTaken ?? '—'}</div>
        </div>
      </div>
      <div className="card">
        <div className="card-header">
          <h2>Spare parts used</h2>
        </div>
        <div className="card-body">
          {r.spareParts?.length ? (
            <div className="chips">
              {r.spareParts.map((p) => (
                <Link key={p.id} className="chip" to={`/parts/${p.id}`}>
                  {p.name}
                </Link>
              ))}
            </div>
          ) : (
            <span className="muted">No parts recorded.</span>
          )}
        </div>
      </div>
      {rejecting && (
        <RejectReasonModal
          title="Reject record"
          onReject={(reason) => rejectRecord(r.id, reason)}
          onClose={() => setRejecting(false)}
          onDone={(updated) => {
            record.setData(updated);
            setRejecting(false);
          }}
        />
      )}
    </>
  );
}
