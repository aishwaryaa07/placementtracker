import { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { getOpenDrive, applyToDrive } from '../../api/studentDriveService';
import { getMyApplications } from '../../api/studentApplicationService';
import { getErrorMessage } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import EmptyState from '../../components/common/EmptyState';
import Icon from '../../components/common/Icon';
import CompanyLogo from '../../components/common/CompanyLogo';
import { LoadingMessage, ErrorMessage } from '../../components/common/StateMessage';

function isPast(dateStr) {
  if (!dateStr) return false;
  return new Date(dateStr) < new Date(new Date().toDateString());
}

export default function StudentDriveDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const showToast = useToast();

  const [entry, setEntry] = useState(null); // { drive, eligible }
  const [alreadyApplied, setAlreadyApplied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState(null);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([getOpenDrive(id), getMyApplications()])
      .then(([driveEntry, applications]) => {
        setEntry(driveEntry);
        setAlreadyApplied(applications.some((a) => a.drive.id === driveEntry.drive.id));
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load this drive.')))
      .finally(() => setLoading(false));
  }

  function confirmApply() {
    setApplying(true);
    setApplyError(null);
    applyToDrive(entry.drive.id)
      .then((application) => {
        showToast('Application submitted!', 'success');
        navigate(`/student/applications/${application.id}`);
      })
      .catch((err) => setApplyError(getErrorMessage(err, 'Could not submit your application.')))
      .finally(() => setApplying(false));
  }

  if (loading) return <LoadingMessage text="Loading drive..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;
  if (!entry) return null;

  const { drive, eligible, ineligibilityReasons } = entry;
  const deadlinePassed = isPast(drive.applicationDeadline);
  const canApply = eligible && !alreadyApplied && !deadlinePassed;

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/student/drives" className="back-link">
            <Icon name="arrowLeft" size={14} />
            Back to Drives
          </Link>
          <h1 className="company-name-cell">
            <CompanyLogo name={drive.company.name} logoUrl={drive.company.logoUrl} size={32} />
            {drive.role} @ {drive.company.name}
          </h1>
        </div>
        <div className="table-actions">
          {alreadyApplied ? (
            <span className="eligibility-badge eligible">Already Applied</span>
          ) : deadlinePassed ? (
            <span className="eligibility-badge not-eligible">Deadline Passed</span>
          ) : (
            <button type="button" className="btn btn-primary" disabled={!canApply} onClick={() => setConfirmOpen(true)}>
              Apply Now
            </button>
          )}
        </div>
      </div>

      <div className="drive-card-badges" style={{ marginBottom: eligible ? 16 : 10 }}>
        <StatusBadge status={drive.status} />
        <span className={`eligibility-badge ${eligible ? 'eligible' : 'not-eligible'}`}>
          {eligible ? 'You are Eligible' : 'You are Not Eligible'}
        </span>
      </div>

      {!eligible && ineligibilityReasons && ineligibilityReasons.length > 0 && (
        <div className="eligibility-reasons">
          <div className="eligibility-reasons-title">
            <Icon name="alertCircle" size={15} />
            Why am I not eligible?
          </div>
          <ul className="eligibility-reasons-list">
            {ineligibilityReasons.map((reason) => (
              <li key={reason.type}>{reason.message}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="detail-card">
        <dl className="detail-list">
          <dt>Description</dt>
          <dd>{drive.description || '-'}</dd>

          <dt>CTC</dt>
          <dd>{drive.ctc ?? '-'}</dd>

          <dt>Min CGPA</dt>
          <dd>{drive.minCgpa ?? 'No minimum'}</dd>

          <dt>Eligible Branches</dt>
          <dd>{drive.eligibleBranches && drive.eligibleBranches.length > 0 ? drive.eligibleBranches.join(', ') : 'All branches'}</dd>

          <dt>Application Deadline</dt>
          <dd>{drive.applicationDeadline || '-'}</dd>

          <dt>Drive Date</dt>
          <dd>{drive.driveDate || '-'}</dd>
        </dl>
      </div>

      <h2 className="section-heading">Rounds</h2>
      {drive.rounds.length === 0 ? (
        <EmptyState compact icon="fileText" title="No rounds published yet" description="Interview rounds for this drive will appear here once added." />
      ) : (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sequence</th>
                  <th>Name</th>
                  <th>Round Date</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                {drive.rounds.map((round) => (
                  <tr key={round.id}>
                    <td>{round.sequence}</td>
                    <td>{round.name}</td>
                    <td>{round.roundDate || '-'}</td>
                    <td className="wrap cell-muted">{round.description || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {confirmOpen && (
        <ConfirmDialog
          title="Apply to this drive?"
          message={`Apply for ${drive.role} at ${drive.company.name}?`}
          confirmLabel="Apply"
          tone="primary"
          busy={applying}
          error={applyError}
          onConfirm={confirmApply}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </div>
  );
}
