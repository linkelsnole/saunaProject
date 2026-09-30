import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';

interface PublicRouteProps {
  children: ReactNode;
}

export function PublicRoute({ children }: PublicRouteProps) {
  const { isAuthenticated, isLoading } = useAuth();

  // Пока идёт bootstrap-refresh, не мигаем формой логина
  if (isLoading) {
    return <div className="min-h-screen bg-background" />;
  }

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
