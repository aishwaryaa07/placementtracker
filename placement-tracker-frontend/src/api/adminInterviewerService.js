import api from './api';

// Backend: AdminInterviewerController
// InterviewerResponse { id, name, email, company (CompanyResponse | null) }
// POST body: RegisterRequest { name, email, password, companyId } - companyId required for
// this path (AuthService.createUser throws ResourceNotFoundException if missing for a role
// INTERVIEWER account), even though RegisterRequest is shared with public student self-register.

export function getInterviewers(companyId) {
  return api.get('/api/admin/interviewers', { params: companyId ? { companyId } : undefined }).then((res) => res.data);
}

export function createInterviewer(request) {
  return api.post('/api/admin/interviewers', request).then((res) => res.data);
}
