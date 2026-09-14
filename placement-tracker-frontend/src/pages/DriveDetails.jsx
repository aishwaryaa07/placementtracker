import { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { getDrive, updateDrive, updateDriveStatus, deleteDrive } from '../api/driveService';
import { createRound, updateRound, deleteRound, finalizeRound } from '../api/roundService';
import { getErrorMessage, getFieldErrors } from '../api/apiError';
import { useToast } from '../context/useToast';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import StatusBadge from '../components/common/StatusBadge';
import EmptyState from '../components/common/EmptyState';
import Icon from '../components/common/Icon';
import CompanyLogo from '../components/common/CompanyLogo';
import { LoadingMessage, ErrorMessage } from '../components/common/StateMessage';
import DriveForm from '../components/drives/DriveForm';
import { driveToFormValues, formValuesToRequest } from '../components/drives/driveFormUtils';
import RoundForm from '../components/drives/RoundForm';
import DriveFunnel from '../components/drives/DriveFunnel';
import { emptyRoundForm, roundToFormValues } from '../components/drives/roundFormUtils';

const DRIVE_STATUSES = ['UPCOMING', 'ONGOING', 'CLOSED'];

function isPastDate(dateStr) {
  if (!dateStr) return false;
  return new Date(dateStr) < new Date(new Date().toDateString());
}

export default function DriveDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const showToast = useToast();

  const [drive, setDrive] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const [statusUpdating, setStatusUpdating] = useState(false);
  const [statusError, setStatusError] = useState(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const [roundModal, setRoundModal] = useState(null); // { mode: 'create' | 'edit', round? }
  const [roundSubmitting, setRoundSubmitting] = useState(false);
  const [roundFormError, setRoundFormError] = useState(null);
  const [roundFieldErrors, setRoundFieldErrors] = useState({});

  const [roundDeleteTarget, setRoundDeleteTarget] = useState(null);
  const [roundDeleting, setRoundDeleting] = useState(false);
  const [roundDeleteError, setRoundDeleteError] = useState(null);

  const [roundFinalizeTarget, setRoundFinalizeTarget] = useState(null);
  const [roundFinalizing, setRoundFinalizing] = useState(false);
  const [roundFinalizeError, setRoundFinalizeError] = useState(null);

  useEffect(() => {
    loadDrive();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function loadDrive() {
    setLoading(true);
    setError(null);
    getDrive(id)
      .then(setDrive)
      .catch((err) => setError(getErrorMessage(err, 'Could not load drive.')))
      .finally(() => setLoading(false));
  }

  function handleStatusChange(status) {
    if (status === drive.status) return;
    setStatusUpdating(true);
    setStatusError(null);
    updateDriveStatus(drive.id, status)
      .then((saved) => {
        setDrive(saved);
        showToast('Drive status updated.', 'success');
      })
      .catch((err) => setStatusError(getErrorMessage(err, 'Could not update status.')))
      .finally(() => setStatusUpdating(false));
  }

  function handleFormSubmit(values) {
    const request = formValuesToRequest(values);
    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});
    updateDrive(drive.id, request)
      .then((saved) => {
        setDrive(saved);
        setEditing(false);
        showToast('Drive updated successfully.', 'success');
      })
      .catch((err) => {
        setFormError(getErrorMessage(err, 'Could not save drive.'));
        setFieldErrors(getFieldErrors(err));
      })
      .finally(() => setSubmitting(false));
  }

  function confirmDelete() {
    setDeleting(true);
    setDeleteError(null);
    deleteDrive(drive.id)
      .then(() => {
        navigate('/drives', { replace: true });
        showToast('Drive deleted.', 'success');
      })
      .catch((err) => setDeleteError(getErrorMessage(err, 'Could not delete drive.')))
      .finally(() => setDeleting(false));
  }

  function openCreateRound() {
    setRoundFormError(null);
    setRoundFieldErrors({});
    setRoundModal({ mode: 'create' });
  }

  function openEditRound(round) {
    setRoundFormError(null);
    setRoundFieldErrors({});
    setRoundModal({ mode: 'edit', round });
  }

  function handleRoundSubmit(request) {
    setRoundSubmitting(true);
    setRoundFormError(null);
    setRoundFieldErrors({});

    const action =
      roundModal.mode === 'create' ? createRound(drive.id, request) : updateRound(roundModal.round.id, request);

    action
      .then(() => {
        setRoundModal(null);
        showToast(roundModal.mode === 'create' ? 'Round added.' : 'Round updated.', 'success');
        return loadDrive();
      })
      .catch((err) => {
        setRoundFormError(getErrorMessage(err, 'Could not save round.'));
        setRoundFieldErrors(getFieldErrors(err));
      })
      .finally(() => setRoundSubmitting(false));
  }

  function confirmRoundDelete() {
    setRoundDeleting(true);
    setRoundDeleteError(null);
    deleteRound(roundDeleteTarget.id)
      .then(() => {
        setRoundDeleteTarget(null);
        showToast('Round deleted.', 'success');
        return loadDrive();
      })
      .catch((err) => setRoundDeleteError(getErrorMessage(err, 'Could not delete round.')))
      .finally(() => setRoundDeleting(false));
  }

  function confirmRoundFinalize() {
    setRoundFinalizing(true);
    setRoundFinalizeError(null);
    finalizeRound(roundFinalizeTarget.id)
      .then((result) => {
        setRoundFinalizeTarget(null);
        showToast(`Round finalized: ${result.passedCount} passed, ${result.failedCount} failed.`, 'success');
        return loadDrive();
      })
      .catch((err) => setRoundFinalizeError(getErrorMessage(err, 'Could not finalize round.')))
      .finally(() => setRoundFinalizing(false));
  }

  if (loading) return <LoadingMessage text="Loading drive..." />;
  if (error) return <ErrorMessage text={error} onRetry={loadDrive} />;
  if (!drive) return null;

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/drives" className="back-link">
            <Icon name="arrowLeft" size={14} />
            Back to Drives
          </Link>
          <h1 className="company-name-cell">
            <CompanyLogo name={drive.company.name} logoUrl={drive.company.logoUrl} size={32} />
            {drive.role} @ {drive.company.name}
          </h1>
        </div>
        <div className="table-actions">
          <button type="button" className="btn btn-secondary" onClick={() => setEditing(true)}>
            Edit
          </button>
          <button type="button" className="btn btn-danger" onClick={() => setDeleteOpen(true)}>
            Delete
          </button>
        </div>
      </div>

      {statusError && <ErrorMessage text={statusError} />}

      <div className="detail-card">
        <dl className="detail-list">
          <dt>Status</dt>
          <dd>
            <StatusBadge status={drive.status} />
            <select
              className="status-select-inline"
              value={drive.status}
              onChange={(e) => handleStatusChange(e.target.value)}
              disabled={statusUpdating}
            >
              {DRIVE_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </dd>

          <dt>Description</dt>
          <dd>{drive.description || '-'}</dd>

          <dt>CTC</dt>
          <dd>{drive.ctc ?? '-'}</dd>

          <dt>Min CGPA</dt>
          <dd>{drive.minCgpa ?? '-'}</dd>

          <dt>Eligible Branches</dt>
          <dd>{drive.eligibleBranches && drive.eligibleBranches.length > 0 ? drive.eligibleBranches.join(', ') : 'All branches'}</dd>

          <dt>Application Deadline</dt>
          <dd>{drive.applicationDeadline || '-'}</dd>

          <dt>Drive Date</dt>
          <dd>{drive.driveDate || '-'}</dd>
        </dl>
      </div>

      <h2 className="section-heading">Funnel</h2>
      <DriveFunnel driveId={drive.id} />

      <div className="section-heading-row">
        <h2 className="section-heading">Rounds</h2>
        <div className="table-actions">
          <Link to={`/applications?driveId=${drive.id}`} className="panel-header-link">
            View applications for this drive
          </Link>
          <button type="button" className="btn btn-primary btn-sm" onClick={openCreateRound}>
            <Icon name="plus" size={14} />
            Add Round
          </button>
        </div>
      </div>

      {roundDeleteError && <ErrorMessage text={roundDeleteError} />}

      {drive.rounds.length === 0 ? (
        <EmptyState compact icon="fileText" title="No rounds yet" description="Add the first round for this drive." />
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
                  <th>Min Score</th>
                  <th>Selection Mode</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {[...drive.rounds]
                  .sort((a, b) => a.sequence - b.sequence)
                  .map((round) => (
                    <tr key={round.id}>
                      <td>{round.sequence}</td>
                      <td>{round.name}</td>
                      <td>
                        {round.roundDate || '-'}
                        {round.roundDate && (
                          <span className="cell-muted" style={{ marginLeft: 6 }}>
                            ({isPastDate(round.roundDate) ? 'past' : 'upcoming'})
                          </span>
                        )}
                      </td>
                      <td className="wrap cell-muted">{round.description || '-'}</td>
                      <td>{round.minScore ?? '-'}</td>
                      <td>
                        {round.selectionMode === 'THRESHOLD' || !round.selectionMode ? (
                          'Threshold'
                        ) : (
                          <>
                            {round.selectionMode === 'TOP_N' ? 'Top N' : 'Threshold then Top N'} ({round.topN})
                            {round.finalizedAt && <span className="cell-muted" style={{ marginLeft: 6 }}>(finalized)</span>}
                          </>
                        )}
                      </td>
                      <td className="table-actions">
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEditRound(round)}>
                          Edit
                        </button>
                        <Link
                          to={`/rounds/${round.id}/score-import`}
                          state={{ roundName: round.name, driveId: drive.id }}
                          className="btn btn-secondary btn-sm"
                        >
                          Scores
                        </Link>
                        <Link
                          to={`/rounds/${round.id}/panel-assignments`}
                          state={{ roundName: round.name, driveId: drive.id }}
                          className="btn btn-secondary btn-sm"
                        >
                          Panel
                        </Link>
                        {round.selectionMode && round.selectionMode !== 'THRESHOLD' && !round.finalizedAt && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => {
                              setRoundFinalizeError(null);
                              setRoundFinalizeTarget(round);
                            }}
                          >
                            Finalize
                          </button>
                        )}
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => {
                            setRoundDeleteError(null);
                            setRoundDeleteTarget(round);
                          }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {editing && (
        <Modal title="Edit Drive" onClose={() => setEditing(false)} width={560}>
          <DriveForm
            initialValues={driveToFormValues(drive)}
            submitting={submitting}
            formError={formError}
            fieldErrors={fieldErrors}
            onSubmit={handleFormSubmit}
            onCancel={() => setEditing(false)}
          />
        </Modal>
      )}

      {deleteOpen && (
        <ConfirmDialog
          title="Delete Drive"
          message={`Delete the ${drive.role} drive at ${drive.company.name}? This cannot be undone.`}
          confirmLabel="Delete"
          busy={deleting}
          error={deleteError}
          onConfirm={confirmDelete}
          onCancel={() => setDeleteOpen(false)}
        />
      )}

      {roundModal && (
        <Modal title={roundModal.mode === 'create' ? 'Add Round' : 'Edit Round'} onClose={() => setRoundModal(null)}>
          <RoundForm
            initialValues={roundModal.mode === 'edit' ? roundToFormValues(roundModal.round) : emptyRoundForm}
            existingRounds={drive.rounds}
            editingRoundId={roundModal.mode === 'edit' ? roundModal.round.id : null}
            submitting={roundSubmitting}
            formError={roundFormError}
            fieldErrors={roundFieldErrors}
            onSubmit={handleRoundSubmit}
            onCancel={() => setRoundModal(null)}
          />
        </Modal>
      )}

      {roundDeleteTarget && (
        <ConfirmDialog
          title="Delete Round"
          message={`Delete round "${roundDeleteTarget.name}" (sequence ${roundDeleteTarget.sequence})? This cannot be undone.`}
          confirmLabel="Delete"
          busy={roundDeleting}
          error={roundDeleteError}
          onConfirm={confirmRoundDelete}
          onCancel={() => setRoundDeleteTarget(null)}
        />
      )}

      {roundFinalizeTarget && (
        <ConfirmDialog
          title="Finalize Round"
          message={`Rank every scored candidate in "${roundFinalizeTarget.name}" and resolve PASSED/FAILED for the whole round? This cannot be undone or re-run.`}
          confirmLabel="Finalize"
          tone="primary"
          busy={roundFinalizing}
          error={roundFinalizeError}
          onConfirm={confirmRoundFinalize}
          onCancel={() => setRoundFinalizeTarget(null)}
        />
      )}
    </div>
  );
}
