import { Outlet } from 'react-router-dom';

export function AuthLayout() {
  return (
    <div className="auth-wrap">
      <div className="card auth-card">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">MI</span>
          Maintenance Intelligence
        </div>
        <Outlet />
      </div>
    </div>
  );
}
