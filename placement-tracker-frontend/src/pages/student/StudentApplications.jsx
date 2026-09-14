import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyApplications } from '../../api/studentApplicationService';
import { getErrorMessage } from '../../api/apiError';
import PageHeader from '../../components/common/PageHeader';
import EmptyState from '../../components/common/EmptyState';
import TableSkeleton from '../../components/common/TableSkeleton';
import StatusBadge from '../../components/common/StatusBadge';
import Icon from '../../components/common/Icon';
import CompanyLogo from '../../components/common/CompanyLogo';
import { ErrorMessage } from '../../components/common/StateMessage';

const APPLICATION_STATUSES = ['APPLIED', 'IN_PROGRESS', 'SELECTED', 'REJECTED', 'WITHDRAWN'];

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function latestRoundResult(roundResults) {
  if (!roundResults || roundResults.length === 0) return null;
  return [...roundResults].sort((a, b) => b.roundSequence - a.roundSequence)[0];
}

export default function StudentApplications() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getMyApplications()
      .then(setApplications)
      .catch((err) => setError(getErrorMessage(err, 'Could not load your applications.')))
      .finally(() => setLoading(false));
  }

  const filteredApplications = useMemo(() => {
    const term = search.trim().toLowerCase();
    return applications.filter((application) => {
      if (statusFilter && application.status !== statusFilter) return false;
      if (term) {
        const haystack = `${application.drive.role} ${application.drive.company.name}`.toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      return true;
    });
  }, [applications, search, statusFilter]);

  return (
    <div>
      <PageHeader title="My Applications" subtitle="Track the status of every drive you've applied to." />

      <div className="toolbar">
        <div className="search-field">
          <Icon name="search" size={16} />
          <input
            type="text"
            placeholder="Search by role or company..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search applications"
          />
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-my-app-status">Status</label>
          <select id="filter-my-app-status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            {APPLICATION_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading && <TableSkeleton rows={5} columns={6} />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && applications.length === 0 && (
        <EmptyState
          icon="fileText"
          title="No applications yet"
          description="Browse open drives and apply to the ones you're eligible for."
          actionLabel="Browse Drives"
          onAction={() => {
            window.location.href = '/student/drives';
          }}
        />
      )}

      {!loading && !error && applications.length > 0 && filteredApplications.length === 0 && (
        <EmptyState icon="search" title="No applications match these filters" description="Try adjusting your search or filters." compact />
      )}

      {!loading && !error && filteredApplications.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Company</th>
                  <th>Role</th>
                  <th>Applied</th>
                  <th>Status</th>
                  <th>Latest Round</th>
                  <th>Offer</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filteredApplications.map((application) => {
                  const latestRound = latestRoundResult(application.roundResults);
                  return (
                    <tr key={application.id}>
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
                        {latestRound ? (
                          <span>
                            <StatusBadge status={latestRound.status} />
                            <span className="cell-muted" style={{ marginLeft: 6 }}>
                              {latestRound.roundName}
                            </span>
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
                        <Link to={`/student/applications/${application.id}`} className="btn btn-secondary btn-sm">
                          View
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
