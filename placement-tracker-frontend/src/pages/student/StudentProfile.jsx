import { useEffect, useState } from 'react';
import { getMyProfileOrNull, upsertMyProfile } from '../../api/studentProfileService';
import { getErrorMessage, getFieldErrors } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import PageHeader from '../../components/common/PageHeader';
import FileUploadField from '../../components/student/FileUploadField';
import { LoadingMessage, ErrorMessage } from '../../components/common/StateMessage';

const COUNTRY_CODES = ['+91', '+1', '+44'];

const emptyForm = {
  branch: '',
  graduationYear: '',
  cgpa: '',
  recentSemesterCgpa: '',
  phoneCountryCode: '+91',
  phoneNumber: '',
  resumeUrl: '',
  tenthMarksheetUrl: '',
  twelfthMarksheetUrl: '',
};

const CURRENT_YEAR = new Date().getFullYear();
const MIN_GRAD_YEAR = CURRENT_YEAR - 10;
const MAX_GRAD_YEAR = CURRENT_YEAR + 10;

// The backend's StudentProfileRequest has no validation annotations on any of
// these fields, so all requiredness/format/range checking has to happen here
// before we submit - the backend itself will accept whatever we send it.
function validateProfile(values) {
  const errors = {};

  if (!values.branch.trim()) {
    errors.branch = 'Branch is required.';
  }

  if (values.graduationYear === '') {
    errors.graduationYear = 'Graduation year is required.';
  } else {
    const year = Number(values.graduationYear);
    if (!Number.isInteger(year)) {
      errors.graduationYear = 'Enter a whole number year, e.g. 2026.';
    } else if (year < MIN_GRAD_YEAR || year > MAX_GRAD_YEAR) {
      errors.graduationYear = `Enter a graduation year between ${MIN_GRAD_YEAR} and ${MAX_GRAD_YEAR}.`;
    }
  }

  const cgpaError = validateCgpa(values.cgpa, 'CGPA');
  if (cgpaError) errors.cgpa = cgpaError;
  const recentSemesterCgpaError = validateCgpa(values.recentSemesterCgpa, 'Most recent semester CGPA');
  if (recentSemesterCgpaError) errors.recentSemesterCgpa = recentSemesterCgpaError;

  if (!/^[0-9]{10}$/.test(values.phoneNumber.trim())) {
    errors.phone = 'Enter exactly 10 digits after the country code.';
  }

  // resumeUrl/tenthMarksheetUrl/twelfthMarksheetUrl are only ever set by a successful upload
  // (see FileUploadField) - never typed by the student - so a presence check is all that's
  // needed here; the URL itself is always well-formed by construction.
  if (!values.resumeUrl) errors.resumeUrl = 'Resume is required.';
  if (!values.tenthMarksheetUrl) errors.tenthMarksheetUrl = '10th marksheet is required.';
  if (!values.twelfthMarksheetUrl) errors.twelfthMarksheetUrl = '12th marksheet is required.';

  return errors;
}

function validateCgpa(rawValue, label) {
  const value = String(rawValue).trim();
  if (value === '') return `${label} is required.`;
  if (!/^\d{1,2}(\.\d)?$/.test(value)) return `${label} can have at most one decimal place, e.g. 8.5.`;
  const cgpa = Number(value);
  if (cgpa < 1 || cgpa > 10) return `${label} must be between 1.0 and 10.0.`;
  return null;
}

// Splits a stored "phone" string (a single free-form field on the backend)
// back into a country code + 10-digit number for editing. A number saved
// before this validation existed (or with an unrecognized code) still shows
// up - defaulted to +91 - so the student can see and fix it.
function splitPhone(phone) {
  if (!phone) return { phoneCountryCode: '+91', phoneNumber: '' };
  const code = COUNTRY_CODES.find((c) => phone.startsWith(c));
  if (code) return { phoneCountryCode: code, phoneNumber: phone.slice(code.length) };
  return { phoneCountryCode: '+91', phoneNumber: phone.replace(/\D/g, '').slice(-10) };
}

function profileToFormValues(profile) {
  if (!profile) return emptyForm;
  const { phoneCountryCode, phoneNumber } = splitPhone(profile.phone);
  return {
    branch: profile.branch || '',
    graduationYear: profile.graduationYear ?? '',
    cgpa: profile.cgpa ?? '',
    recentSemesterCgpa: profile.recentSemesterCgpa ?? '',
    phoneCountryCode,
    phoneNumber,
    resumeUrl: profile.resumeUrl || '',
    tenthMarksheetUrl: profile.tenthMarksheetUrl || '',
    twelfthMarksheetUrl: profile.twelfthMarksheetUrl || '',
  };
}

export default function StudentProfile() {
  const showToast = useToast();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [profile, setProfile] = useState(null);
  const [values, setValues] = useState(emptyForm);

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getMyProfileOrNull()
      .then((data) => {
        setProfile(data);
        setValues(profileToFormValues(data));
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load your profile.')))
      .finally(() => setLoading(false));
  }

  function handleChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.value }));
  }

  function handlePhoneNumberChange(e) {
    const digitsOnly = e.target.value.replace(/\D/g, '').slice(0, 10);
    setValues((prev) => ({ ...prev, phoneNumber: digitsOnly }));
  }

  function handleSubmit(e) {
    e.preventDefault();

    const validationErrors = validateProfile(values);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      setFormError('Please fix the highlighted fields.');
      return;
    }

    const request = {
      branch: values.branch.trim(),
      graduationYear: Number(values.graduationYear),
      cgpa: Number(values.cgpa),
      recentSemesterCgpa: Number(values.recentSemesterCgpa),
      phone: `${values.phoneCountryCode}${values.phoneNumber.trim()}`,
      resumeUrl: values.resumeUrl,
      tenthMarksheetUrl: values.tenthMarksheetUrl,
      twelfthMarksheetUrl: values.twelfthMarksheetUrl,
    };

    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});
    upsertMyProfile(request)
      .then((saved) => {
        setProfile(saved);
        setValues(profileToFormValues(saved));
        showToast('Profile saved.', 'success');
      })
      .catch((err) => {
        setFormError(getErrorMessage(err, 'Could not save your profile.'));
        setFieldErrors(getFieldErrors(err));
      })
      .finally(() => setSubmitting(false));
  }

  if (loading) return <LoadingMessage text="Loading your profile..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;

  return (
    <div>
      <PageHeader
        title="My Profile"
        subtitle={
          profile
            ? 'Keep your details up to date so eligible drives can find you.'
            : 'Complete your profile to become eligible for placement drives.'
        }
      />

      <div className="form-card">
        <form onSubmit={handleSubmit} noValidate>
          {formError && <div className="form-error">{formError}</div>}

          {profile && (
            <>
              <label>Name</label>
              <input value={profile.name} disabled />
              <label>Email</label>
              <input value={profile.email} disabled />
            </>
          )}

          <label htmlFor="profile-branch">
            Branch<span className="required-mark">*</span>
          </label>
          <input
            id="profile-branch"
            value={values.branch}
            onChange={handleChange('branch')}
            placeholder="e.g. CSE"
          />
          {fieldErrors.branch && <div className="field-error">{fieldErrors.branch}</div>}

          <div className="form-row">
            <div>
              <label htmlFor="profile-grad-year">
                Graduation Year<span className="required-mark">*</span>
              </label>
              <input
                id="profile-grad-year"
                type="number"
                inputMode="numeric"
                step="1"
                min={MIN_GRAD_YEAR}
                max={MAX_GRAD_YEAR}
                value={values.graduationYear}
                onChange={handleChange('graduationYear')}
                placeholder={String(CURRENT_YEAR)}
              />
              {fieldErrors.graduationYear && <div className="field-error">{fieldErrors.graduationYear}</div>}
            </div>
            <div>
              <label htmlFor="profile-cgpa">
                CGPA<span className="required-mark">*</span>
              </label>
              <input
                id="profile-cgpa"
                type="number"
                inputMode="decimal"
                step="0.1"
                min="1"
                max="10"
                value={values.cgpa}
                onChange={handleChange('cgpa')}
                placeholder="e.g. 8.5"
              />
              {fieldErrors.cgpa && <div className="field-error">{fieldErrors.cgpa}</div>}
            </div>
          </div>

          <label htmlFor="profile-recent-cgpa">
            Most Recent Semester CGPA<span className="required-mark">*</span>
          </label>
          <input
            id="profile-recent-cgpa"
            type="number"
            inputMode="decimal"
            step="0.1"
            min="1"
            max="10"
            value={values.recentSemesterCgpa}
            onChange={handleChange('recentSemesterCgpa')}
            placeholder="e.g. 8.5"
          />
          {fieldErrors.recentSemesterCgpa && <div className="field-error">{fieldErrors.recentSemesterCgpa}</div>}

          <label htmlFor="profile-phone">
            Phone<span className="required-mark">*</span>
          </label>
          <div className="phone-input-group">
            <select
              id="profile-phone-code"
              aria-label="Country code"
              value={values.phoneCountryCode}
              onChange={handleChange('phoneCountryCode')}
            >
              {COUNTRY_CODES.map((code) => (
                <option key={code} value={code}>
                  {code}
                </option>
              ))}
            </select>
            <input
              id="profile-phone"
              type="tel"
              inputMode="numeric"
              value={values.phoneNumber}
              onChange={handlePhoneNumberChange}
              placeholder="9876543210"
              maxLength={10}
            />
          </div>
          {fieldErrors.phone && <div className="field-error">{fieldErrors.phone}</div>}

          <FileUploadField
            id="profile-resume"
            label="Resume"
            documentType="RESUME"
            value={values.resumeUrl}
            onChange={(url) => setValues((prev) => ({ ...prev, resumeUrl: url }))}
            error={fieldErrors.resumeUrl}
          />

          <FileUploadField
            id="profile-tenth-marksheet"
            label="10th Marksheet"
            documentType="TENTH_MARKSHEET"
            value={values.tenthMarksheetUrl}
            onChange={(url) => setValues((prev) => ({ ...prev, tenthMarksheetUrl: url }))}
            error={fieldErrors.tenthMarksheetUrl}
          />

          <FileUploadField
            id="profile-twelfth-marksheet"
            label="12th Marksheet"
            documentType="TWELFTH_MARKSHEET"
            value={values.twelfthMarksheetUrl}
            onChange={(url) => setValues((prev) => ({ ...prev, twelfthMarksheetUrl: url }))}
            error={fieldErrors.twelfthMarksheetUrl}
          />

          <div className="modal-actions">
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Profile'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
