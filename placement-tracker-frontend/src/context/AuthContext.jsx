import { useState, useCallback } from 'react';
import api from '../api/api';
import { AuthContext } from './auth-context';

// The backend's AuthResponse/JWT carry no role claim (see LoginRequest/AuthResponse,
// AuthController, JwtService.generateToken — subject is just the email). So role isn't
// knowable from the login response itself. Instead, right after login, we probe a
// role-specific endpoint that is only reachable by the expected role
// (SecurityConfig: /api/admin/** -> ROLE_ADMIN, /api/student/** -> ROLE_STUDENT) and
// use the resulting 200/403 to confirm which role this account actually has.
async function verifyRole(expectedRole) {
  if (expectedRole === 'ADMIN') {
    await api.get('/api/admin/companies');
    return;
  }
  if (expectedRole === 'STUDENT') {
    // 404 here just means the student hasn't created a profile yet (ResourceNotFoundException) -
    // it still proves the account is authenticated as STUDENT, since /api/student/** is
    // role-gated before the controller ever runs.
    await api.get('/api/student/profile').catch((err) => {
      if (err.response?.status === 404) return;
      throw err;
    });
  }
  if (expectedRole === 'INTERVIEWER') {
    await api.get('/api/interviewer/assignments');
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [email, setEmail] = useState(() => localStorage.getItem('email'));
  const [name, setName] = useState(() => localStorage.getItem('name'));
  const [role, setRole] = useState(() => localStorage.getItem('role'));

  const login = useCallback(async (email, password, expectedRole) => {
    // Backend: POST /api/auth/login, LoginRequest { email, password } -> AuthResponse { token, email, name }
    const response = await api.post('/api/auth/login', { email, password });
    const { token, email: responseEmail, name: responseName } = response.data;

    // Stash the token first so the role-verification request below is authenticated.
    localStorage.setItem('token', token);

    if (expectedRole) {
      try {
        await verifyRole(expectedRole);
      } catch (err) {
        localStorage.removeItem('token');
        if (err.response?.status === 403) {
          const label = { STUDENT: 'student', ADMIN: 'admin', INTERVIEWER: 'interviewer' }[expectedRole] || expectedRole;
          throw new Error(`This account does not have ${label} access.`, { cause: err });
        }
        throw err;
      }
    }

    localStorage.setItem('email', responseEmail);
    localStorage.setItem('name', responseName);
    if (expectedRole) localStorage.setItem('role', expectedRole);

    setToken(token);
    setEmail(responseEmail);
    setName(responseName);
    if (expectedRole) setRole(expectedRole);

    return response.data;
  }, []);

  // Backend: POST /api/auth/register, RegisterRequest { name, email, password } -> AuthResponse
  // { token, email, name } - always creates a STUDENT account (AuthService.register hardcodes
  // Role.STUDENT), so unlike login this never needs the verifyRole probe: the role is known
  // by construction, not something to confirm.
  const register = useCallback(async (name, email, password) => {
    const response = await api.post('/api/auth/register', { name, email, password });
    const { token, email: responseEmail, name: responseName } = response.data;

    localStorage.setItem('token', token);
    localStorage.setItem('email', responseEmail);
    localStorage.setItem('name', responseName);
    localStorage.setItem('role', 'STUDENT');

    setToken(token);
    setEmail(responseEmail);
    setName(responseName);
    setRole('STUDENT');

    return response.data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('email');
    localStorage.removeItem('name');
    localStorage.removeItem('role');
    setToken(null);
    setEmail(null);
    setName(null);
    setRole(null);
  }, []);

  const value = {
    token,
    email,
    name,
    role,
    isAuthenticated: Boolean(token),
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
