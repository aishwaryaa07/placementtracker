import { useEffect, useMemo, useState } from 'react';
import {
  getCompanies,
  getCompany,
  createCompany,
  updateCompany,
  deleteCompany,
} from '../api/companyService';
import { getDrives } from '../api/driveService';
import { getErrorMessage, getFieldErrors } from '../api/apiError';
import { useToast } from '../context/useToast';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import TableSkeleton from '../components/common/TableSkeleton';
import Icon from '../components/common/Icon';
import CompanyLogo from '../components/common/CompanyLogo';
import { ErrorMessage } from '../components/common/StateMessage';

const emptyForm = { name: '', website: '', description: '', contactEmail: '', logoUrl: '' };

function CompanyForm({ initialValues, submitting, formError, fieldErrors, onSubmit, onCancel }) {
  const [values, setValues] = useState(initialValues);

  function handleChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.value }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit(values);
  }

  return (
    <form onSubmit={handleSubmit}>
      {formError && <div className="form-error">{formError}</div>}

      <label htmlFor="company-name">
        Name<span className="required-mark">*</span>
      </label>
      <input
        id="company-name"
        value={values.name}
        onChange={handleChange('name')}
        placeholder="e.g. Acme Corp"
        required
      />
      {fieldErrors.name && <div className="field-error">{fieldErrors.name}</div>}

      <label htmlFor="company-website">Website</label>
      <input
        id="company-website"
        value={values.website}
        onChange={handleChange('website')}
        placeholder="https://example.com"
      />
      {fieldErrors.website && <div className="field-error">{fieldErrors.website}</div>}

      <label htmlFor="company-email">Contact Email</label>
      <input
        id="company-email"
        type="email"
        value={values.contactEmail}
        onChange={handleChange('contactEmail')}
        placeholder="hr@example.com"
      />
      {fieldErrors.contactEmail && <div className="field-error">{fieldErrors.contactEmail}</div>}

      <label htmlFor="company-logo-url">Logo URL</label>
      <input
        id="company-logo-url"
        value={values.logoUrl}
        onChange={handleChange('logoUrl')}
        placeholder="https://www.google.com/s2/favicons?domain=example.com&sz=128"
      />
      <div className="field-hint">
        A direct image URL - e.g. a favicon-service link for the company&apos;s own official domain. Leave blank to
        use an auto-generated initials avatar instead.
      </div>
      {fieldErrors.logoUrl && <div className="field-error">{fieldErrors.logoUrl}</div>}

      <label htmlFor="company-description">Description</label>
      <textarea
        id="company-description"
        rows={4}
        value={values.description}
        onChange={handleChange('description')}
        placeholder="Brief note about the company"
      />
      {fieldErrors.description && <div className="field-error">{fieldErrors.description}</div>}

      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save'}
        </button>
      </div>
    </form>
  );
}

export default function Companies() {
  const showToast = useToast();

  const [companies, setCompanies] = useState([]);
  const [drives, setDrives] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [sortDir, setSortDir] = useState('asc');

  const [formModal, setFormModal] = useState(null); // { mode: 'create' | 'edit', company? }
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  const [viewState, setViewState] = useState(null); // { loading, error, company }
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  useEffect(() => {
    loadCompanies();
  }, []);

  function loadCompanies() {
    setLoading(true);
    setError(null);
    Promise.all([getCompanies(), getDrives().catch(() => [])])
      .then(([companyList, driveList]) => {
        setCompanies(companyList);
        setDrives(driveList);
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load companies.')))
      .finally(() => setLoading(false));
  }

  const driveCountByCompany = useMemo(() => {
    const counts = new Map();
    drives.forEach((drive) => {
      counts.set(drive.company.id, (counts.get(drive.company.id) || 0) + 1);
    });
    return counts;
  }, [drives]);

  const filteredCompanies = useMemo(() => {
    const term = search.trim().toLowerCase();
    const filtered = term
      ? companies.filter((c) =>
          [c.name, c.website, c.contactEmail].some((field) => field && field.toLowerCase().includes(term))
        )
      : companies;

    const sorted = [...filtered].sort((a, b) => {
      let result;
      if (sortBy === 'drives') {
        result = (driveCountByCompany.get(a.id) || 0) - (driveCountByCompany.get(b.id) || 0);
      } else {
        result = a.name.localeCompare(b.name);
      }
      return sortDir === 'asc' ? result : -result;
    });
    return sorted;
  }, [companies, search, sortBy, sortDir, driveCountByCompany]);

  function toggleSort(column) {
    if (sortBy === column) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(column);
      setSortDir('asc');
    }
  }

  function openCreate() {
    setFormError(null);
    setFieldErrors({});
    setFormModal({ mode: 'create' });
  }

  function openEdit(company) {
    setFormError(null);
    setFieldErrors({});
    setFormModal({ mode: 'edit', company });
  }

  function closeFormModal() {
    setFormModal(null);
  }

  function handleFormSubmit(values) {
    const request = {
      name: values.name.trim(),
      website: values.website.trim() || null,
      description: values.description.trim() || null,
      contactEmail: values.contactEmail.trim() || null,
      logoUrl: values.logoUrl.trim() || null,
    };

    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});

    const isCreate = formModal.mode === 'create';
    const action = isCreate ? createCompany(request) : updateCompany(formModal.company.id, request);

    action
      .then((saved) => {
        setCompanies((prev) => {
          if (isCreate) return [...prev, saved];
          return prev.map((c) => (c.id === saved.id ? saved : c));
        });
        setFormModal(null);
        showToast(isCreate ? 'Company added successfully.' : 'Company updated successfully.', 'success');
      })
      .catch((err) => {
        setFormError(getErrorMessage(err, 'Could not save company.'));
        setFieldErrors(getFieldErrors(err));
      })
      .finally(() => setSubmitting(false));
  }

  function openView(id) {
    setViewState({ loading: true, error: null, company: null });
    getCompany(id)
      .then((company) => setViewState({ loading: false, error: null, company }))
      .catch((err) =>
        setViewState({ loading: false, error: getErrorMessage(err, 'Could not load company.'), company: null })
      );
  }

  function closeView() {
    setViewState(null);
  }

  function openDeleteConfirm(company) {
    setDeleteError(null);
    setDeleteTarget(company);
  }

  function cancelDelete() {
    setDeleteTarget(null);
    setDeleteError(null);
  }

  function confirmDelete() {
    setDeleting(true);
    setDeleteError(null);
    deleteCompany(deleteTarget.id)
      .then(() => {
        setCompanies((prev) => prev.filter((c) => c.id !== deleteTarget.id));
        setDeleteTarget(null);
        showToast('Company deleted.', 'success');
      })
      .catch((err) => setDeleteError(getErrorMessage(err, 'Could not delete company.')))
      .finally(() => setDeleting(false));
  }

  return (
    <div>
      <PageHeader title="Companies" subtitle="Manage companies participating in campus placements.">
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Icon name="plus" size={16} />
          Add Company
        </button>
      </PageHeader>

      {!loading && !error && companies.length > 0 && (
        <div className="toolbar">
          <div className="search-field">
            <Icon name="search" size={16} />
            <input
              type="text"
              placeholder="Search companies..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search companies"
            />
          </div>
        </div>
      )}

      {loading && <TableSkeleton rows={4} columns={4} />}
      {!loading && error && <ErrorMessage text={error} onRetry={loadCompanies} />}

      {!loading && !error && companies.length === 0 && (
        <EmptyState
          icon="building"
          title="No companies yet"
          description="Add your first company to start managing placement drives."
          actionLabel="Add Company"
          onAction={openCreate}
        />
      )}

      {!loading && !error && companies.length > 0 && filteredCompanies.length === 0 && (
        <EmptyState icon="search" title="No matching companies" description="Try a different search term." compact />
      )}

      {!loading && !error && filteredCompanies.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>
                    <button type="button" className="th-sort-button" onClick={() => toggleSort('name')}>
                      Company
                      {sortBy === 'name' && (
                        <Icon name="chevronDown" size={13} className={sortDir === 'asc' ? 'flip' : ''} />
                      )}
                    </button>
                  </th>
                  <th>Website</th>
                  <th>Contact Email</th>
                  <th>
                    <button type="button" className="th-sort-button" onClick={() => toggleSort('drives')}>
                      Drives
                      {sortBy === 'drives' && (
                        <Icon name="chevronDown" size={13} className={sortDir === 'asc' ? 'flip' : ''} />
                      )}
                    </button>
                  </th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filteredCompanies.map((company) => (
                  <tr key={company.id}>
                    <td>
                      <button type="button" className="link-button company-name-cell" onClick={() => openView(company.id)}>
                        <CompanyLogo name={company.name} logoUrl={company.logoUrl} size={32} />
                        {company.name}
                      </button>
                    </td>
                    <td className="cell-muted">{company.website || '-'}</td>
                    <td className="cell-muted">{company.contactEmail || '-'}</td>
                    <td>{driveCountByCompany.get(company.id) || 0}</td>
                    <td className="table-actions">
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => openView(company.id)}>
                        View
                      </button>
                      <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(company)}>
                        Edit
                      </button>
                      <button type="button" className="btn btn-danger btn-sm" onClick={() => openDeleteConfirm(company)}>
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

      {formModal && (
        <Modal title={formModal.mode === 'create' ? 'Add Company' : 'Edit Company'} onClose={closeFormModal}>
          <CompanyForm
            initialValues={formModal.mode === 'edit' ? { ...emptyForm, ...formModal.company } : emptyForm}
            submitting={submitting}
            formError={formError}
            fieldErrors={fieldErrors}
            onSubmit={handleFormSubmit}
            onCancel={closeFormModal}
          />
        </Modal>
      )}

      {viewState && (
        <Modal title="Company Details" onClose={closeView}>
          {viewState.loading && <div className="skeleton skeleton-text" style={{ height: 80 }} />}
          {!viewState.loading && viewState.error && <ErrorMessage text={viewState.error} />}
          {!viewState.loading && viewState.company && (
            <dl className="detail-list single-column">
              <dt>Name</dt>
              <dd>{viewState.company.name}</dd>
              <dt>Website</dt>
              <dd>{viewState.company.website || '-'}</dd>
              <dt>Contact Email</dt>
              <dd>{viewState.company.contactEmail || '-'}</dd>
              <dt>Description</dt>
              <dd>{viewState.company.description || '-'}</dd>
              <dt>Drives</dt>
              <dd>{driveCountByCompany.get(viewState.company.id) || 0}</dd>
            </dl>
          )}
        </Modal>
      )}

      {deleteTarget && (
        <ConfirmDialog
          title="Delete Company"
          message={`Delete "${deleteTarget.name}"? This cannot be undone.`}
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
