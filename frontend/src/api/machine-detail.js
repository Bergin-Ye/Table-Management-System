import request from './request'

export function getList(params) {
  return request.get('/machine-detail', { params })
}

export function getDetail(id) {
  return request.get(`/machine-detail/${id}`)
}

export function create(data) {
  return request.post('/machine-detail', data)
}

export function update(id, data) {
  return request.put(`/machine-detail/${id}`, data)
}

export function remove(id) {
  return request.delete(`/machine-detail/${id}`)
}

export function batchDelete(ids) {
  return request.post('/machine-detail/batch-delete', { ids })
}

export function importExcel(file, companyId) {
  const formData = new FormData()
  formData.append('file', file)
  if (companyId) formData.append('companyId', companyId)
  return request.post('/machine-detail/import', formData)
}

export function exportExcel(params) {
  return request.get('/machine-detail/export', {
    params,
    responseType: 'blob'
  })
}

export function downloadTemplate() {
  return request.get('/machine-detail/template', {
    responseType: 'blob'
  })
}
