import { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

function homePathFor(role) {
  if (role === 'STUDENT') return '/student/dashboard';
  if (role === 'INTERVIEWER') return '/interviewer/dashboard';
  return '/dashboard';
}

function loginPathFor(role) {
  if (role === 'STUDENT') return '/student/login';
  if (role === 'INTERVIEWER') return '/interviewer/login';
  return '/login';
}

// role: 'ADMIN' | 'STUDENT' | 'INTERVIEWER' | undefined (undefined = any authenticated user)
export default function ProtectedRoute({ role }) {
  const { isAuthenticated, role: userRole, logout } = useAuth();
  const location = useLocation();

  // A token can exist with no known role for a session that predates role-tracking
  // (or a partially-cleared localStorage) - there is nothing safe to redirect it to,
  // since "not STUDENT" isn't the same as "is ADMIN". Left alone, a role-gated route
  // like /dashboard would redirect to itself (homePath defaults to /dashboard),
  // rendering nothing but the page background forever. Clear the stale session
  // instead and send them to sign in properly, which always sets role correctly.
  const staleSession = isAuthenticated && !userRole;

  useEffect(() => {
    if (staleSession) logout();
  }, [staleSession, logout]);

  if (!isAuthenticated || staleSession) {
    return <Navigate to={loginPathFor(role)} replace state={{ from: location }} />;
  }

  // Authenticated, but not the role this route requires (e.g. a STUDENT hitting an
  // admin route) - send them to their own portal instead of the login page.
  if (role && userRole !== role) {
    return <Navigate to={homePathFor(userRole)} replace />;
  }

  return <Outlet />;
}
