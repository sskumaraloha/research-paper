import { useState } from 'react';
import { Link } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, Badge, EmptyState, ErrorMessage, PageHeader, Pagination, SuccessMessage } from '../../../components/common';
import { EditApproveModal } from '../components/EditApproveModal';
import { RejectReasonModal } from '../components/RejectReasonModal';
import { approve, getQueue, reject } from '../services/validationService';
import { listFilterOptions } from '../../records/services/recordService';
import { formatConfidence, formatDate, formatMinutes } from '../../../utils/format';

const PAGE_SIZE = 20;

/** One ValidationItemResponse: parsed values next to the raw source row. */
function QueueItem({ item, onApprove, onReject, onEdit, busy }) {
  const [showRaw, setShowRaw] = useState(false);
  const raw = Object.entries(item.rawData ?? {});
  return (
    <li className="stack" style={{ gap: 10 }}>
      <div className="row between">
        <div className="row">
          <strong>
            {item.sourceFilename ?? `Job #${item.jobId}`} · row {item.rowNumber}
          </strong>
          <span className="subtle">Confidence {formatConfidence(item.confidence)}</span>
        </div>
        <div className="actions">
          <button type="button" className="btn btn-sm btn-primary" onClick={onApprove} disabled={busy}>
            Approve
          </button>
          <button type="button" className="btn btn-sm" onClick={onEdit} disabled={busy}>
            Edit & approve
          </button>
          <button type="button" className="btn btn-sm btn-danger" onClick={onReject} disabled={busy}>
            Reject
          </button>
        </div>
      </div>
      {item.reasons?.length > 0 && (
        <div className="chips">
          {item.reasons.map((r) => (
            <Badge key={r} tone="warn">
              {r}
            </Badge>
          ))}
        </div>
      )}
      <dl className="dl">
        <div>
          <dt>Machine</dt>
          <dd>
            {item.machineName ?? <span className="muted">Unmatched</span>}
            {item.machineText && <div className="subtle">Source: “{item.machineText}”</div>}
          </dd>
        </div>
        <div>
          <dt>Failure mode</dt>
          <dd>{item.failureModeName ?? '—'}</dd>
        </div>
        <div>
          <dt>Date</dt>
          <dd>{formatDate(item.recordDate)}</dd>
        </div>
        <div>
          <dt>Downtime</dt>
          <dd>{formatMinutes(item.downtimeMinutes)}</dd>
        </div>
        <div>
          <dt>Technician</dt>
          <dd>{item.technician ?? '—'}</dd>
        </div>
        <div>
          <dt>Parts</dt>
          <dd>{item.partsText ?? '—'}</dd>
        </div>
      </dl>
      {item.description && <p className="prose">{item.description}</p>}
      {item.actionTaken && <p className="prose muted">Action: {item.actionTaken}</p>}
      {raw.length > 0 && (
        <div>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowRaw(!showRaw)} aria-expanded={showRaw}>
            {showRaw ? 'Hide' : 'Show'} source row
          </button>
          {showRaw && (
            <div className="table-wrap">
              <table className="table">
                <tbody>
                  {raw.map(([k, v]) => (
                    <tr key={k}>
                      <th style={{ width: '30%' }}>{k}</th>
                      <td className="prose">{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </li>
  );
}

export function ValidationQueuePage() {
  const { plantId, refreshBadges } = usePlant();
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null);
  const [rejecting, setRejecting] = useState(null);
  const [notice, setNotice] = useState(null);
  const [busyId, setBusyId] = useState(null);

  const queue = useAsync(() => getQueue({ plantId, page, size: PAGE_SIZE }), [plantId, page], { enabled: Boolean(plantId) });
  // Machine and failure-mode choices for the edit form (RecordFilterOptionsResponse, plant-scoped).
  const options = useAsync(() => listFilterOptions(plantId), [plantId], { enabled: Boolean(plantId) });
  const approveMutation = useMutation(approve);

  const afterDecision = (decision, verb) => {
    setNotice(
      <>
        Row {verb}
        {decision?.recordId && (
          <>
            {' · '}
            <Link to={`/records/${decision.recordId}`}>View record #{decision.recordId}</Link>
          </>
        )}
      </>,
    );
    setEditing(null);
    setRejecting(null);
    queue.reload();
    refreshBadges();
  };

  const onApprove = async (item) => {
    setBusyId(item.id);
    const result = await approveMutation.run(item.id);
    setBusyId(null);
    if (result.ok) afterDecision(result.data, 'approved');
  };

  const pendingCount = queue.data?.pendingCount;

  return (
    <>
      <PageHeader
        title="Validation queue"
        subtitle={
          pendingCount !== undefined
            ? `${pendingCount} imported ${pendingCount === 1 ? 'row needs' : 'rows need'} review before becoming records.`
            : 'Imported rows that need review before becoming records.'
        }
        actions={
          <Link className="btn" to="/validation/aliases">
            Alias suggestions
          </Link>
        }
      />
      <SuccessMessage>{notice}</SuccessMessage>
      <ErrorMessage error={approveMutation.error} title="Could not approve" />
      <PlantGate>
        <div className="card">
          <AsyncView
            state={queue}
            isEmpty={(d) => !d.items?.content?.length}
            empty={<EmptyState title="Queue is clear">No imported rows are waiting for review.</EmptyState>}
          >
            {(data) => (
              <>
                <ul className="list">
                  {data.items.content.map((item) => (
                    <QueueItem
                      key={item.id}
                      item={item}
                      busy={busyId === item.id}
                      onApprove={() => onApprove(item)}
                      onEdit={() => setEditing(item)}
                      onReject={() => setRejecting(item)}
                    />
                  ))}
                </ul>
                <Pagination pageData={data.items} onPageChange={setPage} />
              </>
            )}
          </AsyncView>
        </div>
      </PlantGate>
      {editing && (
        <EditApproveModal
          item={editing}
          machines={options.data?.machines ?? []}
          failureModes={options.data?.failureModes ?? []}
          onClose={() => setEditing(null)}
          onDone={(d) => afterDecision(d, 'edited and approved')}
        />
      )}
      {rejecting && (
        <RejectReasonModal
          title={`Reject row ${rejecting.rowNumber ?? ''}`}
          onReject={(reason) => reject(rejecting.id, reason)}
          onClose={() => setRejecting(null)}
          onDone={(d) => afterDecision(d, 'rejected')}
        />
      )}
    </>
  );
}
