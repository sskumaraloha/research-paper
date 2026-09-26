import { useTooltip } from './useTooltip';

/**
 * Horizontal single-series bar list: one hue, value label at the bar tip, per-bar hover tooltip.
 * items: [{ key, label, value, display, tooltip }]
 */
export function BarList({ items, ariaLabel }) {
  const { show, hide, node } = useTooltip();
  const max = Math.max(...items.map((i) => i.value ?? 0), 0) || 1;
  return (
    <div className="barlist" role="list" aria-label={ariaLabel}>
      {items.map((item) => (
        <div className="barlist-row" role="listitem" key={item.key}>
          <span className="barlist-label" title={item.label}>
            {item.label}
          </span>
          <div
            className="barlist-track"
            tabIndex={0}
            aria-label={`${item.label}: ${item.display}`}
            onMouseMove={(e) => show(e, item.tooltip ?? `${item.label}: ${item.display}`)}
            onMouseLeave={hide}
            onFocus={(e) => show(e, item.tooltip ?? `${item.label}: ${item.display}`)}
            onBlur={hide}
          >
            <div className="barlist-bar" style={{ width: `calc(${((item.value ?? 0) / max) * 100}% * 0.82)` }} />
            <span className="barlist-value">{item.display}</span>
          </div>
        </div>
      ))}
      {node}
    </div>
  );
}
