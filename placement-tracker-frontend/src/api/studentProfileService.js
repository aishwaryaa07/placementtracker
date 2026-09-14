import api from './api';

// Backend: StudentProfileController, base path /api/student/profile
// StudentProfileRequest { branch, graduationYear, cgpa, phone, resumeUrl, tenthMarksheetUrl,
//                          twelfthMarksheetUrl, recentSemesterCgpa } (all mandatory)
// StudentProfileResponse mirrors the request, plus { id, name, email }

export function getMyProfile() {
  return api.get('/api/student/profile').then((res) => res.data);
}

// Returns null instead of throwing when no profile has been created yet (404 from
// StudentProfileService.getMyProfile - "No profile found yet - create one with PUT ...").
export function getMyProfileOrNull() {
  return getMyProfile().catch((err) => {
    if (err.response?.status === 404) return null;
    throw err;
  });
}

export function upsertMyProfile(request) {
  return api.put('/api/student/profile', request).then((res) => res.data);
}

// documentType: 'RESUME' | 'TENTH_MARKSHEET' | 'TWELFTH_MARKSHEET' (FileStorageService.DocumentType)
// The shared `api` instance defaults Content-Type to application/json for every request
// (see api.js) - explicitly clearing it (not just omitting it) is required here so axios's
// browser adapter falls back to its own FormData handling, which sets
// "multipart/form-data; boundary=..." with the boundary the browser actually used to encode
// the body. Without this, the request would carry the wrong Content-Type and the backend's
// multipart parser would fail to read it.
export function uploadProfileDocument(file, documentType) {
  const formData = new FormData();
  formData.append('file', file);
  return api
    .post('/api/student/profile/documents', formData, {
      params: { type: documentType },
      headers: { 'Content-Type': undefined },
    })
    .then((res) => res.data.url);
}
