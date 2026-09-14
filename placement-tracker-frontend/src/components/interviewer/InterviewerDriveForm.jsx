import { useState } from 'react';

// Same field set as the admin DriveForm minus companyId - the interviewer's company is
// implicit (resolved server-side from their account), never picked here.
export default function InterviewerDriveForm({ initialValues, submitting, formError, fieldErrors, onSubmit, onCancel }) {
  const [values, setValues] = useState(initialValues);

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
      {formError && <div className="form-error">{formError}</div>}

      <label htmlFor="idrive-role">
        Role<span className="required-mark">*</span>
      </label>
      <input
        id="idrive-role"
        value={values.role}
        onChange={handleChange('role')}
        placeholder="e.g. Software Engineer"
        required
      />
      {fieldErrors.role && <div className="field-error">{fieldErrors.role}</div>}

      <label htmlFor="idrive-description">Description</label>
      <textarea
        id="idrive-description"
        rows={3}
        value={values.description}
        onChange={handleChange('description')}
        placeholder="Role responsibilities, what the team works on..."
      />
      {fieldErrors.description && <div className="field-error">{fieldErrors.description}</div>}

      <div className="form-row">
        <div>
          <label htmlFor="idrive-ctc">CTC</label>
          <input id="idrive-ctc" type="number" step="0.01" min="0" value={values.ctc} onChange={handleChange('ctc')} />
          {fieldErrors.ctc && <div className="field-error">{fieldErrors.ctc}</div>}
        </div>
        <div>
          <label htmlFor="idrive-min-cgpa">Min CGPA</label>
          <input
            id="idrive-min-cgpa"
            type="number"
            step="0.1"
            min="0"
            max="10"
            value={values.minCgpa}
            onChange={handleChange('minCgpa')}
          />
          {fieldErrors.minCgpa && <div className="field-error">{fieldErrors.minCgpa}</div>}
        </div>
      </div>

      <label htmlFor="idrive-branches">Eligible Branches (comma-separated, leave blank for all)</label>
      <input
        id="idrive-branches"
        value={values.eligibleBranches}
        onChange={handleChange('eligibleBranches')}
        placeholder="CSE, ECE, ME"
      />
      {fieldErrors.eligibleBranches && <div className="field-error">{fieldErrors.eligibleBranches}</div>}

      <div className="form-row">
        <div>
          <label htmlFor="idrive-deadline">Application Deadline</label>
          <input
            id="idrive-deadline"
            type="date"
            value={values.applicationDeadline}
            onChange={handleChange('applicationDeadline')}
          />
          {fieldErrors.applicationDeadline && <div className="field-error">{fieldErrors.applicationDeadline}</div>}
        </div>
        <div>
          <label htmlFor="idrive-date">Drive Date</label>
          <input id="idrive-date" type="date" value={values.driveDate} onChange={handleChange('driveDate')} />
          {fieldErrors.driveDate && <div className="field-error">{fieldErrors.driveDate}</div>}
        </div>
      </div>

      <div className="form-row">
        <div>
          <label htmlFor="idrive-min-qualification">Minimum Qualification</label>
          <select id="idrive-min-qualification" value={values.minQualification} onChange={handleChange('minQualification')}>
            <option value="EITHER">UG or PG</option>
            <option value="UG">UG only</option>
            <option value="PG">PG only</option>
          </select>
        </div>
        <div>
          <label htmlFor="idrive-freshers-only">Freshers Only</label>
          <label
            htmlFor="idrive-freshers-only"
            style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 6, fontWeight: 400 }}
          >
            <input
              id="idrive-freshers-only"
              type="checkbox"
              checked={values.freshersOnly}
              onChange={handleCheckboxChange('freshersOnly')}
              style={{ width: 'auto' }}
            />
            This drive is for freshers only
          </label>
        </div>
      </div>

      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save Draft'}
        </button>
      </div>
    </form>
  );
}
