import { useEffect, useState } from 'react';
import { getAllApplications } from '../api/applicationService';
import { getErrorMessage } from '../api/apiError';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import StatusBadge from '../components/common/StatusBadge';
import CompanyLogo from '../components/common/CompanyLogo';
import { LoadingMessage, ErrorMessage } from '../components/common/StateMessage';
import { computeBranchStats } from '../utils/branchStats';
import BranchWisePieChart from '../components/reports/BranchWisePieChart';

const APPLICATION_STATUS_ORDER = ['APPLIED', 'IN_PROGRESS', 'SELECTED', 'REJECTED', 'WITHDRAWN'];

// Applications -> offers per company, computed client-side from applications already fetched
// (each carries its own drive.company) - no separate companies call needed.
function computeCompanyStats(applications) {
  const byCompany = new Map();
  applications.forEach((a) => {
    const company = a.drive.company;
    if (!byCompany.has(company.id)) {
      byCompany.set(company.id, { company, applications: 0, offers: 0, accepted: 0 });
    }
    const entry = byCompany.get(company.id);
    entry.applications += 1;
    if (a.offer) entry.offers += 1;
    if (a.offer?.status === 'ACCEPTED') entry.accepted += 1;
  });
  return [...byCompany.values()].sort((a, b) => b.applications - a.applications);
}

export default function Reports() {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getAllApplications()
      .then(setApplications)
      .catch((err) => setError(getErrorMessage(err, 'Could not load report data.')))
      .finally(() => setLoading(false));
  }

  if (loading) return <LoadingMessage text="Loading reports..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;

  const statusCounts = APPLICATION_STATUS_ORDER.map((status) => ({
    status,
    count: applications.filter((a) => a.status === status).length,
  }));
  const maxStatusCount = Math.max(1, ...statusCounts.map((s) => s.count));

  const branchStats = computeBranchStats(applications);
  const companyStats = computeCompanyStats(applications);

  return (
    <div>
      <PageHeader title="Reports & Analytics" subtitle="Placement pipeline and outcomes across every drive." />

      {applications.length === 0 ? (
        <EmptyState icon="fileText" title="Nothing to report yet" description="Reports fill in once students start applying." />
      ) : (
        <>
          <div className="panel" style={{ marginBottom: 20 }}>
            <div className="panel-header">
              <h2>Applications by Status</h2>
            </div>
            <div className="panel-body padded">
              <div className="breakdown-list">
                {statusCounts.map(({ status, count }) => (
                  <div key={status}>
                    <div className="breakdown-row-head">
                      <StatusBadge status={status} />
                      <span className="count">{count}</span>
                    </div>
                    <div className="breakdown-track">
                      <div className="breakdown-fill" style={{ width: `${(count / maxStatusCount) * 100}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div style={{ marginBottom: 20 }}>
            <BranchWisePieChart data={branchStats.map((b) => ({ branch: b.branch, offers: b.placed }))} />
          </div>

          <div className="panel" style={{ marginBottom: 20 }}>
            <div className="panel-header">
              <h2>Placement Rate by Branch</h2>
            </div>
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
                {branchStats.map((b) => (
                  <tr key={b.branch}>
                    <td className="cell-primary">{b.branch}</td>
                    <td>{b.applications}</td>
                    <td>{b.placed}</td>
                    <td>{b.rate}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="panel">
            <div className="panel-header">
              <h2>Company Conversion</h2>
            </div>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Company</th>
                  <th>Applications</th>
                  <th>Offers Extended</th>
                  <th>Offers Accepted</th>
                  <th>Conversion Rate</th>
                </tr>
              </thead>
              <tbody>
                {companyStats.map((c) => (
                  <tr key={c.company.id}>
                    <td>
                      <span className="company-name-cell">
                        <CompanyLogo name={c.company.name} logoUrl={c.company.logoUrl} size={24} />
                        {c.company.name}
                      </span>
                    </td>
                    <td>{c.applications}</td>
                    <td>{c.offers}</td>
                    <td>{c.accepted}</td>
                    <td>{c.applications === 0 ? '0%' : `${Math.round((c.accepted / c.applications) * 100)}%`}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
