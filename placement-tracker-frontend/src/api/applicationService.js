import api from './api';
import { getDrives } from './driveService';

// Backend: AdminApplicationController
// ApplicationResponse { id, studentName, studentEmail, studentBranch, drive (DriveResponse),
//                        appliedAt, status, roundResults, offer }

export function getApplicationsByDrive(driveId) {
  return api.get(`/api/admin/drives/${driveId}/applications`).then((res) => res.data);
}

export function getApplication(id) {
  return api.get(`/api/admin/applications/${id}`).then((res) => res.data);
}

// There is no "list all applications" endpoint on the backend, only per-drive.
// This aggregates across every drive so the Applications page and Dashboard can
// show a combined view built entirely from real API responses.
export function getAllApplications() {
  return getDrives().then((drives) =>
    Promise.all(drives.map((drive) => getApplicationsByDrive(drive.id))).then((lists) => lists.flat())
  );
}
