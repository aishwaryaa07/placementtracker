import api from './api';

// Backend: StudentDriveController, base path /api/student/drives
// StudentDriveResponse { drive: DriveResponse, eligible: boolean }
// findOpenDrives excludes CLOSED drives server-side (DriveService.findOpenDrivesForStudent).

export function getOpenDrives() {
  return api.get('/api/student/drives').then((res) => res.data);
}

export function getOpenDrive(id) {
  return api.get(`/api/student/drives/${id}`).then((res) => res.data);
}

// Returns the created ApplicationResponse (201).
export function applyToDrive(id) {
  return api.post(`/api/student/drives/${id}/apply`).then((res) => res.data);
}
