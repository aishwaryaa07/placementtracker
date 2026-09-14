import api from './api';

// Backend: AdminRoundScoreImportController
// GET /api/admin/rounds/{roundId}/score-template -> CSV download (roundResultId is the only
// column read back on import - an existing PK, never a name/email match)
// POST /api/admin/rounds/{roundId}/score-import/preview -> RoundScoreImportPreviewResponse
// POST /api/admin/rounds/{roundId}/score-import/confirm -> RoundScoreImportResultResponse

export function downloadScoreTemplate(roundId) {
  return api.get(`/api/admin/rounds/${roundId}/score-template`, { responseType: 'blob' }).then((res) => {
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `round-${roundId}-scores.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  });
}

export function previewScoreImport(roundId, file) {
  const formData = new FormData();
  formData.append('file', file);
  return api
    .post(`/api/admin/rounds/${roundId}/score-import/preview`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((res) => res.data);
}

export function confirmScoreImport(roundId, rows) {
  return api.post(`/api/admin/rounds/${roundId}/score-import/confirm`, { rows }).then((res) => res.data);
}
