import api from './api';

// Backend: StudentApplicationController, base path /api/student/applications
// ApplicationResponse { id, drive (DriveResponse), appliedAt, status, roundResults, offer }
// There is no separate "offers" list endpoint - an application's offer (if any) is
// embedded in ApplicationResponse.offer, so the Offers page is derived from this list.

export function getMyApplications() {
  return api.get('/api/student/applications').then((res) => res.data);
}

export function getMyApplication(id) {
  return api.get(`/api/student/applications/${id}`).then((res) => res.data);
}
