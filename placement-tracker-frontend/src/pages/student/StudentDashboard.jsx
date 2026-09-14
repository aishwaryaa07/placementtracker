import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/useAuth';
import { getMyProfileOrNull } from '../../api/studentProfileService';
import { getOpenDrives } from '../../api/studentDriveService';
import { getMyApplications } from '../../api/studentApplicationService';
import { getErrorMessage } from '../../api/apiError';
import PageHeader from '../../components/common/PageHeader';
import StudentStatTile from '../../components/student/StudentStatTile';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';
import Icon from '../../components/common/Icon';
import CompanyLogo from '../../components/common/CompanyLogo';
import { ErrorMessage } from '../../components/common/StateMessage';

function StatSkeleton() {
  return (
    <div className="stat-grid stat-grid-skeleton">
      {Array.from({ length: 4 }).map((_, i) => (
        <div className="stat-card" key={i}>
          <div className="skeleton skeleton-circle" />
          <div className="stat-card-body" style={{ flex: 1 }}>
            <div className="skeleton skeleton-text" style={{ width: '40%', height: 20, marginBottom: 8 }} />
            <div className="skeleton skeleton-text" style={{ width: '70%' }} />
          </div>
        </div>
      ))}
    </div>
  );
}

function PanelSkeleton() {
  return (
    <div className="panel">
      <div className="panel-body padded">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="skeleton skeleton-text" style={{ width: '90%', height: 14, marginBottom: 14 }} />
        ))}
      </div>
    </div>
  );
}

export default function StudentDashboard() {
  const { name, email } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [profile, setProfile] = useState(null);
  const [openDrives, setOpenDrives] = useState([]);
  const [applications, setApplications] = useState([]);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([getMyProfileOrNull(), getOpenDrives(), getMyApplications()])
      .then(([profileData, driveList, applicationList]) => {
        setProfile(profileData);
        setOpenDrives(driveList);
        setApplications(applicationList);
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load dashboard data.')))
      .finally(() => setLoading(false));
  }

  if (!loading && error) {
    return (
      <div>
        <PageHeader title="Dashboard" subtitle={`Welcome back, ${name || email}.`} />
        <ErrorMessage text={error} onRetry={load} />
      </div>
    );
  }

  const appliedDriveIds = new Set(applications.map((a) => a.drive.id));
  const eligibleOpenDrives = openDrives.filter((d) => d.eligible);
  const offersReceived = applications.filter((a) => a.offer);
  const pendingOffers = applications.filter((a) => a.offer?.status === 'PENDING');

  const recommendedDrives = eligibleOpenDrives
    .filter((d) => !appliedDriveIds.has(d.drive.id) && d.drive.status !== 'CLOSED')
    .sort((a, b) => (a.drive.applicationDeadline || '9999').localeCompare(b.drive.applicationDeadline || '9999'))
    .slice(0, 5);

  const recentApplications = [...applications]
    .sort((a, b) => new Date(b.appliedAt) - new Date(a.appliedAt))
    .slice(0, 5);

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle={
          profile
            ? `Welcome back, ${name || email}. Here's your placement snapshot.`
            : `Welcome, ${name || email}! Let's get your placement snapshot set up.`
        }
      />

      {!loading && !profile && (
        <div className="state-message" style={{ marginBottom: 20 }}>
          <Icon name="alertCircle" size={18} />
          <span>
            Your profile is incomplete, so drive eligibility can't be calculated yet.{' '}
            <Link to="/student/profile" className="link-button">
              Complete your profile
            </Link>
            .
          </span>
        </div>
      )}

      {loading ? (
        <StatSkeleton />
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3" style={{ marginBottom: 20 }}>
          <StudentStatTile label="Eligible Drives" value={eligibleOpenDrives.length} color="#7c5cfc" />
          <StudentStatTile label="My Applications" value={applications.length} color="#9A6A0F" />
          <StudentStatTile label="Offers Received" value={offersReceived.length} color="#5B3DA6" />
          <StudentStatTile label="Profile Status" value={profile ? 'Complete' : 'Incomplete'} color="#1E5FA5" />
        </div>
      )}

      {pendingOffers.length > 0 && (
        <div className="state-message" style={{ marginBottom: 20, borderColor: 'var(--accent)' }}>
          <Icon name="star" size={18} />
          <span>
            You have {pendingOffers.length} pending {pendingOffers.length === 1 ? 'offer' : 'offers'} waiting for your
            response.{' '}
            <Link to="/student/offers" className="link-button">
              Review offers
            </Link>
            .
          </span>
        </div>
      )}

      <div className="dashboard-grid">
        <div className="panel">
          <div className="panel-header">
            <h2>Recommended Drives</h2>
            <Link className="panel-header-link" to="/student/drives">
              View all
            </Link>
          </div>
          {loading ? (
            <PanelSkeleton />
          ) : recommendedDrives.length === 0 ? (
            <div className="panel-body padded">
              <EmptyState
                compact
                icon="briefcase"
                title="No new recommended drives"
                description="Eligible drives you haven't applied to yet will show up here."
              />
            </div>
          ) : (
            <ul className="panel-list">
              {recommendedDrives.map((d) => (
                <li key={d.drive.id} className="panel-list-item">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <CompanyLogo name={d.drive.company.name} logoUrl={d.drive.company.logoUrl} size={28} />
                    <div>
                      <Link to={`/student/drives/${d.drive.id}`} className="panel-list-primary link-button">
                        {d.drive.role}
                      </Link>
                      <div className="panel-list-secondary">{d.drive.company.name}</div>
                    </div>
                  </div>
                  <div className="panel-list-meta">
                    <StatusBadge status={d.drive.status} />
                    <div>{d.drive.applicationDeadline ? `Apply by ${d.drive.applicationDeadline}` : 'No deadline'}</div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Recent Applications</h2>
            <Link className="panel-header-link" to="/student/applications">
              View all
            </Link>
          </div>
          {loading ? (
            <PanelSkeleton />
          ) : recentApplications.length === 0 ? (
            <div className="panel-body padded">
              <EmptyState
                compact
                icon="fileText"
                title="No applications yet"
                description="Drives you apply to will show up here."
              />
            </div>
          ) : (
            <ul className="panel-list">
              {recentApplications.map((application) => (
                <li key={application.id} className="panel-list-item">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <CompanyLogo name={application.drive.company.name} logoUrl={application.drive.company.logoUrl} size={28} />
                    <div>
                      <Link to={`/student/applications/${application.id}`} className="panel-list-primary link-button">
                        {application.drive.role}
                      </Link>
                      <div className="panel-list-secondary">{application.drive.company.name}</div>
                    </div>
                  </div>
                  <div className="panel-list-meta">
                    <StatusBadge status={application.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
