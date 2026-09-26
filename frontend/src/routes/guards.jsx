import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LoadingState } from '../components/common';

export function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <LoadingState label="Restoring your session…" />;
  if (status !== 'authenticated') return <Navigate to="/login" replace state={{ from: location }} />;
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { status } = useAuth();
  if (status === 'loading') return <LoadingState />;
  if (status === 'authenticated') return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
