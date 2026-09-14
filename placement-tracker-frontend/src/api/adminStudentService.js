import api from './api';

// Backend: AdminStudentController
// StudentProfileResponse { id, name, email, branch, graduationYear, cgpa, phone, resumeUrl }
// Only students who've completed a profile show up here.

export function getAllStudents() {
  return api.get('/api/admin/students').then((res) => res.data);
}
