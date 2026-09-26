export function Spinner({ small }) {
  return <span className={`spinner${small ? ' sm' : ''}`} role="status" aria-label="Loading" />;
}

export function LoadingState({ label = 'Loading…' }) {
  return (
    <div className="state" aria-busy="true">
      <Spinner />
      <span>{label}</span>
    </div>
  );
}
