import { useState } from 'react';
import { uploadProfileDocument } from '../../api/studentProfileService';
import { getErrorMessage } from '../../api/apiError';

// Uploads immediately on file selection (not deferred to the surrounding form's submit) -
// onChange only fires once the file is actually saved server-side and a real URL comes back,
// so the parent form never has a "file" value, only ever a URL, same shape as before this
// field existed (a pasted URL).
export default function FileUploadField({ id, label, value, onChange, documentType, error }) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);

  function handleFileSelected(e) {
    const file = e.target.files?.[0];
    e.target.value = ''; // let the same file be re-selected later (e.g. after an error)
    if (!file) return;

    setUploading(true);
    setUploadError(null);
    uploadProfileDocument(file, documentType)
      .then((url) => onChange(url))
      .catch((err) => setUploadError(getErrorMessage(err, 'Could not upload this file.')))
      .finally(() => setUploading(false));
  }

  return (
    <div>
      <label htmlFor={id}>
        {label}
        <span className="required-mark">*</span>
      </label>
      <input
        id={id}
        type="file"
        accept="application/pdf,image/jpeg,image/png"
        onChange={handleFileSelected}
        disabled={uploading}
      />
      <div className="field-hint">
        {uploading
          ? 'Uploading...'
          : value
            ? (
              <a href={value} target="_blank" rel="noreferrer" className="link-button">
                View uploaded file
              </a>
            )
            : 'PDF, JPG, or PNG, up to 5MB.'}
      </div>
      {(uploadError || error) && <div className="field-error">{uploadError || error}</div>}
    </div>
  );
}
