import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { getErrorMessage, getFieldErrors } from '../../api/apiError';

// Student self-registration - the backend's POST /api/auth/register always creates a STUDENT
// account (AuthService.register hardcodes Role.STUDENT), so this page has no role picker.
// Matches LoginPage's split-screen layout/palette for visual consistency.

const valueProps = [
  'Access to every recruiting drive on campus',
  'Track your application status in real time',
  'Manage your resume and profile',
  'Get notified about new openings',
];

export default function StudentRegister() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setSubmitting(true);
    try {
      await register(name, email, password);
      navigate('/student/dashboard', { replace: true });
    } catch (err) {
      setError(getErrorMessage(err, 'Could not create your account. Please try again.'));
      setFieldErrors(getFieldErrors(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-[#f6f5fc] p-4">
      <div className="min-h-[560px] w-full max-w-4xl flex rounded-2xl overflow-hidden border border-[#e7e4fa] shadow-sm">
        <div className="hidden md:flex flex-col justify-center w-1/2 p-10 bg-[#14132a] text-white relative">
          <h1 className="text-[28px] font-medium leading-tight text-white m-0">
            Join the
            <br />
            placement portal
          </h1>
          <p className="text-[14px] text-[#a6a2c7] mt-3 leading-relaxed max-w-sm">
            Create a student account to explore drives, apply to roles, and track your progress.
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
          <h2 className="text-[20px] font-medium text-black">Create your account</h2>
          <p className="text-[13px] text-[#6b6785] mt-1 mb-5">Sign up as a student to get started.</p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div>
              <label htmlFor="register-name" className="text-[13px] font-medium text-black">
                Full name
              </label>
              <input
                id="register-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Enter your full name"
                autoComplete="name"
                required
                className="mt-1 w-full border border-[#c9bff5] rounded-lg px-3 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#7c5cfc]/30"
              />
              {fieldErrors.name && <p className="text-[12px] text-[#B23B3B] mt-1">{fieldErrors.name}</p>}
            </div>

            <div>
              <label htmlFor="register-email" className="text-[13px] font-medium text-black">
                Email
              </label>
              <input
                id="register-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter your email"
                autoComplete="email"
                required
                className="mt-1 w-full border border-[#c9bff5] rounded-lg px-3 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#7c5cfc]/30"
              />
              {fieldErrors.email && <p className="text-[12px] text-[#B23B3B] mt-1">{fieldErrors.email}</p>}
            </div>

            <div>
              <label htmlFor="register-password" className="text-[13px] font-medium text-black">
                Password
              </label>
              <input
                id="register-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="At least 8 characters"
                autoComplete="new-password"
                minLength={8}
                required
                className="mt-1 w-full border border-[#c9bff5] rounded-lg px-3 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#7c5cfc]/30"
              />
              {fieldErrors.password && <p className="text-[12px] text-[#B23B3B] mt-1">{fieldErrors.password}</p>}
            </div>

            {error && <p className="text-[13px] text-[#B23B3B]">{error}</p>}

            <button
              type="submit"
              disabled={submitting}
              className="w-full bg-[#7c5cfc] text-white text-[14px] font-medium rounded-lg py-2.5 hover:bg-[#6d28d9] transition disabled:opacity-60"
            >
              {submitting ? 'Creating account...' : 'Create account'}
            </button>
          </form>

          <p className="text-[13px] text-center text-[#6b6785] mt-4">
            Already have an account?{' '}
            <Link to="/student/login" className="text-[#7c5cfc] font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
