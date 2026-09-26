import { useState } from 'react';

/** Wraps a chart with a Chart/Table toggle so every chart has an accessible table view. */
export function ChartFrame({ title, columns, rows, children, actions }) {
  const [view, setView] = useState('chart');
  return (
    <div className="card">
      <div className="card-header">
        <h2>{title}</h2>
        <div className="row">
          {actions}
          <button
            type="button"
            className="btn btn-ghost btn-sm chart-toggle"
            onClick={() => setView(view === 'chart' ? 'table' : 'chart')}
            aria-pressed={view === 'table'}
          >
            {view === 'chart' ? 'Table view' : 'Chart view'}
          </button>
        </div>
      </div>
      {view === 'chart' ? (
        <div className="card-body">{children}</div>
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                {columns.map((c) => (
                  <th key={c.label} className={c.numeric ? 'num' : undefined}>
                    {c.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={i}>
                  {columns.map((c) => (
                    <td key={c.label} className={c.numeric ? 'num' : undefined}>
                      {c.render(r)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
