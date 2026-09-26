import { usePlant } from '../context/PlantContext';
import { EmptyState, ErrorMessage, LoadingState } from '../components/common';

/** Renders children only once a plant is selected (most operations require plantId). */
export function PlantGate({ children }) {
  const { plantId, loading, error, reload, plants } = usePlant();
  if (plantId) return children;
  if (loading) return <LoadingState label="Loading plants…" />;
  if (error) return <ErrorMessage error={error} onRetry={reload} />;
  if (!plants.length) {
    return (
      <div className="card">
        <EmptyState title="No plants available">Your account is not assigned to any plant.</EmptyState>
      </div>
    );
  }
  return <LoadingState />;
}
