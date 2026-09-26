import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { usePlant } from '../context/PlantContext';

const NAV = [
  {
    label: 'Overview',
    items: [
      { to: '/dashboard', label: 'Dashboard' },
      { to: '/assistant', label: 'Assistant' },
    ],
  },
  {
    label: 'Maintenance',
    items: [
      { to: '/records', label: 'Records' },
      { to: '/entry', label: 'Guided entry' },
      { to: '/machines', label: 'Machines' },
      { to: '/schedules', label: 'Schedules' },
      { to: '/parts', label: 'Spare parts' },
    ],
  },
  {
    label: 'Data quality',
    items: [
      { to: '/imports', label: 'Imports' },
      { to: '/validation', label: 'Validation queue', badge: 'pendingValidation', end: true },
      { to: '/validation/aliases', label: 'Alias suggestions' },
    ],
  },
  {
    label: 'Intelligence',
    items: [
      { to: '/insights', label: 'Insights', badge: 'insights' },
      { to: '/analytics', label: 'Analytics' },
    ],
  },
  {
    label: 'Administration',
    items: [
      { to: '/users', label: 'Users' },
      { to: '/settings', label: 'Plant settings' },
      { to: '/audit', label: 'Audit log' },
      { to: '/platform', label: 'Platform' },
    ],
  },
];

export function AppLayout() {
  const { user, signOut } = useAuth();
  const { plants, plantId, setPlantId, badges } = usePlant();
  const [menuOpen, setMenuOpen] = useState(false);
  const [search, setSearch] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => setMenuOpen(false), [location.pathname]);

  const submitSearch = (e) => {
    e.preventDefault();
    const q = search.trim();
    if (q) navigate(`/search?query=${encodeURIComponent(q)}`);
  };

  const unread = badges?.unreadNotifications ?? 0;

  return (
    <div className="shell">
      <aside className={`sidebar${menuOpen ? ' open' : ''}`} aria-label="Main navigation">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">MI</span>
          Maintenance Intelligence
        </div>
        {NAV.map((group) => (
          <nav className="nav-group" key={group.label} aria-label={group.label}>
            <div className="nav-label">{group.label}</div>
            {group.items.map((item) => {
              const count = item.badge ? badges?.[item.badge] : null;
              return (
                <NavLink key={item.to} to={item.to} end={item.end} className="nav-link">
                  <span>{item.label}</span>
                  {count > 0 && <span className="nav-count">{count}</span>}
                </NavLink>
              );
            })}
          </nav>
        ))}
        <div className="sidebar-footer">
          <div>
            <div style={{ fontWeight: 600 }}>{user?.fullName}</div>
            <div className="subtle">{user?.email}</div>
          </div>
          <button type="button" className="btn btn-sm btn-block" onClick={signOut}>
            Sign out
          </button>
        </div>
      </aside>
      <div className={`scrim${menuOpen ? ' open' : ''}`} onClick={() => setMenuOpen(false)} aria-hidden="true" />

      <div className="main">
        <header className="topbar">
          <button
            type="button"
            className="btn btn-ghost icon-btn menu-toggle"
            aria-label="Open navigation"
            onClick={() => setMenuOpen(true)}
          >
            ☰
          </button>
          <form className="topbar-search hide-sm" role="search" onSubmit={submitSearch}>
            <label className="sr-only" htmlFor="global-search">
              Search machines, records, parts
            </label>
            <input
              id="global-search"
              className="input"
              type="search"
              placeholder="Search machines, records, parts…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </form>
          <NavLink to="/search" className="btn btn-ghost icon-btn show-sm" aria-label="Search">
            <span aria-hidden="true">⌕</span>
          </NavLink>
          <div className="topbar-spacer" />
          {plants.length > 0 && (
            <>
              <label className="sr-only" htmlFor="plant-select">
                Plant
              </label>
              <select
                id="plant-select"
                className="input plant-select"
                value={plantId ?? ''}
                onChange={(e) => setPlantId(Number(e.target.value))}
              >
                {plants.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </>
          )}
          <NavLink
            to="/notifications"
            className="btn btn-ghost icon-btn"
            aria-label={`Notifications${unread ? ` (${unread} unread)` : ''}`}
          >
            <span aria-hidden="true">🔔</span>
            {unread > 0 && <span className="dot">{unread > 99 ? '99+' : unread}</span>}
          </NavLink>
          <div className="row hide-sm" style={{ gap: 10 }}>
            <div style={{ textAlign: 'right', lineHeight: 1.2 }}>
              <div style={{ fontWeight: 600 }}>{user?.fullName}</div>
              <div className="subtle">{user?.role}</div>
            </div>
          </div>
          <button type="button" className="btn btn-sm hide-sm" onClick={signOut}>
            Sign out
          </button>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
