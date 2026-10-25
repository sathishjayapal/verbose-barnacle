import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from 'app/common/auth-context';

/**
 * Forces unauthenticated users to the login page and (optionally) enforces
 * role membership. Wrap protected routes with this element.
 *
 * Usage in routes:
 *   { element: <RequireAuth />, children: [...protected routes] }
 *   { element: <RequireAuth roles={['ADMIN']} />, children: [...admin only] }
 */
export function RequireAuth({ roles }: { roles?: string[] }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="flex justify-center py-16 text-gray-500" role="status">
        Loading…
      </div>
    );
  }

  if (!user) {
    // remember where the user wanted to go so login can send them back
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (roles && roles.length > 0 && !roles.some((r) => user.authorities.includes(r))) {
    return <Navigate to="/error" state={{ msgError: 'You are not authorized to view this page.' }} replace />;
  }

  return <Outlet />;
}

/** Redirects already-authenticated users away from /login. */
export function RedirectIfAuthed({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (user) return <Navigate to="/repositories" replace />;
  return <>{children}</>;
}
