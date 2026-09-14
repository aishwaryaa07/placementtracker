import api from './api';

// Backend: AdminCompanyController, base path /api/admin/companies
// CompanyRequest { name*, website, description, contactEmail }
// CompanyResponse { id, name, website, description, contactEmail }

export function getCompanies() {
  return api.get('/api/admin/companies').then((res) => res.data);
}

export function getCompany(id) {
  return api.get(`/api/admin/companies/${id}`).then((res) => res.data);
}

export function createCompany(request) {
  return api.post('/api/admin/companies', request).then((res) => res.data);
}

export function updateCompany(id, request) {
  return api.put(`/api/admin/companies/${id}`, request).then((res) => res.data);
}

export function deleteCompany(id) {
  return api.delete(`/api/admin/companies/${id}`);
}
