import request from './request'

export function getList(params) {
  return request.get('/machine-count', { params })
}

export function getDetail(id) {
  return request.get(`/machine-count/${id}`)
}

export function create(data) {
  return request.post('/machine-count', data)
}

export function update(id, data) {
  return request.put(`/machine-count/${id}`, data)
}

export function remove(id) {
  return request.delete(`/machine-count/${id}`)
}

export function batchDelete(ids) {
  return request.post('/machine-count/batch-delete', { ids })
}
