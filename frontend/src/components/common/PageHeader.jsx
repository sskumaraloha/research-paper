import { Link } from 'react-router-dom';

export function PageHeader({ title, subtitle, breadcrumb, actions }) {
  return (
    <header className="page-header">
      <div>
        {breadcrumb && (
          <div className="breadcrumb">
            {breadcrumb.map((b, i) => (
              <span key={b.label}>
                {i > 0 && ' / '}
                {b.to ? <Link to={b.to}>{b.label}</Link> : b.label}
              </span>
            ))}
          </div>
        )}
        <h1>{title}</h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="actions">{actions}</div>}
    </header>
  );
}
