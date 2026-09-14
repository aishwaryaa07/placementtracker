import { NavLink } from 'react-router-dom';
import Icon from '../common/Icon';

const navItems = [
  { to: '/interviewer/dashboard', label: 'My Candidates', icon: 'users' },
  { to: '/interviewer/drives', label: 'My Drives', icon: 'briefcase' },
];

export default function InterviewerSidebar({ open, onClose }) {
  return (
    <>
      {open && <div className="sidebar-backdrop" onClick={onClose} />}
      <aside className={`sidebar${open ? ' open' : ''}`}>
        <div className="sidebar-brand">
          <div className="sidebar-brand-mark">
            <Icon name="users" size={18} />
          </div>
          <div className="sidebar-brand-text">
            <span className="sidebar-brand-title">Placement Tracker</span>
            <span className="sidebar-brand-subtitle">Interviewer Portal</span>
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
          Interviewer Portal
        </div>
      </aside>
    </>
  );
}
