import { NavLink } from 'react-router-dom';
import Icon from '../common/Icon';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
  { to: '/companies', label: 'Companies', icon: 'building' },
  { to: '/interviewers', label: 'Interviewers', icon: 'users' },
  { to: '/drives', label: 'Drives', icon: 'briefcase' },
  { to: '/applications', label: 'Applications', icon: 'fileText' },
  { to: '/candidates', label: 'Candidate Profiles', icon: 'user' },
  { to: '/offers', label: 'Offer Tracker', icon: 'award' },
  { to: '/reports', label: 'Reports & Analytics', icon: 'graduationCap' },
];

export default function Sidebar({ open, onClose }) {
  return (
    <>
      {open && <div className="sidebar-backdrop" onClick={onClose} />}
      <aside className={`sidebar${open ? ' open' : ''}`}>
        <div className="sidebar-brand">
          <div className="sidebar-brand-mark">
            <Icon name="briefcase" size={18} />
          </div>
          <div className="sidebar-brand-text">
            <span className="sidebar-brand-title">Placement Tracker</span>
            <span className="sidebar-brand-subtitle">Admin Portal</span>
          </div>
          <button type="button" className="sidebar-close" onClick={onClose} aria-label="Close menu">
            <Icon name="close" size={18} />
          </button>
        </div>

        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => 'sidebar-link' + (isActive ? ' active' : '')}
              onClick={onClose}
            >
              <Icon name={item.icon} size={17} />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <strong>Placement Cell</strong>
          Admin Portal
        </div>
      </aside>
    </>
  );
}
