import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getDrives,
  createDrive,
  updateDrive,
  updateDriveApproval,
  deleteDrive,
} from '../api/driveService';
import { getCompanies } from '../api/companyService';
import { getAllApplications } from '../api/applicationService';
import { getErrorMessage, getFieldErrors } from '../api/apiError';
import { useToast } from '../context/useToast';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import TableSkeleton from '../components/common/TableSkeleton';
import Icon from '../components/common/Icon';
import { ErrorMessage } from '../components/common/StateMessage';
import DriveForm from '../components/drives/DriveForm';
import DriveCard from '../components/drives/DriveCard';
import { emptyDriveForm, driveToFormValues, formValuesToRequest } from '../components/drives/driveFormUtils';

const DRIVE_STATUSES = ['UPCOMING', 'ONGOING', 'CLOSED'];

function truncate(text, max) {
  if (!text) return '';
  return text.length > max ? `${text.slice(0, max).trim()}…` : text;
}

export default function Drives() {
  const showToast = useToast();
  const navigate = useNavigate();

  const [drives, setDrives] = useState([]);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [companyFilter, setCompanyFilter] = useState('');
  const [filterCompanies, setFilterCompanies] = useState([]);

  const [formModal, setFormModal] = useState(null); // { mode: 'create' | 'edit', drive? }
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const [approveBusyId, setApproveBusyId] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectSubmitting, setRejectSubmitting] = useState(false);
  const [rejectError, setRejectError] = useState(null);

  useEffect(() => {
    getCompanies()
      .then(setFilterCompanies)
      .catch(() => setFilterCompanies([]));
    // Stat chips on each card (applications/offers) come from the same aggregated-applications
    // call already used by the Applications page/Dashboard - one fetch, no new backend calls.
    getAllApplications()
      .then(setApplications)
      .catch(() => setApplications([]));
  }, []);

  useEffect(() => {
    loadDrives();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, companyFilter]);

  function loadDrives() {
    setLoading(true);
    setError(null);
    getDrives({ status: statusFilter || undefined, companyId: companyFilter || undefined })
      .then(setDrives)
      .catch((err) => setError(getErrorMessage(err, 'Could not load drives.')))
      .finally(() => setLoading(false));
  }

  const filteredDrives = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return drives;
    return drives.filter(
      (d) => d.role.toLowerCase().includes(term) || d.company.name.toLowerCase().includes(term)
    );
  }, [drives, search]);

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
        showToast(isCreate ? 'Drive created successfully.' : 'Drive updated successfully.', 'success');
      })
      .catch((err) => {
        setFormError(getErrorMessage(err, 'Could not save drive.'));
        setFieldErrors(getFieldErrors(err));
      })
      .finally(() => setSubmitting(false));
  }

  // One entry per drive: real applications/offers/acceptances counts + round count, feeding
  // each card's 2x2 stat grid. Status changes still happen on the drive detail page.
  const statsByDrive = useMemo(() => {
    const map = new Map();
    for (const drive of drives) {
      map.set(drive.id, { applications: 0, offers: 0, accepted: 0, rounds: drive.rounds?.length || 0 });
    }
    for (const application of applications) {
      const entry = map.get(application.drive.id);
      if (!entry) continue;
      entry.applications += 1;
      if (application.offer) {
        entry.offers += 1;
        if (application.offer.status === 'ACCEPTED') entry.accepted += 1;
      }
    }
    return map;
  }, [drives, applications]);

  function handleApprove(drive) {
    setApproveBusyId(drive.id);
    updateDriveApproval(drive.id, 'APPROVED')
      .then((saved) => {
        setDrives((prev) => prev.map((d) => (d.id === saved.id ? saved : d)));
        showToast('Drive approved.', 'success');
      })
      .catch((err) => showToast(getErrorMessage(err, 'Could not approve this drive.'), 'error'))
      .finally(() => setApproveBusyId(null));
  }

  function openReject(drive) {
    setRejectReason('');
    setRejectError(null);
    setRejectTarget(drive);
  }

  function closeReject() {
    setRejectTarget(null);
  }

  function confirmReject() {
    setRejectSubmitting(true);
    setRejectError(null);
    updateDriveApproval(rejectTarget.id, 'REJECTED', rejectReason.trim() || null)
      .then((saved) => {
        setDrives((prev) => prev.map((d) => (d.id === saved.id ? saved : d)));
        setRejectTarget(null);
        showToast('Drive rejected.', 'success');
      })
      .catch((err) => setRejectError(getErrorMessage(err, 'Could not reject this drive.')))
      .finally(() => setRejectSubmitting(false));
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
        showToast('Drive deleted.', 'success');
      })
      .catch((err) => setDeleteError(getErrorMessage(err, 'Could not delete drive.')))
      .finally(() => setDeleting(false));
  }

  const hasAnyFilter = search || statusFilter || companyFilter;

  return (
    <div>
      <PageHeader title="Placement Drives" subtitle="Manage upcoming and ongoing placement drives.">
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Icon name="plus" size={16} />
          Add Drive
        </button>
      </PageHeader>

      <div className="toolbar">
        <div className="search-field">
          <Icon name="search" size={16} />
          <input
            type="text"
            placeholder="Search by role or company..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search drives"
          />
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-status">Status</label>
          <select id="filter-status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            {DRIVE_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-company">Company</label>
          <select id="filter-company" value={companyFilter} onChange={(e) => setCompanyFilter(e.target.value)}>
            <option value="">All</option>
            {filterCompanies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading && <TableSkeleton rows={4} columns={8} />}
      {!loading && error && <ErrorMessage text={error} onRetry={loadDrives} />}

      {!loading && !error && drives.length === 0 && !hasAnyFilter && (
        <EmptyState
          icon="briefcase"
          title="No drives yet"
          description="Add your first placement drive to start tracking applications."
          actionLabel="Add Drive"
          onAction={openCreate}
        />
      )}

      {!loading && !error && drives.length > 0 && filteredDrives.length === 0 && (
        <EmptyState icon="search" title="No drives match these filters" description="Try adjusting your search or filters." compact />
      )}

      {!loading && !error && filteredDrives.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {filteredDrives.map((drive) => {
            const stats = statsByDrive.get(drive.id) || { applications: 0, offers: 0, accepted: 0, rounds: 0 };
            return (
              <DriveCard
                key={drive.id}
                title={`${drive.role} @ ${drive.company.name}`}
                tagline={truncate(drive.description, 120)}
                status={drive.status}
                approvalStatus={drive.approvalStatus}
                createdByName={drive.createdByName}
                rejectionReason={drive.rejectionReason}
                onApprove={
                  drive.approvalStatus === 'PENDING_APPROVAL' && approveBusyId !== drive.id
                    ? () => handleApprove(drive)
                    : undefined
                }
                onReject={drive.approvalStatus === 'PENDING_APPROVAL' ? () => openReject(drive) : undefined}
                stats={[
                  { label: 'Applications', value: stats.applications, color: 'blue' },
                  { label: 'Rounds', value: stats.rounds, color: 'amber' },
                  { label: 'Offers Extended', value: stats.offers, color: 'purple' },
                  { label: 'Offers Accepted', value: stats.accepted, color: 'green' },
                ]}
                onView={() => navigate(`/drives/${drive.id}`)}
                onEdit={() => openEdit(drive)}
                onDelete={() => openDeleteConfirm(drive)}
              />
            );
          })}
        </div>
      )}

      {formModal && (
        <Modal title={formModal.mode === 'create' ? 'Add Drive' : 'Edit Drive'} onClose={closeFormModal} width={560}>
          <DriveForm
            initialValues={formModal.mode === 'edit' ? driveToFormValues(formModal.drive) : emptyDriveForm}
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
          title="Delete Drive"
          message={`Delete the ${deleteTarget.role} drive at ${deleteTarget.company.name}? This cannot be undone.`}
          confirmLabel="Delete"
          busy={deleting}
          error={deleteError}
          onConfirm={confirmDelete}
          onCancel={cancelDelete}
        />
      )}

      {rejectTarget && (
        <Modal title="Reject Drive" onClose={closeReject}>
          {rejectError && <div className="form-error">{rejectError}</div>}
          <p>
            Reject the &quot;{rejectTarget.role}&quot; drive at {rejectTarget.company.name}? The interviewer will see
            this reason and can revise and resubmit.
          </p>
          <label htmlFor="reject-reason">Reason (optional)</label>
          <textarea
            id="reject-reason"
            rows={3}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            placeholder="e.g. CTC seems too low for this role, please double-check."
          />
          <div className="modal-actions">
            <button type="button" className="btn btn-secondary" onClick={closeReject} disabled={rejectSubmitting}>
              Cancel
            </button>
            <button type="button" className="btn btn-danger" onClick={confirmReject} disabled={rejectSubmitting}>
              {rejectSubmitting ? 'Rejecting...' : 'Reject Drive'}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
