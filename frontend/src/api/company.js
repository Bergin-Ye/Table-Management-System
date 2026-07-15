import request from './request'

export function getCompanies() {
  return request.get('/company')
}

export function getCompany(id) {
  return request.get(`/company/${id}`)
}

export function createCompany(data) {
  return request.post('/company', data)
}

export function updateCompany(id, data) {
  return request.put(`/company/${id}`, data)
}

export function deleteCompany(id) {
  return request.delete(`/company/${id}`)
}
