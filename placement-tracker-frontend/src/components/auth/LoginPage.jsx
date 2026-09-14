import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';

// Split-screen login, shared across all 3 real login routes (/login, /student/login,
// /interviewer/login) - each route renders this with its own defaultRole so the page opens
// on the right tab, but the tab switcher works everywhere: picking a different tab and
// submitting logs in as that role directly, from any of the 3 routes. Real auth only -
// onSubmit below calls the same useAuth().login(...) the old per-role pages used.
//
// Colors match the app's re-themed palette (index.css :root) - purple/navy/lavender.

const roles = [
  { key: 'student', label: 'Student', idLabel: 'Email', authRole: 'STUDENT', home: '/student/dashboard' },
  { key: 'admin', label: 'Admin', idLabel: 'Email', authRole: 'ADMIN', home: '/dashboard' },
  { key: 'company', label: 'Company', idLabel: 'Company email', authRole: 'INTERVIEWER', home: '/interviewer/dashboard' },
];

const valueProps = [
  'Access to every recruiting drive on campus',
  'Track your application status in real time',
  'Manage your resume and profile',
  'Get notified about new openings',
];

export default function LoginPage({ defaultRole = 'student' }) {
  const [activeRole, setActiveRole] = useState(defaultRole);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const role = roles.find((r) => r.key === activeRole);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!email.trim() || !password.trim()) {
      setError('Enter your credentials to continue.');
      return;
    }
    setError('');
    setSubmitting(true);
    try {
      await login(email, password, role.authRole);
      const from = location.state?.from?.pathname;
      navigate(from || role.home, { replace: true });
    } catch (err) {
      const message =
        err.response?.data?.message ||
        (err.response?.status === 401 ? 'Invalid email or password' : err.message || 'Login failed. Please try again.');
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-[#f6f5fc] p-4">
      <div className="min-h-[560px] w-full max-w-4xl flex rounded-2xl overflow-hidden border border-[#e7e4fa] shadow-sm">
        <div className="hidden md:flex flex-col justify-center w-1/2 p-10 bg-[#14132a] text-white relative">
          <h1 className="text-[28px] font-medium leading-tight text-white m-0">
            Welcome back,
            <br />
            {role.label.toLowerCase()}!
          </h1>
          <p className="text-[14px] text-[#a6a2c7] mt-3 leading-relaxed max-w-sm">
            Sign in to your placement portal account to explore drives, apply to roles, and track your progress.
          </p>
          <ul className="mt-6 space-y-3">
            {valueProps.map((v) => (
              <li key={v} className="flex items-start gap-2 text-[13px] text-[#c9c5e6]">
                <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#7c5cfc] flex-shrink-0" />
                {v}
              </li>
            ))}
          </ul>
        </div>

        <div className="w-full md:w-1/2 bg-white p-10 flex flex-col justify-center">
          <h2 className="text-[20px] font-medium text-black">Sign in</h2>
          <p className="text-[13px] text-[#6b6785] mt-1 mb-5">Enter your credentials to access your account.</p>

          <div className="flex gap-1 bg-[#ede9f5] rounded-lg p-1 mb-5" role="tablist">
            {roles.map((r) => (
              <button
                key={r.key}
                type="button"
                role="tab"
                aria-selected={activeRole === r.key}
                onClick={() => {
                  setActiveRole(r.key);
                  setError('');
                }}
                className={`flex-1 text-[13px] font-medium py-1.5 rounded-md transition ${
                  activeRole === r.key ? 'bg-white shadow-sm text-black' : 'text-[#6b6785]'
                }`}
              >
                {r.label}
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div>
              <label htmlFor="login-email" className="text-[13px] font-medium text-black">
                {role.idLabel}
              </label>
              <input
                id="login-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder={`Enter your ${role.idLabel.toLowerCase()}`}
                autoComplete="email"
                className="mt-1 w-full border border-[#c9bff5] rounded-lg px-3 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#7c5cfc]/30"
              />
            </div>
            <div>
              <label htmlFor="login-password" className="text-[13px] font-medium text-black">
                Password
              </label>
              <input
                id="login-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter your password"
                autoComplete="current-password"
                className="mt-1 w-full border border-[#c9bff5] rounded-lg px-3 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#7c5cfc]/30"
              />
            </div>

            {error && <p className="text-[13px] text-[#B23B3B]">{error}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-[#7c5cfc] text-white text-[14px] font-medium rounded-lg py-2.5 hover:bg-[#6d28d9] transition disabled:opacity-60"
            >
              {submitting ? 'Signing in...' : `Login as ${role.label.toLowerCase()}`}
            </button>
          </form>

          {activeRole === 'student' && (
            <p className="text-[13px] text-center text-[#6b6785] mt-4">
              New student?{' '}
              <Link to="/register" className="text-[#7c5cfc] font-medium">
                Create an account
              </Link>
            </p>
          )}

          {activeRole !== 'admin' && (
            <p className="text-[13px] text-center text-[#6b6785] mt-2">
              <Link to="/login" className="text-[#7c5cfc] font-medium">
                Placement Cell staff? Admin login
              </Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
