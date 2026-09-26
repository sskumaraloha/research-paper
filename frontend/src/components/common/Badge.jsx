/**
 * Neutral label for backend string values (status, severity, source, role…). The contract defines no
 * enums for these, so values are shown verbatim without guessed color semantics (API_ANALYSIS Q4).
 */
export function Badge({ children, tone }) {
  if (children === null || children === undefined || children === '') return <span className="subtle">—</span>;
  return <span className={`badge${tone ? ` ${tone}` : ''}`}>{String(children).replaceAll('_', ' ')}</span>;
}
