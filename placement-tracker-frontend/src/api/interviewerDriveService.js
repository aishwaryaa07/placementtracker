import api from './api';

// Backend: InterviewerDriveController (/api/interviewer/drives)
// InterviewerDriveRequest { role, description, ctc, minCgpa, eligibleBranches,
//                            applicationDeadline, driveDate, minQualification, freshersOnly }
// - no companyId: the interviewer's own company is resolved server-side from the JWT
// DriveResponse gains approvalStatus/createdById/createdByName/approvedById/approvedByName/
// approvedAt/rejectionReason/minQualification/freshersOnly on top of the admin-side fields.
//
// Visibility/ownership enforced server-side: any interviewer at the same company can see
// every drive there, but only the original author (createdById) can edit/submit/delete one -
// this service doesn't need to re-check that client-side, the backend 403s otherwise.

export function getMyCompanyDrives() {
  return api.get('/api/interviewer/drives').then((res) => res.data);
}

export function getDrive(id) {
  return api.get(`/api/interviewer/drives/${id}`).then((res) => res.data);
}

export function createDrive(request) {
  return api.post('/api/interviewer/drives', request).then((res) => res.data);
}

export function updateDrive(id, request) {
  return api.put(`/api/interviewer/drives/${id}`, request).then((res) => res.data);
}

export function submitForApproval(id) {
  return api.patch(`/api/interviewer/drives/${id}/submit`).then((res) => res.data);
}

export function deleteDrive(id) {
  return api.delete(`/api/interviewer/drives/${id}`).then((res) => res.data);
}
