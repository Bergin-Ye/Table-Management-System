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
