import request from './request'

export function getList(params) {
  return request.get('/delivery-stats', { params })
}

export function getDetail(id) {
  return request.get(`/delivery-stats/${id}`)
}

export function getDailies(id) {
  return request.get(`/delivery-stats/${id}/dailies`)
}

export function create(data) {
  return request.post('/delivery-stats', data)
}

export function update(id, data) {
  return request.put(`/delivery-stats/${id}`, data)
}

export function remove(id) {
  return request.delete(`/delivery-stats/${id}`)
}

export function batchDelete(ids) {
  return request.post('/delivery-stats/batch-delete', { ids })
}

export function importExcel(file, companyId) {
  const formData = new FormData()
  formData.append('file', file)
  if (companyId) formData.append('companyId', companyId)
  return request.post('/delivery-stats/import', formData)
}

export function exportExcel(params) {
  return request.get('/delivery-stats/export', {
    params,
    responseType: 'blob'
  })
}

export function batchRefresh(yearMonth) {
  return request.post('/delivery-stats/batch-refresh', { yearMonth, statMonth: yearMonth })
}

export function autoFill(materialCode, statDate) {
  return request.get('/delivery-stats/auto-fill', { params: { materialCode, statDate } })
}

export function downloadTemplate() {
  return request.get('/delivery-stats/template', {
    responseType: 'blob'
  })
}
