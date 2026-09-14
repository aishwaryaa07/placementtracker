import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { getCompanies } from '../api/companyService';
import { getDrives } from '../api/driveService';
import { getAllApplications } from '../api/applicationService';
import { getErrorMessage } from '../api/apiError';
import PageHeader from '../components/common/PageHeader';
import StatCard from '../components/common/StatCard';
import EmptyState from '../components/common/EmptyState';
import StatusBadge from '../components/common/StatusBadge';
import CompanyLogo from '../components/common/CompanyLogo';
import PlacementFunnelChart from '../components/dashboard/PlacementFunnelChart';
import DrivesCalendar from '../components/dashboard/DrivesCalendar';
import DriveFunnel from '../components/drives/DriveFunnel';
import { ErrorMessage } from '../components/common/StateMessage';
import { computeBranchStats } from '../utils/branchStats';

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

// Furthest round sequence this application has actually PASSED (0 if none). Used to derive
// the "Shortlisted"/"Interviewed" funnel stages generically, since round names/counts vary
// per drive - these are a proxy ("made it past round 1", "made it past round 2"), not a
// literally-named pipeline stage.
function furthestPassedSequence(application) {
  return application.roundResults
    .filter((rr) => rr.status === 'PASSED')
    .reduce((max, rr) => Math.max(max, rr.roundSequence), 0);
}

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

export default function Dashboard() {
  const { name, email } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [companies, setCompanies] = useState([]);
  const [drives, setDrives] = useState([]);
  const [applications, setApplications] = useState([]);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([getCompanies(), getDrives(), getAllApplications()])
      .then(([companyList, driveList, applicationList]) => {
        setCompanies(companyList);
        setDrives(driveList);
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

  const activeDrives = drives.filter((d) => d.status !== 'CLOSED');
  const studentsPlaced = applications.filter((a) => a.offer?.status === 'ACCEPTED');

  const driveCountByCompany = new Map();
  drives.forEach((d) => driveCountByCompany.set(d.company.id, (driveCountByCompany.get(d.company.id) || 0) + 1));

  const recentCompanies = [...companies].sort((a, b) => b.id - a.id).slice(0, 5);
  const drivesWithDates = drives.filter((d) => d.driveDate);
  // "High-value" = strictly a positive-signal list, never a place a rejected candidate can
  // surface - REJECTED is excluded outright, not just out-ranked. What qualifies:
  // 1. Has a confirmed offer (any package), OR
  // 2. status === 'SELECTED', OR
  // 3. Has actually PASSED at least one round.
  // An application with none of those (e.g. freshly APPLIED, nothing decided yet) doesn't
  // qualify either - this panel is "who's worth looking at," not "everyone."
  //
  // Within the qualifying set: offer-holders rank above everyone else, sorted by the offered
  // package (the only real "package" figure once an offer exists); everyone else is ranked by
  // the furthest round they've passed, since they have no package to sort on yet. Most
  // recently applied breaks any remaining tie in either group.
  const recentApplications = applications
    .filter((a) => {
      if (a.status === 'REJECTED') return false;
      return Boolean(a.offer) || a.status === 'SELECTED' || furthestPassedSequence(a) > 0;
    })
    .sort((a, b) => {
      const hasOfferA = Boolean(a.offer);
      const hasOfferB = Boolean(b.offer);
      if (hasOfferA !== hasOfferB) return hasOfferA ? -1 : 1;

      if (hasOfferA && hasOfferB) {
        const ctcDiff = (b.offer.ctcOffered ?? 0) - (a.offer.ctcOffered ?? 0);
        if (ctcDiff !== 0) return ctcDiff;
      } else {
        const roundDiff = furthestPassedSequence(b) - furthestPassedSequence(a);
        if (roundDiff !== 0) return roundDiff;
      }

      return new Date(b.appliedAt) - new Date(a.appliedAt);
    })
    .slice(0, 6);

  // Applied -> Shortlisted -> Interviewed -> Offered -> Joined. Shortlisted/Interviewed are a
  // generic proxy ("passed round 1" / "passed round 2") since round names/counts vary per
  // drive - there's no single named stage that applies to every drive uniformly.
  const funnelStages = [
    { key: 'applied', label: 'Applied', value: applications.length },
    { key: 'shortlisted', label: 'Shortlisted', value: applications.filter((a) => furthestPassedSequence(a) >= 1).length },
    { key: 'interviewed', label: 'Interviewed', value: applications.filter((a) => furthestPassedSequence(a) >= 2).length },
    { key: 'offered', label: 'Offered', value: applications.filter((a) => a.offer).length },
    { key: 'joined', label: 'Joined', value: studentsPlaced.length },
  ];

  const topBranches = computeBranchStats(applications).slice(0, 5);

  // The drive to spotlight: the ONGOING drive with the most applications, falling back to
  // the most recently created UPCOMING one, so the panel always has something relevant to
  // show without needing admin configuration.
  const spotlightDrive = (() => {
    const applicationCountByDrive = new Map();
    applications.forEach((a) => applicationCountByDrive.set(a.drive.id, (applicationCountByDrive.get(a.drive.id) || 0) + 1));
    const ongoing = drives.filter((d) => d.status === 'ONGOING');
    if (ongoing.length > 0) {
      return [...ongoing].sort((a, b) => (applicationCountByDrive.get(b.id) || 0) - (applicationCountByDrive.get(a.id) || 0))[0];
    }
    const upcoming = [...drives].filter((d) => d.status === 'UPCOMING').sort((a, b) => b.id - a.id);
    return upcoming[0] || null;
  })();

  return (
    <div>
      <PageHeader title="Dashboard" subtitle={`Welcome back, ${name || email}. Here's what's happening today.`} />

      {loading ? (
        <StatSkeleton />
      ) : (
        <div className="stat-grid">
          <StatCard icon="building" variant="arctic" value={companies.length} label="Total Companies" />
          <StatCard icon="briefcase" variant="sage" value={activeDrives.length} label="Active Drives" />
          <StatCard icon="fileText" variant="bubblegum" value={applications.length} label="Total Applications" />
          <StatCard icon="award" variant="sapphire" value={studentsPlaced.length} label="Students Placed" />
        </div>
      )}

      <div className="dashboard-grid dashboard-grid-equal">
        <div className="panel">
          <div className="panel-header">
            <h2>Placement Funnel &amp; Selection Status</h2>
          </div>
          {loading ? (
            <PanelSkeleton />
          ) : applications.length === 0 ? (
            <div className="panel-body padded">
              <EmptyState
                compact
                icon="fileText"
                title="No applications yet"
                description="The placement funnel fills in once students start applying."
              />
            </div>
          ) : (
            <div className="panel-body padded">
              <PlacementFunnelChart stages={funnelStages} />
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-header">
            <h2>Upcoming Drives</h2>
            <Link className="panel-header-link" to="/drives">
              View all
            </Link>
          </div>
          {loading ? (
            <PanelSkeleton />
          ) : drivesWithDates.length === 0 ? (
            <div className="panel-body padded">
              <EmptyState
                compact
                icon="briefcase"
                title="No dated drives yet"
                description="Drives with a Drive Date set will show up on the calendar here."
              />
            </div>
          ) : (
            <div className="panel-body padded">
              <DrivesCalendar drives={drivesWithDates} />
            </div>
          )}
        </div>
      </div>

      <div className="panel" style={{ marginBottom: 20 }}>
        <div className="panel-header">
          <h2>Placement Drives</h2>
          {spotlightDrive && (
            <Link className="panel-header-link" to={`/drives/${spotlightDrive.id}`}>
              View drive
            </Link>
          )}
        </div>
        {loading ? (
          <PanelSkeleton />
        ) : !spotlightDrive ? (
          <div className="panel-body padded">
            <EmptyState compact icon="briefcase" title="No drives yet" description="Add a drive to see its pipeline here." />
          </div>
        ) : (
          <div className="panel-body padded">
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
              <CompanyLogo name={spotlightDrive.company.name} logoUrl={spotlightDrive.company.logoUrl} size={32} />
              <div>
                <div className="panel-list-primary">
                  {spotlightDrive.role} @ {spotlightDrive.company.name}
                </div>
                <StatusBadge status={spotlightDrive.status} />
              </div>
            </div>
            <DriveFunnel driveId={spotlightDrive.id} />
          </div>
        )}
      </div>

      <div className="panel" style={{ marginBottom: 20 }}>
        <div className="panel-header">
          <h2>Recent High-Value Applications</h2>
          <Link className="panel-header-link" to="/applications">
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
              title={applications.length === 0 ? 'No applications yet' : 'No high-value applications yet'}
              description={
                applications.length === 0
                  ? 'Student applications to your drives will show up here.'
                  : 'Once a candidate is offered, selected, or passes a round, they\'ll show up here.'
              }
            />
          </div>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Applicant</th>
                  <th>Branch</th>
                  <th>Company</th>
                  <th>Role</th>
                  <th>Applied</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {recentApplications.map((application) => (
                  <tr key={application.id}>
                    <td>
                      <Link to={`/applications/${application.id}`} className="link-button">
                        {application.studentName}
                      </Link>
                    </td>
                    <td className="cell-muted">{application.studentBranch || '-'}</td>
                    <td>
                      <span className="company-name-cell">
                        <CompanyLogo name={application.drive.company.name} logoUrl={application.drive.company.logoUrl} size={24} />
                        {application.drive.company.name}
                      </span>
                    </td>
                    <td>{application.drive.role}</td>
                    <td className="cell-muted">{formatDate(application.appliedAt)}</td>
                    <td>
                      {application.offer ? (
                        <StatusBadge status={application.offer.status} />
                      ) : (
                        <StatusBadge status={application.status} />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Recent Companies</h2>
          <Link className="panel-header-link" to="/companies">
            View all
          </Link>
        </div>
        {loading ? (
          <PanelSkeleton />
        ) : recentCompanies.length === 0 ? (
          <div className="panel-body padded">
            <EmptyState
              compact
              icon="building"
              title="No companies yet"
              description="Companies you add will appear here."
            />
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Company</th>
                <th>Contact Email</th>
                <th>Drives</th>
              </tr>
            </thead>
            <tbody>
              {recentCompanies.map((company) => (
                <tr key={company.id}>
                  <td>
                    <span className="company-name-cell">
                      <CompanyLogo name={company.name} logoUrl={company.logoUrl} size={28} />
                      {company.name}
                    </span>
                  </td>
                  <td className="cell-muted">{company.contactEmail || '-'}</td>
                  <td>{driveCountByCompany.get(company.id) || 0}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="panel">
        <div className="panel-header">
          <h2>Top Performing Branches</h2>
          <Link className="panel-header-link" to="/reports">
            View all
          </Link>
        </div>
        {loading ? (
          <PanelSkeleton />
        ) : topBranches.length === 0 ? (
          <div className="panel-body padded">
            <EmptyState
              compact
              icon="award"
              title="No placements yet"
              description="Placement rate by branch will appear here once students start applying."
            />
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Branch</th>
                <th>Applications</th>
                <th>Placed</th>
                <th>Placement Rate</th>
              </tr>
            </thead>
            <tbody>
              {topBranches.map((b) => (
                <tr key={b.branch}>
                  <td className="cell-primary">{b.branch}</td>
                  <td>{b.applications}</td>
                  <td>{b.placed}</td>
                  <td>{b.rate}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
