import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { PlantGate } from '../../../layouts/PlantGate';
import { Badge, ErrorMessage, FormField, LoadingState, PageHeader, SuccessMessage } from '../../../components/common';
import { confirm, getConversation, requestEdit, sendMessage, startConversation } from '../services/entryService';
import { formatDate, formatDateTime, formatMinutes } from '../../../utils/format';

// EntryMessageResponse.sender values are undocumented (API_ANALYSIS Q4); these are treated as the signed-in user.
const isOwnMessage = (sender) => /^(user|human|technician|me)$/i.test(sender ?? '');

/** StartConversationRequest { plantId*, message? (≤1000) } */
function StartEntry() {
  const { plantId } = usePlant();
  const navigate = useNavigate();
  const form = useContractForm('StartConversationRequest', { message: '' });
  const mutation = useMutation(startConversation);

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate({ plantId });
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) navigate(`/entry/${result.data.id}`);
    else form.applyServerErrors(result.error);
  };

  return (
    <form className="card card-body form" onSubmit={onSubmit} noValidate>
      <p className="muted">
        Describe the maintenance in plain words — the assistant extracts the machine, date, downtime, failure mode and
        parts, and asks for anything missing before creating the record.
      </p>
      <ErrorMessage error={mutation.error} title="Could not start" />
      <FormField label="What happened?" error={form.errors.message} hint="Optional — you can also start empty.">
        <textarea
          className="input"
          rows={4}
          autoFocus
          placeholder="e.g. Press 2 hydraulic leak yesterday, replaced seal kit, down 90 minutes"
          {...form.bind('message')}
        />
      </FormField>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
          {mutation.pending ? 'Starting…' : 'Start'}
        </button>
      </div>
    </form>
  );
}

/** EntryDraftResponse — the record being assembled from the conversation. */
function DraftPanel({ draft }) {
  const missing = new Set(draft?.missingFields ?? []);
  const rows = [
    ['machineId', 'Machine', draft?.machineName ?? draft?.machineText],
    ['recordDate', 'Date', draft?.recordDate && formatDate(draft.recordDate)],
    ['downtimeMinutes', 'Downtime', draft?.downtimeMinutes !== undefined && draft?.downtimeMinutes !== null ? formatMinutes(draft.downtimeMinutes) : null],
    ['failureModeId', 'Failure mode', draft?.failureMode],
    ['description', 'Description', draft?.description],
    ['actionTaken', 'Action taken', draft?.actionTaken],
    ['partsText', 'Parts', draft?.partsText],
  ];
  return (
    <div className="card">
      <div className="card-header">
        <h2>Draft record</h2>
        {missing.size > 0 ? <Badge tone="warn">{missing.size} missing</Badge> : draft && <Badge tone="good">Complete</Badge>}
      </div>
      <div className="card-body">
        <dl className="dl" style={{ gridTemplateColumns: '1fr' }}>
          {rows.map(([key, label, value]) => (
            <div key={key}>
              <dt>{label}</dt>
              <dd className="prose">{value || <span className="subtle">{missing.has(key) ? 'Missing' : '—'}</span>}</dd>
            </div>
          ))}
        </dl>
        {missing.size > 0 && (
          <p className="subtle" style={{ marginTop: 12 }}>
            Missing: {[...missing].join(', ')}
          </p>
        )}
      </div>
    </div>
  );
}

function Conversation({ conversationId }) {
  const conversation = useAsync(() => getConversation(conversationId), [conversationId]);
  const form = useContractForm('EntryMessageRequest', { message: '' });
  const send = useMutation((payload) => sendMessage(conversationId, payload.message));
  const confirmMutation = useMutation(() => confirm(conversationId));
  const editMutation = useMutation(() => requestEdit(conversationId));
  const chatRef = useRef(null);
  const [lastSent, setLastSent] = useState(null);

  const c = conversation.data;
  useEffect(() => {
    chatRef.current?.scrollTo({ top: chatRef.current.scrollHeight });
  }, [c?.messages?.length, lastSent]);

  if (conversation.loading && !c) return <LoadingState />;
  if (conversation.error && !c) return <ErrorMessage error={conversation.error} onRetry={conversation.reload} />;

  const done = Boolean(c.resultingRecordId);
  const busy = send.pending || confirmMutation.pending || editMutation.pending;

  const onSend = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    setLastSent(payload.message);
    const result = await send.run(payload);
    setLastSent(null);
    if (result.ok) {
      conversation.setData(result.data);
      form.setValues({ message: '' });
    }
  };

  const onAction = async (mutation) => {
    const result = await mutation.run();
    if (result.ok) conversation.setData(result.data);
  };

  return (
    <>
      <div className="row">
        <span className="muted">Status:</span> <Badge>{c.status}</Badge>
      </div>
      {done && (
        <SuccessMessage>
          Record created. <Link to={`/records/${c.resultingRecordId}`}>View record #{c.resultingRecordId}</Link>
        </SuccessMessage>
      )}
      <ErrorMessage error={send.error ?? confirmMutation.error ?? editMutation.error} />
      <div className="grid grid-2" style={{ alignItems: 'start' }}>
        <div className="card">
          <div className="chat" ref={chatRef} aria-live="polite">
            {(c.messages ?? []).length === 0 && <span className="muted">No messages yet — describe the maintenance below.</span>}
            {(c.messages ?? []).map((m, i) => {
              const mine = isOwnMessage(m.sender);
              return (
                <div key={i} className={`bubble ${mine ? 'me' : 'them'}`}>
                  {m.content}
                  <div className="bubble-meta">
                    {m.sender} · {formatDateTime(m.createdAt)}
                  </div>
                </div>
              );
            })}
            {lastSent && <div className="bubble me" style={{ opacity: 0.6 }}>{lastSent}</div>}
          </div>
          {!done && (
            <form className="chat-input" onSubmit={onSend} noValidate>
              <label className="sr-only" htmlFor="entry-message">
                Message
              </label>
              <input
                id="entry-message"
                className="input"
                placeholder="Reply or add details…"
                autoComplete="off"
                {...form.bind('message')}
                aria-invalid={form.errors.message ? 'true' : undefined}
              />
              <button type="submit" className="btn btn-primary" disabled={busy || !form.values.message?.trim()}>
                {send.pending ? 'Sending…' : 'Send'}
              </button>
            </form>
          )}
        </div>
        <div className="stack">
          <DraftPanel draft={c.draft} />
          {!done && (
            <div className="form-actions">
              <button type="button" className="btn" onClick={() => onAction(editMutation)} disabled={busy}>
                {editMutation.pending ? 'Requesting…' : 'Request edit'}
              </button>
              <button type="button" className="btn btn-primary" onClick={() => onAction(confirmMutation)} disabled={busy}>
                {confirmMutation.pending ? 'Confirming…' : 'Confirm & create record'}
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

export function EntryPage() {
  const { conversationId } = useParams();
  return (
    <>
      <PageHeader
        breadcrumb={conversationId ? [{ label: 'Guided entry', to: '/entry' }, { label: `#${conversationId}` }] : undefined}
        title="Guided record entry"
        subtitle="Log maintenance by describing it conversationally."
        actions={
          conversationId && (
            <Link className="btn" to="/entry">
              New entry
            </Link>
          )
        }
      />
      <PlantGate>{conversationId ? <Conversation key={conversationId} conversationId={conversationId} /> : <StartEntry />}</PlantGate>
    </>
  );
}
