import api from './api';

// Backend: AdminDriveController, base path /api/admin/drives
// DriveRequest { companyId*, role*, description, ctc, minCgpa, eligibleBranches, applicationDeadline, driveDate }
// DriveResponse { id, company (CompanyResponse), role, description, ctc, minCgpa, eligibleBranches,
//                 applicationDeadline, driveDate, status (UPCOMING|ONGOING|CLOSED), rounds (RoundResponse[]) }
// DriveStatusUpdateRequest { status }

export function getDrives({ status, companyId } = {}) {
  const params = {};
  if (status) params.status = status;
  if (companyId) params.companyId = companyId;
  return api.get('/api/admin/drives', { params }).then((res) => res.data);
}

export function getDrive(id) {
  return api.get(`/api/admin/drives/${id}`).then((res) => res.data);
}

export function createDrive(request) {
  return api.post('/api/admin/drives', request).then((res) => res.data);
}

export function updateDrive(id, request) {
  return api.put(`/api/admin/drives/${id}`, request).then((res) => res.data);
}

export function updateDriveStatus(id, status) {
  return api.patch(`/api/admin/drives/${id}/status`, { status }).then((res) => res.data);
}

// DriveApprovalUpdateRequest { approvalStatus: 'APPROVED' | 'REJECTED', rejectionReason? }
// Only meaningful for interviewer-drafted drives - admin-created ones are already APPROVED.
export function updateDriveApproval(id, approvalStatus, rejectionReason) {
  return api.patch(`/api/admin/drives/${id}/approval`, { approvalStatus, rejectionReason }).then((res) => res.data);
}

export function deleteDrive(id) {
  return api.delete(`/api/admin/drives/${id}`);
}

// DriveFunnelResponse { driveId, totalApplications, stages: FunnelStageResponse[], offeredCount }
// FunnelStageResponse { roundId, sequence, roundName, reachedCount, passedCount, failedCount, awaitingCount }
export function getDriveFunnel(id) {
  return api.get(`/api/admin/drives/${id}/funnel`).then((res) => res.data);
}
