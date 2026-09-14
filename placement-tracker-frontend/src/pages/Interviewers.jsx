import { useEffect, useMemo, useState } from 'react';
import { getInterviewers, createInterviewer } from '../api/adminInterviewerService';
import { getCompanies } from '../api/companyService';
import { getErrorMessage, getFieldErrors } from '../api/apiError';
import { useToast } from '../context/useToast';
import Modal from '../components/common/Modal';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import TableSkeleton from '../components/common/TableSkeleton';
import Icon from '../components/common/Icon';
import CompanyLogo from '../components/common/CompanyLogo';
import { ErrorMessage } from '../components/common/StateMessage';

// Interviewer accounts are deliberately admin-only to create (no self-service signup, the
// same way there's no "create an admin" endpoint) - this is how every drive-posting company
// stays vetted before its interviewer can draft anything. See AuthService.createInterviewer.
const emptyForm = { name: '', email: '', password: '', companyId: '' };

function InterviewerForm({ companies, companiesLoading, submitting, formError, fieldErrors, onSubmit, onCancel }) {
  const [values, setValues] = useState(emptyForm);

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

      <label htmlFor="interviewer-name">
        Name<span className="required-mark">*</span>
      </label>
      <input
        id="interviewer-name"
        value={values.name}
        onChange={handleChange('name')}
        placeholder="e.g. Priya Sharma"
        required
      />
      {fieldErrors.name && <div className="field-error">{fieldErrors.name}</div>}

      <label htmlFor="interviewer-email">
        Email<span className="required-mark">*</span>
      </label>
      <input
        id="interviewer-email"
        type="email"
        value={values.email}
        onChange={handleChange('email')}
        placeholder="priya.sharma@company.com"
        required
      />
      {fieldErrors.email && <div className="field-error">{fieldErrors.email}</div>}

      <label htmlFor="interviewer-password">
        Password<span className="required-mark">*</span>
      </label>
      <input
        id="interviewer-password"
        type="password"
        value={values.password}
        onChange={handleChange('password')}
        placeholder="At least 8 characters"
        minLength={8}
        required
      />
      {fieldErrors.password && <div className="field-error">{fieldErrors.password}</div>}

      <label htmlFor="interviewer-company">
        Company<span className="required-mark">*</span>
      </label>
      {companiesLoading && <div className="field-hint">Loading companies...</div>}
      {!companiesLoading && (
        <select id="interviewer-company" value={values.companyId} onChange={handleChange('companyId')} required>
          <option value="" disabled>
            Select a company
          </option>
          {companies.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      )}
      {fieldErrors.companyId && <div className="field-error">{fieldErrors.companyId}</div>}

      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting || companiesLoading}>
          {submitting ? 'Creating...' : 'Create account'}
        </button>
      </div>
    </form>
  );
}

export default function Interviewers() {
  const showToast = useToast();

  const [interviewers, setInterviewers] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [companiesLoading, setCompaniesLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');

  const [createOpen, setCreateOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    load();
    getCompanies()
      .then(setCompanies)
      .catch(() => setCompanies([]))
      .finally(() => setCompaniesLoading(false));
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getInterviewers()
      .then(setInterviewers)
      .catch((err) => setError(getErrorMessage(err, 'Could not load interviewer accounts.')))
      .finally(() => setLoading(false));
  }

  const filteredInterviewers = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return interviewers;
    return interviewers.filter(
      (i) =>
        i.name.toLowerCase().includes(term) ||
        i.email.toLowerCase().includes(term) ||
        (i.company?.name || '').toLowerCase().includes(term)
    );
  }, [interviewers, search]);

  function openCreate() {
    setFormError(null);
    setFieldErrors({});
    setCreateOpen(true);
  }

  function closeCreate() {
    setCreateOpen(false);
  }

  function handleFormSubmit(values) {
    const request = {
      name: values.name.trim(),
      email: values.email.trim(),
      password: values.password,
      companyId: values.companyId ? Number(values.companyId) : null,
    };

    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});

    createInterviewer(request)
      .then((created) => {
        setInterviewers((prev) => [...prev, created]);
        setCreateOpen(false);
        showToast('Interviewer account created.', 'success');
      })
      .catch((err) => {
        setFormError(getErrorMessage(err, 'Could not create interviewer account.'));
        setFieldErrors(getFieldErrors(err));
      })
      .finally(() => setSubmitting(false));
  }

  return (
    <div>
      <PageHeader title="Interviewers" subtitle="Company representatives who can draft and submit drive postings.">
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          <Icon name="plus" size={16} />
          Add Interviewer
        </button>
      </PageHeader>

      {!loading && !error && interviewers.length > 0 && (
        <div className="toolbar">
          <div className="search-field">
            <Icon name="search" size={16} />
            <input
              type="text"
              placeholder="Search by name, email or company..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search interviewers"
            />
          </div>
        </div>
      )}

      {loading && <TableSkeleton rows={4} columns={3} />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && interviewers.length === 0 && (
        <EmptyState
          icon="user"
          title="No interviewer accounts yet"
          description="Create an account to let a company representative draft and submit drive postings for approval."
          actionLabel="Add Interviewer"
          onAction={openCreate}
        />
      )}

      {!loading && !error && interviewers.length > 0 && filteredInterviewers.length === 0 && (
        <EmptyState icon="search" title="No matching interviewers" description="Try a different search term." compact />
      )}

      {!loading && !error && filteredInterviewers.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Company</th>
                </tr>
              </thead>
              <tbody>
                {filteredInterviewers.map((interviewer) => (
                  <tr key={interviewer.id}>
                    <td className="cell-primary">{interviewer.name}</td>
                    <td className="cell-muted">{interviewer.email}</td>
                    <td>
                      {interviewer.company ? (
                        <span className="company-name-cell">
                          <CompanyLogo name={interviewer.company.name} logoUrl={interviewer.company.logoUrl} size={24} />
                          {interviewer.company.name}
                        </span>
                      ) : (
                        <span className="cell-muted">-</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {createOpen && (
        <Modal title="Add Interviewer" onClose={closeCreate}>
          <InterviewerForm
            companies={companies}
            companiesLoading={companiesLoading}
            submitting={submitting}
            formError={formError}
            fieldErrors={fieldErrors}
            onSubmit={handleFormSubmit}
            onCancel={closeCreate}
          />
        </Modal>
      )}
    </div>
  );
}
