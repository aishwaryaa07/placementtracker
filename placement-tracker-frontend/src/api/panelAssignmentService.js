import api from './api';

// Backend: AdminInterviewerController, AdminPanelAssignmentController (admin-only)

export function listInterviewers() {
  return api.get('/api/admin/interviewers').then((res) => res.data);
}

export function createInterviewer(name, email, password) {
  return api.post('/api/admin/interviewers', { name, email, password }).then((res) => res.data);
}

export function getPanelAssignments(roundId) {
  return api.get(`/api/admin/rounds/${roundId}/panel-assignments`).then((res) => res.data);
}

export function getCandidatePool(roundId) {
  return api.get(`/api/admin/rounds/${roundId}/candidates`).then((res) => res.data);
}

export function assignPanel(roundId, interviewerId, roundResultIds) {
  return api.post(`/api/admin/rounds/${roundId}/panel-assignments`, { interviewerId, roundResultIds }).then((res) => res.data);
}

export function unassignPanel(assignmentId) {
  return api.delete(`/api/admin/panel-assignments/${assignmentId}`);
}
