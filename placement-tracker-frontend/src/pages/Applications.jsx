import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getAllApplications } from '../api/applicationService';
import { getCompanies } from '../api/companyService';
import { getErrorMessage } from '../api/apiError';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import TableSkeleton from '../components/common/TableSkeleton';
import StatusBadge from '../components/common/StatusBadge';
import Icon from '../components/common/Icon';
import CompanyLogo from '../components/common/CompanyLogo';
import { ErrorMessage } from '../components/common/StateMessage';
import ApplicationDetailsPanel from '../components/applications/ApplicationDetailsPanel';

const APPLICATION_STATUSES = ['APPLIED', 'IN_PROGRESS', 'SELECTED', 'REJECTED', 'WITHDRAWN'];

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

// The round result that best answers "where does this candidate actually stand" - the
// furthest round with a final PASSED/FAILED outcome (score included), not just whichever
// round has the highest sequence number (that could still be an untouched PENDING
// placeholder for a locked round the student hasn't reached yet).
function currentRoundResult(roundResults) {
  if (!roundResults || roundResults.length === 0) return null;
  const bySequence = [...roundResults].sort((a, b) => a.roundSequence - b.roundSequence);
  const decided = [...bySequence].reverse().find((rr) => rr.status === 'PASSED' || rr.status === 'FAILED');
  if (decided) return decided;
  // Nothing decided yet - show the earliest round still awaiting a result.
  return bySequence.find((rr) => !rr.locked) || bySequence[0];
}

export default function Applications() {
  const [searchParams, setSearchParams] = useSearchParams();
  const driveIdFilter = searchParams.get('driveId');

  const [applications, setApplications] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [companyFilter, setCompanyFilter] = useState('');
  const [previewApplication, setPreviewApplication] = useState(null);

  useEffect(() => {
    load();
    getCompanies()
      .then(setCompanies)
      .catch(() => setCompanies([]));
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getAllApplications()
      .then(setApplications)
      .catch((err) => setError(getErrorMessage(err, 'Could not load applications.')))
      .finally(() => setLoading(false));
  }

  const driveFilterInfo = useMemo(() => {
    if (!driveIdFilter) return null;
    const match = applications.find((a) => String(a.drive.id) === driveIdFilter);
    return match ? match.drive : null;
  }, [driveIdFilter, applications]);

  const filteredApplications = useMemo(() => {
    const term = search.trim().toLowerCase();
    return applications.filter((application) => {
      if (driveIdFilter && String(application.drive.id) !== driveIdFilter) return false;
      if (statusFilter && application.status !== statusFilter) return false;
      if (companyFilter && String(application.drive.company.id) !== companyFilter) return false;
      if (term) {
        const haystack = `${application.studentName} ${application.studentEmail} ${application.drive.role} ${application.drive.company.name}`.toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      return true;
    });
  }, [applications, search, statusFilter, companyFilter, driveIdFilter]);

  function clearDriveFilter() {
    searchParams.delete('driveId');
    setSearchParams(searchParams);
  }

  return (
    <div>
      <PageHeader title="Applications" subtitle="Track student applications and placement progress." />

      <div className="toolbar">
        <div className="search-field">
          <Icon name="search" size={16} />
          <input
            type="text"
            placeholder="Search by student, role or company..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search applications"
          />
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-app-status">Status</label>
          <select id="filter-app-status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            {APPLICATION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-app-company">Company</label>
          <select id="filter-app-company" value={companyFilter} onChange={(e) => setCompanyFilter(e.target.value)}>
            <option value="">All</option>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {driveIdFilter && (
        <div className="state-message" style={{ marginBottom: 16 }}>
          <span>
            Filtered to drive:{' '}
            <strong>
              {driveFilterInfo ? `${driveFilterInfo.role} @ ${driveFilterInfo.company.name}` : `#${driveIdFilter}`}
            </strong>
          </span>
          <button type="button" className="btn btn-secondary btn-sm" onClick={clearDriveFilter}>
            Clear
          </button>
        </div>
      )}

      {loading && <TableSkeleton rows={5} columns={8} />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && applications.length === 0 && (
        <EmptyState
          icon="fileText"
          title="No applications yet"
          description="Once students start applying to your drives, they will show up here."
        />
      )}

      {!loading && !error && applications.length > 0 && filteredApplications.length === 0 && (
        <EmptyState
          icon="search"
          title="No applications match these filters"
          description="Try adjusting your search or filters."
          compact
        />
      )}

      {!loading && !error && filteredApplications.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Company</th>
                  <th>Role</th>
                  <th>Applied</th>
                  <th>Status</th>
                  <th>Round Progress</th>
                  <th>Offer</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filteredApplications.map((application) => {
                  const currentRound = currentRoundResult(application.roundResults);
                  return (
                  <tr key={application.id}>
                    <td>
                      {application.studentName}
                      <div className="cell-muted">{application.studentEmail}</div>
                    </td>
                    <td className="cell-primary">
                      <span className="company-name-cell">
                        <CompanyLogo name={application.drive.company.name} logoUrl={application.drive.company.logoUrl} size={24} />
                        {application.drive.company.name}
                      </span>
                    </td>
                    <td>{application.drive.role}</td>
                    <td className="cell-muted">{formatDate(application.appliedAt)}</td>
                    <td>
                      <StatusBadge status={application.status} />
                    </td>
                    <td>
                      {currentRound ? (
                        <span>
                          <StatusBadge status={currentRound.locked ? 'LOCKED' : currentRound.status} />
                          <div className="cell-muted">
                            Round {currentRound.roundSequence} — {currentRound.roundName}
                            {currentRound.score != null && ` (score: ${currentRound.score})`}
                          </div>
                        </span>
                      ) : (
                        <span className="cell-muted">-</span>
                      )}
                    </td>
                    <td>
                      {application.offer ? (
                        <StatusBadge status={application.offer.status} />
                      ) : (
                        <span className="cell-muted">-</span>
                      )}
                    </td>
                    <td className="table-actions">
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => setPreviewApplication(application)}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <ApplicationDetailsPanel
        open={Boolean(previewApplication)}
        application={previewApplication}
        onClose={() => setPreviewApplication(null)}
      />
    </div>
  );
}
