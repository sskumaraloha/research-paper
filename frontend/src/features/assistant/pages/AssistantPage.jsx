import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, EmptyState, ErrorMessage, StatTile, PageHeader } from '../../../components/common';
import { RecordsTable } from '../../records/components/RecordsTable';
import { ask, getAssistantConversation, listConversations, suggestions } from '../services/assistantService';
import { formatDateTime } from '../../../utils/format';

/** Renders an AssistantAnswerResponse { intent, title, tiles, sections, records, followUps }. */
function Answer({ answer, onFollowUp, disabled }) {
  return (
    <div className="card">
      <div className="card-header">
        <h2>{answer.title ?? 'Answer'}</h2>
        {answer.intent && <span className="subtle">{answer.intent}</span>}
      </div>
      <div className="card-body stack">
        {answer.tiles?.length > 0 && (
          <div className="tiles">
            {answer.tiles.map((t, i) => (
              <StatTile key={i} label={t.label} value={t.value} unit={t.unit} />
            ))}
          </div>
        )}
        {answer.sections?.map((s, i) => (
          <section key={i} className="stack" style={{ gap: 4 }}>
            {s.heading && <h3>{s.heading}</h3>}
            <p className="prose">{s.text}</p>
          </section>
        ))}
      </div>
      {answer.records?.length > 0 && <RecordsTable records={answer.records} />}
      {answer.followUps?.length > 0 && (
        <div className="card-body" style={{ borderTop: '1px solid var(--border)' }}>
          <div className="subtle" style={{ marginBottom: 6 }}>
            Follow-up questions
          </div>
          <div className="chips">
            {answer.followUps.map((q) => (
              <button key={q} type="button" className="chip" onClick={() => onFollowUp(q)} disabled={disabled}>
                {q}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/** Earlier turns of a stored conversation (AssistantConversationResponse.messages). */
function History({ conversationId }) {
  const conversation = useAsync(() => getAssistantConversation(conversationId), [conversationId]);
  return (
    <AsyncView state={conversation} isEmpty={(d) => !d.messages?.length} empty={null}>
      {(c) => (
        <div className="card">
          <div className="card-header">
            <h2>{c.title ?? 'Conversation'}</h2>
            <span className="subtle">{formatDateTime(c.updatedAt)}</span>
          </div>
          <div className="chat" style={{ maxHeight: 360 }}>
            {c.messages.map((m, i) => (
              <div key={i} className={`bubble ${/^(user|human|me)$/i.test(m.sender ?? '') ? 'me' : 'them'}`}>
                {m.content}
                <div className="bubble-meta">
                  {m.sender}
                  {m.intent ? ` · ${m.intent}` : ''} · {formatDateTime(m.createdAt)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </AsyncView>
  );
}

export function AssistantPage() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const { plantId } = usePlant();
  const conversations = useAsync(listConversations, []);
  const suggested = useAsync(() => suggestions(plantId), [plantId], { enabled: Boolean(plantId) });
  // AskRequest { plantId*, question* (≤500), conversationId? }
  const form = useContractForm('AskRequest', { question: '' });
  const mutation = useMutation(ask);
  const [answers, setAnswers] = useState([]);
  const bottomRef = useRef(null);
  // Set when we navigate to a conversation the backend just created, so its answers stay on screen.
  const selfNavigated = useRef(false);

  useEffect(() => {
    if (selfNavigated.current) selfNavigated.current = false;
    else setAnswers([]);
  }, [conversationId]);
  useEffect(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }), [answers.length]);

  const submit = async (question) => {
    const payload = form.validate({ question, plantId, conversationId: conversationId ?? '' });
    if (!payload) return;
    const result = await mutation.run(payload);
    if (!result.ok) return;
    form.setValues({ question: '' });
    setAnswers((a) => [...a, { question: payload.question, answer: result.data }]);
    if (result.data.conversationId && String(result.data.conversationId) !== conversationId) {
      conversations.reload();
      selfNavigated.current = true;
      navigate(`/assistant/${result.data.conversationId}`, { replace: true });
    }
  };

  const onSubmit = (e) => {
    e.preventDefault();
    submit(form.values.question);
  };

  return (
    <>
      <PageHeader
        title="Maintenance assistant"
        subtitle="Ask questions about machines, failures and downtime in plain language."
        actions={
          conversationId && (
            <Link className="btn" to="/assistant">
              New conversation
            </Link>
          )
        }
      />
      <PlantGate>
        <div className="grid" style={{ gridTemplateColumns: 'minmax(0, 1fr)', gap: 16 }}>
          <form className="card card-body row" onSubmit={onSubmit} noValidate>
            <label className="sr-only" htmlFor="assistant-question">
              Question
            </label>
            <input
              id="assistant-question"
              className="input"
              style={{ flex: 1, minWidth: 200 }}
              placeholder="e.g. Which machines had the most downtime last month?"
              autoFocus
              {...form.bind('question')}
              aria-invalid={form.errors.question ? 'true' : undefined}
            />
            <button type="submit" className="btn btn-primary" disabled={mutation.pending || !form.values.question?.trim()}>
              {mutation.pending ? 'Thinking…' : 'Ask'}
            </button>
            {form.errors.question && <span className="field-error">{form.errors.question}</span>}
          </form>
          <ErrorMessage error={mutation.error} title="The assistant could not answer" />

          {!conversationId && answers.length === 0 && (
            <div className="card card-body stack">
              <h3>Try asking</h3>
              <AsyncView state={suggested} isEmpty={(d) => !d.length} empty={<span className="muted">No suggestions available.</span>}>
                {(list) => (
                  <div className="chips">
                    {list.map((q) => (
                      <button key={q} type="button" className="chip" onClick={() => submit(q)} disabled={mutation.pending}>
                        {q}
                      </button>
                    ))}
                  </div>
                )}
              </AsyncView>
            </div>
          )}

          {conversationId && answers.length === 0 && <History conversationId={conversationId} />}

          {answers.map((a, i) => (
            <div key={i} className="stack">
              <div className="bubble me">{a.question}</div>
              <Answer answer={a.answer} onFollowUp={submit} disabled={mutation.pending} />
            </div>
          ))}
          <div ref={bottomRef} />

          <div className="card">
            <div className="card-header">
              <h2>Recent conversations</h2>
            </div>
            <AsyncView state={conversations} isEmpty={(d) => !d.length} empty={<EmptyState title="No conversations yet" />}>
              {(list) => (
                <ul className="list">
                  {list.map((c) => (
                    <li key={c.id} className="row between">
                      <Link to={`/assistant/${c.id}`} style={{ fontWeight: String(c.id) === conversationId ? 600 : 500 }}>
                        {c.title ?? `Conversation #${c.id}`}
                      </Link>
                      <span className="subtle">{formatDateTime(c.updatedAt)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </AsyncView>
          </div>
        </div>
      </PlantGate>
    </>
  );
}
