import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getApplication } from '../api/applicationService';
import { updateRoundResult } from '../api/roundResultService';
import { createRound } from '../api/roundService';
import { createOffer, updateOfferStatus } from '../api/offerService';
import { getErrorMessage, getFieldErrors } from '../api/apiError';
import { useToast } from '../context/useToast';
import Modal from '../components/common/Modal';
import StatusBadge from '../components/common/StatusBadge';
import EmptyState from '../components/common/EmptyState';
import Icon from '../components/common/Icon';
import CompanyLogo from '../components/common/CompanyLogo';
import { LoadingMessage, ErrorMessage } from '../components/common/StateMessage';
import RoundForm from '../components/drives/RoundForm';
import { emptyRoundForm } from '../components/drives/roundFormUtils';

const ROUND_RESULT_STATUSES = ['PENDING', 'SCORED', 'PASSED', 'FAILED'];
const OFFER_STATUSES = ['PENDING', 'ACCEPTED', 'DECLINED'];

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function RoundResultRow({ roundResult, onSave, busy }) {
  const [remarks, setRemarks] = useState(roundResult.remarks || '');
  const dirty = remarks !== (roundResult.remarks || '');

  if (roundResult.locked) {
    return (
      <tr>
        <td>{roundResult.roundSequence}</td>
        <td>{roundResult.roundName}</td>
        <td>
          <StatusBadge status="LOCKED" />
        </td>
        <td>{roundResult.score ?? '-'}</td>
        <td className="wrap cell-muted">{roundResult.lockReason || 'Complete previous rounds first.'}</td>
      </tr>
    );
  }

  return (
    <tr>
      <td>{roundResult.roundSequence}</td>
      <td>{roundResult.roundName}</td>
      <td>
        <StatusBadge status={roundResult.status} />
        <select
          className="status-select-inline"
          value={roundResult.status}
          onChange={(e) => onSave(roundResult, e.target.value, remarks)}
          disabled={busy}
        >
          {ROUND_RESULT_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </td>
      <td>{roundResult.score ?? '-'}</td>
      <td className="wrap">
        <div className="table-actions">
          <input
            type="text"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            placeholder="Add remarks..."
            disabled={busy}
            style={{ minWidth: 180 }}
          />
          {dirty && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              disabled={busy}
              onClick={() => onSave(roundResult, roundResult.status, remarks)}
            >
              Save
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}

function OfferForm({ submitting, formError, fieldErrors, onSubmit, onCancel }) {
  const [ctcOffered, setCtcOffered] = useState('');
  const [offerDate, setOfferDate] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit({
      ctcOffered: ctcOffered === '' ? null : Number(ctcOffered),
      offerDate: offerDate || null,
    });
  }

  return (
    <form onSubmit={handleSubmit}>
      {formError && <div className="form-error">{formError}</div>}

      <label htmlFor="offer-ctc">
        CTC Offered<span className="required-mark">*</span>
      </label>
      <input
        id="offer-ctc"
        type="number"
        step="0.01"
        min="0"
        value={ctcOffered}
        onChange={(e) => setCtcOffered(e.target.value)}
        required
      />
      {fieldErrors.ctcOffered && <div className="field-error">{fieldErrors.ctcOffered}</div>}

      <label htmlFor="offer-date">Offer Date</label>
      <input id="offer-date" type="date" value={offerDate} onChange={(e) => setOfferDate(e.target.value)} />
      {fieldErrors.offerDate && <div className="field-error">{fieldErrors.offerDate}</div>}

      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Create Offer'}
        </button>
      </div>
    </form>
  );
}

export default function ApplicationDetails() {
  const { id } = useParams();
  const showToast = useToast();

  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [roundBusyId, setRoundBusyId] = useState(null);
  const [roundError, setRoundError] = useState(null);

  const [roundAddOpen, setRoundAddOpen] = useState(false);
  const [roundAddSubmitting, setRoundAddSubmitting] = useState(false);
  const [roundAddFormError, setRoundAddFormError] = useState(null);
  const [roundAddFieldErrors, setRoundAddFieldErrors] = useState({});

  const [offerModalOpen, setOfferModalOpen] = useState(false);
  const [offerSubmitting, setOfferSubmitting] = useState(false);
  const [offerFormError, setOfferFormError] = useState(null);
  const [offerFieldErrors, setOfferFieldErrors] = useState({});

  const [offerStatusUpdating, setOfferStatusUpdating] = useState(false);
  const [offerStatusError, setOfferStatusError] = useState(null);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function load() {
    setLoading(true);
    setError(null);
    return getApplication(id)
      .then(setApplication)
      .catch((err) => setError(getErrorMessage(err, 'Could not load application.')))
      .finally(() => setLoading(false));
  }

  function handleRoundSave(roundResult, status, remarks) {
    setRoundBusyId(roundResult.id);
    setRoundError(null);
    updateRoundResult(roundResult.id, { status, remarks: remarks || null })
      .then(() => {
        showToast('Round result updated.', 'success');
        return load();
      })
      .catch((err) => setRoundError(getErrorMessage(err, 'Could not update round result.')))
      .finally(() => setRoundBusyId(null));
  }

  function openAddRound() {
    setRoundAddFormError(null);
    setRoundAddFieldErrors({});
    setRoundAddOpen(true);
  }

  function handleAddRoundSubmit(request) {
    setRoundAddSubmitting(true);
    setRoundAddFormError(null);
    setRoundAddFieldErrors({});
    createRound(application.drive.id, request)
      .then(() => {
        setRoundAddOpen(false);
        showToast('Round added.', 'success');
        return load();
      })
      .catch((err) => {
        setRoundAddFormError(getErrorMessage(err, 'Could not add round.'));
        setRoundAddFieldErrors(getFieldErrors(err));
      })
      .finally(() => setRoundAddSubmitting(false));
  }

  function openOfferModal() {
    setOfferFormError(null);
    setOfferFieldErrors({});
    setOfferModalOpen(true);
  }

  function handleOfferSubmit(values) {
    setOfferSubmitting(true);
    setOfferFormError(null);
    setOfferFieldErrors({});
    createOffer(application.id, values)
      .then(() => {
        setOfferModalOpen(false);
        showToast('Offer created.', 'success');
        return load();
      })
      .catch((err) => {
        setOfferFormError(getErrorMessage(err, 'Could not create offer.'));
        setOfferFieldErrors(getFieldErrors(err));
      })
      .finally(() => setOfferSubmitting(false));
  }

  function handleOfferStatusChange(status) {
    if (status === application.offer.status) return;
    setOfferStatusUpdating(true);
    setOfferStatusError(null);
    updateOfferStatus(application.offer.id, status)
      .then(() => {
        showToast('Offer status updated.', 'success');
        return load();
      })
      .catch((err) => setOfferStatusError(getErrorMessage(err, 'Could not update offer status.')))
      .finally(() => setOfferStatusUpdating(false));
  }

  if (loading) return <LoadingMessage text="Loading application..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;
  if (!application) return null;

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/applications" className="back-link">
            <Icon name="arrowLeft" size={14} />
            Back to Applications
          </Link>
          <h1 className="company-name-cell">
            <CompanyLogo name={application.drive.company.name} logoUrl={application.drive.company.logoUrl} size={32} />
            {application.studentName} — {application.drive.role} @ {application.drive.company.name}
          </h1>
        </div>
      </div>

      <div className="detail-card">
        <dl className="detail-list">
          <dt>Student</dt>
          <dd>
            {application.studentName}
            <div className="cell-muted">{application.studentEmail}</div>
          </dd>

          <dt>Status</dt>
          <dd>
            <StatusBadge status={application.status} />
          </dd>

          <dt>Applied On</dt>
          <dd>{formatDate(application.appliedAt)}</dd>

          <dt>Company</dt>
          <dd>{application.drive.company.name}</dd>

          <dt>Role</dt>
          <dd>{application.drive.role}</dd>

          <dt>CTC</dt>
          <dd>{application.drive.ctc ?? '-'}</dd>

          <dt>Drive Date</dt>
          <dd>{application.drive.driveDate || '-'}</dd>
        </dl>
      </div>

      <div className="section-heading-row">
        <h2 className="section-heading">Round Results</h2>
        <button type="button" className="btn btn-primary btn-sm" onClick={openAddRound}>
          <Icon name="plus" size={14} />
          Add Round
        </button>
      </div>

      {roundError && <ErrorMessage text={roundError} />}

      {application.roundResults.length === 0 ? (
        <EmptyState
          compact
          icon="fileText"
          title="No round results yet"
          description="Round results appear once rounds are added to this drive."
        />
      ) : (
        <div className="table-card" style={{ marginBottom: 24 }}>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Sequence</th>
                  <th>Round</th>
                  <th>Status</th>
                  <th>Score</th>
                  <th>Remarks</th>
                </tr>
              </thead>
              <tbody>
                {[...application.roundResults].sort((a, b) => a.roundSequence - b.roundSequence).map((roundResult) => (
                  <RoundResultRow
                    key={roundResult.id}
                    roundResult={roundResult}
                    busy={roundBusyId === roundResult.id}
                    onSave={handleRoundSave}
                  />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="section-heading-row">
        <h2 className="section-heading">Offer</h2>
        {!application.offer && (
          <button type="button" className="btn btn-primary btn-sm" onClick={openOfferModal}>
            <Icon name="plus" size={14} />
            Add Offer
          </button>
        )}
      </div>

      {offerStatusError && <ErrorMessage text={offerStatusError} />}

      {application.offer ? (
        <div className="summary-card">
          <div>
            <div className="summary-card-label">CTC Offered</div>
            <div className="summary-card-value">{application.offer.ctcOffered}</div>
          </div>
          <div>
            <div className="summary-card-label">Offer Date</div>
            <div className="summary-card-value">{application.offer.offerDate || '-'}</div>
          </div>
          <div>
            <div className="summary-card-label">Status</div>
            <div>
              <StatusBadge status={application.offer.status} />
              <select
                className="status-select-inline"
                value={application.offer.status}
                onChange={(e) => handleOfferStatusChange(e.target.value)}
                disabled={offerStatusUpdating}
              >
                {OFFER_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      ) : (
        <EmptyState compact icon="award" title="No offer yet" description="Create an offer once this candidate is selected." />
      )}

      {offerModalOpen && (
        <Modal title="Add Offer" onClose={() => setOfferModalOpen(false)}>
          <OfferForm
            submitting={offerSubmitting}
            formError={offerFormError}
            fieldErrors={offerFieldErrors}
            onSubmit={handleOfferSubmit}
            onCancel={() => setOfferModalOpen(false)}
          />
        </Modal>
      )}

      {roundAddOpen && (
        <Modal title="Add Round" onClose={() => setRoundAddOpen(false)}>
          <RoundForm
            initialValues={emptyRoundForm}
            existingRounds={application.drive.rounds}
            editingRoundId={null}
            submitting={roundAddSubmitting}
            formError={roundAddFormError}
            fieldErrors={roundAddFieldErrors}
            onSubmit={handleAddRoundSubmit}
            onCancel={() => setRoundAddOpen(false)}
          />
        </Modal>
      )}
    </div>
  );
}
