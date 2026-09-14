import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getMyApplications } from '../../api/studentApplicationService';
import { respondToOffer } from '../../api/studentOfferService';
import { getErrorMessage } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import PageHeader from '../../components/common/PageHeader';
import EmptyState from '../../components/common/EmptyState';
import TableSkeleton from '../../components/common/TableSkeleton';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import CompanyLogo from '../../components/common/CompanyLogo';
import { ErrorMessage } from '../../components/common/StateMessage';

// The backend has no "list my offers" endpoint (StudentOfferController only exposes
// PATCH .../status) - an offer only ever exists embedded in ApplicationResponse.offer,
// so this page derives its list from GET /api/student/applications.
export default function StudentOffers() {
  const showToast = useToast();

  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [respondTarget, setRespondTarget] = useState(null); // { application, action }
  const [responding, setResponding] = useState(false);
  const [respondError, setRespondError] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getMyApplications()
      .then(setApplications)
      .catch((err) => setError(getErrorMessage(err, 'Could not load your offers.')))
      .finally(() => setLoading(false));
  }

  const offeredApplications = applications
    .filter((a) => a.offer)
    .sort((a, b) => (b.offer.offerDate || '').localeCompare(a.offer.offerDate || ''));

  function openRespond(application, action) {
    setRespondError(null);
    setRespondTarget({ application, action });
  }

  function confirmRespond() {
    const { application, action } = respondTarget;
    setResponding(true);
    setRespondError(null);
    respondToOffer(application.offer.id, action)
      .then(() => {
        showToast(action === 'ACCEPTED' ? 'Offer accepted!' : 'Offer declined.', 'success');
        setRespondTarget(null);
        return load();
      })
      .catch((err) => setRespondError(getErrorMessage(err, 'Could not update the offer.')))
      .finally(() => setResponding(false));
  }

  return (
    <div>
      <PageHeader title="Offers" subtitle="Review and respond to offers you've received." />

      {loading && <TableSkeleton rows={4} columns={6} />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && offeredApplications.length === 0 && (
        <EmptyState
          icon="star"
          title="No offers yet"
          description="Offers from companies you're selected by will appear here."
        />
      )}

      {!loading && !error && offeredApplications.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Company</th>
                  <th>Role</th>
                  <th>CTC Offered</th>
                  <th>Offer Date</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {offeredApplications.map((application) => (
                  <tr key={application.id}>
                    <td className="cell-primary">
                      <span className="company-name-cell">
                        <CompanyLogo name={application.drive.company.name} logoUrl={application.drive.company.logoUrl} size={24} />
                        {application.drive.company.name}
                      </span>
                    </td>
                    <td>{application.drive.role}</td>
                    <td>{application.offer.ctcOffered}</td>
                    <td className="cell-muted">{application.offer.offerDate || '-'}</td>
                    <td>
                      <StatusBadge status={application.offer.status} />
                    </td>
                    <td className="table-actions">
                      {application.offer.status === 'PENDING' ? (
                        <>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => openRespond(application, 'ACCEPTED')}
                          >
                            Accept
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => openRespond(application, 'DECLINED')}
                          >
                            Decline
                          </button>
                        </>
                      ) : (
                        <Link to={`/student/applications/${application.id}`} className="btn btn-secondary btn-sm">
                          View
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {respondTarget && (
        <ConfirmDialog
          title={respondTarget.action === 'ACCEPTED' ? 'Accept this offer?' : 'Decline this offer?'}
          message={
            respondTarget.action === 'ACCEPTED'
              ? `Accept the offer of ${respondTarget.application.offer.ctcOffered} from ${respondTarget.application.drive.company.name}?`
              : `Decline the offer from ${respondTarget.application.drive.company.name}? This cannot be undone.`
          }
          confirmLabel={respondTarget.action === 'ACCEPTED' ? 'Accept Offer' : 'Decline Offer'}
          tone={respondTarget.action === 'ACCEPTED' ? 'primary' : 'danger'}
          busy={responding}
          error={respondError}
          onConfirm={confirmRespond}
          onCancel={() => setRespondTarget(null)}
        />
      )}
    </div>
  );
}
