import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import Icon from '../common/Icon';
import { getInitials } from '../../utils/initials';

const titleMap = [
  { prefix: '/student/dashboard', title: 'Dashboard' },
  { prefix: '/student/profile', title: 'My Profile' },
  { prefix: '/student/drives', title: 'Browse Drives' },
  { prefix: '/student/applications', title: 'My Applications' },
  { prefix: '/student/offers', title: 'Offers' },
];

function getPageTitle(pathname) {
  const match = titleMap.find((entry) => pathname.startsWith(entry.prefix));
  return match ? match.title : 'Placement Tracker';
}

export default function StudentNavbar({ onMenuClick }) {
  const { name, email, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const pageTitle = getPageTitle(location.pathname);
  const displayName = name || email || 'Student';

  function handleLogout() {
    logout();
    navigate('/student/login', { replace: true });
  }

  return (
    <header className="navbar">
      <div className="navbar-left">
        <button type="button" className="navbar-menu-toggle" onClick={onMenuClick} aria-label="Open menu">
          <Icon name="menu" size={20} />
        </button>
        <div className="navbar-breadcrumb">
          <span className="navbar-breadcrumb-root">Placement Tracker</span>
          <span className="navbar-breadcrumb-sep">/</span>
          <span className="navbar-breadcrumb-current">{pageTitle}</span>
        </div>
      </div>

      <div className="navbar-user">
        <div className="navbar-user-info">
          <div className="navbar-avatar">{getInitials(displayName, 'S')}</div>
          <div className="navbar-user-text">
            <span className="navbar-user-name">{displayName}</span>
            <span className="navbar-user-role">Student</span>
          </div>
        </div>
        <div className="navbar-divider" />
        <button type="button" className="navbar-logout" onClick={handleLogout}>
          <Icon name="logout" size={15} />
          Logout
        </button>
      </div>
    </header>
  );
}
