import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function Layout() {
  const { user, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">Placement Tracker</div>
        <nav>
          {user ? (
            <>
              <NavLink to="/drives">Drives</NavLink>
              <NavLink to="/applications">Applications</NavLink>
              <NavLink to="/profile">Profile</NavLink>
              <span className="user-name">{user.name}</span>
              <button type="button" className="link-button" onClick={logout}>
                Log out
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login">Log in</NavLink>
              <NavLink to="/register">Register</NavLink>
            </>
          )}
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
