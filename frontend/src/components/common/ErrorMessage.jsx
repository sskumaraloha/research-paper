/** Inline error banner for a failed request (ApiError from api/errors.js). */
export function ErrorMessage({ error, onRetry, title }) {
  if (!error) return null;
  const heading = title ?? (error.status === 403 ? "You don't have access" : error.status === 404 ? 'Not found' : 'Something went wrong');
  return (
    <div className="alert alert-error" role="alert">
      <div className="alert-body">
        <strong>{heading}</strong>
        <div>{error.message}</div>
      </div>
      {onRetry && error.status !== 403 && error.status !== 404 && (
        <button type="button" className="btn btn-sm" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}

export function SuccessMessage({ children }) {
  if (!children) return null;
  return (
    <div className="alert alert-success" role="status">
      <div className="alert-body">{children}</div>
    </div>
  );
}
