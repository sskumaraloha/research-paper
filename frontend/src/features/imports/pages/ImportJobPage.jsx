import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { Badge, ErrorMessage, LoadingState, PageHeader } from '../../../components/common';
import { ImportCounts } from '../components/ImportCounts';
import { getJob, rerunJob } from '../services/importService';
import { formatDateTime, formatNumber } from '../../../utils/format';

// Status values are undocumented, so "still running" is inferred from finishedAt being unset.
const isFinished = (job) => Boolean(job?.finishedAt) || Boolean(job?.errorMessage);

export function ImportJobPage() {
  const { jobId } = useParams();
  const job = useAsync(() => getJob(jobId), [jobId]);
  const rerun = useMutation(() => rerunJob(jobId));
  const { data, reload } = job;

  useEffect(() => {
    if (!data || isFinished(data)) return undefined;
    const t = setTimeout(reload, 3000);
    return () => clearTimeout(t);
  }, [data, reload]);

  if (job.loading && !data) return <LoadingState />;
  if (job.error && !data) return <ErrorMessage error={job.error} onRetry={reload} />;
  const j = data;

  const onRerun = async () => {
    const result = await rerun.run();
    if (result.ok) job.setData(result.data);
  };

  return (
    <>
      <PageHeader
        breadcrumb={[{ label: 'Imports', to: '/imports' }, { label: `Job #${j.id}` }]}
        title={j.filename ?? `Import job #${j.id}`}
        subtitle={
          <>
            <Badge>{j.status}</Badge> · started {formatDateTime(j.startedAt)}
            {j.finishedAt && ` · finished ${formatDateTime(j.finishedAt)}`}
          </>
        }
        actions={
          <>
            {j.needsValidationCount > 0 && (
              <Link className="btn" to="/validation">
                Review validation queue
              </Link>
            )}
            <button type="button" className="btn btn-primary" onClick={onRerun} disabled={rerun.pending}>
              {rerun.pending ? 'Re-running…' : 'Re-run import'}
            </button>
          </>
        }
      />
      <ErrorMessage error={rerun.error} title="Re-run failed" />
      {j.errorMessage && <ErrorMessage error={{ message: j.errorMessage }} title="Import error" />}
      <ImportCounts job={j} />
      <div className="card">
        <div className="card-header">
          <h2>Pipeline steps</h2>
          {!isFinished(j) && <span className="subtle">Refreshing every 3 s…</span>}
        </div>
        <div className="card-body">
          {j.steps?.length ? (
            <ol className="steps">
              {j.steps.map((s, i) => (
                <li key={`${s.name}-${i}`}>
                  <span className={`step-dot${s.finishedAt ? ' good' : s.startedAt ? ' info' : ''}`} aria-hidden="true" />
                  <div>
                    <div style={{ fontWeight: 500 }}>{s.name}</div>
                    {s.message && <div className="muted">{s.message}</div>}
                    <div className="subtle">
                      {formatNumber(s.processedCount)} processed
                      {s.startedAt && ` · ${formatDateTime(s.startedAt)}`}
                      {s.finishedAt && ` → ${formatDateTime(s.finishedAt)}`}
                    </div>
                  </div>
                  <Badge>{s.status}</Badge>
                </li>
              ))}
            </ol>
          ) : (
            <span className="muted">No step details reported.</span>
          )}
        </div>
      </div>
    </>
  );
}
