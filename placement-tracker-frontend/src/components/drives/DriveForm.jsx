import { useEffect, useState } from 'react';
import { getCompanies } from '../../api/companyService';
import { getErrorMessage } from '../../api/apiError';

function Field({ label, required, error, className = '', children }) {
  return (
    <div className={className}>
      <label className="text-[13px] font-medium text-black">
        {label}
        {required && <span className="text-[#B23B3B]"> *</span>}
      </label>
      <div className="mt-1">{children}</div>
      {error && <p className="text-[12px] text-[#B23B3B] mt-1">{error}</p>}
    </div>
  );
}

function inputClass(error) {
  return `w-full border rounded-lg px-3 py-2 text-[14px] focus:outline-none focus:ring-2 focus:ring-[#7c5cfc]/30 ${
    error ? 'border-[#E38B8B]' : 'border-[#c9bff5]'
  }`;
}

export default function DriveForm({ initialValues, submitting, formError, fieldErrors, onSubmit, onCancel }) {
  const [values, setValues] = useState(initialValues);
  const [companies, setCompanies] = useState([]);
  const [companiesLoading, setCompaniesLoading] = useState(true);
  const [companiesError, setCompaniesError] = useState(null);

  useEffect(() => {
    getCompanies()
      .then(setCompanies)
      .catch((err) => setCompaniesError(getErrorMessage(err, 'Could not load companies.')))
      .finally(() => setCompaniesLoading(false));
  }, []);

  function handleChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.value }));
  }

  function handleCheckboxChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.checked }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit(values);
  }

  return (
    <form onSubmit={handleSubmit}>
      {formError && <p className="text-[13px] text-[#B23B3B] mb-3">{formError}</p>}

      <div className="grid grid-cols-2 gap-4">
        <Field label="Company" required error={fieldErrors.companyId} className="col-span-2">
          {companiesLoading && <p className="text-[13px] text-[#6b6785]">Loading companies...</p>}
          {companiesError && <p className="text-[13px] text-[#B23B3B]">{companiesError}</p>}
          {!companiesLoading && !companiesError && (
            <select
              id="drive-company"
              value={values.companyId}
              onChange={handleChange('companyId')}
              required
              className={inputClass(fieldErrors.companyId)}
            >
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
        </Field>

        <Field label="Role" required error={fieldErrors.role} className="col-span-2">
          <input
            id="drive-role"
            value={values.role}
            onChange={handleChange('role')}
            placeholder="e.g. Software Engineer"
            required
            className={inputClass(fieldErrors.role)}
          />
        </Field>

        <Field label="CTC" error={fieldErrors.ctc}>
          <input
            id="drive-ctc"
            type="number"
            step="0.01"
            min="0"
            value={values.ctc}
            onChange={handleChange('ctc')}
            className={inputClass(fieldErrors.ctc)}
          />
        </Field>

        <Field label="Min CGPA" error={fieldErrors.minCgpa}>
          <input
            id="drive-min-cgpa"
            type="number"
            step="0.1"
            min="0"
            max="10"
            value={values.minCgpa}
            onChange={handleChange('minCgpa')}
            className={inputClass(fieldErrors.minCgpa)}
          />
        </Field>

        <Field label="Minimum qualification" error={fieldErrors.minQualification}>
          <select
            id="drive-min-qualification"
            value={values.minQualification}
            onChange={handleChange('minQualification')}
            className={inputClass(fieldErrors.minQualification)}
          >
            <option value="EITHER">UG or PG</option>
            <option value="UG">UG only</option>
            <option value="PG">PG only</option>
          </select>
        </Field>

        <Field label="Freshers only">
          <label className="flex items-center gap-2 mt-2 text-[13px] text-black">
            <input
              type="checkbox"
              checked={values.freshersOnly}
              onChange={handleCheckboxChange('freshersOnly')}
              className="h-4 w-4 rounded border-[#c9bff5] text-[#7c5cfc] focus:ring-[#7c5cfc]/30"
            />
            This drive is for freshers only
          </label>
        </Field>
      </div>

      <Field label="Description" error={fieldErrors.description} className="mt-4">
        <textarea
          id="drive-description"
          rows={3}
          value={values.description}
          onChange={handleChange('description')}
          placeholder="Role responsibilities, what the team works on..."
          className={inputClass(fieldErrors.description)}
        />
      </Field>

      <Field
        label="Eligible branches (comma-separated, leave blank for all)"
        error={fieldErrors.eligibleBranches}
        className="mt-4"
      >
        <input
          id="drive-branches"
          value={values.eligibleBranches}
          onChange={handleChange('eligibleBranches')}
          placeholder="CSE, ECE, ME"
          className={inputClass(fieldErrors.eligibleBranches)}
        />
      </Field>

      <div className="grid grid-cols-2 gap-4 mt-4">
        <Field label="Application deadline" error={fieldErrors.applicationDeadline}>
          <input
            id="drive-deadline"
            type="date"
            value={values.applicationDeadline}
            onChange={handleChange('applicationDeadline')}
            className={inputClass(fieldErrors.applicationDeadline)}
          />
        </Field>
        <Field label="Drive date" error={fieldErrors.driveDate}>
          <input
            id="drive-date"
            type="date"
            value={values.driveDate}
            onChange={handleChange('driveDate')}
            className={inputClass(fieldErrors.driveDate)}
          />
        </Field>
      </div>

      <div className="flex gap-3 mt-6">
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          className="flex-1 text-[13px] font-medium border border-[#c9bff5] rounded-lg py-2.5 hover:bg-[#f6f3ff] transition disabled:opacity-60"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting || companiesLoading}
          className="flex-1 bg-[#7c5cfc] text-white text-[13px] font-medium rounded-lg py-2.5 hover:bg-[#6d28d9] transition disabled:opacity-60"
        >
          {submitting ? 'Saving...' : 'Save'}
        </button>
      </div>
    </form>
  );
}
