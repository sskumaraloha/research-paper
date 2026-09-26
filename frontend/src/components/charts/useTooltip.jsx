import { useState } from 'react';
import { createPortal } from 'react-dom';

export function useTooltip() {
  const [tip, setTip] = useState(null);
  const show = (e, content) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX ?? rect.left + rect.width / 2;
    const y = e.clientY ?? rect.top;
    setTip({ x, y, content });
  };
  const hide = () => setTip(null);
  const node = tip
    ? createPortal(
        <div
          className="chart-tooltip"
          style={{ left: Math.min(tip.x + 12, window.innerWidth - 220), top: tip.y - 36 }}
          role="tooltip"
        >
          {tip.content}
        </div>,
        document.body,
      )
    : null;
  return { show, hide, node };
}
