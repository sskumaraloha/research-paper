import { LoadingState } from './Spinner';
import { ErrorMessage } from './ErrorMessage';
import { EmptyState } from './EmptyState';

/**
 * Renders the loading / error / empty / success state of a useAsync() result.
 * `isEmpty(data)` decides the empty state; children receive the data.
 */
export function AsyncView({ state, isEmpty, empty, loadingLabel, children }) {
  const { data, error, loading, reload } = state;
  if (loading && data === undefined) return <LoadingState label={loadingLabel} />;
  if (error && data === undefined) {
    return (
      <div className="card-body">
        <ErrorMessage error={error} onRetry={reload} />
      </div>
    );
  }
  if (data === undefined) return null;
  if (isEmpty?.(data)) return empty ?? <EmptyState />;
  return (
    <>
      {error && (
        <div className="card-body">
          <ErrorMessage error={error} onRetry={reload} />
        </div>
      )}
      {children(data)}
    </>
  );
}
