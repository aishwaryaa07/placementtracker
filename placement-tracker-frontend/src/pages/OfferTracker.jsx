import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAllApplications } from '../api/applicationService';
import { getErrorMessage } from '../api/apiError';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import TableSkeleton from '../components/common/TableSkeleton';
import StatusBadge from '../components/common/StatusBadge';
import CompanyLogo from '../components/common/CompanyLogo';
import Icon from '../components/common/Icon';
import { ErrorMessage } from '../components/common/StateMessage';

const OFFER_STATUSES = ['PENDING', 'ACCEPTED', 'DECLINED'];

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export default function OfferTracker() {
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
    getAllApplications()
      .then(setApplications)
      .catch((err) => setError(getErrorMessage(err, 'Could not load offers.')))
      .finally(() => setLoading(false));
  }

  const offeredApplications = useMemo(() => applications.filter((a) => a.offer), [applications]);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return offeredApplications
      .filter((a) => {
        if (statusFilter && a.offer.status !== statusFilter) return false;
        if (term && !`${a.studentName} ${a.drive.company.name} ${a.drive.role}`.toLowerCase().includes(term)) return false;
        return true;
      })
      .sort((a, b) => new Date(b.offer.offerDate || b.appliedAt) - new Date(a.offer.offerDate || a.appliedAt));
  }, [offeredApplications, search, statusFilter]);

  return (
    <div>
      <PageHeader title="Offer Tracker" subtitle="Every offer extended across all drives, in one place." />

      {!loading && !error && offeredApplications.length > 0 && (
        <div className="toolbar">
          <div className="search-field">
            <Icon name="search" size={16} />
            <input
              type="text"
              placeholder="Search by student, company or role..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search offers"
            />
          </div>
          <div className="toolbar-filter">
            <label htmlFor="filter-offer-status">Status</label>
            <select id="filter-offer-status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">All</option>
              {OFFER_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {loading && <TableSkeleton rows={5} columns={6} />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && offeredApplications.length === 0 && (
        <EmptyState icon="award" title="No offers yet" description="Offers extended to candidates will show up here." />
      )}

      {!loading && !error && offeredApplications.length > 0 && filtered.length === 0 && (
        <EmptyState icon="search" title="No matching offers" description="Try adjusting your search or filter." compact />
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Company</th>
                  <th>Role</th>
                  <th>CTC Offered</th>
                  <th>Offer Date</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((a) => (
                  <tr key={a.id}>
                    <td>
                      {a.studentName}
                      <div className="cell-muted">{a.studentEmail}</div>
                    </td>
                    <td>
                      <span className="company-name-cell">
                        <CompanyLogo name={a.drive.company.name} logoUrl={a.drive.company.logoUrl} size={24} />
                        {a.drive.company.name}
                      </span>
                    </td>
                    <td>{a.drive.role}</td>
                    <td>{a.offer.ctcOffered ?? '-'}</td>
                    <td className="cell-muted">{formatDate(a.offer.offerDate)}</td>
                    <td>
                      <StatusBadge status={a.offer.status} />
                    </td>
                    <td className="table-actions">
                      <Link to={`/applications/${a.id}`} className="btn btn-secondary btn-sm">
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
