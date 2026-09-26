import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, Badge, EmptyState, ErrorMessage, PageHeader } from '../../../components/common';
import { ImportCounts } from '../components/ImportCounts';
import { getLatestJob, uploadFile } from '../services/importService';
import { listSourceDocuments } from '../../records/services/recordService';
import { formatBytes, formatDateTime } from '../../../utils/format';

function UploadCard({ plantId, onUploaded }) {
  const inputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [progress, setProgress] = useState(null);
  const [summary, setSummary] = useState(null);
  const mutation = useMutation((f) =>
    uploadFile(plantId, f, (e) => e.total && setProgress(Math.round((e.loaded / e.total) * 100))),
  );

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!file) return;
    setSummary(null);
    setProgress(0);
    const result = await mutation.run(file);
    setProgress(null);
    if (result.ok) {
      // ImportSummaryResponse { jobId, status, totalRows, autoImportedCount, needsValidationCount, rejectedCount, invalidCount }
      setSummary(result.data);
      setFile(null);
      if (inputRef.current) inputRef.current.value = '';
      onUploaded();
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <h2>Upload maintenance log</h2>
      </div>
      <form className="card-body form" onSubmit={onSubmit}>
        <p className="muted">
          Upload a maintenance log file. Rows are matched to machines and failure modes automatically; uncertain rows
          go to the validation queue.
        </p>
        <div className="row">
          <input
            ref={inputRef}
            className="input"
            style={{ flex: 1, minWidth: 200, paddingTop: 6 }}
            type="file"
            aria-label="Choose file"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          />
          <button type="submit" className="btn btn-primary" disabled={!file || mutation.pending}>
            {mutation.pending ? `Uploading${progress !== null ? ` ${progress}%` : '…'}` : 'Upload & import'}
          </button>
        </div>
        {file && (
          <span className="subtle">
            {file.name} · {formatBytes(file.size)}
          </span>
        )}
        <ErrorMessage error={mutation.error} title="Upload failed" />
        {summary && (
          <div className="stack">
            <div className="alert alert-success">
              <div className="alert-body">
                Import job #{summary.jobId} · <Badge>{summary.status}</Badge>{' '}
                <Link to={`/imports/${summary.jobId}`}>View job</Link>
                {summary.needsValidationCount > 0 && (
                  <>
                    {' · '}
                    <Link to="/validation">Review {summary.needsValidationCount} rows</Link>
                  </>
                )}
              </div>
            </div>
            <ImportCounts job={summary} />
          </div>
        )}
      </form>
    </div>
  );
}

function LatestJobCard({ state }) {
  return (
    <div className="card">
      <div className="card-header">
        <h2>Latest import</h2>
        {state.data?.id && <Link to={`/imports/${state.data.id}`}>Details</Link>}
      </div>
      <AsyncView state={state} isEmpty={(d) => !d || !d.id} empty={<EmptyState title="No imports yet" />}>
        {(job) => (
          <div className="card-body stack">
            <div className="row between">
              <strong>{job.filename}</strong>
              <Badge>{job.status}</Badge>
            </div>
            <span className="subtle">
              Started {formatDateTime(job.startedAt)}
              {job.finishedAt && ` · finished ${formatDateTime(job.finishedAt)}`}
            </span>
            {job.errorMessage && <ErrorMessage error={{ message: job.errorMessage }} title="Import error" />}
            <ImportCounts job={job} />
          </div>
        )}
      </AsyncView>
    </div>
  );
}

function SourceDocumentsCard({ state }) {
  return (
    <div className="card">
      <div className="card-header">
        <h2>Source documents</h2>
      </div>
      <AsyncView state={state} isEmpty={(d) => !d.length} empty={<EmptyState title="No documents uploaded" />}>
        {(docs) => (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>File</th>
                  <th className="hide-sm">Type</th>
                  <th className="num">Size</th>
                  <th>Uploaded by</th>
                  <th>Uploaded</th>
                </tr>
              </thead>
              <tbody>
                {docs.map((d) => (
                  <tr key={d.id}>
                    <td>{d.filename}</td>
                    <td className="hide-sm subtle">{d.contentType}</td>
                    <td className="num">{formatBytes(d.sizeBytes)}</td>
                    <td>{d.uploadedByName ?? '—'}</td>
                    <td>{formatDateTime(d.uploadedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AsyncView>
    </div>
  );
}

export function ImportsPage() {
  const { plantId, refreshBadges } = usePlant();
  // A plant with no imports yet may answer 404; show that as the empty state rather than an error.
  const latest = useAsync(
    () => getLatestJob(plantId).catch((e) => (e.status === 404 ? null : Promise.reject(e))),
    [plantId],
    { enabled: Boolean(plantId) },
  );
  const docs = useAsync(() => listSourceDocuments(plantId), [plantId], { enabled: Boolean(plantId) });
  return (
    <>
      <PageHeader title="Imports" subtitle="Bring historical maintenance logs into the platform." />
      <PlantGate>
        <div className="grid grid-2">
          <UploadCard
            plantId={plantId}
            onUploaded={() => {
              latest.reload();
              docs.reload();
              refreshBadges();
            }}
          />
          <LatestJobCard state={latest} />
        </div>
        <SourceDocumentsCard state={docs} />
      </PlantGate>
    </>
  );
}
