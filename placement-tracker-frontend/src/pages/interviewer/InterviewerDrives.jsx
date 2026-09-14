import { useEffect, useState } from 'react';
import { useAuth } from '../../context/useAuth';
import {
  getMyCompanyDrives,
  createDrive,
  updateDrive,
  submitForApproval,
  deleteDrive,
} from '../../api/interviewerDriveService';
import { getErrorMessage, getFieldErrors } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import EmptyState from '../../components/common/EmptyState';
import Icon from '../../components/common/Icon';
import { LoadingMessage, ErrorMessage } from '../../components/common/StateMessage';
import InterviewerDriveForm from '../../components/interviewer/InterviewerDriveForm';
import {
  emptyInterviewerDriveForm,
  driveToFormValues,
  formValuesToRequest,
} from '../../components/interviewer/interviewerDriveFormUtils';

// Every interviewer at the company can see every drive here (server-side: same-company view
// rule); only the original author can edit/submit/withdraw one - "own" is derived from email,
// since the logged-in user object has no id, only email/name/role (see useAuth()).
export default function InterviewerDrives() {
  const { email } = useAuth();
  const showToast = useToast();

  const [drives, setDrives] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [formModal, setFormModal] = useState(null); // { mode: 'create' | 'edit', drive? }
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const [submitBusyId, setSubmitBusyId] = useState(null);
  const [rowError, setRowError] = useState(null);
  const [rowErrorId, setRowErrorId] = useState(null);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    return getMyCompanyDrives()
      .then(setDrives)
      .catch((err) => setError(getErrorMessage(err, 'Could not load your company\'s drives.')))
      .finally(() => setLoading(false));
  }

  function isOwn(drive) {
    return drive.createdByEmail === email;
  }

  function openCreate() {
    setFormError(null);
    setFieldErrors({});
    setFormModal({ mode: 'create' });
  }

  function openEdit(drive) {
    setFormError(null);
    setFieldErrors({});
    setFormModal({ mode: 'edit', drive });
  }

  function closeFormModal() {
    setFormModal(null);
  }

  function handleFormSubmit(values) {
    const request = formValuesToRequest(values);
    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});

    const isCreate = formModal.mode === 'create';
    const action = isCreate ? createDrive(request) : updateDrive(formModal.drive.id, request);

    action
      .then((saved) => {
        setDrives((prev) => {
          if (isCreate) return [...prev, saved];
          return prev.map((d) => (d.id === saved.id ? saved : d));
        });
        setFormModal(null);
        showToast(isCreate ? 'Drive saved as draft.' : 'Drive updated.', 'success');
      })
      .catch((err) => {
        setFormError(getErrorMessage(err, 'Could not save this drive.'));
        setFieldErrors(getFieldErrors(err));
      })
      .finally(() => setSubmitting(false));
  }

  function handleSubmitForApproval(drive) {
    setSubmitBusyId(drive.id);
    setRowError(null);
    setRowErrorId(null);
    submitForApproval(drive.id)
      .then((saved) => {
        setDrives((prev) => prev.map((d) => (d.id === saved.id ? saved : d)));
        showToast(
          saved.approvalStatus === 'APPROVED' ? 'Submitted and auto-approved.' : 'Submitted for approval.',
          'success'
        );
      })
      .catch((err) => {
        setRowErrorId(drive.id);
        setRowError(getErrorMessage(err, 'Could not submit this drive.'));
      })
      .finally(() => setSubmitBusyId(null));
  }

  function openDeleteConfirm(drive) {
    setDeleteError(null);
    setDeleteTarget(drive);
  }

  function cancelDelete() {
    setDeleteTarget(null);
    setDeleteError(null);
  }

  function confirmDelete() {
    setDeleting(true);
    setDeleteError(null);
    deleteDrive(deleteTarget.id)
      .then(() => {
        setDrives((prev) => prev.filter((d) => d.id !== deleteTarget.id));
        setDeleteTarget(null);
        showToast('Draft deleted.', 'success');
      })
      .catch((err) => setDeleteError(getErrorMessage(err, 'Could not delete this draft.')))
      .finally(() => setDeleting(false));
  }

  if (loading) return <LoadingMessage text="Loading your company's drives..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;

  return (
    <div>
      <div className="page-header">
        <h1>My Company&apos;s Drives</h1>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Icon name="plus" size={16} />
          New Drive
        </button>
      </div>

      {drives.length === 0 ? (
        <EmptyState
          icon="briefcase"
          title="No drives yet"
          description="Draft a job posting for your company - it'll need Admin approval before students can see it."
          actionLabel="New Drive"
          onAction={openCreate}
        />
      ) : (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>CTC</th>
                  <th>Posted By</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {drives.map((drive) => {
                  const own = isOwn(drive);
                  const canSubmit = own && (drive.approvalStatus === 'DRAFT' || drive.approvalStatus === 'REJECTED');
                  const canEdit = own;
                  const canDelete = own && drive.approvalStatus === 'DRAFT';
                  return (
                    <tr key={drive.id}>
                      <td className="cell-primary">{drive.role}</td>
                      <td>{drive.ctc ?? '-'}</td>
                      <td className="cell-muted">{drive.createdByName || '-'}{own ? ' (you)' : ''}</td>
                      <td>
                        <StatusBadge status={drive.approvalStatus} />
                        {drive.approvalStatus === 'REJECTED' && drive.rejectionReason && (
                          <div className="cell-muted wrap">Reason: {drive.rejectionReason}</div>
                        )}
                        {rowErrorId === drive.id && <div className="field-error">{rowError}</div>}
                      </td>
                      <td className="table-actions">
                        {canSubmit && (
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            disabled={submitBusyId === drive.id}
                            onClick={() => handleSubmitForApproval(drive)}
                          >
                            {submitBusyId === drive.id ? 'Submitting...' : 'Submit for Approval'}
                          </button>
                        )}
                        {canEdit && (
                          <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(drive)}>
                            Edit
                          </button>
                        )}
                        {canDelete && (
                          <button type="button" className="btn btn-danger btn-sm" onClick={() => openDeleteConfirm(drive)}>
                            Delete
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {formModal && (
        <Modal title={formModal.mode === 'create' ? 'New Drive' : 'Edit Drive'} onClose={closeFormModal} width={560}>
          <InterviewerDriveForm
            initialValues={formModal.mode === 'edit' ? driveToFormValues(formModal.drive) : emptyInterviewerDriveForm}
            submitting={submitting}
            formError={formError}
            fieldErrors={fieldErrors}
            onSubmit={handleFormSubmit}
            onCancel={closeFormModal}
          />
        </Modal>
      )}

      {deleteTarget && (
        <ConfirmDialog
          title="Delete Draft"
          message={`Delete the "${deleteTarget.role}" draft? This cannot be undone.`}
          confirmLabel="Delete"
          busy={deleting}
          error={deleteError}
          onConfirm={confirmDelete}
          onCancel={cancelDelete}
        />
      )}
    </div>
  );
}
