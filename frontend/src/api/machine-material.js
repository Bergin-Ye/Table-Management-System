import request from './request'

export function getList(params) {
  return request.get('/machine-material', { params })
}

export function getDetail(id) {
  return request.get(`/machine-material/${id}`)
}

export function create(data) {
  return request.post('/machine-material', data)
}

export function update(id, data) {
  return request.put(`/machine-material/${id}`, data)
}

export function remove(id) {
  return request.delete(`/machine-material/${id}`)
}

export function batchDelete(ids) {
  return request.post('/machine-material/batch-delete', { ids })
}
