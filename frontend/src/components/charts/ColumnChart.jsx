import { useEffect, useRef, useState } from 'react';
import { useTooltip } from './useTooltip';

function niceMax(value) {
  if (value <= 0) return 1;
  const pow = 10 ** Math.floor(Math.log10(value));
  const n = value / pow;
  return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * pow;
}

/**
 * Vertical single-series column chart (time on x). Columns ≤ 24px, 4px rounded tops, square at the
 * baseline, recessive gridlines, per-column hover tooltip.
 * points: [{ key, label, value, tooltip }]
 */
export function ColumnChart({ points, height = 200, formatValue = String, ariaLabel }) {
  const ref = useRef(null);
  const [width, setWidth] = useState(600);
  const { show, hide, node } = useTooltip();

  useEffect(() => {
    const el = ref.current;
    if (!el) return undefined;
    const ro = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width || 600));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const pad = { top: 12, right: 8, bottom: 26, left: 48 };
  const innerW = Math.max(width - pad.left - pad.right, 10);
  const innerH = height - pad.top - pad.bottom;
  const max = niceMax(Math.max(...points.map((p) => p.value ?? 0), 0));
  const band = innerW / Math.max(points.length, 1);
  const colW = Math.min(24, band * 0.6);
  const ticks = [0, 0.5, 1].map((t) => t * max);
  const y = (v) => pad.top + innerH - (v / max) * innerH;
  const r = 4;
  const labelEvery = Math.ceil(points.length / Math.max(Math.floor(innerW / 56), 1));

  return (
    <div ref={ref} className="col-chart chart">
      <svg width={width} height={height} role="img" aria-label={ariaLabel}>
        {ticks.map((t) => (
          <g key={t}>
            <line className="gridline" x1={pad.left} x2={pad.left + innerW} y1={y(t)} y2={y(t)} />
            <text x={pad.left - 8} y={y(t) + 4} textAnchor="end">
              {formatValue(t)}
            </text>
          </g>
        ))}
        {points.map((p, i) => {
          const cx = pad.left + band * i + band / 2;
          const top = y(p.value ?? 0);
          const h = pad.top + innerH - top;
          const rr = Math.min(r, h / 2, colW / 2);
          const x0 = cx - colW / 2;
          const base = pad.top + innerH;
          const path =
            h > 0
              ? `M${x0},${base} V${top + rr} Q${x0},${top} ${x0 + rr},${top} H${x0 + colW - rr} Q${x0 + colW},${top} ${x0 + colW},${top + rr} V${base} Z`
              : '';
          const tip = p.tooltip ?? `${p.label}: ${formatValue(p.value)}`;
          return (
            <g key={p.key} onMouseMove={(e) => show(e, tip)} onMouseLeave={hide}>
              <rect className="col-hit" x={pad.left + band * i} y={pad.top} width={band} height={innerH} />
              {path && <path className="col" d={path} />}
              {i % labelEvery === 0 && (
                <text x={cx} y={height - 8} textAnchor="middle">
                  {p.label}
                </text>
              )}
            </g>
          );
        })}
        <line className="axis" x1={pad.left} x2={pad.left + innerW} y1={pad.top + innerH} y2={pad.top + innerH} />
      </svg>
      {node}
    </div>
  );
}
