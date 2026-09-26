export function EmptyState({ title = 'Nothing here yet', children, action }) {
  return (
    <div className="state">
      <div className="state-title">{title}</div>
      {children && <div>{children}</div>}
      {action}
    </div>
  );
}
