import { useState } from 'react';
import { useParams, useLocation, Link } from 'react-router-dom';
import { downloadScoreTemplate, previewScoreImport, confirmScoreImport } from '../api/roundScoreImportService';
import { getErrorMessage } from '../api/apiError';
import { useToast } from '../context/useToast';
import ConfirmDialog from '../components/common/ConfirmDialog';
import Icon from '../components/common/Icon';
import { ErrorMessage } from '../components/common/StateMessage';

export default function RoundScoreImport() {
  const { roundId } = useParams();
  const location = useLocation();
  const showToast = useToast();
  const roundName = location.state?.roundName;
  const driveId = location.state?.driveId;

  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState(null);

  const [file, setFile] = useState(null);
  const [previewing, setPreviewing] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const [preview, setPreview] = useState(null);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [confirmError, setConfirmError] = useState(null);
  const [result, setResult] = useState(null);

  function handleDownload() {
    setDownloading(true);
    setDownloadError(null);
    downloadScoreTemplate(roundId)
      .catch((err) => setDownloadError(getErrorMessage(err, 'Could not download the template.')))
      .finally(() => setDownloading(false));
  }

  function handlePreview(e) {
    e.preventDefault();
    if (!file) return;
    setPreviewing(true);
    setPreviewError(null);
    setResult(null);
    previewScoreImport(roundId, file)
      .then(setPreview)
      .catch((err) => setPreviewError(getErrorMessage(err, 'Could not preview the file.')))
      .finally(() => setPreviewing(false));
  }

  function handleConfirm() {
    setConfirming(true);
    setConfirmError(null);
    const rows = preview.rows
      .filter((r) => !r.rowError)
      .map((r) => ({ roundResultId: r.roundResultId, score: r.newScore }));
    confirmScoreImport(roundId, rows)
      .then((res) => {
        setConfirmOpen(false);
        setResult(res);
        setPreview(null);
        setFile(null);
        showToast(`Import committed: ${res.committedCount} scored, ${res.skippedCount} skipped.`, 'success');
      })
      .catch((err) => setConfirmError(getErrorMessage(err, 'Could not commit the import.')))
      .finally(() => setConfirming(false));
  }

  return (
    <div>
      <div className="page-header">
        <div>
          {driveId && (
            <Link to={`/drives/${driveId}`} className="back-link">
              <Icon name="arrowLeft" size={14} />
              Back to Drive
            </Link>
          )}
          <h1>Bulk Score Import{roundName ? ` — ${roundName}` : ''}</h1>
        </div>
      </div>

      <div className="detail-card">
        <h2 className="section-heading" style={{ marginTop: 0 }}>
          1. Download the template
        </h2>
        <p className="cell-muted">
          Lists every candidate currently reachable at this round with a blank score column. Fill in only the
          score column offline, then upload it below.
        </p>
        {downloadError && <ErrorMessage text={downloadError} />}
        <button type="button" className="btn btn-secondary" onClick={handleDownload} disabled={downloading}>
          <Icon name="download" size={14} />
          {downloading ? 'Downloading...' : 'Download Template'}
        </button>
      </div>

      <div className="detail-card">
        <h2 className="section-heading" style={{ marginTop: 0 }}>
          2. Upload &amp; preview
        </h2>
        <form onSubmit={handlePreview}>
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
            disabled={previewing}
          />
          {previewError && <div className="field-error">{previewError}</div>}
          <div className="modal-actions" style={{ justifyContent: 'flex-start', marginTop: 12 }}>
            <button type="submit" className="btn btn-primary" disabled={!file || previewing}>
              <Icon name="upload" size={14} />
              {previewing ? 'Reading...' : 'Preview'}
            </button>
          </div>
        </form>
      </div>

      {preview && (
        <div className="detail-card">
          <h2 className="section-heading" style={{ marginTop: 0 }}>
            3. Review &amp; confirm
          </h2>
          <p className="cell-muted">
            {preview.validCount} of {preview.totalDataRows} row(s) are ready to import
            {preview.errorCount > 0 ? `; ${preview.errorCount} have errors and will be skipped.` : '.'}
          </p>

          <div className="table-card">
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Round Result ID</th>
                    <th>Student</th>
                    <th>Existing</th>
                    <th>New Score</th>
                    <th>Predicted</th>
                    <th>Error</th>
                  </tr>
                </thead>
                <tbody>
                  {preview.rows.map((row) => (
                    <tr key={row.roundResultId} style={row.rowError ? { opacity: 0.6 } : undefined}>
                      <td>{row.roundResultId}</td>
                      <td>
                        {row.studentName || '-'}
                        {row.studentEmail && <div className="cell-muted">{row.studentEmail}</div>}
                      </td>
                      <td>
                        {row.existingScore ?? '-'} ({row.existingStatus || '-'})
                      </td>
                      <td>{row.newScore}</td>
                      <td>{row.predictedStatus || '-'}</td>
                      <td className="wrap cell-muted">{row.rowError || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="modal-actions" style={{ justifyContent: 'flex-start', marginTop: 12 }}>
            <button
              type="button"
              className="btn btn-primary"
              disabled={preview.validCount === 0}
              onClick={() => {
                setConfirmError(null);
                setConfirmOpen(true);
              }}
            >
              Confirm Import ({preview.validCount})
            </button>
          </div>
        </div>
      )}

      {result && (
        <div className="detail-card">
          <h2 className="section-heading" style={{ marginTop: 0 }}>
            Import complete
          </h2>
          <p>
            {result.committedCount} row(s) committed, {result.skippedCount} skipped.
          </p>
          {result.skipped.length > 0 && (
            <ul>
              {result.skipped.map((s) => (
                <li key={s.roundResultId}>
                  Round Result {s.roundResultId}: {s.reason}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {confirmOpen && (
        <ConfirmDialog
          title="Confirm Score Import"
          message={`Commit ${preview.validCount} score(s)? Each row is re-validated and graded through the same rules as manual grading. This cannot be undone.`}
          confirmLabel="Confirm Import"
          tone="primary"
          busy={confirming}
          error={confirmError}
          onConfirm={handleConfirm}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </div>
  );
}
