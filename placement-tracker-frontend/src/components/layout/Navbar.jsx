import { useEffect, useRef, useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { getCompanies } from '../../api/companyService';
import { getDrives } from '../../api/driveService';
import { getAllApplications } from '../../api/applicationService';
import Icon from '../common/Icon';
import { getInitials } from '../../utils/initials';

const titleMap = [
  { prefix: '/dashboard', title: 'Dashboard' },
  { prefix: '/companies', title: 'Companies' },
  { prefix: '/interviewers', title: 'Interviewers' },
  { prefix: '/drives', title: 'Drives' },
  { prefix: '/candidates', title: 'Candidate Profiles' },
  { prefix: '/offers', title: 'Offer Tracker' },
  { prefix: '/reports', title: 'Reports & Analytics' },
  { prefix: '/applications', title: 'Applications' },
];

function getPageTitle(pathname) {
  const match = titleMap.find((entry) => pathname.startsWith(entry.prefix));
  return match ? match.title : 'Placement Tracker';
}

function timeAgo(dateString, now) {
  const diffMs = now - new Date(dateString).getTime();
  const hours = Math.floor(diffMs / (1000 * 60 * 60));
  if (hours < 1) return 'just now';
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

// Real, computed recent-activity - not a persisted/backend notification system (no read/
// unread state, nothing stored). Every render, a live snapshot of applications/offers from
// the last 48h is recomputed from data already fetched for search - real events, just no
// inbox behind it.
const RECENT_WINDOW_MS = 48 * 60 * 60 * 1000;

export default function Navbar({ onMenuClick }) {
  const { name, email, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const pageTitle = getPageTitle(location.pathname);
  const displayName = name || email || 'Admin';

  const [data, setData] = useState(null); // { companies, drives, applications }
  const [now, setNow] = useState(0);
  const [query, setQuery] = useState('');
  const [searchOpen, setSearchOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const blurTimeout = useRef(null);
  const notifRef = useRef(null);

  useEffect(() => {
    Promise.all([getCompanies(), getDrives(), getAllApplications()])
      .then(([companies, drives, applications]) => setData({ companies, drives, applications }))
      .catch(() => setData({ companies: [], drives: [], applications: [] }))
      .finally(() => setNow(Date.now()));
  }, []);

  useEffect(() => {
    function handleClickOutside(e) {
      if (notifRef.current && !notifRef.current.contains(e.target)) setNotifOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  const term = query.trim().toLowerCase();
  const matchedCompanies = term && data ? data.companies.filter((c) => c.name.toLowerCase().includes(term)).slice(0, 5) : [];
  const matchedDrives =
    term && data
      ? data.drives.filter((d) => `${d.role} ${d.company.name}`.toLowerCase().includes(term)).slice(0, 5)
      : [];
  const matchedApplications =
    term && data
      ? data.applications.filter((a) => (a.studentName || '').toLowerCase().includes(term)).slice(0, 5)
      : [];
  const hasResults = matchedCompanies.length + matchedDrives.length + matchedApplications.length > 0;

  function goTo(path) {
    setQuery('');
    setSearchOpen(false);
    navigate(path);
  }

  function handleSearchBlur() {
    // Let a click on a result register before the dropdown closes.
    blurTimeout.current = setTimeout(() => setSearchOpen(false), 150);
  }

  const recentEvents = data
    ? [
        ...data.applications.map((a) => ({
          key: `app-${a.id}`,
          text: `${a.studentName} applied to ${a.drive.company.name}`,
          at: a.appliedAt,
          path: `/applications/${a.id}`,
        })),
        ...data.applications
          .filter((a) => a.offer)
          .map((a) => ({
            key: `offer-${a.id}`,
            text: `Offer extended to ${a.studentName} - ${a.drive.company.name}`,
            at: a.offer.offerDate || a.appliedAt,
            path: `/applications/${a.id}`,
          })),
      ]
        .filter((e) => e.at && now - new Date(e.at).getTime() <= RECENT_WINDOW_MS)
        .sort((a, b) => new Date(b.at) - new Date(a.at))
    : [];

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

        <div className="navbar-search">
          <Icon name="search" size={15} />
          <input
            type="text"
            placeholder="Search companies, drives, students..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => setSearchOpen(true)}
            onBlur={handleSearchBlur}
            onKeyDown={(e) => e.key === 'Escape' && setSearchOpen(false)}
            aria-label="Global search"
          />
          {searchOpen && term && (
            <div className="navbar-search-results">
              {!data ? (
                <div className="navbar-search-empty">Loading...</div>
              ) : !hasResults ? (
                <div className="navbar-search-empty">No matches for &quot;{query}&quot;</div>
              ) : (
                <>
                  {matchedCompanies.length > 0 && (
                    <div className="navbar-search-group">
                      <div className="navbar-search-group-label">Companies</div>
                      {matchedCompanies.map((c) => (
                        <button key={c.id} type="button" onMouseDown={() => goTo('/companies')}>
                          {c.name}
                        </button>
                      ))}
                    </div>
                  )}
                  {matchedDrives.length > 0 && (
                    <div className="navbar-search-group">
                      <div className="navbar-search-group-label">Drives</div>
                      {matchedDrives.map((d) => (
                        <button key={d.id} type="button" onMouseDown={() => goTo(`/drives/${d.id}`)}>
                          {d.role} @ {d.company.name}
                        </button>
                      ))}
                    </div>
                  )}
                  {matchedApplications.length > 0 && (
                    <div className="navbar-search-group">
                      <div className="navbar-search-group-label">Students</div>
                      {matchedApplications.map((a) => (
                        <button key={a.id} type="button" onMouseDown={() => goTo(`/applications/${a.id}`)}>
                          {a.studentName} - {a.drive.company.name}
                        </button>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="navbar-user">
        <div className="navbar-notif" ref={notifRef}>
          <button
            type="button"
            className="navbar-icon-btn"
            onClick={() => setNotifOpen((prev) => !prev)}
            aria-label="Recent activity"
          >
            <Icon name="inbox" size={18} />
            {recentEvents.length > 0 && <span className="navbar-notif-badge">{recentEvents.length}</span>}
          </button>
          {notifOpen && (
            <div className="navbar-notif-dropdown">
              <div className="navbar-notif-header">Recent activity (last 48h)</div>
              {recentEvents.length === 0 ? (
                <div className="navbar-search-empty">Nothing new recently.</div>
              ) : (
                recentEvents.slice(0, 8).map((e) => (
                  <Link key={e.key} to={e.path} className="navbar-notif-item" onClick={() => setNotifOpen(false)}>
                    <span>{e.text}</span>
                    <span className="cell-muted">{timeAgo(e.at, now)}</span>
                  </Link>
                ))
              )}
            </div>
          )}
        </div>

        <div className="navbar-user-info">
          <div className="navbar-avatar">{getInitials(displayName, 'A')}</div>
          <div className="navbar-user-text">
            <span className="navbar-user-name">{displayName}</span>
            <span className="navbar-user-role">Admin</span>
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
